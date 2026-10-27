import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import clsx from "clsx";
import { Clock, Droplet, FlaskConical, Search, SearchX } from "lucide-react";
import { api, query } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useToast } from "../../components/Toast";
import { Button, EmptyState, ErrorNote, Field, Modal, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { addDays, isoDate, money } from "../../lib/format";

const SORTS = {
  popular: () => 0,
  "price-asc": (a, b) => a.price - b.price,
  "price-desc": (a, b) => b.price - a.price,
};

export default function LabTests() {
  const [category, setCategory] = useState("");
  const [text, setText] = useState("");
  const [sort, setSort] = useState("popular");
  const [selected, setSelected] = useState(null);
  const { data: categories } = useApi("/api/lab-tests/categories");
  const { data: tests, loading } = useApi("/api/lab-tests" + query({ category }));

  const visible = useMemo(() => {
    const q = text.trim().toLowerCase();
    return (tests || [])
      .filter((t) => !q || t.name.toLowerCase().includes(q) || t.description?.toLowerCase().includes(q))
      .sort(SORTS[sort]);
  }, [tests, text, sort]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <div className="max-w-2xl">
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Lab tests</h1>
        <p className="mt-2 text-slate-500">
          Routine pathology and advanced genetic testing from our partner labs. Reports appear in your account, and
          diabetes results feed straight into your AI risk check.
        </p>
      </div>

      <div className="mt-8 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap gap-2">
          {["", ...(categories || [])].map((c) => (
            <button
              key={c || "all"}
              onClick={() => setCategory(c)}
              className={clsx(
                "rounded-full px-4 py-1.5 text-sm font-semibold transition",
                category === c ? "bg-brand-600 text-white shadow-lift" : "border border-slate-200 bg-white text-slate-600 hover:border-brand-300",
              )}
            >
              {c || "All tests"}
            </button>
          ))}
        </div>
        <div className="flex gap-2">
          <div className="relative w-full lg:w-64">
            <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input className="input pl-10" placeholder="Search tests" value={text} onChange={(e) => setText(e.target.value)} />
          </div>
          <select className="input w-44" value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort">
            <option value="popular">Recommended</option>
            <option value="price-asc">Price: low to high</option>
            <option value="price-desc">Price: high to low</option>
          </select>
        </div>
      </div>

      {loading ? (
        <Spinner />
      ) : !visible.length ? (
        <div className="mt-8"><EmptyState icon={SearchX} title="No tests found" /></div>
      ) : (
        <div className="mt-6 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {visible.map((t) => (
            <div key={t.id} className="card flex flex-col p-5 transition hover:-translate-y-0.5 hover:shadow-lift">
              <div className="flex items-start justify-between gap-3">
                <div className="rounded-xl bg-brand-50 p-2.5 text-brand-600"><FlaskConical className="h-5 w-5" /></div>
                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">{t.category}</span>
              </div>
              <h3 className="mt-4 font-bold text-slate-900">{t.name}</h3>
              <p className="mt-1.5 line-clamp-3 flex-1 text-sm text-slate-500">{t.description}</p>
              <div className="mt-4 flex gap-4 text-xs text-slate-500">
                <span className="flex items-center gap-1.5"><Droplet className="h-3.5 w-3.5" />{t.sampleType}</span>
                <span className="flex items-center gap-1.5"><Clock className="h-3.5 w-3.5" />Report in {turnaround(t.turnaroundHours)}</span>
              </div>
              <div className="mt-4 flex items-end justify-between border-t border-slate-100 pt-4">
                <div>
                  <p className="text-xl font-extrabold text-slate-900">{money(t.price)}</p>
                  {t.partnerPrice && <p className="text-xs text-slate-500">{money(t.partnerPrice)} with partner</p>}
                </div>
                <Button onClick={() => setSelected(t)}>Book test</Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <BookLabModal test={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

function turnaround(hours) {
  return hours < 48 ? `${hours} hrs` : `${Math.round(hours / 24)} days`;
}

function BookLabModal({ test, onClose }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const notify = useToast();
  const [day, setDay] = useState(isoDate(addDays(new Date(), 1)));
  const [hour, setHour] = useState("08:00");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const book = async () => {
    if (!user) return navigate("/login?next=/lab-tests");
    setSaving(true);
    setError(null);
    try {
      await api.post("/api/lab-bookings", { testId: test.id, scheduledAt: `${day}T${hour}:00` });
      notify(`${test.name} booked. We'll collect your sample on ${day}.`);
      onClose();
      navigate("/app/patient/labs");
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={!!test} onClose={onClose} title={test ? `Book ${test.name}` : ""}>
      {test && (
        <div className="space-y-4">
          <div className="rounded-xl bg-brand-50 p-4 text-sm text-brand-800">
            <b>{money(test.price)}</b> · {test.sampleType} sample · report in {turnaround(test.turnaroundHours)}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Collection date">
              <input type="date" className="input" min={isoDate(addDays(new Date(), 1))} value={day} onChange={(e) => setDay(e.target.value)} />
            </Field>
            <Field label="Time">
              <select className="input" value={hour} onChange={(e) => setHour(e.target.value)}>
                {["07:00", "07:30", "08:00", "08:30", "09:00", "09:30", "10:00", "11:00", "12:00"].map((h) => (
                  <option key={h}>{h}</option>
                ))}
              </select>
            </Field>
          </div>
          {test.category === "Diabetes" && (
            <p className="text-xs text-slate-500">Fast for 8–10 hours before a glucose test. Water is fine.</p>
          )}
          <ErrorNote error={error} />
          {user && user.role !== "PATIENT" ? (
            <p className="rounded-xl bg-slate-50 p-3 text-center text-sm text-slate-500">Log in as a patient to book.</p>
          ) : (
            <Button className="w-full" size="lg" loading={saving} onClick={book}>
              {user ? "Confirm booking" : "Log in to book"}
            </Button>
          )}
        </div>
      )}
    </Modal>
  );
}
