const inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

export const money = (n) => (n === null || n === undefined ? "-" : inr.format(Number(n)));

export const compactMoney = (n) => {
  const v = Number(n || 0);
  if (v >= 1e7) return `₹${(v / 1e7).toFixed(1)}Cr`;
  if (v >= 1e5) return `₹${(v / 1e5).toFixed(1)}L`;
  if (v >= 1e3) return `₹${(v / 1e3).toFixed(1)}K`;
  return inr.format(v);
};

export const date = (iso) =>
  iso ? new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "-";

export const dateTime = (iso) =>
  iso
    ? new Date(iso).toLocaleString("en-IN", {
        weekday: "short",
        day: "numeric",
        month: "short",
        hour: "numeric",
        minute: "2-digit",
      })
    : "-";

export const time = (iso) =>
  iso ? new Date(iso).toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit" }) : "-";

export const initials = (name = "") =>
  name
    .replace(/^Dr\.?\s+/i, "")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");

/** yyyy-mm-dd in local time (toISOString would shift to UTC). */
export const isoDate = (d) => {
  const x = new Date(d);
  return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, "0")}-${String(x.getDate()).padStart(2, "0")}`;
};

export const addDays = (d, n) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};

export const percent = (p) => (p >= 0.995 ? ">99%" : p < 0.005 ? "<1%" : `${Math.round(p * 100)}%`);

export const titleCase = (s = "") =>
  s
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());

export const greeting = () => {
  const h = new Date().getHours();
  return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
};
