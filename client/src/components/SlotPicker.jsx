import { useEffect, useMemo, useState } from "react";
import clsx from "clsx";
import { CalendarX2, Loader2 } from "lucide-react";
import { api } from "../api/client";
import { addDays, isoDate, time } from "../lib/format";

const DAYS_AHEAD = 14;

/** A two-week date strip plus the doctor's slots for the chosen day. */
export default function SlotPicker({ doctorId, workingDays = [], value, onChange }) {
  const days = useMemo(() => Array.from({ length: DAYS_AHEAD }, (_, i) => addDays(new Date(), i)), []);
  const firstWorking = days.find((d) => isWorking(d, workingDays)) || days[0];
  const [day, setDay] = useState(isoDate(firstWorking));
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  // Until the user picks a day, skip ahead past days that are already fully booked
  const [autoAdvance, setAutoAdvance] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    api
      .get(`/api/doctors/${doctorId}/slots?date=${day}`)
      .then((s) => {
        if (cancelled) return;
        if (autoAdvance && !s.some((x) => x.available)) {
          const next = days.find((d) => isoDate(d) > day && isWorking(d, workingDays));
          if (next) {
            setDay(isoDate(next));
            return;
          }
        }
        setSlots(s);
        setLoading(false);
      })
      .catch(() => {
        if (!cancelled) {
          setSlots([]);
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
    // workingDays is a new array on every parent render, so depend on its contents instead
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doctorId, day, autoAdvance, days, workingDays.join(",")]);

  return (
    <div>
      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-2">
        {days.map((d) => {
          const key = isoDate(d);
          const working = isWorking(d, workingDays);
          return (
            <button
              key={key}
              type="button"
              disabled={!working}
              onClick={() => {
                setAutoAdvance(false);
                setDay(key);
                onChange(null);
              }}
              className={clsx(
                "flex w-14 shrink-0 flex-col items-center rounded-xl border py-2 transition",
                key === day
                  ? "border-brand-600 bg-brand-600 text-white shadow-lift"
                  : working
                    ? "border-slate-200 bg-white text-slate-700 hover:border-brand-300"
                    : "cursor-not-allowed border-slate-100 bg-slate-50 text-slate-300",
              )}
            >
              <span className="text-[11px] font-semibold uppercase">{d.toLocaleDateString("en-IN", { weekday: "short" })}</span>
              <span className="text-lg font-bold">{d.getDate()}</span>
            </button>
          );
        })}
      </div>

      <div className="mt-3 min-h-[120px]">
        {loading ? (
          <div className="flex h-28 items-center justify-center">
            <Loader2 className="h-5 w-5 animate-spin text-brand-600" />
          </div>
        ) : slots.filter((s) => s.available).length === 0 ? (
          <div className="flex h-28 flex-col items-center justify-center text-center text-sm text-slate-500">
            <CalendarX2 className="mb-2 h-5 w-5 text-slate-400" />
            No free slots on this day. Try another date.
          </div>
        ) : (
          <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
            {slots.map((s) => (
              <button
                key={s.start}
                type="button"
                disabled={!s.available}
                onClick={() => onChange(s.start)}
                className={clsx(
                  "rounded-lg border px-2 py-2 text-sm font-semibold transition",
                  value === s.start
                    ? "border-brand-600 bg-brand-50 text-brand-700 ring-2 ring-brand-500/30"
                    : s.available
                      ? "border-slate-200 bg-white text-slate-700 hover:border-brand-300 hover:text-brand-700"
                      : "cursor-not-allowed border-slate-100 bg-slate-50 text-slate-300 line-through",
                )}
              >
                {time(s.start)}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function isWorking(d, workingDays) {
  if (!workingDays.length) return true;
  const name = d.toLocaleDateString("en-US", { weekday: "long" }).toUpperCase();
  return workingDays.includes(name);
}
