import { useState } from "react";
import { api } from "../../api/client";
import { useToast } from "../../components/Toast";
import { Avatar, Badge, Button, Card, PageHeader, Spinner, Stars, Tabs } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date, money, titleCase } from "../../lib/format";

export default function AdminPeople() {
  const notify = useToast();
  const [tab, setTab] = useState("doctors");
  const { data: doctors, loading, reload } = useApi("/api/admin/doctors");
  const { data: users, reload: reloadUsers } = useApi("/api/admin/users");

  const approve = async (d, approved) => {
    try {
      await api.post(`/api/admin/doctors/${d.id}/approve`, { enabled: approved });
      notify(approved ? `${d.name} is now visible to patients.` : `${d.name} has been hidden from search.`);
      reload();
    } catch (e) {
      notify(e.message, "error");
    }
  };

  const toggleUser = async (u) => {
    try {
      await api.patch(`/api/admin/users/${u.id}/enabled`, { enabled: !u.enabled });
      notify(`${u.fullName} ${u.enabled ? "disabled" : "enabled"}.`);
      reloadUsers();
    } catch (e) {
      notify(e.message, "error");
    }
  };

  if (loading) return <Spinner />;
  const pending = doctors.filter((d) => !d.approved);

  return (
    <div>
      <PageHeader title="Doctors & users" subtitle="Verify new doctors and manage account access." />
      <Tabs value={tab} onChange={setTab} tabs={[{ value: "doctors", label: "Doctors", count: doctors.length }, { value: "users", label: "All users", count: users?.length }]} />

      {tab === "doctors" ? (
        <div className="mt-5 space-y-6">
          {pending.length > 0 && (
            <div>
              <h2 className="mb-3 text-sm font-bold uppercase tracking-wide text-amber-700">Awaiting approval</h2>
              <div className="grid gap-4 md:grid-cols-2">
                {pending.map((d) => (
                  <Card key={d.id} className="border-amber-200 p-5">
                    <div className="flex items-center gap-4">
                      <Avatar name={d.name} />
                      <div className="flex-1">
                        <p className="font-bold text-slate-900">{d.name}</p>
                        <p className="text-sm text-slate-500">{d.specialization} · {d.qualification} · {d.experienceYears} yrs</p>
                        <p className="text-sm text-slate-500">{d.hospitalName || "Independent"} · {money(d.fee)}</p>
                      </div>
                    </div>
                    <div className="mt-4 flex gap-2">
                      <Button className="flex-1" onClick={() => approve(d, true)}>Approve</Button>
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          )}
          <Card className="overflow-x-auto">
            <table className="w-full min-w-[720px]">
              <thead className="border-b border-slate-100 bg-slate-50">
                <tr>
                  <th className="table-head">Doctor</th>
                  <th className="table-head">Specialty</th>
                  <th className="table-head">Hospital</th>
                  <th className="table-head">Rating</th>
                  <th className="table-head">Status</th>
                  <th className="table-head" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {doctors.filter((d) => d.approved).map((d) => (
                  <tr key={d.id}>
                    <td className="table-cell"><div className="flex items-center gap-3"><Avatar name={d.name} size="sm" /><span className="font-semibold">{d.name}</span></div></td>
                    <td className="table-cell">{d.specialization}</td>
                    <td className="table-cell">{d.hospitalName || "-"}</td>
                    <td className="table-cell"><div className="flex items-center gap-2"><Stars value={d.rating} size="h-3.5 w-3.5" />{Number(d.rating).toFixed(1)}</div></td>
                    <td className="table-cell"><Badge status="COMPLETED">Approved</Badge></td>
                    <td className="table-cell text-right"><Button size="sm" variant="danger" onClick={() => approve(d, false)}>Suspend</Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </div>
      ) : (
        <Card className="mt-5 overflow-x-auto">
          <table className="w-full min-w-[720px]">
            <thead className="border-b border-slate-100 bg-slate-50">
              <tr>
                <th className="table-head">Name</th>
                <th className="table-head">Email</th>
                <th className="table-head">Role</th>
                <th className="table-head">Joined</th>
                <th className="table-head">Status</th>
                <th className="table-head" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {users?.map((u) => (
                <tr key={u.id}>
                  <td className="table-cell font-semibold">{u.fullName}</td>
                  <td className="table-cell text-slate-500">{u.email}</td>
                  <td className="table-cell">{titleCase(u.role)}</td>
                  <td className="table-cell text-slate-500">{date(u.createdAt)}</td>
                  <td className="table-cell">{u.enabled ? <Badge status="COMPLETED">Active</Badge> : <Badge status="CANCELLED">Disabled</Badge>}</td>
                  <td className="table-cell text-right">
                    {u.role !== "ADMIN" && (
                      <Button size="sm" variant={u.enabled ? "danger" : "secondary"} onClick={() => toggleUser(u)}>{u.enabled ? "Disable" : "Enable"}</Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}
