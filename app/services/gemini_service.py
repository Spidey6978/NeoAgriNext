import json
import asyncio
import re
from google import genai
from google.genai import types
from app.config import settings
from app.schemas.input_schema import CropInput
from app.services.cache_service import get_gemini_cache, save_gemini_cache

# --- CONFIGURATION ---
client = genai.Client(api_key=settings.GEMINI_API_KEY)

SAFETY_SETTINGS = [
    types.SafetySetting(category=types.HarmCategory.HARM_CATEGORY_HARASSMENT, threshold=types.HarmBlockThreshold.BLOCK_NONE),
    types.SafetySetting(category=types.HarmCategory.HARM_CATEGORY_HATE_SPEECH, threshold=types.HarmBlockThreshold.BLOCK_NONE),
    types.SafetySetting(category=types.HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, threshold=types.HarmBlockThreshold.BLOCK_NONE),
    types.SafetySetting(category=types.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, threshold=types.HarmBlockThreshold.BLOCK_NONE),
]
CONFIG = types.GenerateContentConfig(safety_settings=SAFETY_SETTINGS)

# Confirmed present in your own client.models.list() output. Ordered so
# early failures land on a DIFFERENT model family (separate free-tier quota).
MODEL_CANDIDATES = [
    getattr(settings, "GEMINI_MODEL", None),
    "gemini-3.8-flash",        # Google's explicit recommended replacement for 2.5-flash
    "gemini-flash-latest",
    "gemini-2.5-flash-lite",
    "gemini-flash-lite-latest",
    "gemini-3-flash-preview",
    "gemini-3.5-flash",
    "gemini-pro-latest",
    "gemini-2.5-pro",
]
MODEL_CANDIDATES = [m for m in dict.fromkeys(MODEL_CANDIDATES) if m]

_ACTIVE_MODEL_NAME = None
_BLACKLISTED_MODELS = set()


def _list_available_models():
    try:
        available = set()
        for m in client.models.list(config={"query_base": True}):
            name = m.name.replace("models/", "") if hasattr(m, "name") else str(m)
            available.add(name)
        return available
    except Exception as e:
        print(f"⚠️ Could not list Gemini models ({e}); trying candidates blind.")
        return None


def _pick_next_candidate(available):
    for candidate in MODEL_CANDIDATES:
        if candidate in _BLACKLISTED_MODELS:
            continue
        if available is not None and candidate not in available:
            continue
        return candidate
    return None


def _invalidate_active_model():
    global _ACTIVE_MODEL_NAME
    if _ACTIVE_MODEL_NAME:
        _BLACKLISTED_MODELS.add(_ACTIVE_MODEL_NAME)
        print(f"🚫 Blacklisting '{_ACTIVE_MODEL_NAME}' for the rest of this run.")
    _ACTIVE_MODEL_NAME = None


async def _generate_with_failover(prompt, loop, max_model_switches=6):
    global _ACTIVE_MODEL_NAME
    available = _list_available_models()
    switches = 0

    while True:
        if _ACTIVE_MODEL_NAME is None:
            _ACTIVE_MODEL_NAME = _pick_next_candidate(available)
            if _ACTIVE_MODEL_NAME is None:
                raise RuntimeError(f"No working Gemini model left. Blacklisted: {_BLACKLISTED_MODELS or 'none'}")
            print(f"➡️ Trying Gemini model: {_ACTIVE_MODEL_NAME}")

        model_name = _ACTIVE_MODEL_NAME
        backoffs = [2, 5]

        for attempt in range(len(backoffs) + 1):
            try:
                response = await loop.run_in_executor(
                    None,
                    lambda: client.models.generate_content(model=model_name, contents=prompt, config=CONFIG),
                )
                print(f"✅ Gemini responded successfully using: {model_name}")
                return response
            except Exception as e:
                msg = str(e).lower()
                print(f"❌ '{model_name}' failed: {e}")

                if "429" in msg or "quota" in msg or "503" in msg or "unavailable" in msg or "high demand" in msg:
                    if attempt < len(backoffs):
                        wait = backoffs[attempt]
                        print(f"⏳ '{model_name}' rate-limited, retrying in {wait}s...")
                        await asyncio.sleep(wait)
                        continue
                    print(f"🚫 '{model_name}' still rate-limited after retries. Switching model...")
                    _invalidate_active_model()
                    break

                if "404" in msg or "no longer available" in msg or "not found" in msg:
                    _invalidate_active_model()
                    break

                raise

        switches += 1
        if switches >= max_model_switches:
            raise RuntimeError(f"Gemini failing on every available model (last: {model_name}).")


def clean_and_parse_json(text):
    try:
        text = text.replace("```json", "").replace("```", "").strip()
        match = re.search(r'\{.*\}', text, re.DOTALL)
        if match:
            text = match.group(0)
        return json.loads(text)
    except Exception:
        return None


def _build_prompt(weather_data, crop_input, market_data):
    """Matches AdvisoryResponse / CropRoadmap in input_schema.py and the Android app's expected shape."""
    region_str = market_data.get('meta', {}).get('region', 'India')
    currency_symbol = market_data.get('meta', {}).get('currency', '₹')
    expert_persona = (
        "Act as an expert Indian Agronomist."
        if currency_symbol == '₹' or "India" in region_str
        else "Act as an expert International Agronomist."
    )

    return f"""
    {expert_persona} Provide analysis for a farmer growing {crop_input.crop} in {region_str}.

    DATA:
    - Crop: {crop_input.crop}
    - Market Strategy: {market_data.get('advisory', {}).get('action', 'N/A')} ({market_data.get('advisory', {}).get('reason', 'N/A')})
    - Weather: {weather_data.get('description')}, Temp: {weather_data.get('temp')}°C

    TASK:
    Return a JSON object with this EXACT structure:
    {{
        "explanation": "A short 3-sentence advice summary.",
        "roadmap": {{
            "name": "{crop_input.crop}",
            "temp_range": "e.g. 20-30C",
            "ph_range": "e.g. 6.0-7.0",
            "water_requirement": "e.g. 500mm",
            "sowing_window": "Best sowing months",
            "harvest_window": "Best harvest months",
            "duration_days": 120,
            "fertilizer_plan": "Short fertilizer tip",
            "pests": ["Pest1", "Pest2"]
        }}
    }}
    """


def _get_fallback_data(crop_name, error_msg):
    return {
        "explanation": f"Detailed AI advice is currently unavailable ({error_msg[:50]}). Please follow standard agricultural practices for {crop_name}.",
        "roadmap": {
            "name": crop_name,
            "temp_range": "20-30°C",
            "ph_range": "6.0-7.0",
            "water_requirement": "Moderate",
            "sowing_window": "June-July",
            "harvest_window": "Oct-Nov",
            "duration_days": 120,
            "fertilizer_plan": "Apply balanced NPK fertilizer.",
            "pests": ["Aphids", "Bollworms"],
        },
    }


async def get_gemini_advice(weather_data, crop_input: CropInput, market_data):
    cached_data = get_gemini_cache(crop_input.lat, crop_input.lon, crop_input.crop)
    if cached_data and not cached_data.get("_is_fallback"):
        return cached_data

    try:
        prompt = _build_prompt(weather_data, crop_input, market_data)
        loop = asyncio.get_running_loop()
        response = await _generate_with_failover(prompt, loop)

        data = clean_and_parse_json(response.text)
        if not data:
            raise ValueError("Could not parse JSON from AI response")

        if "roadmap" in data:
            try:
                val = str(data["roadmap"].get("duration_days", 120))
                matches = re.findall(r'\d+', val)
                data["roadmap"]["duration_days"] = int(matches[0]) if matches else 120
            except Exception:
                data["roadmap"]["duration_days"] = 120
            if not isinstance(data["roadmap"].get("pests"), list):
                data["roadmap"]["pests"] = ["Common Pests"]

        save_gemini_cache(crop_input.lat, crop_input.lon, crop_input.crop, data, ttl_seconds=86400)
        return data

    except Exception as e:
        print(f"❌ Gemini Service Error: {e}")
        fallback_data = _get_fallback_data(crop_input.crop, str(e))
        fallback_data["_is_fallback"] = True
        save_gemini_cache(crop_input.lat, crop_input.lon, crop_input.crop, fallback_data, ttl_seconds=300)
        return fallback_data