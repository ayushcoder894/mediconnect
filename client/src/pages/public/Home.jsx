import { Link } from "react-router-dom";
import {
  Activity,
  ArrowRight,
  BadgeCheck,
  Bot,
  CalendarCheck,
  Droplets,
  FileText,
  FlaskConical,
  QrCode,
  ShieldCheck,
  Sparkles,
  Stethoscope,
} from "lucide-react";
import { Button } from "../../components/ui";
import { useApi } from "../../lib/useApi";

const STEPS = [
  { icon: FlaskConical, title: "Get tested", text: "Book an HbA1c, lipid or any of 37 lab tests. Reports arrive in your account." },
  { icon: Activity, title: "Understand your risk", text: "Your lab values feed an ML model that estimates your diabetes risk and explains why." },
  { icon: CalendarCheck, title: "See the right doctor", text: "MediBot suggests a specialist. Book a free slot in seconds, no double-bookings." },
  { icon: FileText, title: "Follow your plan", text: "Check in with a QR code, then get your prescription and reminders digitally." },
];

const FEATURES = [
  { icon: Stethoscope, title: "Verified specialists", text: "Every doctor is approved by the hospital before they appear in search." },
  { icon: Activity, title: "AI diabetes risk check", text: "LightGBM model with clinical guardrails, prefilled from your own lab reports." },
  { icon: Bot, title: "MediBot assistant", text: "Ask about insurance cover with answers cited from real policy documents." },
  { icon: QrCode, title: "QR check-in", text: "Skip the queue. The front desk scans your appointment slip." },
  { icon: Droplets, title: "Blood & organ finder", text: "Live availability across partner hospitals, filterable by city." },
  { icon: ShieldCheck, title: "Private by design", text: "Role-based access and an audit trail of every record a doctor opens." },
];

export default function Home() {
  const { data: stats } = useApi("/api/public/stats");
  const { data: specializations } = useApi("/api/doctors/specializations");

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="bg-grid absolute inset-0 [mask-image:radial-gradient(ellipse_at_top,black,transparent_70%)]" />
        <div className="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-brand-200/50 blur-3xl" />
        <div className="relative mx-auto grid max-w-7xl items-center gap-12 px-4 pb-20 pt-14 sm:px-6 lg:grid-cols-2 lg:pt-20">
          <div className="animate-fade-up">
            <span className="inline-flex items-center gap-2 rounded-full border border-brand-200 bg-white px-3 py-1 text-xs font-bold text-brand-700 shadow-sm">
              <Sparkles className="h-3.5 w-3.5" /> AI-assisted care, end to end
            </span>
            <h1 className="mt-5 text-4xl font-extrabold leading-[1.1] tracking-tight text-slate-900 sm:text-5xl lg:text-6xl">
              Your health, <span className="bg-gradient-to-r from-brand-600 to-sky-500 bg-clip-text text-transparent">connected</span> from first symptom to final prescription.
            </h1>
            <p className="mt-5 max-w-xl text-lg text-slate-600">
              Book trusted doctors and lab tests, see your diabetes risk from your own reports, and keep every
              prescription in one secure place.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/doctors">
                <Button size="lg" icon={Stethoscope}>Find a doctor</Button>
              </Link>
              <Link to="/app/patient/risk">
                <Button size="lg" variant="secondary" icon={Activity}>Check diabetes risk</Button>
              </Link>
            </div>
            <dl className="mt-10 grid max-w-lg grid-cols-4 gap-4">
              {[
                ["Doctors", stats?.doctors],
                ["Specialties", stats?.specializations],
                ["Lab tests", stats?.labTests],
                ["Hospitals", stats?.hospitals],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</dt>
                  <dd className="mt-1 text-2xl font-extrabold text-slate-900">{value ?? "–"}</dd>
                </div>
              ))}
            </dl>
          </div>

          <HeroPreview />
        </div>
      </section>

      {/* How it works */}
      <section className="border-y border-slate-200/70 bg-white py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6">
          <div className="max-w-2xl">
            <p className="text-sm font-bold uppercase tracking-wider text-brand-600">How it works</p>
            <h2 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">One connected journey instead of five separate apps</h2>
          </div>
          <div className="mt-12 grid gap-6 md:grid-cols-4">
            {STEPS.map((s, i) => (
              <div key={s.title} className="relative">
                {i < STEPS.length - 1 && (
                  <div className="absolute left-14 right-0 top-6 hidden h-px bg-gradient-to-r from-brand-300 to-transparent md:block" />
                )}
                <div className="relative flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-lift">
                  <s.icon className="h-6 w-6" />
                </div>
                <p className="mt-4 text-xs font-bold text-brand-600">STEP {i + 1}</p>
                <h3 className="mt-1 text-lg font-bold text-slate-900">{s.title}</h3>
                <p className="mt-1.5 text-sm text-slate-500">{s.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6">
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-sm font-bold uppercase tracking-wider text-brand-600">Built for patients, doctors and hospitals</p>
            <h2 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">Everything a modern clinic needs</h2>
          </div>
          <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map((f) => (
              <div key={f.title} className="card group p-6 transition hover:-translate-y-1 hover:shadow-lift">
                <div className="inline-flex rounded-xl bg-brand-50 p-2.5 text-brand-600 transition group-hover:bg-brand-600 group-hover:text-white">
                  <f.icon className="h-5 w-5" />
                </div>
                <h3 className="mt-4 font-bold text-slate-900">{f.title}</h3>
                <p className="mt-1.5 text-sm text-slate-500">{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Specialties */}
      {specializations?.length > 0 && (
        <section className="pb-20">
          <div className="mx-auto max-w-7xl px-4 sm:px-6">
            <h2 className="text-2xl font-extrabold tracking-tight text-slate-900">Browse by specialty</h2>
            <div className="mt-6 flex flex-wrap gap-3">
              {specializations.map((s) => (
                <Link
                  key={s}
                  to={`/doctors?specialization=${encodeURIComponent(s)}`}
                  className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-brand-400 hover:text-brand-700"
                >
                  {s}
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* CTA */}
      <section className="px-4 pb-20 sm:px-6">
        <div className="relative mx-auto max-w-7xl overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-900 to-brand-900 px-8 py-14 sm:px-14">
          <div className="absolute -right-20 -top-20 h-72 w-72 rounded-full bg-brand-500/30 blur-3xl" />
          <div className="relative flex flex-col items-start justify-between gap-6 lg:flex-row lg:items-center">
            <div>
              <h2 className="text-3xl font-extrabold text-white">Are you a doctor?</h2>
              <p className="mt-2 max-w-xl text-slate-300">
                Set your hours, see your patients' reports before they walk in, and write digital prescriptions in under a minute.
              </p>
            </div>
            <Link to="/register?as=doctor">
              <Button size="lg" className="bg-white !text-slate-900 hover:bg-brand-50">
                Join as a doctor <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

function HeroPreview() {
  return (
    <div className="relative mx-auto w-full max-w-md animate-fade-up [animation-delay:120ms] lg:max-w-none">
      <div className="card relative z-10 p-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-sky-400 to-indigo-500 font-bold text-white">RM</div>
            <div>
              <p className="text-sm font-bold text-slate-900">Dr. Rohan Mehta</p>
              <p className="text-xs text-slate-500">Endocrinology · ★ 4.8</p>
            </div>
          </div>
          <span className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-700">Confirmed</span>
        </div>
        <div className="mt-4 grid grid-cols-3 gap-2">
          {["10:00", "10:30", "11:00", "11:30", "12:00", "12:30"].map((t, i) => (
            <div
              key={t}
              className={`rounded-lg border py-2 text-center text-sm font-semibold ${i === 2 ? "border-brand-600 bg-brand-50 text-brand-700" : i === 4 ? "border-slate-100 bg-slate-50 text-slate-300 line-through" : "border-slate-200 text-slate-600"}`}
            >
              {t}
            </div>
          ))}
        </div>
      </div>

      <div className="card relative z-20 -mt-6 ml-auto w-[78%] p-5 sm:-mr-6">
        <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wide text-slate-500">
          <Activity className="h-4 w-4 text-brand-600" /> Diabetes risk
        </div>
        <div className="mt-3 flex items-end justify-between">
          <div>
            <p className="text-3xl font-extrabold text-slate-900">63%</p>
            <p className="text-xs text-slate-500">From your HbA1c 6.4% · Glucose 132</p>
          </div>
          <span className="rounded-full bg-orange-50 px-2.5 py-1 text-xs font-bold text-orange-700 ring-1 ring-orange-200">High</span>
        </div>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100">
          <div className="h-full w-[63%] rounded-full bg-gradient-to-r from-emerald-400 via-amber-400 to-orange-500" />
        </div>
      </div>

      <div className="card relative z-30 -mt-4 w-[82%] p-4 sm:-ml-6">
        <div className="flex gap-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-600 text-white">
            <Bot className="h-4 w-4" />
          </div>
          <p className="text-sm text-slate-600">
            Based on your results, an <b className="text-slate-900">Endocrinology</b> specialist is the right doctor to see.
          </p>
        </div>
        <div className="mt-3 flex items-center gap-1.5 text-xs font-semibold text-brand-700">
          <BadgeCheck className="h-4 w-4" /> Cited from Activ One policy, page 10
        </div>
      </div>
    </div>
  );
}
