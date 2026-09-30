from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from typing import List, Optional
from app.schemas.input_schema import CropInput, AdvisoryResponse
from app.services.weather_service import get_current_weather
from app.services.market_service import get_real_market_price
from app.services.ndvi_service import get_satellite_analysis
from app.services.gemini_service import get_gemini_advice

router = APIRouter(
    prefix="/api/v1/dpg",
    tags=["DPG Open APIs (AgriStack Standard)"]
)

class ModelRegistryResponse(BaseModel):
    model_id: str
    region: str
    version: str
    status: str
    description: str

@router.get("/model-registry", response_model=List[ModelRegistryResponse])
async def get_model_registry():
    """
    Returns the federated list of state-wise models available in the DPG.
    Required by Hackathon DPG Interoperability Guidelines.
    """
    return [
        {
            "model_id": "agri-lgbm-v1",
            "region": "Maharashtra",
            "version": "1.0",
            "status": "active",
            "description": "Regenerative advisory engine optimized for Western India soil profiles."
        },
        {
            "model_id": "gemini-agronomist-v2",
            "region": "Global",
            "version": "2.0",
            "status": "active",
            "description": "Multimodal LLM wrapper for pest identification and dynamic crop roadmaps."
        }
    ]

@router.post("/advisory", response_model=AdvisoryResponse)
async def generate_dpg_advisory(data: CropInput):
    """
    Standardized OpenAPI 3.0 endpoint for generating interoperable farm advisories.
    Fuses Earth Engine, Weather, and Gemini AI.
    """
    try:
        weather = await get_current_weather(data.lat, data.lon)
        market_data = await get_real_market_price(data.lat, data.lon, data.crop)
        satellite_data = await get_satellite_analysis(data.lat, data.lon)
        ai_result = await get_gemini_advice(weather, data, market_data)
        
        explanation = ai_result.get("explanation", "No advice generated.")
        roadmap = ai_result.get("roadmap")

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
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
