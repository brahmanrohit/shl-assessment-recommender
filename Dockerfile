FROM python:3.11-slim

WORKDIR /app

# deps first for layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# app code + prebuilt catalog
COPY app ./app
COPY data/catalog.json ./data/catalog.json

ENV PYTHONUNBUFFERED=1
EXPOSE 8000

# Render/most PaaS inject $PORT; default to 8000 locally.
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
