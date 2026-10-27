import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Building2, Clock, MapPin, Search, SearchX } from "lucide-react";
import { query } from "../../api/client";
import { Avatar, Button, EmptyState, Spinner, Stars } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { money } from "../../lib/format";

export default function Doctors() {
  const [params, setParams] = useSearchParams();
  const [text, setText] = useState(params.get("q") || "");
  const specialization = params.get("specialization") || "";
  const city = params.get("city") || "";
  const maxFee = params.get("maxFee") || "";

  const { data: specializations } = useApi("/api/doctors/specializations");
  const { data: cities } = useApi("/api/hospitals/cities");
  const path = useMemo(
    () => "/api/doctors" + query({ specialization, city, maxFee, q: params.get("q") }),
    [specialization, city, maxFee, params],
  );
  const { data: doctors, loading } = useApi(path);

  const set = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <div className="max-w-2xl">
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Find the right doctor</h1>
        <p className="mt-2 text-slate-500">Verified specialists across our partner hospitals. Book a free slot instantly.</p>
      </div>

      <div className="card mt-8 grid gap-3 p-4 md:grid-cols-[1.6fr_1fr_1fr_1fr]">
        <form
          className="relative"
          onSubmit={(e) => {
            e.preventDefault();
            set("q", text.trim());
          }}
        >
          <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input className="input pl-10" placeholder="Search by name or specialty" value={text} onChange={(e) => setText(e.target.value)} />
        </form>
        <select className="input" value={specialization} onChange={(e) => set("specialization", e.target.value)} aria-label="Specialization">
          <option value="">All specialties</option>
          {specializations?.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
        <select className="input" value={city} onChange={(e) => set("city", e.target.value)} aria-label="City">
          <option value="">All cities</option>
          {cities?.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
        <select className="input" value={maxFee} onChange={(e) => set("maxFee", e.target.value)} aria-label="Maximum fee">
          <option value="">Any fee</option>
          <option value="500">Up to ₹500</option>
          <option value="800">Up to ₹800</option>
          <option value="1000">Up to ₹1,000</option>
        </select>
      </div>

      {loading ? (
        <Spinner />
      ) : !doctors?.length ? (
        <div className="mt-8">
          <EmptyState icon={SearchX} title="No doctors match these filters" text="Try another specialty or clear the filters." />
        </div>
      ) : (
        <>
          <p className="mt-6 text-sm text-slate-500">{doctors.length} doctor{doctors.length === 1 ? "" : "s"} available</p>
          <div className="mt-3 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            {doctors.map((d) => (
              <DoctorCard key={d.id} doctor={d} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function DoctorCard({ doctor: d }) {
  return (
    <div className="card flex flex-col p-5 transition hover:-translate-y-0.5 hover:shadow-lift">
      <div className="flex gap-4">
        <Avatar name={d.name} />
        <div className="min-w-0">
          <h3 className="truncate font-bold text-slate-900">{d.name}</h3>
          <p className="text-sm font-semibold text-brand-700">{d.specialization}</p>
          <p className="truncate text-xs text-slate-500">{d.qualification}</p>
        </div>
      </div>
      <div className="mt-4 flex items-center gap-2 text-sm">
        <Stars value={d.rating} />
        <span className="font-semibold text-slate-700">{Number(d.rating).toFixed(1)}</span>
        <span className="text-slate-400">({d.ratingCount} reviews)</span>
      </div>
      <div className="mt-3 space-y-1.5 text-sm text-slate-500">
        <p className="flex items-center gap-2"><Building2 className="h-4 w-4 text-slate-400" />{d.hospitalName || "Independent practice"}</p>
        <p className="flex items-center gap-2"><MapPin className="h-4 w-4 text-slate-400" />{d.city || "-"}</p>
        <p className="flex items-center gap-2"><Clock className="h-4 w-4 text-slate-400" />{d.experienceYears} years experience</p>
      </div>
      <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4">
        <div>
          <p className="text-xs text-slate-500">Consultation</p>
          <p className="text-lg font-extrabold text-slate-900">{money(d.fee)}</p>
        </div>
        <Link to={`/doctors/${d.id}`}>
          <Button>Book visit</Button>
        </Link>
      </div>
    </div>
  );
}
