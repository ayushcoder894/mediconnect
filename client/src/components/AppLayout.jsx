import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import clsx from "clsx";
import {
  Activity,
  Bot,
  CalendarDays,
  ClipboardList,
  Droplets,
  FlaskConical,
  LayoutDashboard,
  LogOut,
  Menu,
  Pill,
  ScanLine,
  ScrollText,
  Search,
  Stethoscope,
  UserRound,
  Users,
  X,
} from "lucide-react";
import Logo from "./Logo";
import { Avatar } from "./ui";
import { homeFor, useAuth } from "../auth/AuthContext";

const NAV = {
  PATIENT: [
    { to: "/app/patient", label: "Overview", icon: LayoutDashboard, end: true },
    { to: "/app/patient/appointments", label: "Appointments", icon: CalendarDays },
    { to: "/app/patient/labs", label: "Lab tests", icon: FlaskConical },
    { to: "/app/patient/risk", label: "Diabetes risk check", icon: Activity },
    { to: "/app/patient/assistant", label: "MediBot assistant", icon: Bot },
    { to: "/app/patient/prescriptions", label: "Prescriptions", icon: Pill },
    { to: "/app/patient/profile", label: "My profile", icon: UserRound },
    { divider: true },
    { to: "/doctors", label: "Find doctors", icon: Search },
  ],
  DOCTOR: [
    { to: "/app/doctor", label: "Schedule", icon: CalendarDays, end: true },
    { to: "/app/doctor/practice", label: "My practice", icon: Stethoscope },
  ],
  ADMIN: [
    { to: "/app/admin", label: "Analytics", icon: LayoutDashboard, end: true },
    { to: "/app/admin/check-in", label: "QR check-in", icon: ScanLine },
    { to: "/app/admin/lab", label: "Lab desk", icon: FlaskConical },
    { to: "/app/admin/people", label: "Doctors & users", icon: Users },
    { to: "/app/admin/stock", label: "Blood & organs", icon: Droplets },
    { to: "/app/admin/logs", label: "Audit & emails", icon: ScrollText },
  ],
};

const ROLE_LABEL = { PATIENT: "Patient", DOCTOR: "Doctor", ADMIN: "Hospital admin" };

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  useEffect(() => setOpen(false), [location.pathname]);

  const onLogout = async () => {
    await logout();
    navigate("/");
  };

  const sidebar = (
    <div className="flex h-full flex-col">
      <div className="flex h-16 items-center px-5">
        <Logo to={homeFor(user.role)} />
      </div>
      <nav className="flex-1 space-y-1 px-3 py-4">
        {NAV[user.role].map((item, i) =>
          item.divider ? (
            <div key={i} className="my-3 border-t border-slate-100" />
          ) : (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                clsx(
                  "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition",
                  isActive ? "bg-brand-600 text-white shadow-lift" : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
                )
              }
            >
              <item.icon className="h-[18px] w-[18px]" />
              {item.label}
            </NavLink>
          ),
        )}
      </nav>
      <div className="border-t border-slate-100 p-3">
        <div className="flex items-center gap-3 rounded-xl p-2">
          <Avatar name={user.fullName} size="sm" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-bold text-slate-900">{user.fullName}</p>
            <p className="truncate text-xs text-slate-500">{ROLE_LABEL[user.role]}</p>
          </div>
          <button onClick={onLogout} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-rose-600" title="Log out" aria-label="Log out">
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-slate-200/70 bg-white lg:block">{sidebar}</aside>

      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-72 bg-white shadow-xl">{sidebar}</aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200/70 bg-white/80 px-4 backdrop-blur-lg sm:px-6 lg:hidden">
          <button onClick={() => setOpen(true)} className="rounded-lg p-2 text-slate-600" aria-label="Open menu">
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
          <Logo to={homeFor(user.role)} />
          <Link to={homeFor(user.role)}>
            <Avatar name={user.fullName} size="sm" />
          </Link>
        </header>
        {user.role === "DOCTOR" && user.approved === false && (
          <div className="border-b border-amber-200 bg-amber-50 px-6 py-3 text-sm text-amber-800">
            <ClipboardList className="mr-2 inline h-4 w-4" />
            Your profile is waiting for admin approval. Patients can book you once it is approved.
          </div>
        )}
        <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
