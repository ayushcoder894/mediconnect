import { Link } from "react-router-dom";
import clsx from "clsx";

export default function Logo({ light, to = "/" }) {
  return (
    <Link to={to} className="flex items-center gap-2.5">
      <svg viewBox="0 0 32 32" className="h-9 w-9" aria-hidden="true">
        <rect width="32" height="32" rx="10" fill="#0d9488" />
        <path
          d="M6 17h5l2.5-5 4 10 2.5-5H26"
          fill="none"
          stroke="#fff"
          strokeWidth="2.6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      <span className={clsx("text-lg font-extrabold tracking-tight", light ? "text-white" : "text-slate-900")}>
        Medi<span className={light ? "text-brand-200" : "text-brand-600"}>Connect</span>
      </span>
    </Link>
  );
}
