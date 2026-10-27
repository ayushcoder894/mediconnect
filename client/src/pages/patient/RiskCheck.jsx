import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Activity, CalendarPlus, FileCheck2, Info, Sparkles } from "lucide-react";
import { api } from "../../api/client";
import { Badge, Button, Card, ErrorNote, Field, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date, percent } from "../../lib/format";

const BAND_COLOR = { LOW: "#10b981", MODERATE: "#f59e0b", HIGH: "#f97316", VERY_HIGH: "#e11d48" };
const BAND_LABEL = { LOW: "Low risk", MODERATE: "Moderate risk", HIGH: "High risk", VERY_HIGH: "Very high risk" };

export default function RiskCheck() {
  const { data: prefill, loading } = useApi("/api/risk/diabetes/prefill");
  const { data: history, reload } = useApi("/api/risk/diabetes/history");
  const [form, setForm] = useState(null);
  const [result, setResult] = useState(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (prefill && !form) {
      setForm({
        gender: prefill.gender || "Female",
        age: prefill.age ?? "",
        hypertension: prefill.hypertension,
        heartDisease: prefill.heartDisease,
        smokingHistory: prefill.smokingHistory || "No Info",
        bmi: prefill.bmi ?? "",
        hba1c: prefill.hba1c?.value ?? "",
        glucose: prefill.glucose?.value ?? "",
      });
    }
  }, [prefill, form]);

  if (loading || !form) return <Spinner />;

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value });

  const run = async (e) => {
    e.preventDefault();
    setRunning(true);
    setError(null);
    try {
      const r = await api.post("/api/risk/diabetes", {
        ...form,
        age: Number(form.age),
        bmi: Number(form.bmi),
        hba1c: Number(form.hba1c),
        glucose: Number(form.glucose),
      });
      setResult(r);
      reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setRunning(false);
    }
  };

  const chart = (history || [])
    .slice()
    .reverse()
    .map((h) => ({ date: date(h.createdAt), risk: Math.round(h.probability * 100) }));

  return (
    <div>
      <PageHeader title="Diabetes risk check" subtitle="A LightGBM model trained on 100,000 patient records, with clinical guardrails." />

      <div className="grid gap-6 lg:grid-cols-[1fr_1.1fr]">
        <Card className="p-6">
          <form onSubmit={run} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Gender">
                <select className="input" value={form.gender} onChange={set("gender")}>
                  <option>Female</option>
                  <option>Male</option>
                  <option>Other</option>
                </select>
              </Field>
              <Field label="Age">
                <input required type="number" min="1" max="110" className="input" value={form.age} onChange={set("age")} />
              </Field>
              <Field label="BMI" hint={prefill.bmi ? "From your height and weight" : "Weight (kg) ÷ height (m)²"}>
                <input required type="number" step="0.1" min="10" max="80" className="input" value={form.bmi} onChange={set("bmi")} />
              </Field>
              <Field label="Smoking">
                <select className="input" value={form.smokingHistory} onChange={set("smokingHistory")}>
                  <option value="never">Never</option>
                  <option value="former">Former</option>
                  <option value="current">Current</option>
                  <option value="not current">Not current</option>
                  <option value="ever">Ever</option>
                  <option value="No Info">Prefer not to say</option>
                </select>
              </Field>
              <LabField label="HbA1c (%)" value={form.hba1c} onChange={set("hba1c")} source={prefill.hba1c} step="0.1" min="3" max="15" />
              <LabField label="Fasting glucose (mg/dL)" value={form.glucose} onChange={set("glucose")} source={prefill.glucose} step="1" min="50" max="400" />
            </div>
            <div className="flex flex-wrap gap-5 pt-1">
              <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                <input type="checkbox" className="h-4 w-4 rounded accent-brand-600" checked={form.hypertension} onChange={set("hypertension")} />
                High blood pressure
              </label>
              <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                <input type="checkbox" className="h-4 w-4 rounded accent-brand-600" checked={form.heartDisease} onChange={set("heartDisease")} />
                Heart disease
              </label>
            </div>
            {prefill.missing?.some((m) => m === "hba1c" || m === "glucose") && (
              <p className="flex gap-2 rounded-xl bg-sky-50 p-3 text-xs text-sky-800">
                <Info className="h-4 w-4 shrink-0" />
                No recent HbA1c or glucose report found. <Link to="/lab-tests" className="font-semibold underline">Book a Diabetes Screening Panel</Link> to fill these in automatically.
              </p>
            )}
            <ErrorNote error={error} />
            <Button type="submit" size="lg" className="w-full" icon={Sparkles} loading={running}>Calculate my risk</Button>
          </form>
        </Card>

        <div className="space-y-6">
          <Card className="p-6">
            {result ? (
              <ResultPanel result={result} />
            ) : (
              <div className="flex flex-col items-center py-10 text-center">
                <div className="rounded-2xl bg-brand-50 p-4 text-brand-600"><Activity className="h-8 w-8" /></div>
                <h3 className="mt-4 font-bold text-slate-900">Your result appears here</h3>
                <p className="mt-1 max-w-sm text-sm text-slate-500">
                  We've filled in what we know from your profile and reports. Check the values and press Calculate.
                </p>
              </div>
            )}
          </Card>

          {chart.length > 1 && (
            <Card className="p-6">
              <h3 className="font-bold text-slate-900">Your risk over time</h3>
              <div className="mt-4 h-48">
                <ResponsiveContainer>
                  <LineChart data={chart} margin={{ left: -20, right: 8 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                    <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(v) => [`${v}%`, "Risk"]} />
                    <Line type="monotone" dataKey="risk" stroke="#0d9488" strokeWidth={2.5} dot={{ r: 4 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </Card>
          )}
        </div>
      </div>
      <p className="mt-6 text-xs text-slate-400">
        This estimate is for awareness only and is not a diagnosis. The model output is smoothed over nearby values and never
        reported below what ADA guideline thresholds (HbA1c 5.7% / 6.5%, fasting glucose 100 / 126 mg/dL) indicate.
      </p>
    </div>
  );
}

function LabField({ label, value, onChange, source, ...rest }) {
  return (
    <Field label={label} hint={source ? undefined : "Enter from your latest report"}>
      <input required type="number" className="input" value={value} onChange={onChange} {...rest} />
      {source && (
        <span className="mt-1 flex items-center gap-1 text-xs font-medium text-brand-700">
          <FileCheck2 className="h-3.5 w-3.5" /> From lab report, {date(source.measuredAt)}
        </span>
      )}
    </Field>
  );
}

function ResultPanel({ result }) {
  const color = BAND_COLOR[result.riskBand];
  return (
    <div className="animate-fade-up">
      <div className="flex flex-col items-center">
        <Gauge value={result.probability} color={color} />
        <Badge status={result.riskBand} className="mt-1 text-sm">{BAND_LABEL[result.riskBand]}</Badge>
      </div>
      <p className="mt-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-700">{result.recommendation}</p>
      <h4 className="mt-5 text-xs font-bold uppercase tracking-wide text-slate-500">What drives this result</h4>
      <ul className="mt-2 space-y-2">
        {result.factors.map((f) => (
          <li key={f} className="flex gap-2 text-sm text-slate-700">
            <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full" style={{ background: color }} />
            {f}
          </li>
        ))}
      </ul>
      {result.suggestedSpecialization && (
        <Link to={`/doctors?specialization=${encodeURIComponent(result.suggestedSpecialization)}`}>
          <Button className="mt-6 w-full" size="lg" icon={CalendarPlus}>Book an {result.suggestedSpecialization.toLowerCase()} specialist</Button>
        </Link>
      )}
      <p className="mt-3 text-center text-xs text-slate-400">Model {result.modelVersion}</p>
    </div>
  );
}

function Gauge({ value, color }) {
  const radius = 80;
  const circumference = Math.PI * radius;
  return (
    <svg viewBox="0 0 200 115" className="w-56">
      <path d="M20 100 A80 80 0 0 1 180 100" fill="none" stroke="#e2e8f0" strokeWidth="16" strokeLinecap="round" />
      <path
        d="M20 100 A80 80 0 0 1 180 100"
        fill="none"
        stroke={color}
        strokeWidth="16"
        strokeLinecap="round"
        strokeDasharray={circumference}
        strokeDashoffset={circumference * (1 - Math.max(0.01, value))}
        style={{ transition: "stroke-dashoffset 0.8s ease" }}
      />
      <text x="100" y="92" textAnchor="middle" className="fill-slate-900" style={{ fontSize: 34, fontWeight: 800 }}>
        {percent(value)}
      </text>
    </svg>
  );
}
