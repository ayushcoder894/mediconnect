import { useState } from "react";
import { Link } from "react-router-dom";
import { QRCodeSVG } from "qrcode.react";
import { CalendarClock, CalendarDays, FileDown, MapPin, Pill, QrCode, Star, XCircle } from "lucide-react";
import { api, openPdf } from "../../api/client";
import SlotPicker from "../../components/SlotPicker";
import { useToast } from "../../components/Toast";
import { Avatar, Badge, Button, Card, EmptyState, ErrorNote, Field, Modal, PageHeader, Spinner, Stars, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { dateTime, money } from "../../lib/format";

const TAGS = ["Explains well", "On time", "Friendly", "Thorough", "Good follow-up"];

export default function MyAppointments() {
  const notify = useToast();
  const { data, loading, reload } = useApi("/api/appointments/mine");
  const [tab, setTab] = useState("upcoming");
  const [qr, setQr] = useState(null);
  const [reschedule, setReschedule] = useState(null);
  const [review, setReview] = useState(null);

  if (loading) return <Spinner />;

  const now = new Date();
  const upcoming = (data || [])
    .filter((a) => ["BOOKED", "CONFIRMED", "CHECKED_IN"].includes(a.status) && new Date(a.slotEnd) > now)
    .sort((a, b) => new Date(a.slotStart) - new Date(b.slotStart));
  const past = (data || []).filter((a) => !upcoming.includes(a));
  const list = tab === "upcoming" ? upcoming : past;

  const cancel = async (a) => {
    if (!window.confirm(`Cancel your appointment with ${a.doctorName} on ${dateTime(a.slotStart)}?`)) return;
    try {
      await api.post(`/api/appointments/${a.id}/cancel`);
      notify("Appointment cancelled. The slot is now free for others.");
      reload();
    } catch (e) {
      notify(e.message, "error");
    }
  };

  return (
    <div>
      <PageHeader
        title="Appointments"
        subtitle="Manage visits, download slips and leave reviews."
        actions={<Link to="/doctors"><Button>Book new</Button></Link>}
      />
      <Tabs
        value={tab}
        onChange={setTab}
        tabs={[
          { value: "upcoming", label: "Upcoming", count: upcoming.length },
          { value: "past", label: "Past & cancelled", count: past.length },
        ]}
      />

      <div className="mt-5 space-y-4">
        {!list.length ? (
          <EmptyState icon={CalendarDays} title={tab === "upcoming" ? "No upcoming visits" : "No past visits yet"} action={tab === "upcoming" && <Link to="/doctors"><Button>Find a doctor</Button></Link>} />
        ) : (
          list.map((a) => (
            <Card key={a.id} className="p-5">
              <div className="flex flex-col gap-4 md:flex-row md:items-center">
                <div className="flex flex-1 items-center gap-4">
                  <Avatar name={a.doctorName} />
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-bold text-slate-900">{a.doctorName}</p>
                      <Badge status={a.status} />
                    </div>
                    <p className="text-sm text-brand-700">{a.specialization}</p>
                    <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-500">
                      <span className="flex items-center gap-1.5"><CalendarClock className="h-4 w-4" />{dateTime(a.slotStart)}</span>
                      <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" />{a.hospitalName}</span>
                      <span>{money(a.fee)}</span>
                    </div>
                    {a.reason && <p className="mt-1 text-sm italic text-slate-500">"{a.reason}"</p>}
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  {tab === "upcoming" && (
                    <>
                      <Button size="sm" variant="secondary" icon={QrCode} onClick={() => setQr(a)}>QR</Button>
                      <Button size="sm" variant="secondary" icon={FileDown} onClick={() => openPdf(`/api/appointments/${a.id}/slip.pdf`)}>Slip</Button>
                      {a.status !== "CHECKED_IN" && (
                        <>
                          <Button size="sm" variant="secondary" icon={CalendarClock} onClick={() => setReschedule(a)}>Reschedule</Button>
                          <Button size="sm" variant="danger" icon={XCircle} onClick={() => cancel(a)}>Cancel</Button>
                        </>
                      )}
                    </>
                  )}
                  {a.hasPrescription && (
                    <Link to="/app/patient/prescriptions"><Button size="sm" variant="secondary" icon={Pill}>Prescription</Button></Link>
                  )}
                  {a.status === "COMPLETED" && !a.reviewed && (
                    <Button size="sm" icon={Star} onClick={() => setReview(a)}>Rate visit</Button>
                  )}
                  {a.reviewed && <span className="self-center text-xs font-semibold text-slate-400">Reviewed ✓</span>}
                </div>
              </div>
            </Card>
          ))
        )}
      </div>

      <Modal open={!!qr} onClose={() => setQr(null)} title="Check-in code">
        {qr && (
          <div className="flex flex-col items-center text-center">
            <div className="rounded-2xl border border-slate-100 p-4 shadow-card">
              <QRCodeSVG value={qr.checkInCode} size={220} />
            </div>
            <p className="mt-4 font-bold text-slate-900">{qr.doctorName}</p>
            <p className="text-sm text-slate-500">{dateTime(qr.slotStart)}</p>
            <p className="mt-3 text-xs text-slate-400">Show this at the front desk on the day of your visit.</p>
          </div>
        )}
      </Modal>

      <RescheduleModal appt={reschedule} onClose={() => setReschedule(null)} onDone={reload} />
      <ReviewModal appt={review} onClose={() => setReview(null)} onDone={reload} />
    </div>
  );
}

function RescheduleModal({ appt, onClose, onDone }) {
  const notify = useToast();
  const [slot, setSlot] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      await api.post(`/api/appointments/${appt.id}/reschedule`, { slotStart: slot });
      notify(`Moved to ${dateTime(slot)}.`);
      setSlot(null);
      onClose();
      onDone();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={!!appt} onClose={onClose} title="Reschedule appointment">
      {appt && (
        <div className="space-y-4">
          <p className="text-sm text-slate-500">
            Currently <b className="text-slate-800">{dateTime(appt.slotStart)}</b> with {appt.doctorName}. If the new slot is
            taken, your current booking is kept.
          </p>
          <SlotPicker doctorId={appt.doctorId} value={slot} onChange={setSlot} />
          <ErrorNote error={error} />
          <Button className="w-full" disabled={!slot} loading={saving} onClick={save}>
            {slot ? `Move to ${dateTime(slot)}` : "Pick a new slot"}
          </Button>
        </div>
      )}
    </Modal>
  );
}

function ReviewModal({ appt, onClose, onDone }) {
  const notify = useToast();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [tags, setTags] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      await api.post(`/api/appointments/${appt.id}/review`, { rating, comment, tags });
      notify("Thanks for your feedback!");
      onClose();
      onDone();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={!!appt} onClose={onClose} title={appt ? `Rate your visit with ${appt.doctorName}` : ""}>
      <div className="space-y-4">
        <div className="flex justify-center"><Stars value={rating} size="h-9 w-9" onChange={setRating} /></div>
        <div className="flex flex-wrap justify-center gap-2">
          {TAGS.map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setTags(tags.includes(t) ? tags.filter((x) => x !== t) : [...tags, t])}
              className={`rounded-full border px-3 py-1 text-xs font-semibold ${tags.includes(t) ? "border-brand-600 bg-brand-50 text-brand-700" : "border-slate-200 text-slate-600"}`}
            >
              {t}
            </button>
          ))}
        </div>
        <Field label="Comment (optional)">
          <textarea className="input min-h-[90px]" maxLength={1000} value={comment} onChange={(e) => setComment(e.target.value)} />
        </Field>
        <ErrorNote error={error} />
        <Button className="w-full" loading={saving} onClick={save}>Submit review</Button>
      </div>
    </Modal>
  );
}
