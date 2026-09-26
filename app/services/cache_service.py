import os
import sqlite3
import json
import time

_BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DB_NAME = os.path.join(_BASE_DIR, "agrinext_cache.db")


def init_db():
    print(f"💽 CACHE DB path: {DB_NAME}")
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    
    # 1. Unified Table: Stores everything in a flat, queryable format
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS smart_farm_data (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            location_key TEXT UNIQUE,
            latitude REAL,
            longitude REAL,
            crop_name TEXT,
            
            -- Raw JSON Blobs
            weather_json TEXT,
            market_json TEXT,
            gemini_json TEXT,
            satellite_json TEXT,
            
            -- Meta
            timestamp REAL,
            source_type TEXT DEFAULT 'live'
        )
    ''')
    
    # 2. Fast Key-Value Cache (For instant App response)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS crop_cache (
            location_key TEXT PRIMARY KEY,
            data TEXT,
            timestamp REAL,
            data_type TEXT
        )
    ''')
    
    conn.commit()
    conn.close()
    print("💽 CACHE: Smart Farm Database Initialized (ML-Ready).")

def get_from_cache(key: str, max_age_seconds: int = 86400):
    """Retrieves from the fast key-value store."""
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        cursor.execute("SELECT data, timestamp FROM crop_cache WHERE location_key = ?", (key,))
        result = cursor.fetchone()
        conn.close()

        if result:
            data_json, timestamp = result
            if time.time() - timestamp < max_age_seconds:
                print(f"⚡ CACHE HIT: {key}")
                return json.loads(data_json)
    except Exception:
        pass
    return None

def save_to_cache(key: str, data: dict, data_type: str):
    """Saves to both Fast Cache and ML Storage."""
    try:
        conn = sqlite3.connect(DB_NAME)
        cursor = conn.cursor()
        
        # 1. Save to Fast Cache
        cursor.execute('''
            INSERT OR REPLACE INTO crop_cache (location_key, data, timestamp, data_type)
            VALUES (?, ?, ?, ?)
        ''', (key, json.dumps(data), time.time(), data_type))
        
        # 2. Save to ML Storage (Only if it's a full analysis result)
        if data_type == "full_analysis":
            # Extract features from the composite key or data object if available
            cursor.execute('''
                INSERT OR IGNORE INTO smart_farm_data (location_key, timestamp)
                VALUES (?, ?)
            ''', (key, time.time()))

        conn.commit()
        conn.close()
        print(f"💾 CACHE SAVED: {key}")
    except Exception as e:
        print(f"⚠️ Cache Write Error: {e}")

# --- HELPER FUNCTIONS ---
def get_crop_cache(lat, lon):
    return get_from_cache(f"crop_{round(lat, 2)}_{round(lon, 2)}")

def save_crop_cache(lat, lon, data):
    save_to_cache(f"crop_{round(lat, 2)}_{round(lon, 2)}", data, "crop_recommendation")

def get_market_cache(lat, lon, crop):
    return get_from_cache(f"market_{round(lat, 2)}_{round(lon, 2)}_{crop.lower()}", 3600)

def save_market_cache(lat, lon, crop, data):
    save_to_cache(f"market_{round(lat, 2)}_{round(lon, 2)}_{crop.lower()}", data, "market_analysis")

def get_weather_cache(lat, lon):
    return get_from_cache(f"weather_{round(lat, 2)}_{round(lon, 2)}", 1800)

def save_weather_cache(lat, lon, data):
    save_to_cache(f"weather_{round(lat, 2)}_{round(lon, 2)}", data, "weather")

def save_gemini_cache(lat, lon, crop, data, ttl_seconds=86400):
    save_to_cache(f"gemini_{round(lat, 2)}_{round(lon, 2)}_{crop.lower()}", data, "gemini_advice")

def get_gemini_cache(lat, lon, crop, max_age_seconds=86400):
    return get_from_cache(f"gemini_{round(lat, 2)}_{round(lon, 2)}_{crop.lower()}", max_age_seconds)