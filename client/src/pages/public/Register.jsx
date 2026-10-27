import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { homeFor, useAuth } from "../../auth/AuthContext";
import { Button, ErrorNote, Field, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import AuthShell from "./AuthShell";

const SPECIALIZATIONS = [
  "Cardiology", "Dermatology", "Endocrinology", "ENT", "General Medicine", "Gynecology", "Nephrology",
  "Neurology", "Ophthalmology", "Orthopedics", "Pediatrics", "Psychiatry",
];

export default function Register() {
  const [params] = useSearchParams();
  const [kind, setKind] = useState(params.get("as") === "doctor" ? "doctor" : "patient");
  const { register } = useAuth();
  const navigate = useNavigate();
  const { data: hospitals } = useApi("/api/hospitals");
  const [form, setForm] = useState({ gender: "", specialization: "General Medicine", experienceYears: 0, consultationFee: 500 });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setErrors({});
    const common = { fullName: form.fullName, email: form.email, password: form.password, phone: form.phone || null };
    const body =
      kind === "patient"
        ? { ...common, dateOfBirth: form.dateOfBirth || null, gender: form.gender || null }
        : {
            ...common,
            specialization: form.specialization,
            qualification: form.qualification,
            experienceYears: Number(form.experienceYears),
            consultationFee: Number(form.consultationFee),
            hospitalId: form.hospitalId ? Number(form.hospitalId) : null,
          };
    try {
      const me = await register(kind, body);
      navigate(homeFor(me.role), { replace: true });
    } catch (err) {
      setError(err.message);
      setErrors(err.fields || {});
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      title="Create your account"
      subtitle={kind === "patient" ? "Book doctors and tests, and keep your health records in one place." : "Join the network. An admin verifies new doctors before they appear in search."}
    >
      <Tabs value={kind} onChange={setKind} tabs={[{ value: "patient", label: "I'm a patient" }, { value: "doctor", label: "I'm a doctor" }]} />
      <form onSubmit={submit} className="mt-6 space-y-4">
        <Field label="Full name" error={errors.fullName}>
          <input required className="input" value={form.fullName || ""} onChange={set("fullName")} placeholder={kind === "doctor" ? "Dr. Asha Rao" : "Asha Rao"} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Email" error={errors.email}>
            <input required type="email" className="input" value={form.email || ""} onChange={set("email")} />
          </Field>
          <Field label="Phone" error={errors.phone}>
            <input className="input" value={form.phone || ""} onChange={set("phone")} placeholder="+91 98765 43210" />
          </Field>
        </div>
        <Field label="Password" error={errors.password} hint="At least 8 characters with a letter and a number">
          <input required type="password" autoComplete="new-password" className="input" value={form.password || ""} onChange={set("password")} />
        </Field>

        {kind === "patient" ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Date of birth" error={errors.dateOfBirth}>
              <input type="date" className="input" value={form.dateOfBirth || ""} onChange={set("dateOfBirth")} />
            </Field>
            <Field label="Gender" error={errors.gender}>
              <select className="input" value={form.gender} onChange={set("gender")}>
                <option value="">Prefer not to say</option>
                <option>Female</option>
                <option>Male</option>
                <option>Other</option>
              </select>
            </Field>
          </div>
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Specialization" error={errors.specialization}>
                <select className="input" value={form.specialization} onChange={set("specialization")}>
                  {SPECIALIZATIONS.map((s) => <option key={s}>{s}</option>)}
                </select>
              </Field>
              <Field label="Qualification" error={errors.qualification}>
                <input required className="input" value={form.qualification || ""} onChange={set("qualification")} placeholder="MBBS, MD" />
              </Field>
              <Field label="Experience (years)" error={errors.experienceYears}>
                <input type="number" min="0" max="60" className="input" value={form.experienceYears} onChange={set("experienceYears")} />
              </Field>
              <Field label="Consultation fee (₹)" error={errors.consultationFee}>
                <input type="number" min="0" className="input" value={form.consultationFee} onChange={set("consultationFee")} />
              </Field>
            </div>
            <Field label="Hospital">
              <select className="input" value={form.hospitalId || ""} onChange={set("hospitalId")}>
                <option value="">Independent practice</option>
                {hospitals?.map((h) => <option key={h.id} value={h.id}>{h.name}, {h.city}</option>)}
              </select>
            </Field>
          </>
        )}

        <ErrorNote error={error} />
        <Button type="submit" size="lg" className="w-full" loading={loading}>Create account</Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        Already have an account? <Link to="/login" className="font-semibold text-brand-700 hover:underline">Log in</Link>
      </p>
    </AuthShell>
  );
}
