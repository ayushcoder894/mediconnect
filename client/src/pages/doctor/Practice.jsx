import { useEffect, useState } from "react";
import { Save } from "lucide-react";
import { api } from "../../api/client";
import { useToast } from "../../components/Toast";
import { Button, Card, ErrorNote, Field, PageHeader, Spinner } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { titleCase } from "../../lib/format";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

export default function Practice() {
  const notify = useToast();
  const { data, loading, setData } = useApi("/api/doctors/me");
  const { data: hospitals } = useApi("/api/hospitals");
  const [profile, setProfile] = useState(null);
  const [hours, setHours] = useState(null);
  const [saving, setSaving] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!data) return;
    const d = data.doctor;
    setProfile({ qualification: d.qualification, experienceYears: d.experienceYears, consultationFee: d.fee, bio: d.bio || "", slotMinutes: d.slotMinutes, hospitalId: d.hospitalId || "" });
    setHours(
      DAYS.map((day) => {
        const a = data.availability.find((x) => x.dayOfWeek === day);
        return { day, on: !!a, start: a?.startTime.slice(0, 5) || "10:00", end: a?.endTime.slice(0, 5) || "14:00" };
      }),
    );
  }, [data]);

  if (loading || !profile || !hours) return <Spinner />;

  const saveProfile = async (e) => {
    e.preventDefault();
    setSaving("profile");
    setError(null);
    try {
      setData(await api.put("/api/doctors/me", { ...profile, experienceYears: Number(profile.experienceYears), consultationFee: Number(profile.consultationFee), slotMinutes: Number(profile.slotMinutes), hospitalId: profile.hospitalId ? Number(profile.hospitalId) : null }));
      notify("Practice details saved.");
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(null);
    }
  };

  const saveHours = async () => {
    setSaving("hours");
    setError(null);
    try {
      setData(await api.put("/api/doctors/me/availability", hours.filter((h) => h.on).map((h) => ({ dayOfWeek: h.day, startTime: `${h.start}:00`, endTime: `${h.end}:00` }))));
      notify("Weekly hours updated. Patients now see the new slots.");
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(null);
    }
  };

  const setP = (k) => (e) => setProfile({ ...profile, [k]: e.target.value });
  const setH = (i, k, v) => setHours(hours.map((h, j) => (j === i ? { ...h, [k]: v } : h)));

  return (
    <div>
      <PageHeader title="My practice" subtitle="Your public profile and the hours patients can book." />
      <ErrorNote error={error} />
      <div className="mt-4 grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Weekly hours</h2>
          <p className="text-sm text-slate-500">Slots of {profile.slotMinutes} minutes are generated inside these hours.</p>
          <div className="mt-5 space-y-2">
            {hours.map((h, i) => (
              <div key={h.day} className="flex items-center gap-3 rounded-xl border border-slate-100 p-3">
                <label className="flex w-36 items-center gap-2.5 text-sm font-semibold text-slate-700">
                  <input type="checkbox" className="h-4 w-4 accent-brand-600" checked={h.on} onChange={(e) => setH(i, "on", e.target.checked)} />
                  {titleCase(h.day)}
                </label>
                {h.on ? (
                  <div className="flex flex-1 items-center gap-2">
                    <input type="time" className="input py-1.5" value={h.start} onChange={(e) => setH(i, "start", e.target.value)} />
                    <span className="text-slate-400">–</span>
                    <input type="time" className="input py-1.5" value={h.end} onChange={(e) => setH(i, "end", e.target.value)} />
                  </div>
                ) : (
                  <span className="text-sm text-slate-400">Not working</span>
                )}
              </div>
            ))}
          </div>
          <Button className="mt-5 w-full" icon={Save} loading={saving === "hours"} onClick={saveHours}>Save hours</Button>
        </Card>

        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Profile</h2>
          <form onSubmit={saveProfile} className="mt-5 space-y-4">
            <Field label="Qualification"><input required className="input" value={profile.qualification} onChange={setP("qualification")} /></Field>
            <div className="grid grid-cols-3 gap-3">
              <Field label="Experience (yrs)"><input type="number" min="0" className="input" value={profile.experienceYears} onChange={setP("experienceYears")} /></Field>
              <Field label="Fee (₹)"><input type="number" min="0" className="input" value={profile.consultationFee} onChange={setP("consultationFee")} /></Field>
              <Field label="Slot length">
                <select className="input" value={profile.slotMinutes} onChange={setP("slotMinutes")}>
                  {[15, 20, 30, 45, 60].map((m) => <option key={m} value={m}>{m} min</option>)}
                </select>
              </Field>
            </div>
            <Field label="Hospital">
              <select className="input" value={profile.hospitalId} onChange={setP("hospitalId")}>
                <option value="">Independent practice</option>
                {hospitals?.map((h) => <option key={h.id} value={h.id}>{h.name}, {h.city}</option>)}
              </select>
            </Field>
            <Field label="About you"><textarea className="input min-h-[110px]" maxLength={2000} value={profile.bio} onChange={setP("bio")} /></Field>
            <Button type="submit" className="w-full" icon={Save} loading={saving === "profile"}>Save profile</Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
