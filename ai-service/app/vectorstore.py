"""FAISS vector index over the policy passages.

Passages are embedded with BAAI/bge-small-en-v1.5 (384 dimensions) through fastembed,
which runs the model locally with ONNX: no API key, no per-query cost. Vectors are
L2-normalised and stored in a FAISS inner-product index, so a search is a cosine
similarity search.

Embedding 1,000+ passages takes a while on a small CPU, so the index is built once
(at Docker build time: `python -m app.vectorstore`) and saved next to the PDFs. At
startup it is loaded from disk if it matches the current passages; otherwise it is
rebuilt.
"""

from __future__ import annotations

import hashlib
import json
import logging
import os
from pathlib import Path

import numpy as np

log = logging.getLogger("medibot.vectors")

MODEL_NAME = "BAAI/bge-small-en-v1.5"
INDEX_DIR = Path(__file__).resolve().parent.parent / "data" / "index"


def fingerprint(texts: list[str]) -> str:
    """Identifies a set of passages, so a stale index on disk is never used."""
    h = hashlib.sha256(MODEL_NAME.encode())
    for t in texts:
        h.update(t.encode("utf-8"))
    return h.hexdigest()[:16]


class VectorIndex:
    def __init__(self, texts: list[str], directory: Path = INDEX_DIR):
        import faiss
        from fastembed import TextEmbedding

        self.model = TextEmbedding(MODEL_NAME, cache_dir=os.getenv("FASTEMBED_CACHE_PATH"))
        self.size = len(texts)
        key = fingerprint(texts)
        index_file, meta_file = directory / "policies.faiss", directory / "policies.json"

        if index_file.exists() and meta_file.exists() and json.loads(meta_file.read_text()).get("fingerprint") == key:
            self.index = faiss.read_index(str(index_file))
            log.info("Loaded FAISS index with %d vectors", self.index.ntotal)
            return

        log.info("Embedding %d passages with %s...", len(texts), MODEL_NAME)
        vectors = np.array(list(self.model.passage_embed(texts, batch_size=64)), dtype="float32")
        faiss.normalize_L2(vectors)
        self.index = faiss.IndexFlatIP(vectors.shape[1])
        self.index.add(vectors)
        directory.mkdir(parents=True, exist_ok=True)
        faiss.write_index(self.index, str(index_file))
        meta_file.write_text(json.dumps({"fingerprint": key, "model": MODEL_NAME, "vectors": len(texts),
                                         "dimensions": int(vectors.shape[1])}))
        log.info("Built and saved FAISS index with %d vectors", self.index.ntotal)

    def search(self, query: str, k: int = 20) -> list[tuple[int, float]]:
        """Passage positions and cosine similarities, best first."""
        import faiss

        q = np.array(list(self.model.query_embed([query])), dtype="float32")
        faiss.normalize_L2(q)
        scores, ids = self.index.search(q, min(k, self.size))
        return [(int(i), float(s)) for i, s in zip(ids[0], scores[0]) if i >= 0]


if __name__ == "__main__":
    # Build the index ahead of time: python -m app.vectorstore
    logging.basicConfig(level=logging.INFO)
    from .assistant import PolicyIndex

    PolicyIndex()
