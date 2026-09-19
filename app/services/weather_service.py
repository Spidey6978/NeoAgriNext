import httpx
import sqlite3
import json
import time
from app.config import settings
from app.services.cache_service import DB_NAME

# Remove hardcoded key if it exists, rely on settings
BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

# --- CACHE HELPERS ---
def get_weather_cache(lat, lon):
    """
    Retrieves weather data from SQLite.
    Key Format: weather_{lat}_{lon}
    Expiry: 30 Minutes (1800s) - Weather updates frequently.
    """
    key = f"weather_{round(lat, 2)}_{round(lon, 2)}"
    
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        # reusing the 'crop_cache' table 
        cursor.execute("SELECT data, timestamp FROM crop_cache WHERE location_key = ?", (key,))
        result = cursor.fetchone()
        conn.close()

        if result:
            data_json, timestamp = result
            if time.time() - timestamp < 1800: # 30 Minute Cache
                print(f"⚡ CACHE HIT: Found weather data for {key}")
                return json.loads(data_json)
    except Exception as e:
        print(f"⚠️ Cache Read Error: {e}")
    return None

def save_weather_cache(lat, lon, data):
    """Saves weather data to SQLite."""
    key = f"weather_{round(lat, 2)}_{round(lon, 2)}"
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute('''
            INSERT OR REPLACE INTO crop_cache (location_key, data, timestamp)
            VALUES (?, ?, ?)
        ''', (key, json.dumps(data), time.time()))
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"⚠️ Cache Write Error: {e}")

async def get_current_weather(lat: float, lon: float):
    """
    Fetches real-time weather from OpenWeatherMap.
    """
    # 1. CHECK CACHE
    cached_data = get_weather_cache(lat, lon)
    if cached_data:
        return cached_data

    params = {
        "lat": lat,
        "lon": lon,
        "appid": settings.OPENWEATHER_API_KEY,
        "units": "metric"  # Get Celsius
    }
    
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(BASE_URL, params=params)
            data = response.json()
            
            if response.status_code == 200:
                result = {
                    "temp": data["main"]["temp"],
                    "humidity": data["main"]["humidity"],
                    "description": data["weather"][0]["description"],
                    "location": data["name"],
                    "wind_speed": data["wind"]["speed"]
                }
                # 2. SAVE CACHE
                save_weather_cache(lat, lon, result)
                return result
            else:
                # Fallback if API key is wrong or limit reached
                return {"error": data.get("message", "Unknown error")}
                
        except Exception as e:
            return {"error": str(e)}