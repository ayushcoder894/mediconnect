import { Link } from "react-router-dom";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { CalendarCheck2, IndianRupee, Mail, ScanLine, Stethoscope, UserCheck, UserX, Users } from "lucide-react";
import { Button, Card, PageHeader, Spinner, StatCard } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { compactMoney, titleCase } from "../../lib/format";

const STATUS_COLORS = {
  COMPLETED: "#10b981",
  BOOKED: "#38bdf8",
  CONFIRMED: "#14b8a6",
  CHECKED_IN: "#8b5cf6",
  CANCELLED: "#94a3b8",
  NO_SHOW: "#f43f5e",
};
const RISK_COLORS = { LOW: "#10b981", MODERATE: "#f59e0b", HIGH: "#f97316", VERY_HIGH: "#e11d48" };

export default function AdminDashboard() {
  const { data, loading } = useApi("/api/admin/dashboard");
  if (loading || !data) return <Spinner />;
  const t = data.totals;
  const perDay = data.appointmentsPerDay.map((d) => ({ ...d, label: d.date.slice(5).replace("-", "/") }));

  return (
    <div className="space-y-6">
      <PageHeader
        title="Hospital analytics"
        subtitle="Last 30 days across all doctors, labs and AI services."
        actions={<Link to="/app/admin/check-in"><Button icon={ScanLine}>Open check-in desk</Button></Link>}
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={Users} label="Patients" value={t.patients} />
        <StatCard icon={Stethoscope} label="Active doctors" value={t.doctors} tone="blue" hint={t.pendingDoctors ? `${t.pendingDoctors} awaiting approval` : "All approved"} />
        <StatCard icon={IndianRupee} label="Revenue" value={compactMoney(Number(t.consultationRevenue) + Number(t.labRevenue))} tone="violet" hint={`${compactMoney(t.consultationRevenue)} visits · ${compactMoney(t.labRevenue)} labs`} />
        <StatCard icon={CalendarCheck2} label="Appointments" value={t.appointments} tone="amber" hint={`${t.labBookings} lab bookings`} />
        <StatCard icon={UserCheck} label="Today" value={`${t.todayCheckedIn}/${t.todayAppointments}`} hint="Checked in / booked" />
        <StatCard icon={UserX} label="No-show rate" value={`${(t.noShowRate * 100).toFixed(1)}%`} tone="rose" />
        <StatCard icon={Mail} label="Email queue" value={t.emailsPending} tone="slate" hint={t.emailsFailed ? `${t.emailsFailed} failed` : "No failures"} />
        <StatCard icon={UserCheck} label="Pending doctors" value={t.pendingDoctors} tone={t.pendingDoctors ? "amber" : "slate"} hint={t.pendingDoctors ? <Link to="/app/admin/people" className="font-semibold text-brand-700">Review now →</Link> : null} />
      </div>

      <Card className="p-6">
        <h2 className="font-bold text-slate-900">Appointments per day</h2>
        <div className="mt-4 h-72">
          <ResponsiveContainer>
            <AreaChart data={perDay} margin={{ left: -20, right: 8 }}>
              <defs>
                <linearGradient id="booked" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#14b8a6" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#14b8a6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} interval={3} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Legend />
              <Area type="monotone" dataKey="booked" name="Booked" stroke="#0d9488" fill="url(#booked)" strokeWidth={2.5} />
              <Area type="monotone" dataKey="completed" name="Completed" stroke="#10b981" fill="transparent" strokeWidth={2} />
              <Area type="monotone" dataKey="cancelled" name="Cancelled" stroke="#94a3b8" fill="transparent" strokeWidth={2} strokeDasharray="4 4" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Revenue by doctor</h2>
          <div className="mt-4 h-72">
            <ResponsiveContainer>
              <BarChart data={data.revenueByDoctor} layout="vertical" margin={{ left: 40, right: 16 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" horizontal={false} />
                <XAxis type="number" tickFormatter={compactMoney} tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v) => compactMoney(v)} />
                <Bar dataKey="amount" name="Revenue" fill="#0d9488" radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Appointment outcomes</h2>
          <div className="mt-4 h-72">
            <ResponsiveContainer>
              <PieChart>
                <Pie data={data.statusBreakdown} dataKey="count" nameKey="name" innerRadius={60} outerRadius={100} paddingAngle={2}>
                  {data.statusBreakdown.map((s) => <Cell key={s.name} fill={STATUS_COLORS[s.name] || "#cbd5e1"} />)}
                </Pie>
                <Tooltip formatter={(v, n) => [v, titleCase(n)]} />
                <Legend formatter={(v) => titleCase(v)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Demand by specialty</h2>
          <div className="mt-4 h-72">
            <ResponsiveContainer>
              <BarChart data={data.specializationDemand} margin={{ left: -20, right: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 10 }} interval={0} angle={-30} textAnchor="end" height={70} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="count" name="Appointments" fill="#38bdf8" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <div className="grid gap-6">
          <Card className="p-6">
            <h2 className="font-bold text-slate-900">Diabetes risk across patients</h2>
            <div className="mt-4 flex h-32 overflow-hidden rounded-xl">
              {data.riskBands.map((r) => {
                const total = data.riskBands.reduce((a, b) => a + b.count, 0) || 1;
                return (
                  <div key={r.name} className="flex flex-col items-center justify-center text-white" style={{ width: `${(r.count / total) * 100}%`, background: RISK_COLORS[r.name] }}>
                    <span className="text-lg font-extrabold">{r.count}</span>
                    <span className="text-[11px] font-semibold opacity-90">{titleCase(r.name)}</span>
                  </div>
                );
              })}
            </div>
            <p className="mt-3 text-xs text-slate-500">AI risk checks run by patients. High and very-high patients are prompted to book an endocrinologist.</p>
          </Card>
          <Card className="p-6">
            <h2 className="font-bold text-slate-900">Top lab tests</h2>
            <ul className="mt-3 space-y-2.5">
              {data.topLabTests.map((l) => {
                const max = data.topLabTests[0]?.count || 1;
                return (
                  <li key={l.name}>
                    <div className="flex justify-between text-sm">
                      <span className="font-semibold text-slate-700">{l.name}</span>
                      <span className="text-slate-500">{l.count} · {compactMoney(l.amount)}</span>
                    </div>
                    <div className="mt-1 h-1.5 rounded-full bg-slate-100">
                      <div className="h-full rounded-full bg-brand-500" style={{ width: `${(l.count / max) * 100}%` }} />
                    </div>
                  </li>
                );
              })}
            </ul>
          </Card>
        </div>
      </div>
    </div>
  );
}
