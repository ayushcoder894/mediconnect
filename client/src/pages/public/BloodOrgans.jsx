import { useState } from "react";
import clsx from "clsx";
import { Droplets, HeartPulse, Phone, SearchX } from "lucide-react";
import { query } from "../../api/client";
import { EmptyState, Spinner, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date } from "../../lib/format";

const GROUPS = ["A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"];

export default function BloodOrgans() {
  const [tab, setTab] = useState("blood");
  const [item, setItem] = useState("");
  const [city, setCity] = useState("");
  const { data: cities } = useApi("/api/hospitals/cities");
  const { data: organTypes } = useApi("/api/hospitals/organ-types");
  const path =
    tab === "blood"
      ? "/api/hospitals/blood" + query({ group: item, city })
      : "/api/hospitals/organs" + query({ organ: item, city });
  const { data: rows, loading } = useApi(path);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <div className="max-w-2xl">
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900">Blood & organ availability</h1>
        <p className="mt-2 text-slate-500">Live stock across partner hospitals, updated by hospital staff. Call ahead to confirm in emergencies.</p>
      </div>

      <div className="mt-8 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <Tabs
          value={tab}
          onChange={(t) => {
            setTab(t);
            setItem("");
          }}
          tabs={[
            { value: "blood", label: "Blood units" },
            { value: "organs", label: "Organs" },
          ]}
        />
        <div className="flex gap-2">
          <select className="input w-44" value={item} onChange={(e) => setItem(e.target.value)} aria-label={tab === "blood" ? "Blood group" : "Organ"}>
            <option value="">{tab === "blood" ? "All blood groups" : "All organs"}</option>
            {(tab === "blood" ? GROUPS : organTypes || []).map((g) => (
              <option key={g}>{g}</option>
            ))}
          </select>
          <select className="input w-40" value={city} onChange={(e) => setCity(e.target.value)} aria-label="City">
            <option value="">All cities</option>
            {cities?.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <Spinner />
      ) : !rows?.length ? (
        <div className="mt-8"><EmptyState icon={SearchX} title="Nothing found for this search" /></div>
      ) : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {rows.map((r) => (
            <StockCard key={r.id} row={r} blood={tab === "blood"} />
          ))}
        </div>
      )}
    </div>
  );
}

function StockCard({ row, blood }) {
  const level = row.quantity === 0 ? "none" : row.quantity < (blood ? 5 : 2) ? "low" : "ok";
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={clsx("flex h-12 w-12 items-center justify-center rounded-2xl text-lg font-extrabold", blood ? "bg-rose-50 text-rose-600" : "bg-violet-50 text-violet-600")}>
            {blood ? row.item : <HeartPulse className="h-6 w-6" />}
          </div>
          <div>
            <p className="font-bold text-slate-900">{blood ? `${row.item} blood` : row.item}</p>
            <p className="text-sm text-slate-500">{row.hospitalName}</p>
          </div>
        </div>
        <span
          className={clsx(
            "rounded-full px-2.5 py-1 text-xs font-bold",
            level === "ok" && "bg-emerald-50 text-emerald-700",
            level === "low" && "bg-amber-50 text-amber-700",
            level === "none" && "bg-slate-100 text-slate-500",
          )}
        >
          {level === "none" ? "Unavailable" : level === "low" ? "Low stock" : "Available"}
        </span>
      </div>
      <div className="mt-4 flex items-end justify-between">
        <div>
          <p className="text-3xl font-extrabold text-slate-900">{row.quantity}</p>
          <p className="text-xs text-slate-500">{blood ? "units" : "available"} · {row.city}</p>
        </div>
        <a href={`tel:${row.phone}`} className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand-700 hover:underline">
          <Phone className="h-4 w-4" /> {row.phone}
        </a>
      </div>
      <p className="mt-3 flex items-center gap-1.5 border-t border-slate-100 pt-3 text-xs text-slate-400">
        <Droplets className="h-3.5 w-3.5" /> Updated {date(row.updatedAt)}
      </p>
    </div>
  );
}
