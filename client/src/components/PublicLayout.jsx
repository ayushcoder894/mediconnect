import { useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import clsx from "clsx";
import { Menu, X } from "lucide-react";
import Logo from "./Logo";
import { Button } from "./ui";
import { homeFor, useAuth } from "../auth/AuthContext";

const LINKS = [
  { to: "/doctors", label: "Find Doctors" },
  { to: "/lab-tests", label: "Lab Tests" },
  { to: "/blood-organs", label: "Blood & Organs" },
];

export default function PublicLayout() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-40 border-b border-slate-200/70 bg-white/80 backdrop-blur-lg">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6">
          <Logo />
          <nav className="hidden items-center gap-1 md:flex">
            {LINKS.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                className={({ isActive }) =>
                  clsx(
                    "rounded-lg px-3.5 py-2 text-sm font-semibold transition",
                    isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:text-slate-900",
                  )
                }
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
          <div className="hidden items-center gap-2 md:flex">
            {user ? (
              <Link to={homeFor(user.role)}>
                <Button>Go to dashboard</Button>
              </Link>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost">Log in</Button>
                </Link>
                <Link to="/register">
                  <Button>Get started</Button>
                </Link>
              </>
            )}
          </div>
          <button className="rounded-lg p-2 text-slate-600 md:hidden" onClick={() => setOpen(!open)} aria-label="Menu">
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
        {open && (
          <div className="border-t border-slate-100 bg-white px-4 py-3 md:hidden">
            {LINKS.map((l) => (
              <Link key={l.to} to={l.to} onClick={() => setOpen(false)} className="block rounded-lg px-3 py-2.5 font-semibold text-slate-700">
                {l.label}
              </Link>
            ))}
            <Link
              to={user ? homeFor(user.role) : "/login"}
              onClick={() => setOpen(false)}
              className="mt-2 block rounded-xl bg-brand-600 px-3 py-2.5 text-center font-semibold text-white"
            >
              {user ? "Go to dashboard" : "Log in"}
            </Link>
          </div>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-4">
          <div className="md:col-span-2">
            <Logo />
            <p className="mt-3 max-w-sm text-sm text-slate-500">
              Book doctors and lab tests, understand your health with AI, and keep every prescription and report in one place.
            </p>
          </div>
          <div>
            <h4 className="text-sm font-bold text-slate-900">Patients</h4>
            <ul className="mt-3 space-y-2 text-sm text-slate-500">
              <li><Link to="/doctors" className="hover:text-brand-700">Find a doctor</Link></li>
              <li><Link to="/lab-tests" className="hover:text-brand-700">Book a lab test</Link></li>
              <li><Link to="/blood-organs" className="hover:text-brand-700">Blood & organ availability</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-sm font-bold text-slate-900">Doctors</h4>
            <ul className="mt-3 space-y-2 text-sm text-slate-500">
              <li><Link to="/register?as=doctor" className="hover:text-brand-700">Join as a doctor</Link></li>
              <li><Link to="/login" className="hover:text-brand-700">Doctor login</Link></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-slate-100 py-4 text-center text-xs text-slate-400">
          MediConnect is a portfolio project. AI features give guidance, not medical diagnoses.
        </div>
      </footer>
    </div>
  );
}
