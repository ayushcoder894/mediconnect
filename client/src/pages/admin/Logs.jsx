import { useState } from "react";
import { Badge, Card, PageHeader, Spinner, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { dateTime, titleCase } from "../../lib/format";

export default function Logs() {
  const [tab, setTab] = useState("audit");
  return (
    <div>
      <PageHeader title="Audit & email logs" subtitle="Audit and delivery logs live in MongoDB; the email outbox lives in MySQL." />
      <Tabs
        value={tab}
        onChange={setTab}
        tabs={[
          { value: "audit", label: "Audit trail" },
          { value: "outbox", label: "Email outbox" },
          { value: "deliveries", label: "Delivery attempts" },
        ]}
      />
      <div className="mt-5">{tab === "audit" ? <Audit /> : tab === "outbox" ? <Outbox /> : <Deliveries />}</div>
    </div>
  );
}

function Audit() {
  const { data, loading } = useApi("/api/admin/audit-logs?limit=150");
  if (loading) return <Spinner />;
  return (
    <Card className="overflow-x-auto">
      <table className="w-full min-w-[760px]">
        <thead className="border-b border-slate-100 bg-slate-50">
          <tr>
            <th className="table-head">When</th>
            <th className="table-head">Who</th>
            <th className="table-head">Action</th>
            <th className="table-head">Target</th>
            <th className="table-head">Details</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data?.map((a) => (
            <tr key={a.id}>
              <td className="table-cell whitespace-nowrap text-slate-500">{dateTime(a.at)}</td>
              <td className="table-cell">
                <p className="font-semibold">{a.actorEmail || "System"}</p>
                <p className="text-xs text-slate-400">{titleCase(a.actorRole)}</p>
              </td>
              <td className="table-cell"><span className="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-xs font-semibold text-slate-700">{a.action}</span></td>
              <td className="table-cell text-slate-500">{a.entityType} #{a.entityId}</td>
              <td className="table-cell text-slate-500">{a.details || "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}

function Outbox() {
  const { data, loading } = useApi("/api/admin/notifications/outbox");
  if (loading) return <Spinner />;
  return (
    <Card className="overflow-x-auto">
      <table className="w-full min-w-[760px]">
        <thead className="border-b border-slate-100 bg-slate-50">
          <tr>
            <th className="table-head">Queued</th>
            <th className="table-head">To</th>
            <th className="table-head">Subject</th>
            <th className="table-head">Attempts</th>
            <th className="table-head">Status</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data?.map((m) => (
            <tr key={m.id}>
              <td className="table-cell whitespace-nowrap text-slate-500">{dateTime(m.createdAt)}</td>
              <td className="table-cell">{m.recipient}</td>
              <td className="table-cell">{m.subject}{m.lastError && <p className="text-xs text-rose-600">{m.lastError}</p>}</td>
              <td className="table-cell">{m.attempts}</td>
              <td className="table-cell"><Badge status={m.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}

function Deliveries() {
  const { data, loading } = useApi("/api/admin/notifications/logs");
  if (loading) return <Spinner />;
  return (
    <Card className="overflow-x-auto">
      <table className="w-full min-w-[640px]">
        <thead className="border-b border-slate-100 bg-slate-50">
          <tr>
            <th className="table-head">When</th>
            <th className="table-head">To</th>
            <th className="table-head">Subject</th>
            <th className="table-head">Attempt</th>
            <th className="table-head">Outcome</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {data?.map((l) => (
            <tr key={l.id}>
              <td className="table-cell whitespace-nowrap text-slate-500">{dateTime(l.at)}</td>
              <td className="table-cell">{l.recipient}</td>
              <td className="table-cell">{l.subject}</td>
              <td className="table-cell">#{l.attempt}</td>
              <td className="table-cell"><Badge status={l.outcome === "ERROR" ? "FAILED" : "SENT"}>{titleCase(l.outcome)}</Badge></td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
