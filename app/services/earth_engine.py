import os
import datetime
from app.config import settings

try:
    import ee
except ImportError:
    ee = None

GEE_INITIALIZED = False


def init_earth_engine():
    """
    Tries a service-account login first (needed for servers/CI where there's
    no interactive browser). Falls back to a locally cached user login
    (from `earthengine authenticate`) for local dev. Falls back to
    simulation if neither works.
    """
    global GEE_INITIALIZED

    if ee is None:
        print("⚠️ 'earthengine-api' not installed. Using Simulation.")
        return False

    email = settings.GEE_SERVICE_ACCOUNT_EMAIL
    key_file = settings.GEE_SERVICE_ACCOUNT_KEY_FILE

    try:
        if email and key_file and os.path.exists(key_file):
            credentials = ee.ServiceAccountCredentials(email, key_file)
            ee.Initialize(credentials, project=settings.GEE_PROJECT_ID or None)
            print(f"✅ GEE Initialized via service account: {email}")
        else:
            ee.Initialize(project=settings.GEE_PROJECT_ID or None)
            print("✅ GEE Initialized via local user credentials.")

        GEE_INITIALIZED = True
        return True
    except Exception as e:
        print(f"⚠️ GEE Authentication missing/failed ({e}). Using Simulation Mode.")
        return False

def get_sentinel_collection(lat, lon, date_start, date_end):
    if not GEE_INITIALIZED or ee is None:
        return None
    try:
        point = ee.Geometry.Point([lon, lat])
        s2 = (
            ee.ImageCollection("COPERNICUS/S2_SR_HARMONIZED")
            .filterBounds(point.buffer(100))
            .filterDate(date_start, date_end)
            .filter(ee.Filter.lt("CLOUDY_PIXEL_PERCENTAGE", 20))
            .sort("CLOUDY_PIXEL_PERCENTAGE")
        )
        return s2.first()
    except Exception as e:
        print(f"⚠️ GEE Fetch Error: {e}")
        return None