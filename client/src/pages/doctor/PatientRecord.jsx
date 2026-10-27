import { Link, useParams } from "react-router-dom";
import clsx from "clsx";
import { ArrowLeft, ShieldCheck } from "lucide-react";
import { Avatar, Badge, Card, ErrorNote, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date, dateTime, percent } from "../../lib/format";

export default function PatientRecord() {
  const { id } = useParams();
  const { data, loading, error } = useApi(`/api/doctor/patients/${id}`);

  if (loading) return <Spinner />;
  if (error) return <ErrorNote error={error} />;

  const { patient: p, appointments, labBookings, prescriptions, riskAssessments } = data;
  const reports = labBookings.filter((b) => b.status === "REPORT_READY");

  return (
    <div className="space-y-6">
      <Link to="/app/doctor" className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-500 hover:text-brand-700">
        <ArrowLeft className="h-4 w-4" /> Back to schedule
      </Link>

      <Card className="p-6">
        <div className="flex flex-col gap-5 md:flex-row md:items-center">
          <Avatar name={p.fullName} size="lg" />
          <div className="flex-1">
            <h1 className="text-2xl font-extrabold text-slate-900">{p.fullName}</h1>
            <p className="text-sm text-slate-500">
              {[p.age && `${p.age} yrs`, p.gender, p.bloodGroup && `Blood ${p.bloodGroup}`, p.phone].filter(Boolean).join(" · ")}
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              {p.hypertension && <Badge status="HIGH">Hypertension</Badge>}
              {p.heartDisease && <Badge status="VERY_HIGH">Heart disease</Badge>}
              {p.allergies && <Badge status="NO_SHOW">Allergy: {p.allergies}</Badge>}
              {p.bmi && <Badge>BMI {p.bmi}</Badge>}
            </div>
          </div>
          <p className="flex items-center gap-1.5 text-xs text-slate-400"><ShieldCheck className="h-4 w-4" /> Access recorded in audit log</p>
        </div>
        {p.medicalHistory && <p className="mt-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-700"><b>History:</b> {p.medicalHistory}</p>}
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Lab results</h2>
          {!reports.length ? <p className="mt-3 text-sm text-slate-500">No reports.</p> : (
            <div className="mt-3 space-y-4">
              {reports.map((b) => (
                <div key={b.id}>
                  <p className="text-sm font-semibold text-slate-800">{b.testName} <span className="font-normal text-slate-400">· {date(b.scheduledAt)}</span></p>
                  <div className="mt-1.5 flex flex-wrap gap-2">
                    {b.results.map((r) => (
                      <span key={r.parameter} className={clsx("rounded-lg px-2.5 py-1 text-xs font-semibold", r.flag === "HIGH" ? "bg-rose-50 text-rose-700" : r.flag === "LOW" ? "bg-amber-50 text-amber-700" : "bg-slate-100 text-slate-600")}>
                        {r.parameter}: {r.value} {r.unit}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Diabetes risk checks</h2>
          {!riskAssessments.length ? <p className="mt-3 text-sm text-slate-500">None yet.</p> : (
            <ul className="mt-3 divide-y divide-slate-100">
              {riskAssessments.map((r) => (
                <li key={r.createdAt} className="flex items-center justify-between py-2.5 text-sm">
                  <span className="text-slate-500">{date(r.createdAt)}</span>
                  <span className="text-slate-600">HbA1c {r.hba1c} · Glucose {r.glucose}</span>
                  <span className="flex items-center gap-2 font-bold">{percent(r.probability)} <Badge status={r.riskBand} /></span>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Prescriptions</h2>
          {!prescriptions.length ? <p className="mt-3 text-sm text-slate-500">None yet.</p> : (
            <ul className="mt-3 space-y-3">
              {prescriptions.map((rx) => (
                <li key={rx.id} className="rounded-xl bg-slate-50 p-3">
                  <p className="text-sm font-semibold text-slate-800">{rx.diagnosis}</p>
                  <p className="text-xs text-slate-500">{rx.doctorName} · {date(rx.visitDate)}</p>
                  <p className="mt-1 text-xs text-slate-600">{rx.items.map((i) => `${i.medicine} ${i.dosage}`).join(", ")}</p>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Visit history</h2>
          <ul className="mt-3 divide-y divide-slate-100">
            {appointments.map((a) => (
              <li key={a.id} className="flex items-center justify-between py-2.5 text-sm">
                <div>
                  <p className="font-semibold text-slate-800">{a.doctorName}</p>
                  <p className="text-xs text-slate-500">{a.specialization} · {dateTime(a.slotStart)}</p>
                </div>
                <Badge status={a.status} />
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  );
}
