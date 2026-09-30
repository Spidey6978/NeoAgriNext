import httpx
from app.config import settings
from geopy.geocoders import Nominatim
MAPBOX_GEOCODE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/{lon},{lat}.json"


async def reverse_geocode_mapbox(lat: float, lon: float):
    """
    Mapbox Geocoding — swapped in for Google Maps Platform due to the
    India billing-verification deposit. Falls back to OpenStreetMap/
    Nominatim if the token is missing or the call fails.
    """
    if settings.MAPBOX_ACCESS_TOKEN:
        try:
            url = MAPBOX_GEOCODE_URL.format(lon=lon, lat=lat)
            params = {
                "access_token": settings.MAPBOX_ACCESS_TOKEN,
                "types": "place,district,region,locality",
                "limit": 1,
            }
            async with httpx.AsyncClient(timeout=8) as client:
                resp = await client.get(url, params=params)
                resp.raise_for_status()
                data = resp.json()

            features = data.get("features", [])
            if features:
                feature = features[0]
                context = feature.get("context", [])

                district, state = "", ""
                for ctx in context:
                    ctx_id = ctx.get("id", "")
                    if ctx_id.startswith("district"):
                        district = ctx.get("text", "")
                    elif ctx_id.startswith("region"):
                        state = ctx.get("text", "")

                sublocality = feature.get("text", "")

                return {
                    "district": district or sublocality,
                    "state": state,
                    "sublocality": sublocality,
                    "formatted_address": feature.get("place_name", ""),
                    "source": "Mapbox Geocoding API",
                }
        except Exception as e:
            print(f"⚠️ Mapbox Geocoding failed ({e}); falling back to Nominatim.")
    else:
        print("⚠️ MAPBOX_ACCESS_TOKEN not set; using Nominatim fallback.")

    try:
        geolocator = Nominatim(user_agent=settings.USER_AGENT)
        loc = geolocator.reverse((lat, lon), language="en", timeout=5)
        if loc:
            addr = loc.raw.get("address", {})
            return {
                "district": addr.get("state_district") or addr.get("county", ""),
                "state": addr.get("state", ""),
                "sublocality": addr.get("suburb") or addr.get("village", ""),
                "formatted_address": loc.address,
                "source": "OpenStreetMap Nominatim (fallback)",
            }
    except Exception as e:
        print(f"⚠️ Nominatim fallback also failed: {e}")

    return {"district": "", "state": "", "sublocality": "", "formatted_address": "", "source": "unavailable"}