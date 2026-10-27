import { useState } from "react";
import { FileDown, FlaskConical, TestTube2 } from "lucide-react";
import { api, openPdf, query } from "../../api/client";
import { useToast } from "../../components/Toast";
import { Badge, Button, Card, EmptyState, ErrorNote, Field, Modal, PageHeader, Spinner, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { dateTime } from "../../lib/format";

const FILTERS = [
  { value: "BOOKED", label: "To collect" },
  { value: "SAMPLE_COLLECTED", label: "Awaiting results" },
  { value: "REPORT_READY", label: "Reported" },
  { value: "", label: "All" },
];

export default function LabDesk() {
  const notify = useToast();
  const [status, setStatus] = useState("BOOKED");
  const { data, loading, reload } = useApi("/api/admin/lab-bookings" + query({ status }));
  const [entry, setEntry] = useState(null);

  const collect = async (b) => {
    try {
      await api.patch(`/api/admin/lab-bookings/${b.id}/status`, { status: "SAMPLE_COLLECTED" });
      notify(`Sample collected for ${b.patientName}.`);
      reload();
    } catch (e) {
      notify(e.message, "error");
    }
  };

  return (
    <div>
      <PageHeader title="Lab desk" subtitle="Collect samples and publish results. Patients are emailed when a report is ready." />
      <Tabs value={status} onChange={setStatus} tabs={FILTERS} />
      <div className="mt-5">
        {loading ? (
          <Spinner />
        ) : !data?.length ? (
          <EmptyState icon={FlaskConical} title="Nothing here" text="No bookings in this state." />
        ) : (
          <Card className="overflow-x-auto">
            <table className="w-full min-w-[760px]">
              <thead className="border-b border-slate-100 bg-slate-50">
                <tr>
                  <th className="table-head">Patient</th>
                  <th className="table-head">Test</th>
                  <th className="table-head">Scheduled</th>
                  <th className="table-head">Ordered by</th>
                  <th className="table-head">Status</th>
                  <th className="table-head" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((b) => (
                  <tr key={b.id}>
                    <td className="table-cell font-semibold">{b.patientName}</td>
                    <td className="table-cell">{b.testName}<p className="text-xs text-slate-400">{b.category}</p></td>
                    <td className="table-cell">{dateTime(b.scheduledAt)}</td>
                    <td className="table-cell text-slate-500">{b.orderedBy || "Self"}</td>
                    <td className="table-cell"><Badge status={b.status} /></td>
                    <td className="table-cell text-right">
                      <div className="flex justify-end gap-2">
                        {b.status === "BOOKED" && <Button size="sm" variant="secondary" icon={TestTube2} onClick={() => collect(b)}>Collected</Button>}
                        {["BOOKED", "SAMPLE_COLLECTED"].includes(b.status) && <Button size="sm" onClick={() => setEntry(b)}>Enter results</Button>}
                        {b.status === "REPORT_READY" && <Button size="sm" variant="secondary" icon={FileDown} onClick={() => openPdf(`/api/lab-bookings/${b.id}/report.pdf`)}>Report</Button>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}
      </div>
      <ResultsModal booking={entry} onClose={() => setEntry(null)} onDone={reload} />
    </div>
  );
}

function ResultsModal({ booking, onClose, onDone }) {
  const notify = useToast();
  const [values, setValues] = useState({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const params = booking?.parameters?.length ? booking.parameters : [{ name: "Finding", unit: "", referenceRange: "" }];

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post(`/api/admin/lab-bookings/${booking.id}/results`, { values });
      notify(`Report published for ${booking.patientName}.`);
      setValues({});
      onClose();
      onDone();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={!!booking} onClose={onClose} title={booking ? `${booking.testName} · ${booking.patientName}` : ""}>
      <form onSubmit={save} className="space-y-4">
        {params.map((p) => (
          <Field key={p.name} label={`${p.name}${p.unit ? ` (${p.unit})` : ""}`} hint={p.referenceRange ? `Reference ${p.referenceRange}` : undefined}>
            {p.name === "Finding" ? (
              <textarea required className="input min-h-[90px]" value={values[p.name] || ""} onChange={(e) => setValues({ ...values, [p.name]: e.target.value })} />
            ) : (
              <input required inputMode="decimal" className="input" value={values[p.name] || ""} onChange={(e) => setValues({ ...values, [p.name]: e.target.value })} />
            )}
          </Field>
        ))}
        <ErrorNote error={error} />
        <Button type="submit" className="w-full" loading={saving}>Publish report</Button>
      </form>
    </Modal>
  );
}
