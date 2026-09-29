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


import asyncio
import json
from typing import List

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.schemas.input_schema import CropInput, AdvisoryResponse, NewsItem, FarmScheduleResponse
from app.services.weather_service import get_current_weather, get_hyperlocal_weather
from app.services.gemini_service import get_gemini_advice
from app.services.market_service import get_real_market_price
from app.services.cache_service import init_db
from app.services.ndvi_service import get_satellite_analysis
from app.services.news_service import get_agri_news
from app.services.crop_recommender import get_crop_recommendations
from app.services.schedule_service import generate_farm_schedule
from app.services.analytics_service import AgriAnalyticsEngine
from app.routers import cache_dashboard
from app.services.maps_service import reverse_geocode_mapbox
from app.services.treatment_service import get_organic_treatment
from app.services.regenerative_engine import get_regenerative_recommendation

app = FastAPI(title="AgriNext API", version="1.0.0")

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


@app.get("/news", response_model=List[NewsItem])
async def get_news():
    return await get_agri_news()


@app.get("/recommend_crops")
async def recommend_crops(lat: float, lon: float):
    result = await get_crop_recommendations(lat, lon)
    return result


@app.get("/schedule", response_model=FarmScheduleResponse)
async def get_schedule(crop: str, sowing_date: str = None):
    return generate_farm_schedule(crop, sowing_date)


@app.get("/analytics/report")
async def get_analytics_report():
    engine = AgriAnalyticsEngine()
    report = await asyncio.to_thread(engine.run_pipeline)
    return report


# --- NEW: sub-district weather (OpenWeatherMap + IMD ground station) ---
@app.get("/weather/hyperlocal")
async def weather_hyperlocal(lat: float, lon: float):
    print(f"🌦️ HYPERLOCAL WEATHER REQUEST for {lat},{lon}")
    return await get_hyperlocal_weather(lat, lon)


@app.post("/analyze", response_model=AdvisoryResponse)
async def analyze_crop(data: CropInput):
    print(f"\n📡 ANALYZE: {data.lat},{data.lon} crop={data.crop}")

    weather = await get_current_weather(data.lat, data.lon)
    market_data = await get_real_market_price(data.lat, data.lon, data.crop)
    satellite_data = await get_satellite_analysis(data.lat, data.lon)
    ai_result = await get_gemini_advice(weather, data, market_data)

    explanation = ai_result.get("explanation", "No advice generated.")
    roadmap = ai_result.get("roadmap")

    return {
        "irrigation": {"decision": "monitor", "confidence": 0.85},
        "crop_health": {"stress_level": "low"},
        "market": {
            "action": market_data["advisory"]["action"],
            "reason": market_data["advisory"]["reason"],
        },
        "gemini_explanation": explanation,
        "roadmap": roadmap,
        "satellite": satellite_data,
    }

@app.get("/location/geocode")
async def location_geocode(lat: float, lon: float):
    return await reverse_geocode_mapbox(lat, lon)

@app.get("/treatment/organic")
def organic_treatment(disease: str):
    return get_organic_treatment(disease)

@app.get("/recommend/regenerative")
async def recommend_regenerative(lat: float, lon: float, n: float = 280, p: float = 15, k: float = 150):
    return await get_regenerative_recommendation(lat, lon, n, p, k)