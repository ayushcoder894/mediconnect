import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ShieldCheck, Stethoscope, UserRound } from "lucide-react";
import { homeFor, useAuth } from "../../auth/AuthContext";
import { HOSTED } from "../../api/client";
import { Button, ErrorNote, Field } from "../../components/ui";
import AuthShell from "./AuthShell";

const DEMO = [
  { label: "Patient", email: "patient@mediconnect.dev", icon: UserRound },
  { label: "Doctor", email: "sameer.khan@mediconnect.dev", icon: Stethoscope },
  { label: "Admin", email: "admin@mediconnect.dev", icon: ShieldCheck },
];
const DEMO_PASSWORD = "Demo@1234";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const submit = async (e, creds) => {
    e?.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const me = await login(creds?.email ?? email, creds?.password ?? password);
      const next = params.get("next");
      navigate(next && next.startsWith("/") ? next : homeFor(me.role), { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Welcome back" subtitle="Log in to manage your appointments, reports and prescriptions.">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email">
          <input type="email" required autoComplete="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <Field label="Password">
          <input type="password" required autoComplete="current-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <ErrorNote error={error} />
        <Button type="submit" size="lg" className="w-full" loading={loading}>Log in</Button>
        {HOSTED && loading && (
          <p className="text-center text-xs text-slate-500">Waking up the free server, this can take up to a minute…</p>
        )}
      </form>

      <div className="mt-8 rounded-2xl border border-dashed border-brand-200 bg-brand-50/50 p-4">
        <p className="text-xs font-bold uppercase tracking-wide text-brand-700">Try a demo account</p>
        <div className="mt-3 grid grid-cols-3 gap-2">
          {DEMO.map((d) => (
            <button
              key={d.label}
              type="button"
              disabled={loading}
              onClick={(e) => submit(e, { email: d.email, password: DEMO_PASSWORD })}
              className="flex flex-col items-center gap-1.5 rounded-xl border border-slate-200 bg-white p-3 text-sm font-semibold text-slate-700 transition hover:border-brand-400 hover:text-brand-700"
            >
              <d.icon className="h-5 w-5" />
              {d.label}
            </button>
          ))}
        </div>
      </div>

      <p className="mt-6 text-center text-sm text-slate-500">
        New to MediConnect? <Link to="/register" className="font-semibold text-brand-700 hover:underline">Create an account</Link>
      </p>
    </AuthShell>
  );
}
