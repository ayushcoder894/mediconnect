import { Activity, Bot, QrCode } from "lucide-react";
import Logo from "../../components/Logo";

/** Split-screen frame shared by the login and registration pages. */
export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-brand-700 via-brand-800 to-slate-900 p-12 lg:flex lg:flex-col lg:justify-between">
        <div className="bg-grid absolute inset-0 opacity-20" />
        <div className="absolute -bottom-24 -right-24 h-96 w-96 rounded-full bg-brand-400/30 blur-3xl" />
        <div className="relative"><Logo light /></div>
        <div className="relative">
          <h2 className="max-w-md text-4xl font-extrabold leading-tight text-white">Care that follows you from test to treatment.</h2>
          <ul className="mt-8 space-y-4">
            {[
              [Activity, "AI diabetes risk from your own lab reports"],
              [Bot, "MediBot answers insurance questions with sources"],
              [QrCode, "QR check-in and digital prescriptions"],
            ].map(([Icon, text]) => (
              <li key={text} className="flex items-center gap-3 text-brand-50">
                <span className="rounded-xl bg-white/10 p-2"><Icon className="h-5 w-5" /></span>
                {text}
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-sm text-brand-200">Portfolio project · Not for clinical use</p>
      </div>
      <div className="flex items-center justify-center px-4 py-12 sm:px-8">
        <div className="w-full max-w-md">
          <div className="lg:hidden"><Logo /></div>
          <h1 className="mt-8 text-3xl font-extrabold tracking-tight text-slate-900 lg:mt-0">{title}</h1>
          {subtitle && <p className="mt-2 text-slate-500">{subtitle}</p>}
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  );
}
