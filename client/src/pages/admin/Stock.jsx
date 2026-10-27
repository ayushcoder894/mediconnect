import { useMemo, useState } from "react";
import clsx from "clsx";
import { Check } from "lucide-react";
import { api } from "../../api/client";
import { useToast } from "../../components/Toast";
import { Card, PageHeader, Spinner, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";

export default function Stock() {
  const [tab, setTab] = useState("blood");
  const { data, loading, reload } = useApi(tab === "blood" ? "/api/hospitals/blood" : "/api/hospitals/organs");

  const byHospital = useMemo(() => {
    const map = new Map();
    (data || []).forEach((r) => {
      if (!map.has(r.hospitalName)) map.set(r.hospitalName, { city: r.city, rows: [] });
      map.get(r.hospitalName).rows.push(r);
    });
    map.forEach((v) => v.rows.sort((a, b) => a.item.localeCompare(b.item)));
    return [...map.entries()].sort();
  }, [data]);

  return (
    <div>
      <PageHeader title="Blood & organ stock" subtitle="Changes appear instantly on the public availability page and are recorded in the audit log." />
      <Tabs value={tab} onChange={setTab} tabs={[{ value: "blood", label: "Blood units" }, { value: "organs", label: "Organs" }]} />
      {loading ? (
        <Spinner />
      ) : (
        <div className="mt-5 grid gap-5 lg:grid-cols-2">
          {byHospital.map(([name, { city, rows }]) => (
            <Card key={name} className="p-5">
              <h2 className="font-bold text-slate-900">{name}</h2>
              <p className="text-xs text-slate-500">{city}</p>
              <div className={clsx("mt-4 grid gap-2", tab === "blood" ? "grid-cols-4" : "grid-cols-2 sm:grid-cols-3")}>
                {rows.map((r) => (
                  <StockInput key={r.id} row={r} kind={tab} onSaved={reload} />
                ))}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

function StockInput({ row, kind, onSaved }) {
  const notify = useToast();
  const [value, setValue] = useState(row.quantity);
  const [saving, setSaving] = useState(false);
  const dirty = Number(value) !== row.quantity;
  const low = row.quantity < (kind === "blood" ? 5 : 2);

  const save = async () => {
    setSaving(true);
    try {
      await api.put(`/api/admin/${kind === "blood" ? "blood" : "organs"}/${row.id}`, { quantity: Number(value) });
      notify(`${row.item} at ${row.hospitalName} set to ${value}.`);
      onSaved();
    } catch (e) {
      notify(e.message, "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className={clsx("rounded-xl border p-2.5", low ? "border-amber-200 bg-amber-50/60" : "border-slate-100 bg-slate-50/60")}>
      <p className="text-xs font-bold text-slate-600">{row.item}</p>
      <div className="mt-1 flex items-center gap-1">
        <input
          type="number"
          min="0"
          className="w-full rounded-lg border border-slate-200 bg-white px-2 py-1 text-sm font-bold text-slate-900 focus:border-brand-500 focus:outline-none"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && dirty && save()}
          aria-label={`${row.item} quantity`}
        />
        {dirty && (
          <button onClick={save} disabled={saving} className="rounded-lg bg-brand-600 p-1.5 text-white" aria-label="Save">
            <Check className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </div>
  );
}
