import datetime

# --- SAFE IMPORT FOR HACKATHON ---
# This allows the code to exist without crashing if the 'earthengine-api' library isn't installed.
try:
    import ee
except ImportError:
    ee = None

# Flag to track if GEE is actually active
GEE_INITIALIZED = False

def init_earth_engine():
    """
    Attempts to authenticate and initialize Google Earth Engine.
    Returns True if successful, False otherwise.
    """
    global GEE_INITIALIZED
    
    # 1. Check if library exists first
    if ee is None:
        print("⚠️ 'earthengine-api' not installed. Skipping Real Satellite (Using Simulation).")
        return False

    try:
        # 2. Try to use existing credentials
        try:
            ee.Initialize()
        except Exception:
            print("⚠️ GEE Authentication missing. Using Simulation Mode.")
            return False
            
        GEE_INITIALIZED = True
        print("✅ Google Earth Engine Initialized Successfully.")
        return True
    except Exception as e:
        print(f"❌ Earth Engine Init Failed: {e}")
        return False

def get_sentinel_collection(lat, lon, date_start, date_end):
    """
    Fetches raw Sentinel-2 Surface Reflectance data.
    """
    # Safety check
    if not GEE_INITIALIZED or ee is None:
        return None

    try:
        point = ee.Geometry.Point([lon, lat])
        
        # Load Sentinel-2 Collection
        s2 = ee.ImageCollection("COPERNICUS/S2_SR") \
            .filterBounds(point) \
            .filterDate(date_start, date_end) \
            .filter(ee.Filter.lt('CLOUDY_PIXEL_PERCENTAGE', 20)) \
            .sort('CLOUDY_PIXEL_PERCENTAGE')

        return s2.first()
    except Exception as e:
        print(f"⚠️ GEE Fetch Error: {e}")
        return None