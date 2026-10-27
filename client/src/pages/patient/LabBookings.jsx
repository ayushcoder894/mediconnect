import { Link } from "react-router-dom";
import clsx from "clsx";
import { Check, FileDown, FlaskConical, XCircle } from "lucide-react";
import { api, openPdf } from "../../api/client";
import { useToast } from "../../components/Toast";
import { Badge, Button, Card, EmptyState, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { dateTime, money } from "../../lib/format";

const STEPS = ["BOOKED", "SAMPLE_COLLECTED", "REPORT_READY"];
const STEP_LABEL = { BOOKED: "Booked", SAMPLE_COLLECTED: "Sample collected", REPORT_READY: "Report ready" };

export default function MyLabTests() {
  const notify = useToast();
  const { data, loading, reload } = useApi("/api/lab-bookings/mine");

  const cancel = async (b) => {
    if (!window.confirm(`Cancel ${b.testName}?`)) return;
    try {
      await api.post(`/api/lab-bookings/${b.id}/cancel`);
      notify("Lab booking cancelled.");
      reload();
    } catch (e) {
      notify(e.message, "error");
    }
  };

  if (loading) return <Spinner />;

  return (
    <div>
      <PageHeader title="Lab tests" subtitle="Track sample collection and view your reports." actions={<Link to="/lab-tests"><Button>Book a test</Button></Link>} />
      {!data?.length ? (
        <EmptyState icon={FlaskConical} title="No lab tests yet" action={<Link to="/lab-tests"><Button>Browse tests</Button></Link>} />
      ) : (
        <div className="space-y-4">
          {data.map((b) => (
            <Card key={b.id} className="p-5">
              <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-bold text-slate-900">{b.testName}</h3>
                    {b.status === "CANCELLED" && <Badge status="CANCELLED" />}
                  </div>
                  <p className="text-sm text-slate-500">
                    {dateTime(b.scheduledAt)} · {money(b.price)}
                    {b.orderedBy && <> · Ordered by <b className="text-slate-700">{b.orderedBy}</b></>}
                  </p>
                </div>
                <div className="flex gap-2">
                  {b.status === "REPORT_READY" && (
                    <Button size="sm" icon={FileDown} onClick={() => openPdf(`/api/lab-bookings/${b.id}/report.pdf`)}>Report PDF</Button>
                  )}
                  {b.status === "BOOKED" && <Button size="sm" variant="danger" icon={XCircle} onClick={() => cancel(b)}>Cancel</Button>}
                </div>
              </div>

              {b.status !== "CANCELLED" && <Progress status={b.status} />}

              {b.results.length > 0 && (
                <div className="mt-5 overflow-x-auto rounded-xl border border-slate-100">
                  <table className="w-full min-w-[480px]">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="table-head">Parameter</th>
                        <th className="table-head">Result</th>
                        <th className="table-head">Reference</th>
                        <th className="table-head">Flag</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {b.results.map((r) => (
                        <tr key={r.parameter}>
                          <td className="table-cell font-semibold">{r.parameter}</td>
                          <td className={clsx("table-cell font-bold", r.flag === "HIGH" && "text-rose-600", r.flag === "LOW" && "text-amber-600")}>
                            {r.value} <span className="font-normal text-slate-400">{r.unit}</span>
                          </td>
                          <td className="table-cell text-slate-500">{r.referenceRange || "-"}</td>
                          <td className="table-cell">{r.flag ? <Badge status={r.flag} /> : "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              {b.results.some((r) => r.parameter === "HbA1c" || r.parameter === "Blood Glucose") && (
                <Link to="/app/patient/risk" className="mt-3 inline-block text-sm font-semibold text-brand-700 hover:underline">
                  Use these results in your diabetes risk check →
                </Link>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

function Progress({ status }) {
  const current = STEPS.indexOf(status);
  return (
    <div className="mt-5 flex items-center">
      {STEPS.map((s, i) => (
        <div key={s} className="flex flex-1 items-center last:flex-none">
          <div className="flex items-center gap-2">
            <div
              className={clsx(
                "flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold",
                i <= current ? "bg-brand-600 text-white" : "bg-slate-100 text-slate-400",
              )}
            >
              {i < current || status === "REPORT_READY" ? <Check className="h-4 w-4" /> : i + 1}
            </div>
            <span className={clsx("hidden text-xs font-semibold sm:inline", i <= current ? "text-slate-800" : "text-slate-400")}>
              {STEP_LABEL[s]}
            </span>
          </div>
          {i < STEPS.length - 1 && <div className={clsx("mx-3 h-0.5 flex-1 rounded", i < current ? "bg-brand-500" : "bg-slate-200")} />}
        </div>
      ))}
    </div>
  );
}
