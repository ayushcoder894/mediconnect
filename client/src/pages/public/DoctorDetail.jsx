import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Building2, CalendarCheck, Clock, GraduationCap, MapPin, MessageSquareQuote } from "lucide-react";
import { api } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import SlotPicker from "../../components/SlotPicker";
import { useToast } from "../../components/Toast";
import { Avatar, Button, Card, EmptyState, ErrorNote, Field, Spinner, Stars } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date, dateTime, money, titleCase } from "../../lib/format";

export default function DoctorDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const notify = useToast();
  const { data, loading, error } = useApi(`/api/doctors/${id}`);
  const { data: reviews } = useApi(`/api/doctors/${id}/reviews`);
  const [slot, setSlot] = useState(null);
  const [reason, setReason] = useState("");
  const [booking, setBooking] = useState(false);
  const [bookError, setBookError] = useState(null);

  if (loading) return <Spinner className="min-h-[60vh]" />;
  if (error) return <div className="mx-auto max-w-3xl px-4 py-16"><ErrorNote error={error} /></div>;

  const { doctor: d, availability } = data;

  const book = async () => {
    if (!user) {
      navigate(`/login?next=/doctors/${id}`);
      return;
    }
    setBooking(true);
    setBookError(null);
    try {
      await api.post("/api/appointments", { doctorId: d.id, slotStart: slot, reason: reason || null });
      notify(`Booked with ${d.name} for ${dateTime(slot)}. Confirmation email queued.`);
      navigate("/app/patient/appointments");
    } catch (e) {
      setBookError(e.message);
      setSlot(null);
    } finally {
      setBooking(false);
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <Link to="/doctors" className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-500 hover:text-brand-700">
        <ArrowLeft className="h-4 w-4" /> All doctors
      </Link>

      <div className="mt-5 grid gap-6 lg:grid-cols-[1fr_420px]">
        <div className="space-y-6">
          <Card className="p-6">
            <div className="flex flex-col gap-5 sm:flex-row">
              <Avatar name={d.name} size="lg" />
              <div className="flex-1">
                <h1 className="text-2xl font-extrabold text-slate-900">{d.name}</h1>
                <p className="font-semibold text-brand-700">{d.specialization}</p>
                <div className="mt-2 flex items-center gap-2 text-sm">
                  <Stars value={d.rating} />
                  <span className="font-semibold">{Number(d.rating).toFixed(1)}</span>
                  <span className="text-slate-400">({d.ratingCount} reviews)</span>
                </div>
                <div className="mt-4 grid gap-2 text-sm text-slate-600 sm:grid-cols-2">
                  <p className="flex items-center gap-2"><GraduationCap className="h-4 w-4 text-slate-400" />{d.qualification}</p>
                  <p className="flex items-center gap-2"><Clock className="h-4 w-4 text-slate-400" />{d.experienceYears} years experience</p>
                  <p className="flex items-center gap-2"><Building2 className="h-4 w-4 text-slate-400" />{d.hospitalName || "Independent practice"}</p>
                  <p className="flex items-center gap-2"><MapPin className="h-4 w-4 text-slate-400" />{d.city || "-"}</p>
                </div>
              </div>
            </div>
            {d.bio && <p className="mt-5 border-t border-slate-100 pt-5 text-slate-600">{d.bio}</p>}
          </Card>

          <Card className="p-6">
            <h2 className="font-bold text-slate-900">Weekly hours</h2>
            <div className="mt-4 grid gap-2 sm:grid-cols-2">
              {availability.map((a) => (
                <div key={a.dayOfWeek} className="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-2.5 text-sm">
                  <span className="font-semibold text-slate-700">{titleCase(a.dayOfWeek)}</span>
                  <span className="text-slate-500">{a.startTime.slice(0, 5)} – {a.endTime.slice(0, 5)}</span>
                </div>
              ))}
            </div>
          </Card>

          <Card className="p-6">
            <h2 className="font-bold text-slate-900">Patient reviews</h2>
            {!reviews?.length ? (
              <div className="mt-4">
                <EmptyState icon={MessageSquareQuote} title="No reviews yet" />
              </div>
            ) : (
              <ul className="mt-4 divide-y divide-slate-100">
                {reviews.map((r) => (
                  <li key={r.id} className="py-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <Avatar name={r.patientName} size="sm" />
                        <div>
                          <p className="text-sm font-semibold text-slate-800">{r.patientName}</p>
                          <p className="text-xs text-slate-400">{date(r.createdAt)}</p>
                        </div>
                      </div>
                      <Stars value={r.rating} />
                    </div>
                    {r.comment && <p className="mt-2 text-sm text-slate-600">{r.comment}</p>}
                    {r.tags?.length > 0 && (
                      <div className="mt-2 flex gap-1.5">
                        {r.tags.map((t) => (
                          <span key={t} className="rounded-full bg-brand-50 px-2 py-0.5 text-xs font-semibold text-brand-700">{t}</span>
                        ))}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>

        <div>
          <Card className="sticky top-24 p-6">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 font-bold text-slate-900">
                <CalendarCheck className="h-5 w-5 text-brand-600" /> Book a visit
              </h2>
              <span className="text-lg font-extrabold text-slate-900">{money(d.fee)}</span>
            </div>
            <p className="mt-1 text-xs text-slate-500">{d.slotMinutes}-minute consultation</p>
            <div className="mt-5">
              <SlotPicker doctorId={d.id} workingDays={availability.map((a) => a.dayOfWeek)} value={slot} onChange={setSlot} />
            </div>
            <Field label="Reason for visit (optional)" className="mt-4">
              <textarea className="input min-h-[80px]" maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. Raised blood sugar in my last report" />
            </Field>
            <div className="mt-4"><ErrorNote error={bookError} /></div>
            {user && user.role !== "PATIENT" ? (
              <p className="mt-4 rounded-xl bg-slate-50 p-3 text-center text-sm text-slate-500">Log in as a patient to book.</p>
            ) : (
              <Button className="mt-4 w-full" size="lg" disabled={!slot} loading={booking} onClick={book}>
                {slot ? `Confirm ${dateTime(slot)}` : "Choose a time slot"}
              </Button>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
