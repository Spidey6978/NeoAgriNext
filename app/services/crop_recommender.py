import json
import asyncio
import requests
from bs4 import BeautifulSoup
import re
from geopy.geocoders import Nominatim
from google import genai
from app.config import settings

# Initialize new Google GenAI Client
client = genai.Client(api_key=settings.GEMINI_API_KEY)

# Updated list of valid, active models for google-genai
MODEL_CANDIDATES = [
    getattr(settings, "GEMINI_MODEL", None),
    "gemini-3.8-flash",        # Google's explicit recommended replacement for 2.5-flash
    "gemini-flash-latest",
    "gemini-2.5-flash-lite",
    "gemini-flash-lite-latest",
    "gemini-3-flash-preview",
    "gemini-3.5-flash",
    "gemini-pro-latest",
    "gemini-2.5-pro",
]
MODEL_CANDIDATES = [m for m in dict.fromkeys(MODEL_CANDIDATES) if m]

async def _generate_recommendation_with_failover(prompt, loop):
    """Failover generator that cycles through available models on 429/404 errors."""
    backoffs = [5, 10]
    for model_name in MODEL_CANDIDATES:
        for attempt in range(len(backoffs) + 1):
            try:
                # Synchronous client call inside executor
                response = await loop.run_in_executor(
                    None, 
                    lambda: client.models.generate_content(
                        model=model_name,
                        contents=prompt
                    )
                )
                return response.text
            except Exception as e:
                msg = str(e).lower()
                if "429" in msg or "quota" in msg:
                    if attempt < len(backoffs):
                        await asyncio.sleep(backoffs[attempt])
                        continue
                    print(f"⚠️ Model {model_name} rate limited. Switching candidate...")
                    break
                if "404" in msg or "not found" in msg:
                    print(f"⚠️ Model {model_name} unavailable. Switching candidate...")
                    break
                raise e
    raise RuntimeError("All Gemini models failed for Crop Recommendations.")

# --- LAYER 2: REAL-TIME SCRAPER (The "Non-Cheating" Fallback) ---
def scrape_crops_from_web(state_name):
    print(f"🕷️ SCRAPER: Fetching real agri-data for {state_name}...")
    try:
        url = f"https://en.wikipedia.org/wiki/Agriculture_in_{state_name}"
        response = requests.get(url, timeout=3)
        
        if response.status_code != 200:
            url = f"https://en.wikipedia.org/wiki/{state_name}"
            response = requests.get(url, timeout=3)

        if response.status_code != 200:
            return []

        soup = BeautifulSoup(response.content, 'html.parser')
        text = soup.get_text().lower()
        
        common_crops = {
            "rice": {"season": "Kharif", "duration": "120 days"},
            "wheat": {"season": "Rabi", "duration": "140 days"},
            "cotton": {"season": "Kharif", "duration": "160 days"},
            "sugarcane": {"season": "Annual", "duration": "365 days"},
            "maize": {"season": "Kharif", "duration": "100 days"},
            "soybean": {"season": "Kharif", "duration": "90 days"},
            "groundnut": {"season": "Kharif", "duration": "110 days"},
            "tea": {"season": "Perennial", "duration": "N/A"},
            "coffee": {"season": "Perennial", "duration": "N/A"},
            "rubber": {"season": "Perennial", "duration": "N/A"},
            "apple": {"season": "Perennial", "duration": "N/A"},
            "saffron": {"season": "Autumn", "duration": "N/A"},
            "mustard": {"season": "Rabi", "duration": "100 days"},
            "onion": {"season": "Rabi", "duration": "120 days"},
            "potato": {"season": "Rabi", "duration": "90 days"},
            "jute": {"season": "Kharif", "duration": "120 days"},
            "pulses": {"season": "Rabi", "duration": "100 days"}
        }
        
        found_crops = []
        for crop, details in common_crops.items():
            if crop in text:
                found_crops.append({
                    "name": crop.capitalize(),
                    "season": details["season"],
                    "sowing_months": "Regional Standard",
                    "harvest_months": "Regional Standard",
                    "duration": details["duration"],
                    "fertilizer": "Standard NPK",
                    "tool": "Standard Agri-Tools",
                    "market_potential": "High (Locally Grown)"
                })
                if len(found_crops) >= 4: 
                    break
        
        if found_crops:
            print(f"✅ SCRAPER FOUND: {[c['name'] for c in found_crops]}")
            return found_crops
            
    except Exception as e:
        print(f"⚠️ Scraper Error: {e}")
    
    return []

# --- LAYER 3: STATIC BACKUP (Last Resort) ---
STATE_CROP_MAP = {
    "Maharashtra": ["Sugarcane", "Cotton", "Soybean", "Onion"],
    "Punjab": ["Wheat", "Rice", "Cotton", "Maize"],
    "Gujarat": ["Groundnut", "Cotton", "Tobacco", "Cumin"],
    "Karnataka": ["Coffee", "Maize", "Jowar", "Sunflower"],
    "Tamil Nadu": ["Rice", "Banana", "Coconut", "Turmeric"],
    "Uttar Pradesh": ["Wheat", "Sugarcane", "Potato", "Rice"],
    "Madhya Pradesh": ["Soybean", "Wheat", "Gram", "Maize"],
    "Rajasthan": ["Mustard", "Bajra", "Maize", "Cotton"],
    "West Bengal": ["Rice", "Jute", "Potato", "Tea"],
    "Bihar": ["Rice", "Wheat", "Maize", "Pulses"]
}

async def get_crop_recommendations(lat: float, lon: float):
    print(f"\n🌱 RECOMMENDER: Starting for {lat}, {lon}")
    
    location_name = f"Lat: {lat:.2f}, Lon: {lon:.2f}"
    state_context = "India"
    loop = asyncio.get_running_loop()
    
    # 1. Geocoding
    try:
        geolocator = Nominatim(user_agent=settings.USER_AGENT)
        location = await loop.run_in_executor(None, lambda: geolocator.reverse((lat, lon), language='en', timeout=5))
        
        if location:
            address = location.raw.get('address', {})
            district = address.get('state_district', '') or address.get('county', '')
            state = address.get('state', '')
            if state: 
                state_context = state
            location_name = f"{district}, {state}" if district else state
    except Exception as e:
        print(f"⚠️ Geocoding Warning: {e}")

    print(f"📍 Context: {location_name} (State: {state_context})")

    # 2. Try Gemini AI (Layer 1)
    try:
        prompt = f"""
        Act as a Senior Agronomist for {location_name} in {state_context}.
        
        TASK:
        Suggest top 3 most profitable crops for this specific location.
        
        CRITICAL OUTPUT FORMAT:
        Return ONLY a raw JSON array. Do NOT use markdown code blocks.
        
        JSON Structure:
        [
          {{
            "name": "Crop Name",
            "season": "Season Name",
            "sowing_months": "Month-Month",
            "harvest_months": "Month-Month",
            "duration": "Days",
            "fertilizer": "Tip",
            "tool": "Tool Name",
            "market_potential": "High/Medium"
          }}
        ]
        """
        
        raw_text = await _generate_recommendation_with_failover(prompt, loop)
        print(f"🤖 AI RAW OUTPUT: {raw_text[:100]}...") 

        cleaned_json = raw_text.replace("```json", "").replace("```", "").strip()
        match = re.search(r'\[.*\]', cleaned_json, re.DOTALL)
        if match:
            cleaned_json = match.group(0)

        crops_data = json.loads(cleaned_json)
        
        if not crops_data: 
            raise ValueError("Empty AI Response")
            
        return {
            "location": location_name,
            "crops": crops_data
        }

    except Exception as e:
        print(f"❌ AI Failed ({e}). Switching to Layer 2 (Live Web Scraper).")
        
        # 3. Try Web Scraper (Layer 2)
        scraped_data = await loop.run_in_executor(None, scrape_crops_from_web, state_context)
        if scraped_data:
            return {
                "location": location_name,
                "crops": scraped_data
            }
            
        # 4. Use State-Based Fallback (Layer 3)
        print("⚠️ All Real-Time Methods Failed. Using State Database.")
        fallback_names = STATE_CROP_MAP.get(state_context, ["Rice", "Wheat", "Maize"])[:3]
        fallback_crops = [
            {
                "name": name,
                "season": "Regional Season",
                "sowing_months": "Standard",
                "harvest_months": "Standard",
                "duration": "120 days",
                "fertilizer": "Standard NPK",
                "tool": "Standard Tools",
                "market_potential": "High"
            } for name in fallback_names
        ]
            
        return {
            "location": location_name,
            "crops": fallback_crops
        }