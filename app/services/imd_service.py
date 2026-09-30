import httpx
import time
import json
import sqlite3
from geopy.distance import geodesic
from app.services.cache_service import DB_NAME
from app.config import settings

# From IMD's own API reference (https://api.imd.gov.in/public/api_reference.html)
IMD_STATE_IDS = {
    "TELANGANA": 1, "ANDHRA PRADESH": 2, "HIMACHAL PRADESH": 3, "KERALA": 4,
    "UTTAR PRADESH": 5, "MEGHALAYA": 6, "DELHI": 7, "RAJASTHAN": 8,
    "GUJARAT": 9, "ODISHA": 10, "BIHAR": 11, "CHHATTISGARH": 12,
    "KARNATAKA": 13, "MIZORAM": 14, "JHARKHAND": 15, "TRIPURA": 16,
    "CHANDIGARH": 17, "JAMMU AND KASHMIR": 18, "GOA": 19, "SIKKIM": 20,
    "MAHARASHTRA": 21, "HARYANA": 22, "LADAKH": 23, "ASSAM": 24,
    "TAMIL NADU": 25, "WEST BENGAL": 26, "MADHYA PRADESH": 27,
    "ARUNACHAL PRADESH": 28, "LAKSHADWEEP": 29, "MANIPUR": 30,
    "UTTARAKHAND": 31, "NAGALAND": 32, "PUDUCHERRY": 33,
    "PUNJAB": 34, "ANDAMAN AND NICOBAR": 35, "DAMAN AND DIU": 36,
}


def _cache_get(key, max_age=900):
    try:
        conn = sqlite3.connect(DB_NAME)
        cur = conn.cursor()
        cur.execute("SELECT data, timestamp FROM crop_cache WHERE location_key = ?", (key,))
        row = cur.fetchone()
        conn.close()
        if row:
            data_json, ts = row
            if time.time() - ts < max_age:
                return json.loads(data_json)
    except Exception:
        pass
    return None


def _cache_set(key, data):
    try:
        conn = sqlite3.connect(DB_NAME)
        cur = conn.cursor()
        cur.execute(
            """INSERT OR REPLACE INTO crop_cache (location_key, data, timestamp, data_type)
               VALUES (?, ?, ?, ?)""",
            (key, json.dumps(data), time.time(), "imd_aws"),
        )
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"⚠️ IMD Cache Write Error: {e}")


async def _fetch_state_stations(state_id: int):
    cache_key = f"imd_state_{state_id}"
    cached = _cache_get(cache_key)
    if cached is not None:
        return cached

    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.get(settings.IMD_AWS_URL, params={"sid": state_id})
        resp.raise_for_status()
        data = resp.json()
        stations = data if isinstance(data, list) else data.get("data", [])
        if stations:
            _cache_set(cache_key, stations)
        return stations


def _nearest_station(lat, lon, stations):
    best, best_dist = None, None
    for st in stations:
        try:
            s_lat = float(st.get("Latitude"))
            s_lon = float(st.get("Longitude"))
        except (TypeError, ValueError):
            continue
        dist = geodesic((lat, lon), (s_lat, s_lon)).km
        if best_dist is None or dist < best_dist:
            best, best_dist = st, dist
    return best, best_dist


async def get_imd_ground_reading(lat: float, lon: float, state_name: str):
    """
    Real-first, fail-soft: returns the nearest IMD AWS/ARG ground station
    reading for this state, or None if IMD is unreachable / state unmapped.
    """
    state_id = IMD_STATE_IDS.get((state_name or "").strip().upper())
    if not state_id:
        return None

    try:
        stations = await _fetch_state_stations(state_id)
        if not stations:
            return None

        station, distance_km = _nearest_station(lat, lon, stations)
        if not station:
            return None

        def _f(key):
            v = station.get(key)
            try:
                return float(v)
            except (TypeError, ValueError):
                return None

        return {
            "station": station.get("STATION"),
            "district": station.get("DISTRICT"),
            "distance_km": round(distance_km, 1),
            "observed_at": f"{station.get('DATE')} {station.get('TIME')} UTC",
            "temp_c": _f("CURR_TEMP"),
            "humidity_pct": _f("RH"),
            "wind_speed_kmph": _f("WIND_SPEED"),
            "mslp_hpa": _f("MSLP"),
            "rainfall_24h_mm": None,  # Dropped "Feel Like" to prevent data corruption
            "source": "IMD AWS/ARG (ground station)",
        }
    except Exception as e:
        print(f"⚠️ IMD Fetch Error: {e}")
        return None