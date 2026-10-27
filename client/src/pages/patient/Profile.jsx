import { useEffect, useState } from "react";
import { Save } from "lucide-react";
import { api } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import { useToast } from "../../components/Toast";
import { Button, Card, ErrorNote, Field, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";

export default function Profile() {
  const notify = useToast();
  const { refresh } = useAuth();
  const { data, loading } = useApi("/api/patients/me");
  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (data) setForm({ ...data });
  }, [data]);

  if (loading || !form) return <Spinner />;

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value });
  const bmi = form.heightCm && form.weightKg ? (form.weightKg / (form.heightCm / 100) ** 2).toFixed(1) : null;

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setErrors({});
    const blank = (v) => (v === "" ? null : v);
    try {
      await api.put("/api/patients/me", {
        fullName: form.fullName,
        phone: blank(form.phone),
        dateOfBirth: blank(form.dateOfBirth),
        gender: blank(form.gender),
        bloodGroup: blank(form.bloodGroup),
        heightCm: blank(form.heightCm),
        weightKg: blank(form.weightKg),
        address: blank(form.address),
        medicalHistory: blank(form.medicalHistory),
        allergies: blank(form.allergies),
        smokingHistory: form.smokingHistory || "No Info",
        hypertension: form.hypertension,
        heartDisease: form.heartDisease,
      });
      notify("Profile saved.");
      refresh();
    } catch (err) {
      setError(err.message);
      setErrors(err.fields || {});
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <PageHeader title="My profile" subtitle="Doctors see this before your visit. It also prefills your risk check." />
      <form onSubmit={save} className="grid gap-6 lg:grid-cols-3">
        <Card className="space-y-4 p-6 lg:col-span-2">
          <h2 className="font-bold text-slate-900">Personal details</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Full name" error={errors.fullName}><input required className="input" value={form.fullName || ""} onChange={set("fullName")} /></Field>
            <Field label="Email"><input className="input" value={form.email} disabled /></Field>
            <Field label="Phone" error={errors.phone}><input className="input" value={form.phone || ""} onChange={set("phone")} /></Field>
            <Field label="Date of birth" error={errors.dateOfBirth}><input type="date" className="input" value={form.dateOfBirth || ""} onChange={set("dateOfBirth")} /></Field>
            <Field label="Gender">
              <select className="input" value={form.gender || ""} onChange={set("gender")}>
                <option value="">Not set</option><option>Female</option><option>Male</option><option>Other</option>
              </select>
            </Field>
            <Field label="Blood group">
              <select className="input" value={form.bloodGroup || ""} onChange={set("bloodGroup")}>
                <option value="">Not set</option>
                {["A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"].map((g) => <option key={g}>{g}</option>)}
              </select>
            </Field>
          </div>
          <Field label="Address" error={errors.address}><input className="input" value={form.address || ""} onChange={set("address")} /></Field>
          <h2 className="pt-2 font-bold text-slate-900">Medical background</h2>
          <Field label="Medical history" error={errors.medicalHistory}>
            <textarea className="input min-h-[90px]" value={form.medicalHistory || ""} onChange={set("medicalHistory")} placeholder="Conditions, surgeries, family history" />
          </Field>
          <Field label="Allergies" error={errors.allergies}><input className="input" value={form.allergies || ""} onChange={set("allergies")} placeholder="e.g. Penicillin" /></Field>
        </Card>

        <div className="space-y-6">
          <Card className="space-y-4 p-6">
            <h2 className="font-bold text-slate-900">Body & lifestyle</h2>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Height (cm)" error={errors.heightCm}><input type="number" step="0.1" className="input" value={form.heightCm || ""} onChange={set("heightCm")} /></Field>
              <Field label="Weight (kg)" error={errors.weightKg}><input type="number" step="0.1" className="input" value={form.weightKg || ""} onChange={set("weightKg")} /></Field>
            </div>
            {bmi && (
              <div className="rounded-xl bg-brand-50 p-3 text-sm">
                BMI <b className="text-lg text-slate-900">{bmi}</b>{" "}
                <span className="text-slate-500">{bmi < 18.5 ? "Underweight" : bmi < 25 ? "Healthy" : bmi < 30 ? "Overweight" : "Obese"}</span>
              </div>
            )}
            <Field label="Smoking">
              <select className="input" value={form.smokingHistory || "No Info"} onChange={set("smokingHistory")}>
                <option value="never">Never</option><option value="former">Former</option><option value="current">Current</option>
                <option value="not current">Not current</option><option value="ever">Ever</option><option value="No Info">Prefer not to say</option>
              </select>
            </Field>
            <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
              <input type="checkbox" className="h-4 w-4 accent-brand-600" checked={form.hypertension} onChange={set("hypertension")} /> High blood pressure
            </label>
            <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
              <input type="checkbox" className="h-4 w-4 accent-brand-600" checked={form.heartDisease} onChange={set("heartDisease")} /> Heart disease
            </label>
          </Card>
          <ErrorNote error={error} />
          <Button type="submit" size="lg" className="w-full" icon={Save} loading={saving}>Save profile</Button>
        </div>
      </form>
    </div>
  );
}
