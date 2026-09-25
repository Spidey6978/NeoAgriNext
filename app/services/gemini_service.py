import json
import sqlite3
import time
import asyncio
import re
from google import genai
from google.genai import types
from app.config import settings
from app.schemas.input_schema import CropInput
from app.services.cache_service import DB_NAME

# --- CONFIGURATION ---
client = genai.Client(api_key=settings.GEMINI_API_KEY)

# Safety settings formatted for google-genai
SAFETY_SETTINGS = [
    types.SafetySetting(
        category=types.HarmCategory.HARM_CATEGORY_HARASSMENT,
        threshold=types.HarmBlockThreshold.BLOCK_NONE,
    ),
    types.SafetySetting(
        category=types.HarmCategory.HARM_CATEGORY_HATE_SPEECH,
        threshold=types.HarmBlockThreshold.BLOCK_NONE,
    ),
    types.SafetySetting(
        category=types.HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
        threshold=types.HarmBlockThreshold.BLOCK_NONE,
    ),
    types.SafetySetting(
        category=types.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
        threshold=types.HarmBlockThreshold.BLOCK_NONE,
    ),
]

CONFIG = types.GenerateContentConfig(
    safety_settings=SAFETY_SETTINGS
)

# Updated list of valid, active models for google-genai
MODEL_CANDIDATES = [
    getattr(settings, "GEMINI_MODEL", None),
    "gemini-2.5-flash",
    "gemini-2.0-flash",
    "gemini-1.5-flash",
]
MODEL_CANDIDATES = [m for m in dict.fromkeys(MODEL_CANDIDATES) if m]

_ACTIVE_MODEL_NAME = None
_BLACKLISTED_MODELS = set()  # models confirmed dead this run — never retry them


def _resolve_working_model():
    """
    Picks the first candidate that (a) client.models.list() shows, and
    (b) hasn't already failed a real generate_content call this run.
    """
    global _ACTIVE_MODEL_NAME

    if _ACTIVE_MODEL_NAME is not None:
        return _ACTIVE_MODEL_NAME

    available = None
    try:
        # Fetch available models using google-genai SDK
        available_models = list(client.models.list(config={'query_base': True}))
        
        # Normalize model names by stripping 'models/' prefix
        available = set()
        for m in available_models:
            model_name = m.name.replace("models/", "") if hasattr(m, "name") else str(m)
            available.add(model_name)

        print(f"🔍 Available Gemini Models: {available}")
    except Exception as e:
        print(f"⚠️ Could not list Gemini models ({e}); trying candidates blind.")

    for candidate in MODEL_CANDIDATES:
        if candidate in _BLACKLISTED_MODELS:
            continue
        # If listing worked, ensure the candidate exists in available models
        if available is not None and candidate not in available:
            continue
        
        _ACTIVE_MODEL_NAME = candidate
        print(f"✅ Gemini model locked in: {candidate}")
        return _ACTIVE_MODEL_NAME

    raise RuntimeError(
        f"No working Gemini model found. Blacklisted this run: {_BLACKLISTED_MODELS or 'none'}"
    )


def _invalidate_active_model():
    global _ACTIVE_MODEL_NAME
    if _ACTIVE_MODEL_NAME:
        _BLACKLISTED_MODELS.add(_ACTIVE_MODEL_NAME)
        print(f"🚫 Blacklisting '{_ACTIVE_MODEL_NAME}' for the rest of this run.")
    _ACTIVE_MODEL_NAME = None


async def _generate_with_failover(prompt, loop, max_model_switches=3):
    """
    - 404 / retired model  -> blacklist permanently, move to next candidate.
    - 429 / quota exceeded -> brief backoff, retry same model twice, then move to next candidate.
    """
    switches = 0
    while True:
        model_name = _resolve_working_model()
        backoffs = [2, 5]

        for attempt in range(len(backoffs) + 1):
            try:
                response = await loop.run_in_executor(
                    None,
                    lambda: client.models.generate_content(
                        model=model_name,
                        contents=prompt,
                        config=CONFIG
                    )
                )
                return response
            except Exception as e:
                msg = str(e).lower()

                if "429" in msg or "quota" in msg:
                    if attempt < len(backoffs):
                        wait = backoffs[attempt]
                        print(f"⏳ '{model_name}' rate-limited, retrying in {wait}s...")
                        await asyncio.sleep(wait)
                        continue
                    print(f"🚫 '{model_name}' still rate-limited after retries. Switching model...")
                    _invalidate_active_model()
                    break

                if "404" in msg or "no longer available" in msg or "not found" in msg:
                    print(f"⚠️ Candidate '{model_name}' unavailable.")
                    _invalidate_active_model()
                    break

                raise e  # safety blocks etc. bubble straight up

        switches += 1
        if switches >= max_model_switches:
            raise RuntimeError(f"Gemini failing on every available model (last: {model_name}).")


# --- CACHE HELPERS ---
def get_gemini_cache(lat, lon, crop_name):
    key = f"gemini_{round(lat, 2)}_{round(lon, 2)}_{crop_name.lower()}"
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [r[0] for r in cursor.fetchall()]
        table_name = "crop_cache" if "crop_cache" in tables else "api_cache"
        if table_name not in tables:
            return None
        cursor.execute(f"SELECT data, timestamp FROM {table_name} WHERE location_key = ?", (key,))
        result = cursor.fetchone()
        conn.close()
        if result:
            data_json, timestamp = result
            if time.time() - timestamp < 86400:
                print(f"⚡ CACHE HIT: Found AI advice for {key}")
                return json.loads(data_json)
    except Exception as e:
        print(f"⚠️ Cache Read Error: {e}")
    return None


def save_gemini_cache(lat, lon, crop_name, data, ttl_seconds=86400):
    key = f"gemini_{round(lat, 2)}_{round(lon, 2)}_{crop_name.lower()}"
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [r[0] for r in cursor.fetchall()]
        table_name = "crop_cache" if "crop_cache" in tables else "api_cache"
        if table_name not in tables:
            return
        cursor.execute(f'''
            INSERT OR REPLACE INTO {table_name} (location_key, data, timestamp)
            VALUES (?, ?, ?)
        ''', (key, json.dumps(data), time.time()))
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"⚠️ Cache Write Error: {e}")


def clean_and_parse_json(text):
    try:
        text = text.replace("```json", "").replace("```", "").strip()
        match = re.search(r'\{.*\}', text, re.DOTALL)
        if match:
            text = match.group(0)
        return json.loads(text)
    except Exception:
        return None

# --- HELPER FUNCTIONS FOR GEMINI SERVICE ---

def _build_prompt(weather_data, crop_input, market_data):
    """Constructs a structured JSON prompt for Gemini."""
    return f"""
You are an expert agronomist AI analyzing farming data.
Analyze the following crop conditions and provide actionable insights.

CROP & LOCATION:
- Crop: {crop_input.crop}
- Latitude: {crop_input.lat}, Longitude: {crop_input.lon}

WEATHER DATA:
{json.dumps(weather_data, indent=2)}

MARKET DATA:
{json.dumps(market_data, indent=2)}

Respond strictly in valid JSON format with the following keys:
{{
  "recommendations": ["list of actionable advice"],
  "risk_assessment": "low/medium/high description",
  "market_insight": "summary of market trend",
  "yield_forecast": "estimated impact"
}}
"""


def _parse_gemini_response(response_text):
    """Cleans and parses JSON returned by Gemini."""
    parsed = clean_and_parse_json(response_text)
    if parsed:
        return parsed
    return {
        "recommendations": [response_text],
        "risk_assessment": "Unknown (unstructured response)",
        "market_insight": "N/A",
        "yield_forecast": "N/A"
    }

def _get_fallback_data(crop_name, error_msg):
    """Provides safe default fallback data if Gemini API fails."""
    return {
        "recommendations": [
            f"Unable to fetch live AI advice for {crop_name} due to service error.",
            "Follow standard regional agronomic practices and monitor soil moisture."
        ],
        "risk_assessment": "Data currently unavailable",
        "market_insight": "Check local mandi rates manually.",
        "yield_forecast": "Neutral",
        "error_details": error_msg
    }

async def get_gemini_advice(weather_data, crop_input, market_data):
    # 1. Define cache_key upfront
    lat_round = round(crop_input.lat, 2)
    lon_round = round(crop_input.lon, 2)
    crop_clean = crop_input.crop.lower().strip()
    cache_key = f"gemini_{lat_round}_{lon_round}_{crop_clean}"

    # 2. Cache check
    cached_data = get_gemini_cache(crop_input.lat, crop_input.lon, crop_input.crop)
    if cached_data and not cached_data.get("_is_fallback"):
        return cached_data

    try:
        prompt = _build_prompt(weather_data, crop_input, market_data)
        loop = asyncio.get_running_loop()
        response = await _generate_with_failover(prompt, loop)
        
        # Parse response...
        parsed_data = _parse_gemini_response(response.text)
        
        # 1. SUCCESS: Save to cache with default 24h TTL (86400s)
        save_gemini_cache(crop_input.lat, crop_input.lon, crop_input.crop, parsed_data, ttl_seconds=86400)
        return parsed_data

    except Exception as e:
        print(f"❌ Gemini Service Error: {e}")
        fallback_data = _get_fallback_data(crop_input.crop, str(e))
        fallback_data["_is_fallback"] = True
        
        # 2. FALLBACK: Save to cache with short 5m TTL (300s)
        save_gemini_cache(crop_input.lat, crop_input.lon, crop_input.crop, fallback_data, ttl_seconds=300)
        return fallback_data