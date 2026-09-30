# from app.services.crop_recommender import get_crop_recommendations
# import json
# from fastapi import FastAPI
# from app.schemas.input_schema import CropInput, AdvisoryResponse
# from app.services.weather_service import get_current_weather
# from app.services.gemini_service import get_gemini_advice # <--- Importing the Brain
# from app.services.market_service import get_real_market_price

# app = FastAPI()

# @app.get("/")
# def read_root():
#     return {"status": "ONLINE", "message": "AgriNext Brain is Running"}


# # This handles the "Ping Connection" button (GET /status)
# @app.get("/status")
# def health_check():
#     return {"status": "ONLINE", "message": "AgriNext Brain is Active"}


# @app.post("/analyze", response_model=AdvisoryResponse)
# async def analyze_crop(data: CropInput):

#     print("\n" + "="*40)
#     print(f"📡 INCOMING REQUEST FROM APP:")
#     print(f"📍 LATITUDE : {data.lat}")
#     print(f"📍 LONGITUDE: {data.lon}")
#     print(f"🌾 CROP     : {data.crop}")
#     print("="*40 + "\n")

#     # 1. Weather
#     weather = await get_current_weather(data.lat, data.lon)
    
#     # 2. Market (With Fail-Safe)
#     market_data = await get_real_market_price(data.lat, data.lon, data.crop)
    
#     # 3. Gemini (Now simpler, just explaining the data)
#     # Update your gemini_service to accept 'market_data' as an argument
#     gemini_text = await get_gemini_advice(weather, data, market_data)

#     return {
#         "irrigation": { "decision": "monitor", "confidence": 0.85 },
#         "crop_health": { "stress_level": "low" },
#         "market": { 
#             "action": market_data['advisory']['action'], 
#             "reason": market_data['advisory']['reason'] 
#         },
#         "gemini_explanation": gemini_text
#     }


# @app.get("/recommend_crops")
# async def recommend_crops(lat: float, lon: float):
#     # 1. Run the Recommender
#     result = await get_crop_recommendations(lat, lon)
    
#     # 2. Parse the AI's string JSON into real JSON
#     try:
#         crops_list = json.loads(result["recommendations"])
#         print(crops_list)
#     except:
#         # Fallback if AI output was malformed
#         crops_list = []

#     return {
#         "location": result.get("location", "Unknown"),
#         "crops": crops_list
#     }

'''


from app.services.crop_recommender import get_crop_recommendations
import json
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.schemas.input_schema import CropInput, AdvisoryResponse
from app.services.weather_service import get_current_weather
from app.services.gemini_service import get_gemini_advice
from app.services.market_service import get_real_market_price
from app.services.cache_service import init_db # <--- Add Import
from app.routers import cache_dashboard  # <--- Import

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(cache_dashboard.router)


@app.on_event("startup")
async def startup_event():
    init_db() # <--- Initialize DB here
    print("🚀 AgriNext Server Started")

@app.get("/")
def read_root():
    return {"status": "ONLINE", "message": "AgriNext Brain is Running"}

@app.get("/status")
def health_check():
    return {"status": "ONLINE", "message": "AgriNext Brain is Active"}

# --- FIXED ENDPOINT ---
@app.get("/recommend_crops")
async def recommend_crops(lat: float, lon: float):
    # The service ALREADY returns a dictionary with 'crops' list.
    # We do NOT need to parse it again.
    print(f"\n🌱 [REQUEST] RECOMMEND CROPS for {lat}, {lon}")
    result = await get_crop_recommendations(lat, lon)
    
    # Debug print to see what we are sending
    print(f"✅ Sending {len(result.get('crops', []))} crops to App")
    
    return result

@app.post("/analyze", response_model=AdvisoryResponse)
async def analyze_crop(data: CropInput):

    print("\n" + "="*40)
    print(f"📡 INCOMING REQUEST FROM APP:")
    print(f"📍 LATITUDE : {data.lat}")
    print(f"📍 LONGITUDE: {data.lon}")
    print(f"🌾 CROP     : {data.crop}")
    print("="*40 + "\n")

    # 1. Weather
    weather = await get_current_weather(data.lat, data.lon)
    
    # 2. Market
    market_data = await get_real_market_price(data.lat, data.lon, data.crop)
    
    # 3. Gemini
    print(f"🤖 Asking Gemini for Roadmap...")
    ai_result = await get_gemini_advice(weather, data, market_data)
    
    explanation = ai_result.get("explanation", "No advice generated.")
    roadmap = ai_result.get("roadmap")

    if roadmap:
        print(f"✅ Roadmap Generated: {roadmap.get('name')}")
    else:
        print("❌ Roadmap Missing")

    return {
        "irrigation": { "decision": "monitor", "confidence": 0.85 },
        "crop_health": { "stress_level": "low" },
        "market": { 
            "action": market_data['advisory']['action'], 
            "reason": market_data['advisory']['reason'] 
        },
        "gemini_explanation": explanation,
        "roadmap": roadmap
    }
    
'''



'''
from app.services.crop_recommender import get_crop_recommendations
import json
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.schemas.input_schema import CropInput, AdvisoryResponse
from app.services.weather_service import get_current_weather
from app.services.gemini_service import get_gemini_advice
from app.services.market_service import get_real_market_price
from app.services.cache_service import init_db
# --- IMPORT SATELLITE SERVICE ---
from app.services.ndvi_service import get_satellite_analysis 
from app.routers import cache_dashboard

from app.services.news_service import get_agri_news

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(cache_dashboard.router)

@app.on_event("startup")
async def startup_event():
    init_db()
    print("🚀 AgriNext Server Started")

@app.get("/")
def read_root():
    return {"status": "ONLINE", "message": "AgriNext Brain is Running"}

@app.get("/status")
def health_check():
    return {"status": "ONLINE", "message": "AgriNext Brain is Active"}

@app.get("/recommend_crops")
async def recommend_crops(lat: float, lon: float):
    print(f"\n🌱 [REQUEST] RECOMMEND CROPS for {lat}, {lon}")
    result = await get_crop_recommendations(lat, lon)
    print(f"✅ Sending {len(result.get('crops', []))} crops to App")
    return result

@app.post("/analyze", response_model=AdvisoryResponse)
async def analyze_crop(data: CropInput):

    print("\n" + "="*40)
    print(f"📡 INCOMING REQUEST FROM APP:")
    print(f"📍 LATITUDE : {data.lat}")
    print(f"📍 LONGITUDE: {data.lon}")
    print(f"🌾 CROP     : {data.crop}")
    print("="*40 + "\n")

    # 1. Weather
    weather = await get_current_weather(data.lat, data.lon)
    
    # 2. Market
    market_data = await get_real_market_price(data.lat, data.lon, data.crop)

    # 3. Satellite (NDVI) - THIS WAS MISSING
    print(f"🛰️ Fetching Satellite Data...")
    satellite_data = await get_satellite_analysis(data.lat, data.lon)
    
    # 4. Gemini
    print(f"🤖 Asking Gemini for Roadmap...")
    ai_result = await get_gemini_advice(weather, data, market_data)
    
    explanation = ai_result.get("explanation", "No advice generated.")
    roadmap = ai_result.get("roadmap")

    if roadmap:
        print(f"✅ Roadmap Generated: {roadmap.get('name')}")
    else:
        print("❌ Roadmap Missing")

    return {
        "irrigation": { "decision": "monitor", "confidence": 0.85 },
        "crop_health": { "stress_level": "low" },
        "market": { 
            "action": market_data['advisory']['action'], 
            "reason": market_data['advisory']['reason'] 
        },
        "gemini_explanation": explanation,
        "roadmap": roadmap,
        # --- INCLUDE SATELLITE DATA ---
        "satellite": satellite_data 
    }
    
'''


from app.services.crop_recommender import get_crop_recommendations
import json
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from typing import List # <--- Added for List response
from app.schemas.input_schema import CropInput, AdvisoryResponse, NewsItem # <--- Added NewsItem
from app.services.weather_service import get_current_weather
from app.services.gemini_service import get_gemini_advice
from app.services.market_service import get_real_market_price
from app.services.cache_service import init_db
from app.services.ndvi_service import get_satellite_analysis 
from app.services.news_service import get_agri_news # <--- Added News Service
from app.routers import cache_dashboard
from app.routers import dpg_router # <--- Added DPG Router
from app.services.schedule_service import generate_farm_schedule # <--- New Import
from app.schemas.input_schema import FarmScheduleResponse # <--- New Import
from app.services.analytics_service import AgriAnalyticsEngine # <--- New Import
import asyncio



app = FastAPI(
    title="AgriNext DPG API",
    description="Interoperable Digital Agriculture Network OpenAPI 3.0",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(cache_dashboard.router)
app.include_router(dpg_router.router) # <--- Include DPG router


@app.on_event("startup")
async def startup_event():
    init_db()
    print("🚀 AgriNext Server Started")

@app.get("/")
def read_root():
    return {"status": "ONLINE", "message": "AgriNext Brain is Running"}

@app.get("/status")
def health_check():
    return {"status": "ONLINE", "message": "AgriNext Brain is Active"}

# --- NEWS ENDPOINT ---
@app.get("/news", response_model=List[NewsItem])
async def get_news():
    print("📰 Fetching News Feed...")
    return await get_agri_news()

@app.get("/recommend_crops")
async def recommend_crops(lat: float, lon: float):
    print(f"\n🌱 [REQUEST] RECOMMEND CROPS for {lat}, {lon}")
    result = await get_crop_recommendations(lat, lon)
    print(f"✅ Sending {len(result.get('crops', []))} crops to App")
    return result
    
@app.get("/schedule", response_model=FarmScheduleResponse)
async def get_schedule(crop: str, sowing_date: str = None):
    print(f"📅 SCHEDULE REQUEST: {crop}, Sowing: {sowing_date}")
    return generate_farm_schedule(crop, sowing_date)
    
    
# --- NEW ANALYTICS ENDPOINT ---
@app.get("/analytics/report")
async def get_analytics_report():
    """
    Triggers the Advanced Data Science Pipeline.
    Returns structured insights and base64 encoded graphs.
    """
    print("\n📊 STARTING ANALYTICS PIPELINE...")
    engine = AgriAnalyticsEngine()
    report = await asyncio.to_thread(engine.run_pipeline) # Run in thread to not block
    return report

@app.post("/analyze", response_model=AdvisoryResponse)
async def analyze_crop(data: CropInput):

    print("\n" + "="*40)
    print(f"📡 INCOMING REQUEST FROM APP:")
    print(f"📍 LATITUDE : {data.lat}")
    print(f"📍 LONGITUDE: {data.lon}")
    print(f"🌾 CROP     : {data.crop}")
    print("="*40 + "\n")

    # 1. Weather
    weather = await get_current_weather(data.lat, data.lon)
    
    # 2. Market
    market_data = await get_real_market_price(data.lat, data.lon, data.crop)

    # 3. Satellite
    print(f"🛰️ Fetching Satellite Data...")
    satellite_data = await get_satellite_analysis(data.lat, data.lon)
    
    # 4. Gemini
    print(f"🤖 Asking Gemini for Roadmap...")
    ai_result = await get_gemini_advice(weather, data, market_data)
    
    explanation = ai_result.get("explanation", "No advice generated.")
    roadmap = ai_result.get("roadmap")

    if roadmap:
        print(f"✅ Roadmap Generated: {roadmap.get('name')}")
    else:
        print("❌ Roadmap Missing")

    return {
        "irrigation": { "decision": "monitor", "confidence": 0.85 },
        "crop_health": { "stress_level": "low" },
        "market": { 
            "action": market_data['advisory']['action'], 
            "reason": market_data['advisory']['reason'] 
        },
        "gemini_explanation": explanation,
        "roadmap": roadmap,
        "satellite": satellite_data 
    }