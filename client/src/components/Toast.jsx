import { createContext, useCallback, useContext, useState } from "react";
import { CheckCircle2, AlertTriangle, X } from "lucide-react";

const ToastContext = createContext(() => {});

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);

  const notify = useCallback(
    (message, type = "success") => {
      const id = Math.random().toString(36).slice(2);
      setToasts((t) => [...t, { id, message, type }]);
      setTimeout(() => dismiss(id), 4500);
    },
    [dismiss],
  );

  return (
    <ToastContext.Provider value={notify}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-[min(92vw,380px)] flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            role="status"
            className="pointer-events-auto flex animate-fade-up items-start gap-3 rounded-xl border border-slate-200 bg-white p-3.5 shadow-lg"
          >
            {t.type === "error" ? (
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-rose-500" />
            ) : (
              <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-brand-600" />
            )}
            <p className="flex-1 text-sm text-slate-700">{t.message}</p>
            <button onClick={() => dismiss(t.id)} className="text-slate-400 hover:text-slate-600" aria-label="Dismiss">
              <X className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
