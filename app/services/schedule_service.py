import datetime
from datetime import timedelta

# --- REAL AGRONOMY DATA (Simplified for Hackathon) ---
CROP_CONFIG = {
    "rice": {"duration": 120, "stages": {"Vegetative": 45, "Reproductive": 35, "Ripening": 40}},
    "wheat": {"duration": 140, "stages": {"Vegetative": 60, "Reproductive": 40, "Ripening": 40}},
    "cotton": {"duration": 160, "stages": {"Vegetative": 55, "Reproductive": 65, "Ripening": 40}},
    "maize": {"duration": 100, "stages": {"Vegetative": 40, "Reproductive": 35, "Ripening": 25}},
    "grapes": {"duration": 150, "stages": {"Pruning": 20, "Vegetative": 50, "Fruiting": 80}},
}

def generate_farm_schedule(crop_name: str, sowing_date_str: str = None):
    """
    Generates a dynamic crop schedule based on crop type and sowing date.
    Includes Intelligent Scoring.
    """
    # 1. Parse Date
    if not sowing_date_str:
        sowing_date = datetime.date.today()
    else:
        try:
            sowing_date = datetime.datetime.strptime(sowing_date_str, "%Y-%m-%d").date()
        except:
            sowing_date = datetime.date.today()

    # 2. Get Crop Specifics (Default to Rice if unknown)
    crop_key = crop_name.lower().split()[0] 
    config = CROP_CONFIG.get(crop_key, CROP_CONFIG["rice"])
    duration_factor = config["duration"] / 120.0 

    template_tasks = [
        {"stage": "Pre-Season Planning", "trigger": "Before Sowing", "title": "Soil Testing & Planning", "subtitle": "Check NPK values", "base_offset": -15, "weight": 1},
        {"stage": "Land Preparation", "trigger": "Pre-Sowing", "title": "Ploughing & Tilling", "subtitle": "Ensure soil friability", "base_offset": -7, "weight": 2},
        {"stage": "Seed Selection", "trigger": "Pre-Sowing", "title": "Buy Certified Seeds", "subtitle": "Treat with fungicides", "base_offset": -3, "weight": 2},
        {"stage": "Sowing Day", "trigger": "Day 0", "title": f"Sowing {crop_name}", "subtitle": "Maintain proper spacing", "base_offset": 0, "weight": 3},
        {"stage": "Germination", "trigger": "Early Growth", "title": "Check Germination", "subtitle": "Re-sow gaps if needed", "base_offset": int(10 * duration_factor), "weight": 2},
        {"stage": "Nutrient Management", "trigger": "Vegetative Phase", "title": "First Fertilizer Dose", "subtitle": "Nitrogen rich", "base_offset": int(20 * duration_factor), "weight": 2},
        {"stage": "Weed Control", "trigger": "Vegetative Phase", "title": "De-weeding", "subtitle": "Manual or Herbicide", "base_offset": int(30 * duration_factor), "weight": 2},
        {"stage": "Irrigation", "trigger": "Growth Phase", "title": "Critical Irrigation", "subtitle": "Maintain moisture", "base_offset": int(45 * duration_factor), "weight": 3},
        {"stage": "Pest Management", "trigger": "Mid-Season", "title": "Pest Scouting", "subtitle": "Check under leaves", "base_offset": int(60 * duration_factor), "weight": 3},
        {"stage": "Flowering", "trigger": "Reproductive Phase", "title": "Monitor Flowering", "subtitle": "Avoid water stress", "base_offset": int(80 * duration_factor), "weight": 2},
        {"stage": "Harvest Prep", "trigger": "Ripening", "title": "Stop Irrigation", "subtitle": "Allow grain hardening", "base_offset": int(config["duration"] - 10), "weight": 2},
        {"stage": "Harvesting", "trigger": "Maturity", "title": "Harvest Crop", "subtitle": "Store in dry place", "base_offset": config["duration"], "weight": 3},
        {"stage": "Post-Harvest", "trigger": "After Harvest", "title": "Stubble Management", "subtitle": "Do not burn residue", "base_offset": config["duration"] + 5, "weight": 1}
    ]

    final_tasks = []
    today = datetime.date.today()
    
    # Scoring Variables
    total_weight_due = 0
    earned_weight = 0

    print(f"\n📅 GENERATING SCHEDULE FOR {crop_name.upper()} ({config['duration']} days)")

    for idx, task in enumerate(template_tasks):
        task_date = sowing_date + timedelta(days=task['base_offset'])
        
        # Is this task in the past or today? (It is "Due")
        is_due = task_date <= today
        
        # Logic: If it's due, we assume it's completed for the demo logic (or check DB in real app)
        is_completed = is_due 
        
        if is_due:
            total_weight_due += task['weight']
            if is_completed:
                earned_weight += task['weight']

        final_tasks.append({
            "id": idx + 1,
            "stage": task['stage'],
            "triggerInfo": task['trigger'],
            "actionTitle": task['title'],
            "actionSubtitle": task['subtitle'],
            "daysOffset": task['base_offset'],
            "date": task_date.strftime("%d %b, %Y"),
            "isCompleted": is_completed,
            "isPhotoUploaded": is_completed 
        })

    # --- INTELLIGENT SCORING ---
    # Score = (Earned Weight / Total Weight DUE so far) * 100
    # If nothing is due yet (future sowing), score is 100% (Pre-planning readiness)
    if total_weight_due > 0:
        score = int((earned_weight / total_weight_due) * 100)
    else:
        score = 100 

    return {
        "farm_score": score,
        "sowing_date": sowing_date.strftime("%Y-%m-%d"),
        "tasks": final_tasks
    }