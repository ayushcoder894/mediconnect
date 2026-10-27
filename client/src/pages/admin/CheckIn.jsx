import { useEffect, useRef, useState } from "react";
import { Html5Qrcode } from "html5-qrcode";
import { Camera, CameraOff, CheckCircle2, Keyboard, ScanLine } from "lucide-react";
import { api } from "../../api/client";
import { Avatar, Button, Card, ErrorNote, Field, PageHeader } from "../../components/ui";
import { dateTime } from "../../lib/format";

const READER_ID = "qr-reader";

export default function CheckIn() {
  const [scanning, setScanning] = useState(false);
  const [code, setCode] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const scanner = useRef(null);
  const handled = useRef(false);

  const checkIn = async (value) => {
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      setResult(await api.post("/api/admin/check-in", { code: value.trim() }));
      setCode("");
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const stop = async () => {
    if (scanner.current?.isScanning) await scanner.current.stop();
    setScanning(false);
  };

  const start = async () => {
    setError(null);
    setResult(null);
    handled.current = false;
    try {
      scanner.current = scanner.current || new Html5Qrcode(READER_ID);
      setScanning(true);
      await scanner.current.start({ facingMode: "environment" }, { fps: 10, qrbox: 220 }, async (text) => {
        if (handled.current) return;
        handled.current = true;
        await stop();
        checkIn(text);
      });
    } catch {
      setScanning(false);
      setError("Camera not available. Use the code printed under the QR instead.");
    }
  };

  useEffect(() => () => {
    if (scanner.current?.isScanning) scanner.current.stop();
  }, []);

  return (
    <div>
      <PageHeader title="Front desk check-in" subtitle="Scan the QR on the patient's appointment slip, or type the code below it." />
      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="relative flex aspect-square max-h-[420px] w-full items-center justify-center overflow-hidden rounded-2xl bg-slate-900">
            <div id={READER_ID} className="h-full w-full [&_video]:h-full [&_video]:w-full [&_video]:object-cover" />
            {!scanning && (
              <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-300">
                <ScanLine className="h-14 w-14" />
                <p className="mt-3 text-sm">Camera is off</p>
              </div>
            )}
          </div>
          <Button className="mt-4 w-full" size="lg" variant={scanning ? "secondary" : "primary"} icon={scanning ? CameraOff : Camera} onClick={scanning ? stop : start}>
            {scanning ? "Stop camera" : "Start scanning"}
          </Button>
          <form
            className="mt-6 border-t border-slate-100 pt-6"
            onSubmit={(e) => {
              e.preventDefault();
              checkIn(code);
            }}
          >
            <Field label="Or enter the code manually">
              <div className="flex gap-2">
                <input className="input font-mono" placeholder="e.g. 3f2b9c1e-…" value={code} onChange={(e) => setCode(e.target.value)} />
                <Button type="submit" icon={Keyboard} disabled={!code.trim()} loading={busy}>Check in</Button>
              </div>
            </Field>
          </form>
        </Card>

        <div className="space-y-4">
          <ErrorNote error={error} />
          {result ? (
            <Card className="animate-fade-up overflow-hidden">
              <div className="flex items-center gap-3 bg-emerald-500 px-6 py-4 text-white">
                <CheckCircle2 className="h-7 w-7" />
                <div>
                  <p className="text-lg font-bold">Checked in</p>
                  <p className="text-sm text-emerald-50">The doctor's queue has been updated.</p>
                </div>
              </div>
              <div className="space-y-4 p-6">
                <div className="flex items-center gap-4">
                  <Avatar name={result.patientName} />
                  <div>
                    <p className="text-lg font-bold text-slate-900">{result.patientName}</p>
                    <p className="text-sm text-slate-500">{result.patientPhone}</p>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div className="rounded-xl bg-slate-50 p-3"><p className="text-xs text-slate-500">Doctor</p><p className="font-semibold">{result.doctorName}</p></div>
                  <div className="rounded-xl bg-slate-50 p-3"><p className="text-xs text-slate-500">Slot</p><p className="font-semibold">{dateTime(result.slotStart)}</p></div>
                  <div className="col-span-2 rounded-xl bg-slate-50 p-3"><p className="text-xs text-slate-500">Reason</p><p className="font-semibold">{result.reason || "-"}</p></div>
                </div>
              </div>
            </Card>
          ) : (
            <Card className="p-6 text-sm text-slate-500">
              <h3 className="font-bold text-slate-900">How it works</h3>
              <ol className="mt-3 list-decimal space-y-2 pl-5">
                <li>Patients get a QR code on their appointment slip and in the app.</li>
                <li>Scanning it checks them in for today's appointment.</li>
                <li>Codes for other days, cancelled visits or repeat scans are rejected with a clear reason.</li>
              </ol>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
