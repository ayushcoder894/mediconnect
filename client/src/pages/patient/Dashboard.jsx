import { Link } from "react-router-dom";
import { QRCodeSVG } from "qrcode.react";
import { Activity, ArrowRight, Bot, CalendarDays, CalendarPlus, FileDown, FlaskConical, Pill, Stethoscope } from "lucide-react";
import { openPdf } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { Avatar, Badge, Button, Card, EmptyState, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date, dateTime, greeting, percent } from "../../lib/format";

export default function PatientDashboard() {
  const { user } = useAuth();
  const { data: appts, loading } = useApi("/api/appointments/mine");
  const { data: labs } = useApi("/api/lab-bookings/mine");
  const { data: risks } = useApi("/api/risk/diabetes/history");
  const { data: rx } = useApi("/api/prescriptions/mine");

  if (loading) return <Spinner />;

  const upcoming = (appts || [])
    .filter((a) => ["BOOKED", "CONFIRMED", "CHECKED_IN"].includes(a.status) && new Date(a.slotEnd) > new Date())
    .sort((a, b) => new Date(a.slotStart) - new Date(b.slotStart));
  const next = upcoming[0];
  const reports = (labs || []).filter((l) => l.status === "REPORT_READY").slice(0, 3);
  const latestRisk = risks?.[0];

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-semibold text-brand-700">{greeting()},</p>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">{user.fullName.split(" ")[0]} 👋</h1>
        </div>
        <Link to="/doctors"><Button icon={CalendarPlus}>Book an appointment</Button></Link>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="overflow-hidden lg:col-span-2">
          {next ? (
            <div className="flex flex-col sm:flex-row">
              <div className="flex-1 p-6">
                <div className="flex items-center gap-2">
                  <p className="text-xs font-bold uppercase tracking-wide text-slate-500">Next appointment</p>
                  <Badge status={next.status} />
                </div>
                <div className="mt-4 flex items-center gap-4">
                  <Avatar name={next.doctorName} />
                  <div>
                    <p className="text-lg font-bold text-slate-900">{next.doctorName}</p>
                    <p className="text-sm text-brand-700">{next.specialization}</p>
                  </div>
                </div>
                <div className="mt-5 grid gap-3 sm:grid-cols-2">
                  <div className="rounded-xl bg-slate-50 p-3">
                    <p className="text-xs text-slate-500">When</p>
                    <p className="font-semibold text-slate-800">{dateTime(next.slotStart)}</p>
                  </div>
                  <div className="rounded-xl bg-slate-50 p-3">
                    <p className="text-xs text-slate-500">Where</p>
                    <p className="truncate font-semibold text-slate-800">{next.hospitalName}</p>
                  </div>
                </div>
                <div className="mt-5 flex flex-wrap gap-2">
                  <Button variant="secondary" size="sm" icon={FileDown} onClick={() => openPdf(`/api/appointments/${next.id}/slip.pdf`)}>
                    Download slip
                  </Button>
                  <Link to="/app/patient/appointments"><Button variant="ghost" size="sm">Manage <ArrowRight className="h-4 w-4" /></Button></Link>
                </div>
              </div>
              <div className="flex flex-col items-center justify-center gap-2 border-t border-slate-100 bg-gradient-to-br from-brand-50 to-white p-6 sm:border-l sm:border-t-0">
                <div className="rounded-2xl bg-white p-3 shadow-card">
                  <QRCodeSVG value={next.checkInCode} size={132} fgColor="#0f172a" />
                </div>
                <p className="text-xs font-semibold text-slate-500">Scan at the front desk</p>
              </div>
            </div>
          ) : (
            <div className="p-6">
              <EmptyState
                icon={CalendarDays}
                title="No upcoming appointments"
                text="Find a specialist and book a slot that suits you."
                action={<Link to="/doctors"><Button>Find a doctor</Button></Link>}
              />
            </div>
          )}
        </Card>

        <Card className="flex flex-col p-6">
          <div className="flex items-center justify-between">
            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">Diabetes risk</p>
            <Activity className="h-5 w-5 text-brand-600" />
          </div>
          {latestRisk ? (
            <>
              <p className="mt-4 text-4xl font-extrabold text-slate-900">{percent(latestRisk.probability)}</p>
              <div className="mt-2 flex items-center gap-2">
                <Badge status={latestRisk.riskBand} />
                <span className="text-xs text-slate-500">checked {date(latestRisk.createdAt)}</span>
              </div>
              <p className="mt-3 flex-1 text-sm text-slate-500">HbA1c {latestRisk.hba1c}% · Glucose {latestRisk.glucose} mg/dL</p>
            </>
          ) : (
            <p className="mt-4 flex-1 text-sm text-slate-500">
              See your risk in 30 seconds. We fill in your latest HbA1c and glucose from your lab reports automatically.
            </p>
          )}
          <Link to="/app/patient/risk" className="mt-4"><Button variant="secondary" className="w-full">{latestRisk ? "Check again" : "Check my risk"}</Button></Link>
        </Card>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { to: "/doctors", icon: Stethoscope, label: "Find a doctor", text: "11 specialties" },
          { to: "/lab-tests", icon: FlaskConical, label: "Book a lab test", text: "Home sample collection" },
          { to: "/app/patient/assistant", icon: Bot, label: "Ask MediBot", text: "Insurance & care questions" },
          { to: "/app/patient/prescriptions", icon: Pill, label: "Prescriptions", text: `${rx?.length ?? 0} on file` },
        ].map((q) => (
          <Link key={q.label} to={q.to} className="card group flex items-center gap-4 p-4 transition hover:-translate-y-0.5 hover:shadow-lift">
            <div className="rounded-xl bg-brand-50 p-3 text-brand-600 transition group-hover:bg-brand-600 group-hover:text-white">
              <q.icon className="h-5 w-5" />
            </div>
            <div>
              <p className="font-bold text-slate-900">{q.label}</p>
              <p className="text-xs text-slate-500">{q.text}</p>
            </div>
          </Link>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-slate-900">Recent lab reports</h2>
            <Link to="/app/patient/labs" className="text-sm font-semibold text-brand-700 hover:underline">View all</Link>
          </div>
          {!reports.length ? (
            <p className="mt-4 text-sm text-slate-500">No reports yet.</p>
          ) : (
            <ul className="mt-4 divide-y divide-slate-100">
              {reports.map((r) => (
                <li key={r.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="font-semibold text-slate-800">{r.testName}</p>
                    <p className="text-xs text-slate-500">
                      {date(r.scheduledAt)} ·{" "}
                      {r.results.filter((x) => x.flag && x.flag !== "NORMAL").length
                        ? <span className="font-semibold text-orange-600">{r.results.filter((x) => x.flag && x.flag !== "NORMAL").length} value(s) out of range</span>
                        : "All values normal"}
                    </p>
                  </div>
                  <Button size="sm" variant="secondary" icon={FileDown} onClick={() => openPdf(`/api/lab-bookings/${r.id}/report.pdf`)}>PDF</Button>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-slate-900">Upcoming</h2>
            <Link to="/app/patient/appointments" className="text-sm font-semibold text-brand-700 hover:underline">All appointments</Link>
          </div>
          {upcoming.length <= 1 ? (
            <p className="mt-4 text-sm text-slate-500">Nothing else scheduled.</p>
          ) : (
            <ul className="mt-4 divide-y divide-slate-100">
              {upcoming.slice(1, 4).map((a) => (
                <li key={a.id} className="flex items-center gap-3 py-3">
                  <Avatar name={a.doctorName} size="sm" />
                  <div className="flex-1">
                    <p className="font-semibold text-slate-800">{a.doctorName}</p>
                    <p className="text-xs text-slate-500">{dateTime(a.slotStart)}</p>
                  </div>
                  <Badge status={a.status} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
