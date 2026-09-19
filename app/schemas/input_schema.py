from pydantic import BaseModel
from typing import List, Optional

# 1. What Android sends to Python
class CropInput(BaseModel):
    # We make these optional so the app doesn't crash if they are missing
    lat: float = 20.0059  # Default to Nashik
    lon: float = 73.7898
    state: str = "Maharashtra"
    district: str = "Nashik"
    crop: str = "Grapes"
    crop_stage: str = "Vegetative"

# 2. What Python sends back to Android
class IrrigationDecision(BaseModel):
    decision: str
    confidence: float

class CropHealth(BaseModel):
    stress_level: str

class MarketAction(BaseModel):
    action: str
    reason: str

class AdvisoryResponse(BaseModel):
    irrigation: IrrigationDecision
    crop_health: CropHealth
    market: MarketAction
    gemini_explanation: str


# --- NEW: THE ROADMAP SCHEMA ---
class CropRoadmap(BaseModel):
    name: str
    temp_range: str  # e.g. "18-30°C"
    ph_range: str    # e.g. "6.0-7.5"
    water_requirement: str # e.g. "700mm"
    sowing_window: str # e.g. "June-July"
    harvest_window: str # e.g. "Oct-Nov"
    duration_days: int
    fertilizer_plan: str
    pests: List[str]

class SatelliteData(BaseModel):
    ndvi_current: float
    ndvi_last_week: float
    trend: str
    status: str
    satellite: str
    cloud_coverage: str

class AdvisoryResponse(BaseModel):
    irrigation: IrrigationDecision
    crop_health: CropHealth
    market: MarketAction
    gemini_explanation: str
    roadmap: Optional[CropRoadmap] = None # <--- NEW FIELD
    satellite: Optional[SatelliteData] = None # <--- NEW FIELD
    
    
class NewsItem(BaseModel):
    id: int
    title: str
    source: str
    timeAgo: str
    category: str
    imageUrl: str # We send a URL, Android loads it
    
    
# --- NEW: FARM SCHEDULE SCHEMA ---
class ScheduleTask(BaseModel):
    id: int
    stage: str
    triggerInfo: str
    actionTitle: str
    actionSubtitle: Optional[str] = None
    daysOffset: int
    date: str
    isCompleted: bool = False
    isPhotoUploaded: bool = False

class FarmScheduleResponse(BaseModel):
    farm_score: int # Overall score 0-100
    sowing_date: str
    tasks: List[ScheduleTask]