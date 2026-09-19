import google.generativeai as genai
import json
import sqlite3
import time
import asyncio
import re
from app.config import settings
from app.schemas.input_schema import CropInput
from app.services.cache_service import DB_NAME

# --- CONFIGURATION ---
genai.configure(api_key=settings.GEMINI_API_KEY)

# CRITICAL: Disable safety filters for Agriculture (Pesticides/Chemicals often trigger false positives)
SAFETY_SETTINGS = [
    {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
    {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
    {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
    {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"},
]

model = genai.GenerativeModel(settings.GEMINI_MODEL, safety_settings=SAFETY_SETTINGS)

# --- CACHE HELPERS ---
def get_gemini_cache(lat, lon, crop_name):
    """Retrieves AI advice from SQLite."""
    key = f"gemini_{round(lat, 2)}_{round(lon, 2)}_{crop_name.lower()}"
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        
        # Check table existence to avoid crash on fresh DB
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [r[0] for r in cursor.fetchall()]
        table_name = "crop_cache" if "crop_cache" in tables else "api_cache"
        if table_name not in tables: return None

        cursor.execute(f"SELECT data, timestamp FROM {table_name} WHERE location_key = ?", (key,))
        result = cursor.fetchone()
        conn.close()

        if result:
            data_json, timestamp = result
            if time.time() - timestamp < 86400: # 24 Hour Cache
                print(f"⚡ CACHE HIT: Found AI advice for {key}")
                return json.loads(data_json)
    except Exception as e:
        print(f"⚠️ Cache Read Error: {e}")
    return None

def save_gemini_cache(lat, lon, crop_name, data):
    """Saves AI advice to SQLite."""
    key = f"gemini_{round(lat, 2)}_{round(lon, 2)}_{crop_name.lower()}"
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [r[0] for r in cursor.fetchall()]
        # Default to crop_cache if it exists, otherwise api_cache
        table_name = "crop_cache" if "crop_cache" in tables else "api_cache"
        
        # Just in case table doesn't exist yet (rare race condition)
        if table_name not in tables: return

        cursor.execute(f'''
            INSERT OR REPLACE INTO {table_name} (location_key, data, timestamp)
            VALUES (?, ?, ?)
        ''', (key, json.dumps(data), time.time()))
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"⚠️ Cache Write Error: {e}")

# --- ROBUST JSON CLEANER ---
def clean_and_parse_json(text):
    """Extracts valid JSON from Gemini's response using Regex."""
    try:
        # 1. Remove Markdown
        text = text.replace("```json", "").replace("```", "").strip()
        
        # 2. Regex to find the first '{' and last '}' (Handles intro text)
        match = re.search(r'\{.*\}', text, re.DOTALL)
        if match:
            text = match.group(0)
            
        return json.loads(text)
    except Exception:
        return None

async def get_gemini_advice(weather_data: dict, crop_data: CropInput, market_data: dict):
    # 1. CHECK CACHE
    cached_data = get_gemini_cache(crop_data.lat, crop_data.lon, crop_data.crop)
    if cached_data:
        return cached_data

    # Get event loop for non-blocking call
    loop = asyncio.get_event_loop()

    try:
        # --- DYNAMIC PERSONA SELECTION ---
        # Detect region from market data to set the correct expert persona
        region_str = market_data.get('meta', {}).get('region', 'India')
        currency_symbol = market_data.get('meta', {}).get('currency', '₹')
        
        if currency_symbol == '₹' or "India" in region_str:
            expert_persona = "Act as an expert Indian Agronomist."
        else:
            expert_persona = "Act as an expert International Agronomist."

        prompt = f"""
        {expert_persona} Provide analysis for a farmer growing {crop_data.crop} in {region_str}.
        
        DATA:
        - Crop: {crop_data.crop}
        - Market Strategy: {market_data['advisory']['action']} ({market_data['advisory']['reason']})
        - Weather: {weather_data.get('description')}, Temp: {weather_data.get('temp')}°C
        
        TASK:
        Return a JSON object with this EXACT structure:
        {{
            "explanation": "A short 3-sentence advice summary.",
            "roadmap": {{
                "name": "{crop_data.crop}",
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

        # 2. RUN API CALL (Non-Blocking)
        response = await loop.run_in_executor(None, model.generate_content, prompt)
        
        # Check for safety blocks
        if not response.parts:
             print(f"⚠️ Gemini Safety Block Triggered: {response.prompt_feedback}")
             raise ValueError("AI blocked the content due to safety filters.")

        # 3. ROBUST PARSING
        data = clean_and_parse_json(response.text)
        if not data:
            raise ValueError("Could not parse JSON from AI response")

        # 4. TYPE ENFORCEMENT (Crucial for Android)
        if "roadmap" in data:
            try:
                # Ensure duration_days is INT (Android crashes if String)
                val = str(data["roadmap"].get("duration_days", 120))
                # Extract numbers from string like "120 days"
                matches = re.findall(r'\d+', val)
                if matches:
                    data["roadmap"]["duration_days"] = int(matches[0])
                else:
                    data["roadmap"]["duration_days"] = 120
            except:
                data["roadmap"]["duration_days"] = 120
            
            # Ensure pests is a LIST
            if not isinstance(data["roadmap"].get("pests"), list):
                data["roadmap"]["pests"] = ["Common Pests"]

        # 5. SAVE CACHE & RETURN
        save_gemini_cache(crop_data.lat, crop_data.lon, crop_data.crop, data)
        return data

    except Exception as e:
        print(f"❌ Gemini Service Error: {e}")
        
        # 6. FAIL-SAFE FALLBACK (So App doesn't crash)
        fallback_data = {
            "explanation": f"Detailed AI advice is currently unavailable ({str(e)[:50]}). Please follow standard agricultural practices for {crop_data.crop}.",
            "roadmap": {
                "name": crop_data.crop,
                "temp_range": "20-30°C",
                "ph_range": "6.0-7.0",
                "water_requirement": "Moderate",
                "sowing_window": "June-July",
                "harvest_window": "Oct-Nov",
                "duration_days": 120,
                "fertilizer_plan": "Apply balanced NPK fertilizer.",
                "pests": ["Aphids", "Bollworms"]
            }
        }
        
        # Save fallback to cache so dashboard shows the error state
        save_gemini_cache(crop_data.lat, crop_data.lon, crop_data.crop, fallback_data)
        
        return fallback_data