import { FileDown, Pill } from "lucide-react";
import { openPdf } from "../../api/client";
import { Avatar, Button, Card, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date, dateTime } from "../../lib/format";

export default function Prescriptions() {
  const { data, loading } = useApi("/api/prescriptions/mine");
  if (loading) return <Spinner />;

  return (
    <div>
      <PageHeader title="Prescriptions" subtitle="Every prescription your doctors have issued, ready to download." />
      {!data?.length ? (
        <EmptyState icon={Pill} title="No prescriptions yet" text="After a visit, your doctor's prescription appears here." />
      ) : (
        <div className="grid gap-5 xl:grid-cols-2">
          {data.map((p) => (
            <Card key={p.id} className="flex flex-col p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <Avatar name={p.doctorName} size="sm" />
                  <div>
                    <p className="font-bold text-slate-900">{p.doctorName}</p>
                    <p className="text-xs text-slate-500">{p.specialization} · {dateTime(p.visitDate)}</p>
                  </div>
                </div>
                <Button size="sm" variant="secondary" icon={FileDown} onClick={() => openPdf(`/api/prescriptions/${p.id}/pdf`)}>PDF</Button>
              </div>
              <div className="mt-4 rounded-xl bg-brand-50 px-4 py-2.5">
                <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">Diagnosis</p>
                <p className="font-semibold text-slate-900">{p.diagnosis}</p>
              </div>
              <ul className="mt-4 flex-1 divide-y divide-slate-100">
                {p.items.map((i) => (
                  <li key={i.medicine} className="flex items-start justify-between gap-4 py-2.5">
                    <div>
                      <p className="font-semibold text-slate-800">{i.medicine} <span className="font-normal text-slate-500">{i.dosage}</span></p>
                      {i.instructions && <p className="text-xs text-slate-500">{i.instructions}</p>}
                    </div>
                    <p className="shrink-0 text-right text-xs text-slate-500">{i.frequency}<br />{i.durationDays} days</p>
                  </li>
                ))}
              </ul>
              {(p.notes || p.followUpDate) && (
                <div className="mt-3 border-t border-slate-100 pt-3 text-sm text-slate-600">
                  {p.notes && <p>{p.notes}</p>}
                  {p.followUpDate && <p className="mt-1 text-xs font-semibold text-brand-700">Follow-up on {date(p.followUpDate)}</p>}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
