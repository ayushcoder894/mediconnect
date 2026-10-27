import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { CalendarCheck2, CheckCircle2, ClipboardList, FlaskConical, FolderOpen, Plus, Star, Trash2, UserX, Users } from "lucide-react";
import { api, query } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useToast } from "../../components/Toast";
import { Avatar, Badge, Button, Card, EmptyState, ErrorNote, Field, Modal, PageHeader, Spinner, StatCard, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { addDays, date, greeting, isoDate, time } from "../../lib/format";

const RANGES = {
  today: () => [new Date(), new Date()],
  week: () => [addDays(new Date(), 1), addDays(new Date(), 7)],
  past: () => [addDays(new Date(), -14), addDays(new Date(), -1)],
};

export default function DoctorDashboard() {
  const { user } = useAuth();
  const notify = useToast();
  const [range, setRange] = useState("today");
  const [from, to] = RANGES[range]();
  const { data: me } = useApi("/api/doctors/me");
  const { data: appts, loading, reload } = useApi("/api/doctor/appointments" + query({ from: isoDate(from), to: isoDate(to) }));
  const { data: todayAppts, reload: reloadToday } = useApi(
    "/api/doctor/appointments" + query({ from: isoDate(new Date()), to: isoDate(new Date()) }),
  );
  const [rxFor, setRxFor] = useState(null);
  const [labFor, setLabFor] = useState(null);

  const today = useMemo(() => (todayAppts || []).filter((a) => a.status !== "CANCELLED"), [todayAppts]);

  const setStatus = async (a, status) => {
    try {
      await api.patch(`/api/doctor/appointments/${a.id}/status`, { status });
      notify(`${a.patientName}: marked ${status.toLowerCase().replace("_", " ")}.`);
      reload();
      reloadToday();
    } catch (e) {
      notify(e.message, "error");
    }
  };

  return (
    <div>
      <PageHeader title={`${greeting()}, ${user.fullName.replace(/^Dr\.?\s*/, "Dr. ").split(" ").slice(0, 2).join(" ")}`} subtitle={me ? `${me.doctor.specialization} · ${me.doctor.hospitalName || "Independent practice"}` : ""} />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={CalendarCheck2} label="Today's patients" value={today.length} />
        <StatCard icon={Users} label="Checked in" value={today.filter((a) => a.status === "CHECKED_IN").length} tone="violet" hint="Waiting to be seen" />
        <StatCard icon={CheckCircle2} label="Completed today" value={today.filter((a) => a.status === "COMPLETED").length} tone="blue" />
        <StatCard icon={Star} label="Rating" value={me ? Number(me.doctor.rating).toFixed(2) : "-"} tone="amber" hint={me ? `${me.doctor.ratingCount} reviews` : ""} />
      </div>

      <div className="mt-8 flex items-center justify-between">
        <Tabs
          value={range}
          onChange={setRange}
          tabs={[
            { value: "today", label: "Today" },
            { value: "week", label: "Next 7 days" },
            { value: "past", label: "Past 2 weeks" },
          ]}
        />
      </div>

      <div className="mt-4">
        {loading ? (
          <Spinner />
        ) : !appts?.length ? (
          <EmptyState icon={ClipboardList} title="No appointments in this period" />
        ) : (
          <Card className="overflow-hidden">
            <ul className="divide-y divide-slate-100">
              {appts.map((a) => (
                <li key={a.id} className="flex flex-col gap-3 p-4 lg:flex-row lg:items-center">
                  <div className="w-24 shrink-0">
                    <p className="font-bold text-slate-900">{time(a.slotStart)}</p>
                    <p className="text-xs text-slate-500">{range === "today" ? "Today" : date(a.slotStart)}</p>
                  </div>
                  <div className="flex flex-1 items-center gap-3">
                    <Avatar name={a.patientName} size="sm" />
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="font-semibold text-slate-900">{a.patientName}</p>
                        <Badge status={a.status} />
                      </div>
                      <p className="truncate text-sm text-slate-500">{a.reason || "No reason given"}</p>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Link to={`/app/doctor/patients/${a.patientId}`}><Button size="sm" variant="secondary" icon={FolderOpen}>Record</Button></Link>
                    {a.status === "BOOKED" && <Button size="sm" variant="secondary" onClick={() => setStatus(a, "CONFIRMED")}>Confirm</Button>}
                    {["CONFIRMED", "CHECKED_IN", "BOOKED"].includes(a.status) && isoDate(a.slotStart) <= isoDate(new Date()) && (
                      <Button size="sm" icon={ClipboardList} onClick={() => setRxFor(a)}>Prescribe</Button>
                    )}
                    {["CONFIRMED", "CHECKED_IN", "COMPLETED"].includes(a.status) && (
                      <Button size="sm" variant="secondary" icon={FlaskConical} onClick={() => setLabFor(a)}>Order test</Button>
                    )}
                    {["BOOKED", "CONFIRMED"].includes(a.status) && new Date(a.slotStart) < new Date() && (
                      <Button size="sm" variant="danger" icon={UserX} onClick={() => setStatus(a, "NO_SHOW")}>No-show</Button>
                    )}
                    {a.status === "COMPLETED" && a.hasPrescription && <span className="self-center text-xs font-semibold text-emerald-600">Prescription sent ✓</span>}
                  </div>
                </li>
              ))}
            </ul>
          </Card>
        )}
      </div>

      <PrescriptionModal appt={rxFor} onClose={() => setRxFor(null)} onDone={() => { reload(); reloadToday(); }} />
      <OrderLabModal appt={labFor} onClose={() => setLabFor(null)} />
    </div>
  );
}

const EMPTY_ITEM = { medicine: "", dosage: "", frequency: "Once daily", durationDays: 5, instructions: "" };

function PrescriptionModal({ appt, onClose, onDone }) {
  const notify = useToast();
  const [diagnosis, setDiagnosis] = useState("");
  const [notes, setNotes] = useState("");
  const [followUpDate, setFollowUp] = useState("");
  const [items, setItems] = useState([{ ...EMPTY_ITEM }]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const setItem = (i, k, v) => setItems(items.map((it, j) => (j === i ? { ...it, [k]: v } : it)));

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post(`/api/doctor/appointments/${appt.id}/prescription`, {
        diagnosis,
        notes: notes || null,
        followUpDate: followUpDate || null,
        items: items.map((i) => ({ ...i, durationDays: Number(i.durationDays), instructions: i.instructions || null })),
      });
      notify(`Prescription sent to ${appt.patientName}. Visit marked completed.`);
      setDiagnosis("");
      setNotes("");
      setFollowUp("");
      setItems([{ ...EMPTY_ITEM }]);
      onClose();
      onDone();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={!!appt} onClose={onClose} title={appt ? `Prescription for ${appt.patientName}` : ""} wide>
      <form onSubmit={save} className="space-y-4">
        <Field label="Diagnosis"><input required className="input" value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} placeholder="e.g. Type 2 diabetes mellitus" /></Field>
        <div>
          <span className="label">Medicines</span>
          <div className="space-y-2">
            {items.map((it, i) => (
              <div key={i} className="grid gap-2 rounded-xl border border-slate-100 bg-slate-50/50 p-3 sm:grid-cols-[1.4fr_0.8fr_1fr_0.6fr_1.2fr_auto]">
                <input required className="input" placeholder="Medicine" value={it.medicine} onChange={(e) => setItem(i, "medicine", e.target.value)} />
                <input required className="input" placeholder="Dose" value={it.dosage} onChange={(e) => setItem(i, "dosage", e.target.value)} />
                <select className="input" value={it.frequency} onChange={(e) => setItem(i, "frequency", e.target.value)}>
                  {["Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed"].map((f) => <option key={f}>{f}</option>)}
                </select>
                <input required type="number" min="1" max="365" className="input" title="Days" value={it.durationDays} onChange={(e) => setItem(i, "durationDays", e.target.value)} />
                <input className="input" placeholder="Instructions" value={it.instructions} onChange={(e) => setItem(i, "instructions", e.target.value)} />
                <button type="button" disabled={items.length === 1} onClick={() => setItems(items.filter((_, j) => j !== i))} className="rounded-lg p-2 text-slate-400 hover:text-rose-600 disabled:opacity-30" aria-label="Remove medicine">
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
          <Button type="button" variant="ghost" size="sm" icon={Plus} className="mt-2" onClick={() => setItems([...items, { ...EMPTY_ITEM }])}>Add medicine</Button>
        </div>
        <div className="grid gap-4 sm:grid-cols-[2fr_1fr]">
          <Field label="Notes for the patient"><textarea className="input min-h-[70px]" value={notes} onChange={(e) => setNotes(e.target.value)} /></Field>
          <Field label="Follow-up date"><input type="date" min={isoDate(addDays(new Date(), 1))} className="input" value={followUpDate} onChange={(e) => setFollowUp(e.target.value)} /></Field>
        </div>
        <ErrorNote error={error} />
        <Button type="submit" className="w-full" size="lg" loading={saving}>Send prescription</Button>
      </form>
    </Modal>
  );
}

function OrderLabModal({ appt, onClose }) {
  const notify = useToast();
  const { data: tests } = useApi("/api/lab-tests");
  const [testId, setTestId] = useState("");
  const [saving, setSaving] = useState(false);

  const order = async () => {
    setSaving(true);
    try {
      await api.post("/api/doctor/lab-orders", { appointmentId: appt.id, testId: Number(testId) });
      notify(`Lab test ordered for ${appt.patientName}.`);
      setTestId("");
      onClose();
    } catch (e) {
      notify(e.message, "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={!!appt} onClose={onClose} title={appt ? `Order a test for ${appt.patientName}` : ""}>
      <div className="space-y-4">
        <Field label="Lab test">
          <select className="input" value={testId} onChange={(e) => setTestId(e.target.value)}>
            <option value="">Choose a test</option>
            {tests?.filter((t) => t.parameters.length).map((t) => <option key={t.id} value={t.id}>{t.name} ({t.category})</option>)}
          </select>
        </Field>
        <p className="text-xs text-slate-500">The patient is booked for sample collection tomorrow at 9:00 and notified by email.</p>
        <Button className="w-full" disabled={!testId} loading={saving} onClick={order}>Order test</Button>
      </div>
    </Modal>
  );
}
