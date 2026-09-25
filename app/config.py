import os
from dotenv import load_dotenv

# Load environment variables from a .env file (if you use one)
load_dotenv()

class Config:
    # --- API KEYS ---
    # Safely pulling the keys from the .env file!
    GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
    OPENWEATHER_API_KEY = os.getenv("OPENWEATHER_API_KEY", "")

    # --- GOOGLE EARTH ENGINE ---
    GEE_PROJECT_ID = os.getenv("GEE_PROJECT_ID", "")
    GEE_SERVICE_ACCOUNT_EMAIL = os.getenv("GEE_SERVICE_ACCOUNT_EMAIL", "")
    GEE_SERVICE_ACCOUNT_KEY_FILE = os.getenv("GEE_SERVICE_ACCOUNT_KEY_FILE", "")

    # --- IMD ---
    IMD_AWS_URL = "https://api.imd.gov.in/api/v1/aws_data"
    
    # --- ENDPOINTS ---
    OPENWEATHER_URL = "https://api.openweathermap.org/data/2.5/weather"
    OVERPASS_URL = "http://overpass-api.de/api/interpreter"
    
    # --- SETTINGS ---
    GEMINI_MODEL = "gemini-2.5-flash" # Centralized model name
    USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

settings = Config()