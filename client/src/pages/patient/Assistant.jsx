import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import clsx from "clsx";
import { Bot, BookOpen, CalendarPlus, Loader2, MessageSquarePlus, Send, Trash2, WifiOff } from "lucide-react";
import { api } from "../../api/client";
import { useToast } from "../../components/Toast";
import { Button, Card } from "../../components/ui";
import { useApi } from "../../lib/useApi";
import { date } from "../../lib/format";

const SUGGESTIONS = [
  "What is the waiting period for pre-existing diseases in Activ One?",
  "Does Aspire Gold cover maternity?",
  "Is there a room rent limit in Care Supreme?",
  "I get knee pain when I climb stairs",
];

export default function Assistant() {
  const notify = useToast();
  const { data: sessions, reload } = useApi("/api/assistant/sessions");
  const [session, setSession] = useState(null);
  const [text, setText] = useState("");
  const [sending, setSending] = useState(false);
  const [pending, setPending] = useState(null);
  const bottom = useRef(null);

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: "smooth" });
  }, [session, pending]);

  const open = async (id) => setSession(await api.get(`/api/assistant/sessions/${id}`));

  const send = async (message) => {
    const msg = (message ?? text).trim();
    if (!msg || sending) return;
    setText("");
    setSending(true);
    setPending(msg);
    try {
      const s = await api.post("/api/assistant/chat", { sessionId: session?.id ?? null, message: msg });
      setSession(s);
      reload();
    } catch (e) {
      notify(e.message, "error");
      setText(msg);
    } finally {
      setSending(false);
      setPending(null);
    }
  };

  const remove = async (id) => {
    await api.del(`/api/assistant/sessions/${id}`);
    if (session?.id === id) setSession(null);
    reload();
  };

  const messages = session?.messages || [];
  const lastMode = [...messages].reverse().find((m) => m.role === "assistant")?.mode;

  return (
    <div className="grid h-[calc(100vh-7rem)] gap-5 lg:h-[calc(100vh-4rem)] lg:grid-cols-[260px_1fr]">
      <Card className="hidden flex-col overflow-hidden lg:flex">
        <div className="border-b border-slate-100 p-3">
          <Button className="w-full" icon={MessageSquarePlus} onClick={() => setSession(null)}>New chat</Button>
        </div>
        <div className="flex-1 space-y-1 overflow-y-auto p-2">
          {sessions?.map((s) => (
            <div
              key={s.id}
              className={clsx(
                "group flex items-center gap-2 rounded-lg px-3 py-2 text-sm",
                session?.id === s.id ? "bg-brand-50 text-brand-800" : "text-slate-600 hover:bg-slate-50",
              )}
            >
              <button className="min-w-0 flex-1 text-left" onClick={() => open(s.id)}>
                <p className="truncate font-semibold">{s.title}</p>
                <p className="text-xs text-slate-400">{date(s.updatedAt)}</p>
              </button>
              <button onClick={() => remove(s.id)} className="hidden text-slate-400 hover:text-rose-600 group-hover:block" aria-label="Delete chat">
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          ))}
          {!sessions?.length && <p className="px-3 py-2 text-xs text-slate-400">Your conversations are saved here.</p>}
        </div>
      </Card>

      <Card className="flex min-h-0 flex-col overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-3">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white"><Bot className="h-5 w-5" /></div>
            <div>
              <p className="font-bold text-slate-900">MediBot</p>
              <p className="text-xs text-slate-500">Insurance questions and finding the right specialist</p>
            </div>
          </div>
          {lastMode === "offline" && (
            <span className="hidden items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 sm:flex" title="No LLM key configured: answers come straight from the policy documents">
              <WifiOff className="h-3.5 w-3.5" /> Document search mode
            </span>
          )}
        </div>

        <div className="flex-1 space-y-5 overflow-y-auto bg-slate-50/60 p-5">
          {!messages.length && !pending && (
            <div className="mx-auto flex max-w-lg flex-col items-center py-8 text-center">
              <div className="rounded-2xl bg-brand-50 p-4 text-brand-600"><Bot className="h-8 w-8" /></div>
              <h2 className="mt-4 text-xl font-bold text-slate-900">How can I help today?</h2>
              <p className="mt-1 text-sm text-slate-500">
                I answer from the Activ One, Aspire Gold, Care Supreme and Health Premia policy documents, and point you to the right doctor.
              </p>
              <div className="mt-6 grid w-full gap-2">
                {SUGGESTIONS.map((s) => (
                  <button key={s} onClick={() => send(s)} className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-left text-sm text-slate-700 transition hover:border-brand-400 hover:text-brand-700">
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}
          {messages.map((m, i) => (
            <Message key={i} m={m} />
          ))}
          {pending && (
            <>
              <Message m={{ role: "user", content: pending }} />
              <div className="flex items-center gap-2 text-sm text-slate-500">
                <Loader2 className="h-4 w-4 animate-spin text-brand-600" /> MediBot is searching the documents…
              </div>
            </>
          )}
          <div ref={bottom} />
        </div>

        <form
          className="flex gap-2 border-t border-slate-100 p-3"
          onSubmit={(e) => {
            e.preventDefault();
            send();
          }}
        >
          <input
            className="input flex-1"
            placeholder="Ask about cover, waiting periods, claims, or describe your symptoms…"
            value={text}
            maxLength={1000}
            onChange={(e) => setText(e.target.value)}
          />
          <Button type="submit" disabled={!text.trim()} loading={sending} aria-label="Send">
            <Send className="h-4 w-4" />
          </Button>
        </form>
        <p className="px-4 pb-2 text-center text-[11px] text-slate-400">MediBot gives guidance, not medical advice. In an emergency, call 112.</p>
      </Card>
    </div>
  );
}

function Message({ m }) {
  if (m.role === "user") {
    return (
      <div className="flex justify-end">
        <div className="max-w-[80%] rounded-2xl rounded-br-md bg-brand-600 px-4 py-2.5 text-sm text-white">{m.content}</div>
      </div>
    );
  }
  return (
    <div className="flex gap-3">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white text-brand-600 shadow-card"><Bot className="h-4 w-4" /></div>
      <div className="max-w-[85%] space-y-3">
        <div className={clsx("rounded-2xl rounded-tl-md bg-white px-4 py-3 text-sm text-slate-700 shadow-card", m.mode === "safety" && "border border-rose-200 bg-rose-50 text-rose-800")}>
          <RichText text={m.content} />
        </div>
        {m.sources?.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {m.sources.map((s, i) => (
              <span key={i} title={s.snippet} className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-600">
                <BookOpen className="h-3 w-3 text-brand-600" /> {s.policy}, p.{s.page}
              </span>
            ))}
          </div>
        )}
        {m.suggestedSpecialization && (
          <Link to={`/doctors?specialization=${encodeURIComponent(m.suggestedSpecialization)}`}>
            <Button size="sm" icon={CalendarPlus}>See {m.suggestedSpecialization} doctors</Button>
          </Link>
        )}
      </div>
    </div>
  );
}

/** Minimal markdown: **bold**, "- " bullets and paragraphs. */
function RichText({ text }) {
  const blocks = text.split(/\n{2,}/);
  return (
    <div className="space-y-2">
      {blocks.map((block, i) => {
        const lines = block.split("\n");
        if (lines.every((l) => l.trim().startsWith("- "))) {
          return (
            <ul key={i} className="space-y-2">
              {lines.map((l, j) => (
                <li key={j} className="flex gap-2">
                  <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" />
                  <span>{bold(l.trim().slice(2))}</span>
                </li>
              ))}
            </ul>
          );
        }
        return <p key={i}>{bold(block)}</p>;
      })}
    </div>
  );
}

function bold(s) {
  return s.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith("**") ? <b key={i} className="text-slate-900">{part.slice(2, -2)}</b> : part,
  );
}
