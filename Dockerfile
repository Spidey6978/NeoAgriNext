FROM python:3.11-slim

WORKDIR /code

# System dependencies for scientific computing (Pandas/Scikit etc)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt /code/
RUN pip install --no-cache-dir -r requirements.txt

# Copy the app source
COPY app /code/app

# Cloud Run requirement: use $PORT variable, defaults to 8080
ENV PORT=8080

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
