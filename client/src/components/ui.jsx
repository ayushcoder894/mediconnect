import { useEffect } from "react";
import clsx from "clsx";
import { Loader2, Star, X } from "lucide-react";
import { initials, titleCase } from "../lib/format";

export function Button({ variant = "primary", size = "md", loading, className, children, icon: Icon, ...props }) {
  const styles = {
    primary: "bg-brand-600 text-white hover:bg-brand-700 shadow-sm shadow-brand-600/20",
    secondary: "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50 hover:border-slate-300",
    ghost: "text-slate-600 hover:bg-slate-100",
    danger: "bg-white text-rose-600 border border-rose-200 hover:bg-rose-50",
    dark: "bg-slate-900 text-white hover:bg-slate-800",
  };
  const sizes = { sm: "px-3 py-1.5 text-xs", md: "px-4 py-2.5 text-sm", lg: "px-6 py-3 text-base" };
  return (
    <button
      className={clsx(
        "inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition focus:outline-none focus-visible:ring-4 focus-visible:ring-brand-500/25 disabled:cursor-not-allowed disabled:opacity-60",
        styles[variant],
        sizes[size],
        className,
      )}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : Icon ? <Icon className="h-4 w-4" /> : null}
      {children}
    </button>
  );
}

export function Card({ className, children, ...props }) {
  return (
    <div className={clsx("card", className)} {...props}>
      {children}
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function Field({ label, error, hint, children, className }) {
  return (
    <label className={clsx("block", className)}>
      {label && <span className="label">{label}</span>}
      {children}
      {error ? (
        <span className="mt-1 block text-xs font-medium text-rose-600">{error}</span>
      ) : hint ? (
        <span className="mt-1 block text-xs text-slate-400">{hint}</span>
      ) : null}
    </label>
  );
}

export function Spinner({ className }) {
  return (
    <div className={clsx("flex items-center justify-center py-16", className)}>
      <Loader2 className="h-7 w-7 animate-spin text-brand-600" />
    </div>
  );
}

export function EmptyState({ icon: Icon, title, text, action }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white/60 px-6 py-14 text-center">
      {Icon && (
        <div className="mb-3 rounded-2xl bg-brand-50 p-3 text-brand-600">
          <Icon className="h-6 w-6" />
        </div>
      )}
      <h3 className="font-semibold text-slate-800">{title}</h3>
      {text && <p className="mt-1 max-w-sm text-sm text-slate-500">{text}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function StatCard({ icon: Icon, label, value, hint, tone = "brand" }) {
  const tones = {
    brand: "bg-brand-50 text-brand-600",
    blue: "bg-sky-50 text-sky-600",
    amber: "bg-amber-50 text-amber-600",
    rose: "bg-rose-50 text-rose-600",
    violet: "bg-violet-50 text-violet-600",
    slate: "bg-slate-100 text-slate-600",
  };
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
          {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
        </div>
        {Icon && (
          <div className={clsx("rounded-xl p-2.5", tones[tone])}>
            <Icon className="h-5 w-5" />
          </div>
        )}
      </div>
    </Card>
  );
}

const STATUS_STYLES = {
  BOOKED: "bg-sky-50 text-sky-700 ring-sky-200",
  CONFIRMED: "bg-brand-50 text-brand-700 ring-brand-200",
  CHECKED_IN: "bg-violet-50 text-violet-700 ring-violet-200",
  COMPLETED: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  CANCELLED: "bg-slate-100 text-slate-500 ring-slate-200",
  NO_SHOW: "bg-rose-50 text-rose-700 ring-rose-200",
  SAMPLE_COLLECTED: "bg-amber-50 text-amber-700 ring-amber-200",
  REPORT_READY: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  PENDING: "bg-amber-50 text-amber-700 ring-amber-200",
  SENDING: "bg-sky-50 text-sky-700 ring-sky-200",
  SENT: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  FAILED: "bg-rose-50 text-rose-700 ring-rose-200",
  LOW: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  MODERATE: "bg-amber-50 text-amber-700 ring-amber-200",
  HIGH: "bg-orange-50 text-orange-700 ring-orange-200",
  VERY_HIGH: "bg-rose-50 text-rose-700 ring-rose-200",
  NORMAL: "bg-emerald-50 text-emerald-700 ring-emerald-200",
};

export function Badge({ status, children, className }) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset",
        STATUS_STYLES[status] || "bg-slate-100 text-slate-600 ring-slate-200",
        className,
      )}
    >
      {children || titleCase(status)}
    </span>
  );
}

const AVATAR_COLORS = [
  "from-brand-400 to-brand-600",
  "from-sky-400 to-indigo-500",
  "from-violet-400 to-fuchsia-500",
  "from-amber-400 to-orange-500",
  "from-rose-400 to-pink-500",
  "from-emerald-400 to-teal-600",
];

export function Avatar({ name, size = "md", className }) {
  const sizes = { sm: "h-9 w-9 text-xs", md: "h-12 w-12 text-sm", lg: "h-20 w-20 text-2xl" };
  const color = AVATAR_COLORS[[...(name || "")].reduce((a, c) => a + c.charCodeAt(0), 0) % AVATAR_COLORS.length];
  return (
    <div
      className={clsx(
        "flex shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br font-bold text-white",
        color,
        sizes[size],
        className,
      )}
    >
      {initials(name)}
    </div>
  );
}

export function Stars({ value = 0, size = "h-4 w-4", onChange }) {
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          disabled={!onChange}
          onClick={() => onChange?.(n)}
          className={clsx(!onChange && "cursor-default")}
          aria-label={`${n} star${n > 1 ? "s" : ""}`}
        >
          <Star
            className={clsx(size, n <= Math.round(value) ? "fill-amber-400 text-amber-400" : "fill-slate-200 text-slate-200")}
          />
        </button>
      ))}
    </div>
  );
}

export function Modal({ open, onClose, title, children, wide }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/40 p-0 backdrop-blur-sm sm:items-center sm:p-4" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        className={clsx(
          "max-h-[92vh] w-full animate-fade-up overflow-y-auto rounded-t-2xl bg-white shadow-2xl sm:rounded-2xl",
          wide ? "sm:max-w-3xl" : "sm:max-w-lg",
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-100 bg-white px-5 py-4">
          <h2 className="text-lg font-bold text-slate-900">{title}</h2>
          <button onClick={onClose} className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600" aria-label="Close">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}

export function Tabs({ tabs, value, onChange }) {
  return (
    <div className="inline-flex rounded-xl bg-slate-100 p-1">
      {tabs.map((t) => (
        <button
          key={t.value}
          onClick={() => onChange(t.value)}
          className={clsx(
            "rounded-lg px-3.5 py-1.5 text-sm font-semibold transition",
            value === t.value ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-700",
          )}
        >
          {t.label}
          {t.count !== undefined && <span className="ml-1.5 text-xs text-slate-400">{t.count}</span>}
        </button>
      ))}
    </div>
  );
}

export function ErrorNote({ error }) {
  if (!error) return null;
  return <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div>;
}
