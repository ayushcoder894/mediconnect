FROM python:3.11-slim

# LightGBM needs the OpenMP runtime
RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY models ./models
COPY data ./data

# Download the embedding model and build the FAISS index at build time,
# so the container starts in seconds instead of embedding 1,000+ passages
ENV FASTEMBED_CACHE_PATH=/app/models/embeddings
RUN python -m app.vectorstore

RUN useradd --create-home app && chown -R app /app/models /app/data
USER app
EXPOSE 8000
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -fs http://localhost:${PORT:-8000}/health || exit 1
# Hosting platforms such as Render pass the port to listen on in $PORT
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
