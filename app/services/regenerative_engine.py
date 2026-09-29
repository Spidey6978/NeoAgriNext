"""
Day 2 — Regenerative Crop Recommendation Engine.
Combines soil N-P-K, satellite NDVI trend, and rainfall into a single,
explainable recommendation.
"""

from app.services.ndvi_service import get_satellite_analysis
from app.services.weather_service import get_current_weather

REGEN_CROP_LIBRARY = [
    {"name": "Moong (Green Gram)", "fixes_nitrogen": True, "water_need": "low", "duration_days": 65,
     "note": "Nitrogen-fixing legume; ideal short-duration soil restorer between main crops."},
    {"name": "Chickpea (Chana)", "fixes_nitrogen": True, "water_need": "low", "duration_days": 100,
     "note": "Deep-rooted legume, improves soil structure and fixes nitrogen."},
    {"name": "Soybean", "fixes_nitrogen": True, "water_need": "medium", "duration_days": 100,
     "note": "High-value nitrogen-fixer, good rotation crop after cereals."},
    {"name": "Sunn Hemp (Green Manure)", "fixes_nitrogen": True, "water_need": "low", "duration_days": 60,
     "note": "Classic green-manure cover crop; tilled back into soil to rebuild organic matter."},
    {"name": "Pearl Millet (Bajra)", "fixes_nitrogen": False, "water_need": "low", "duration_days": 80,
     "note": "Extremely drought-tolerant; stabilizes yield on degraded soil in low rainfall."},
    {"name": "Sorghum (Jowar)", "fixes_nitrogen": False, "water_need": "low", "duration_days": 100,
     "note": "Deep roots reduce topsoil erosion; strong drought tolerance."},
    {"name": "Sugarcane", "fixes_nitrogen": False, "water_need": "high", "duration_days": 365,
     "note": "High water/nutrient demand — only advisable where soil health and rainfall are both strong."},
    {"name": "Rice (Paddy)", "fixes_nitrogen": False, "water_need": "high", "duration_days": 120,
     "note": "Needs sustained water availability; not soil-restorative on its own."},
]


def _classify_ndvi(ndvi_current: float, trend: str):
    if ndvi_current < 0.3:
        return "degraded"
    if "declin" in (trend or "").lower():
        return "declining"
    return "healthy"


def _classify_rainfall(weather_data: dict):
    rain_mm = 0
    if isinstance(weather_data.get("rain"), dict):
        rain_mm = weather_data["rain"].get("1h", 0) or weather_data["rain"].get("3h", 0) or 0
    humidity = weather_data.get("humidity", 50)
    if rain_mm > 5 or humidity > 80:
        return "high"
    if rain_mm > 0 or humidity > 55:
        return "medium"
    return "low"


def _classify_npk(n: float, p: float, k: float):
    n_status = "low" if n < 280 else ("medium" if n < 560 else "high")
    p_status = "low" if p < 10 else ("medium" if p < 25 else "high")
    k_status = "low" if k < 110 else ("medium" if k < 280 else "high")
    return {"nitrogen": n_status, "phosphorus": p_status, "potassium": k_status}


def score_crop(crop, soil_status, rainfall_class, ndvi_class):
    score = 50
    if ndvi_class in ("degraded", "declining") and crop["fixes_nitrogen"]:
        score += 30
    if soil_status["nitrogen"] == "low" and crop["fixes_nitrogen"]:
        score += 25
    if rainfall_class == "low" and crop["water_need"] == "low":
        score += 20
    elif rainfall_class == "high" and crop["water_need"] == "high":
        score += 15
    elif rainfall_class == "low" and crop["water_need"] == "high":
        score -= 35
    elif rainfall_class == "high" and crop["water_need"] == "low":
        score -= 5
    if ndvi_class == "degraded" and crop["duration_days"] <= 90:
        score += 10
    return max(0, min(100, score))


async def get_regenerative_recommendation(
    lat: float, lon: float,
    nitrogen_kg_ha: float = 280, phosphorus_kg_ha: float = 15, potassium_kg_ha: float = 150,
):
    """N-P-K defaults are ICAR's 'medium fertility' reference bands, used only when no real soil-test reading is supplied."""
    satellite = await get_satellite_analysis(lat, lon)
    weather = await get_current_weather(lat, lon)

    ndvi_class = _classify_ndvi(satellite.get("ndvi_current", 0.4), satellite.get("trend", ""))
    rainfall_class = _classify_rainfall(weather)
    soil_status = _classify_npk(nitrogen_kg_ha, phosphorus_kg_ha, potassium_kg_ha)

    ranked = sorted(REGEN_CROP_LIBRARY, key=lambda c: score_crop(c, soil_status, rainfall_class, ndvi_class), reverse=True)

    recommendations = [
        {
            "name": c["name"], "score": score_crop(c, soil_status, rainfall_class, ndvi_class),
            "duration_days": c["duration_days"], "fixes_nitrogen": c["fixes_nitrogen"],
            "water_need": c["water_need"], "why": c["note"],
        }
        for c in ranked[:4]
    ]

    return {
        "location": {"lat": lat, "lon": lon},
        "inputs": {
            "soil_npk_kg_ha": {"n": nitrogen_kg_ha, "p": phosphorus_kg_ha, "k": potassium_kg_ha},
            "soil_status": soil_status,
            "ndvi_current": satellite.get("ndvi_current"),
            "ndvi_class": ndvi_class,
            "rainfall_class": rainfall_class,
        },
        "recommendations": recommendations,
    }