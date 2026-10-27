"""MediBot: health-insurance and care-navigation assistant.

Built on the Health-Insurance-AI-Assistant project. Policy PDFs are split into
chunks and indexed with TF-IDF, so retrieval works offline with no API key.
When OPENAI_API_KEY is set, an LLM answers through LangChain tool-calling,
using the same retriever and the specialist matcher as its tools. Without a
key, MediBot answers directly from the retrieved passages.
"""

from __future__ import annotations

import logging
import os
import re
from dataclasses import dataclass, field
from pathlib import Path

from pypdf import PdfReader
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import linear_kernel

log = logging.getLogger("medibot")

POLICY_DIR = Path(__file__).resolve().parent.parent / "data" / "policies"
POLICIES = {
    "activ_one.pdf": "Activ One",
    "aspire_gold.pdf": "Aspire Gold",
    "care_supreme.pdf": "Care Supreme",
    "health_premia.pdf": "Health Premia",
}
CHUNK_SIZE = 900
CHUNK_OVERLAP = 150

INSURANCE_WORDS = re.compile(
    r"\b(polic(y|ies)|insur\w*|cover(ed|age)?|claim\w*|premium|waiting period|deductible|room rent|"
    r"exclu\w*|rider|co-?pay\w*|cashless|sum insured|hospitali[sz]ation|pre-?existing|ped|maternity|"
    r"ayush|day care|ambulance|no claim bonus|ncb|renew\w*|activ one|aspire|care supreme|health premia)\b",
    re.IGNORECASE,
)

EMERGENCY = re.compile(
    r"(severe chest pain|can'?t breathe|cannot breathe|unconscious|fainted|stroke|heavy bleeding|"
    r"suicid\w*|kill myself|seizure now|choking)",
    re.IGNORECASE,
)

# Keyword -> specialization. Names match the specializations doctors register with.
SPECIALISTS: list[tuple[str, re.Pattern]] = [
    ("Cardiology", re.compile(r"chest pain|palpitation|heart|blood pressure|hypertension|breathless", re.I)),
    ("Endocrinology", re.compile(r"diabet|blood sugar|\bsugar\b|thyroid|hba1c|insulin|hormon", re.I)),
    ("Dermatology", re.compile(r"\bskin\b|rash|acne|itch|eczema|hair ?fall|pimple|psoriasis", re.I)),
    ("Orthopedics", re.compile(r"joint|knee|back pain|fracture|\bbone|shoulder|sprain|neck pain|arthritis", re.I)),
    ("Pediatrics", re.compile(r"\bchild|\bbaby|infant|\bkid|toddler|vaccin", re.I)),
    ("Neurology", re.compile(r"headache|migraine|seizure|numb|dizz|tingling|memory loss|tremor", re.I)),
    ("Gynecology", re.compile(r"period|menstrua|pregnan|pcos|pcod|pelvic|vaginal", re.I)),
    ("Psychiatry", re.compile(r"anxiety|anxious|depress|stress|insomnia|can'?t sleep|panic|low mood", re.I)),
    ("Nephrology", re.compile(r"kidney|creatinine|urin|dialysis|swelling in (my )?(feet|legs)", re.I)),
    ("Ophthalmology", re.compile(r"\beye|vision|blurr|cataract|glaucoma", re.I)),
    ("ENT", re.compile(r"\bear\b|earache|throat|sinus|\bnose\b|tonsil|hearing", re.I)),
    ("General Medicine", re.compile(r"fever|cold|cough|fatigue|tired|vomit|stomach|diarrh|body ache|flu", re.I)),
]

SYSTEM_PROMPT = """You are MediBot, the assistant inside the MediConnect healthcare platform.
You help patients with two things:
1. Health insurance questions. Always call search_policy_documents before answering anything about
   coverage, waiting periods, exclusions, claims or benefits, and cite the policy name and page.
   If the documents do not answer the question, say so instead of guessing.
2. Finding the right doctor. When someone describes symptoms, call suggest_specialist and tell them which
   specialist to book on MediConnect. Never diagnose or prescribe. For emergencies (severe chest pain,
   trouble breathing, stroke signs, thoughts of self-harm) tell them to call 112 immediately.
Keep answers short, warm and in plain language. End with one specific follow-up question that fits
what the user asked."""


@dataclass
class Chunk:
    policy: str
    page: int
    text: str


@dataclass
class Reply:
    reply: str
    sources: list[dict] = field(default_factory=list)
    suggested_specialization: str | None = None
    mode: str = "offline"


def clean_pdf_text(text: str) -> str:
    """Undoes ligature artifacts from PDF extraction ("a/fter", "prima/r_y")
    without touching real slashes such as "and/or"."""
    text = text.replace("ﬁ", "fi").replace("ﬂ", "fl").replace("ﬀ", "ff")
    text = re.sub(r"(?<=[a-z])/(?=[a-z])(?!or\b)", "", text)
    text = re.sub(r"(?<=[a-z])_(?=[a-z])", "", text)
    return re.sub(r"\s+", " ", text).strip()


RRF_K = 60
CANDIDATES = 60


class PolicyIndex:
    """Hybrid search over chunked policy documents: TF-IDF keyword ranking plus a
    FAISS vector index, merged with reciprocal rank fusion. Keywords keep exact
    terms precise ("room rent", "AYUSH"); vectors catch questions worded differently
    from the policy ("having a baby" finds the maternity clause). If the vector
    index cannot load, search falls back to keywords alone."""

    def __init__(self, directory: Path = POLICY_DIR, vectors: bool | None = None):
        self.chunks: list[Chunk] = []
        for file, name in POLICIES.items():
            path = directory / file
            if not path.exists():
                log.warning("Policy document missing: %s", path)
                continue
            for page_no, page in enumerate(PdfReader(path).pages, start=1):
                text = clean_pdf_text(page.extract_text() or "")
                for start in range(0, len(text), CHUNK_SIZE - CHUNK_OVERLAP):
                    piece = text[start:start + CHUNK_SIZE]
                    if len(piece) > 120:
                        self.chunks.append(Chunk(name, page_no, piece))
        self.vectorizer = TfidfVectorizer(stop_words="english", ngram_range=(1, 2), sublinear_tf=True, min_df=1)
        self.matrix = self.vectorizer.fit_transform([c.text for c in self.chunks])
        log.info("Indexed %d chunks from %d policies", len(self.chunks), len(POLICIES))

        self.vectors = None
        if vectors if vectors is not None else os.getenv("VECTOR_SEARCH", "on") != "off":
            try:
                from .vectorstore import VectorIndex

                self.vectors = VectorIndex([c.text for c in self.chunks])
            except Exception as e:  # missing package or model download failure: keywords still work
                log.warning("Vector search unavailable, using keyword search only: %s", e)

    @property
    def mode(self) -> str:
        return "hybrid (TF-IDF + FAISS)" if self.vectors else "keyword (TF-IDF)"

    def _keyword_ranking(self, query: str) -> list[int]:
        scores = linear_kernel(self.vectorizer.transform([focus(query)]), self.matrix).ravel()
        ranked = scores.argsort()[::-1][:CANDIDATES]
        return [int(i) for i in ranked if scores[i] > 0]

    def search(self, query: str, policy: str | None = None, k: int = 4) -> list[tuple[Chunk, float]]:
        # Reciprocal rank fusion: each passage scores 1 / (60 + rank) in every list it appears in.
        # Ranks rather than raw scores, because TF-IDF and cosine scores are on different scales.
        fused: dict[int, float] = {}
        rankings = [self._keyword_ranking(query)]
        if self.vectors:
            rankings.append([i for i, _ in self.vectors.search(query, CANDIDATES)])
        for ranking in rankings:
            for rank, i in enumerate(ranking):
                fused[i] = fused.get(i, 0.0) + 1.0 / (RRF_K + rank + 1)

        results = []
        seen_pages = set()
        for i, score in sorted(fused.items(), key=lambda kv: kv[1], reverse=True):
            chunk = self.chunks[i]
            if policy and chunk.policy.lower() != policy.lower():
                continue
            # overlapping chunks of one page would repeat the same passage
            if (chunk.policy, chunk.page) in seen_pages:
                continue
            seen_pages.add((chunk.policy, chunk.page))
            results.append((chunk, score))
            if len(results) == k:
                break
        return results


POLICY_NAMES = re.compile("|".join(re.escape(n) for n in POLICIES.values()), re.IGNORECASE)
GENERIC_WORDS = re.compile(
    r"\b(does|do|is|are|my|the|policy|policies|plan|insurance|cover(s|ed|age)?|expenses?|benefits?|"
    r"what|how|much|tell|me|about|under|in|for|of)\b",
    re.IGNORECASE,
)


def focus(query: str) -> str:
    """Drops policy names and words that appear on nearly every page, which would
    otherwise outweigh the term the user actually cares about."""
    stripped = GENERIC_WORDS.sub(" ", POLICY_NAMES.sub(" ", query))
    return stripped if stripped.strip() else query


def mentioned_policy(text: str) -> str | None:
    lowered = text.lower()
    for name in POLICIES.values():
        if name.lower() in lowered:
            return name
    return None


def suggest_specialist(text: str) -> str | None:
    for specialization, pattern in SPECIALISTS:
        if pattern.search(text):
            return specialization
    return None


def snippet(text: str, query: str, length: int = 320) -> str:
    """The part of a chunk around the first query term it contains."""
    words = [w for w in re.findall(r"[a-zA-Z]{4,}", focus(query).lower())]
    lowered = text.lower()
    hits = [lowered.find(w) for w in words if lowered.find(w) >= 0]
    start = max(0, min(hits) - 80) if hits else 0
    piece = text[start:start + length].strip()
    return ("..." if start > 0 else "") + piece + ("..." if start + length < len(text) else "")


class MediBot:
    def __init__(self, index: PolicyIndex):
        self.index = index
        self.llm = None
        if os.getenv("OPENAI_API_KEY"):
            try:
                from langchain_openai import ChatOpenAI

                self.llm = ChatOpenAI(model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"), temperature=0.2, timeout=25)
                log.info("MediBot LLM mode enabled")
            except Exception as e:  # pragma: no cover - depends on environment
                log.warning("Could not initialise LLM, using offline mode: %s", e)

    @property
    def llm_enabled(self) -> bool:
        return self.llm is not None

    def chat(self, message: str, history: list[dict]) -> Reply:
        if EMERGENCY.search(message):
            return Reply(
                "This sounds like it could be an emergency. Please call 112 or go to the nearest emergency "
                "department right now. MediBot cannot help with urgent medical situations.",
                mode="safety",
            )
        if self.llm is not None:
            try:
                return self._chat_llm(message, history)
            except Exception as e:  # network, quota, bad key: fall back rather than fail the user
                log.warning("LLM call failed, answering offline: %s", e)
        return self._chat_offline(message)

    # ---- offline mode ----

    def _chat_offline(self, message: str) -> Reply:
        specialist = suggest_specialist(message)
        if INSURANCE_WORDS.search(message) or not specialist:
            hits = self.index.search(message, mentioned_policy(message), k=3) if INSURANCE_WORDS.search(message) else []
            if hits:
                sources = [{"policy": c.policy, "page": c.page, "snippet": snippet(c.text, message)} for c, _ in hits]
                lines = [f"- **{s['policy']}** (page {s['page']}): {s['snippet']}" for s in sources]
                return Reply(
                    "Here is what the policy documents say about that:\n\n" + "\n".join(lines)
                    + "\n\nWould you like me to compare how the other policies handle this?",
                    sources=sources,
                )
        if specialist:
            return Reply(
                f"Based on what you describe, a **{specialist}** specialist is the right doctor to see. "
                f"You can book one from Find Doctors. This is guidance, not a diagnosis; if symptoms get "
                f"worse suddenly, seek urgent care.\n\nHow long have you had these symptoms?",
                suggested_specialization=specialist,
            )
        return Reply(
            "I can help you understand your health insurance (Activ One, Aspire Gold, Care Supreme and "
            "Health Premia) or find the right specialist for your symptoms. Try asking \"What is the waiting "
            "period for pre-existing diseases in Activ One?\" or \"I have knee pain when climbing stairs.\""
        )

    # ---- LLM mode ----

    def _chat_llm(self, message: str, history: list[dict]) -> Reply:
        from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
        from langchain_core.tools import tool

        sources: list[dict] = []
        suggested: list[str] = []

        @tool
        def search_policy_documents(query: str, policy: str | None = None) -> str:
            """Search the health insurance policy documents. Optionally restrict to one policy:
            Activ One, Aspire Gold, Care Supreme or Health Premia."""
            hits = self.index.search(query, policy, k=4)
            if not hits:
                return "No relevant passages found."
            for c, _ in hits:
                sources.append({"policy": c.policy, "page": c.page, "snippet": snippet(c.text, query)})
            return "\n\n".join(f"[{c.policy}, page {c.page}] {c.text}" for c, _ in hits)

        @tool
        def suggest_specialist_for(symptoms: str) -> str:
            """Suggest which medical specialization the patient should book for the given symptoms."""
            specialization = suggest_specialist(symptoms) or "General Medicine"
            suggested.append(specialization)
            return f"Recommended specialization: {specialization}"

        @tool
        def list_policies() -> str:
            """List the insurance policies MediBot has documents for."""
            return ", ".join(POLICIES.values())

        tools = {t.name: t for t in (search_policy_documents, suggest_specialist_for, list_policies)}
        model = self.llm.bind_tools(list(tools.values()))
        messages = [SystemMessage(SYSTEM_PROMPT)]
        for turn in history[-10:]:
            messages.append(HumanMessage(turn["content"]) if turn["role"] == "user" else AIMessage(turn["content"]))
        messages.append(HumanMessage(message))

        for _ in range(4):
            response = model.invoke(messages)
            messages.append(response)
            if not response.tool_calls:
                return Reply(str(response.content), sources=sources[:6],
                             suggested_specialization=suggested[-1] if suggested else None, mode="llm")
            for call in response.tool_calls:
                result = tools[call["name"]].invoke(call["args"]) if call["name"] in tools else "Unknown tool"
                messages.append(ToolMessage(content=str(result), tool_call_id=call["id"]))
        raise RuntimeError("Too many tool rounds")
