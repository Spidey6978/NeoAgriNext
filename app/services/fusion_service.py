import asyncio
import time
from app.services.ndvi_service import get_satellite_analysis
from app.services.weather_service import get_hyperlocal_weather
from app.services.regenerative_engine import _classify_npk, _classify_ndvi, _classify_rainfall


async def get_fused_farm_snapshot(lat: float, lon: float, n: float = 280, p: float = 15, k: float = 150):
    """Fuses satellite telemetry, weather, and soil health into ONE payload — ready to sync downstream."""
    satellite, hyperlocal_weather = await asyncio.gather(
        get_satellite_analysis(lat, lon),
        get_hyperlocal_weather(lat, lon),
    )

    ow = hyperlocal_weather.get("openweathermap") or {}
    ndvi_class = _classify_ndvi(satellite.get("ndvi_current", 0.4), satellite.get("trend", ""))
    rainfall_class = _classify_rainfall(ow)
    soil_status = _classify_npk(n, p, k)

    return {
        "timestamp": time.time(),
        "location": {
            "lat": lat, "lon": lon,
            "district": hyperlocal_weather.get("location", {}).get("district"),
            "state": hyperlocal_weather.get("location", {}).get("state"),
        },
        "satellite": satellite,
        "weather": hyperlocal_weather,
        "soil": {"npk_kg_ha": {"n": n, "p": p, "k": k}, "status": soil_status},
        "derived": {"ndvi_class": ndvi_class, "rainfall_class": rainfall_class},
    }