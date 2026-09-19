'''

import random
import json
import asyncio
import google.generativeai as genai
from app.config import settings

# Configure Gemini
genai.configure(api_key=settings.GEMINI_API_KEY)
model = genai.GenerativeModel(settings.GEMINI_MODEL)

# --- IMAGE LIBRARY (High Quality Unsplash IDs) ---
# We map keywords to specific reliable images to ensure they always load and look good.
IMAGE_LIBRARY = {
    "weather": [
        "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=600&q=80", # Rain/Cloud
        "https://images.unsplash.com/photo-1504608524841-42fe6f032b4b?auto=format&fit=crop&w=600&q=80", # Storm
        "https://images.unsplash.com/photo-1561470508-fd4df1ed90b2?auto=format&fit=crop&w=600&q=80", # Sunny Field
    ],
    "market": [
        "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=600&q=80", # Veggies Market
        "https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=600&q=80", # Fruit Stall
        "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=600&q=80", # Money/Business
    ],
    "tech": [
        "https://images.unsplash.com/photo-1508614589041-895b88991e3e?auto=format&fit=crop&w=600&q=80", # Drone
        "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=600&q=80", # Laptop/Field
        "https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=600&q=80", # Smart Farm
    ],
    "policy": [
        "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80", # Grain Sacks
        "https://images.unsplash.com/photo-1625246333195-5848c4281413?auto=format&fit=crop&w=600&q=80", # Farmer Check
        "https://images.unsplash.com/photo-1532629345422-7515f3d16bb6?auto=format&fit=crop&w=600&q=80", # Hands/Plant
    ],
    "general": [
        "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=600&q=80", # Sunset Field
        "https://images.unsplash.com/photo-1605000797499-95a51c5269ae?auto=format&fit=crop&w=600&q=80", # Tractor
        "https://images.unsplash.com/photo-1499529112042-449518cf8141?auto=format&fit=crop&w=600&q=80", # Green Field
    ]
}

# Fallback data in case AI fails
FALLBACK_NEWS = [
    {
        "title": "Government Announces New Subsidy for Organic Fertilizers",
        "source": "AgriWire India",
        "category": "Policy",
        "imageUrl": IMAGE_LIBRARY["policy"][0]
    },
    {
        "title": "Monsoon Update: Heavy Rains Predicted for Maharashtra",
        "source": "IMD Weather",
        "category": "Weather",
        "imageUrl": IMAGE_LIBRARY["weather"][0]
    },
    {
        "title": "Tomato Prices Surge by 20% in Nashik Markets",
        "source": "MarketWatch",
        "category": "Market",
        "imageUrl": IMAGE_LIBRARY["market"][0]
    }
]

def get_smart_image(title: str, category: str):
    """
    Selects the best image URL based on the news content.
    """
    title_lower = title.lower()
    cat_lower = category.lower()
    
    # Check title keywords first (more specific)
    if "rain" in title_lower or "monsoon" in title_lower or "storm" in title_lower:
        return random.choice(IMAGE_LIBRARY["weather"])
    if "drone" in title_lower or "tech" in title_lower or "app" in title_lower:
        return random.choice(IMAGE_LIBRARY["tech"])
    if "price" in title_lower or "market" in title_lower or "export" in title_lower:
        return random.choice(IMAGE_LIBRARY["market"])
    
    # Fallback to category
    if "weather" in cat_lower:
        return random.choice(IMAGE_LIBRARY["weather"])
    if "market" in cat_lower or "economy" in cat_lower:
        return random.choice(IMAGE_LIBRARY["market"])
    if "tech" in cat_lower:
        return random.choice(IMAGE_LIBRARY["tech"])
    if "policy" in cat_lower or "govt" in cat_lower:
        return random.choice(IMAGE_LIBRARY["policy"])
        
    # Default
    return random.choice(IMAGE_LIBRARY["general"])

async def get_agri_news():
    """
    Uses AI to generate fresh, realistic agricultural news on the fly.
    """
    print("📰 NEWS: Generating fresh content via AI...")
    
    try:
        # Prompt Gemini to be a news aggregator
        prompt = """
        Generate 5 realistic, short agricultural news headlines for India.
        Mix categories like 'Policy', 'Weather', 'Market', 'Technology'.
        Return a valid JSON array of objects with keys: 'title', 'source', 'category'.
        Do not use markdown. Just the raw JSON.
        """
        
        # Non-blocking call
        loop = asyncio.get_event_loop()
        response = await loop.run_in_executor(None, model.generate_content, prompt)
        
        text = response.text.replace("```json", "").replace("```", "").strip()
        ai_news = json.loads(text)
        
        # Add Images & Times dynamically
        final_news = []
        for idx, item in enumerate(ai_news):
            
            # --- INTELLIGENT IMAGE SELECTION ---
            title = item.get('title', '')
            category = item.get('category', 'General')
            image_url = get_smart_image(title, category)

            # Random time ago
            time_val = random.randint(1, 10)
            
            final_news.append({
                "id": idx + 1,
                "title": title,
                "source": item.get('source', 'AgriNext News'),
                "timeAgo": f"{time_val}h ago",
                "category": category,
                "imageUrl": image_url
            })
            
        # --- DEBUG: Print News with Images ---
        print("\n" + "="*40)
        print("📰 GENERATED NEWS FEED:")
        for news in final_news:
            print(f"🔹 [{news['category']}] {news['title']}")
            print(f"   🖼️ Image: {news['imageUrl']}")
        print("="*40 + "\n")
            
        return final_news

    except Exception as e:
        print(f"⚠️ News AI Failed: {e}")
        # Return fallback if AI fails (quota/network)
        formatted_fallback = []
        for idx, item in enumerate(FALLBACK_NEWS):
            formatted_fallback.append({
                "id": idx + 1,
                "title": item["title"],
                "source": item["source"],
                "timeAgo": "4h ago",
                "category": item["category"],
                "imageUrl": item["imageUrl"]
            })
        return formatted_fallback
'''


import random
import json
import asyncio
import google.generativeai as genai
from app.config import settings

# Configure Gemini
genai.configure(api_key=settings.GEMINI_API_KEY)
model = genai.GenerativeModel(settings.GEMINI_MODEL)

# --- IMAGE LIBRARY (Massively Expanded High Quality Unsplash IDs) ---
# We map keywords to specific reliable images to ensure they always load and look good.
IMAGE_LIBRARY = {
    "weather": [
        "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=600&q=80", # Rain/Cloud
        "https://images.unsplash.com/photo-1504608524841-42fe6f032b4b?auto=format&fit=crop&w=600&q=80", # Storm
        "https://images.unsplash.com/photo-1561470508-fd4df1ed90b2?auto=format&fit=crop&w=600&q=80", # Sunny Field
        "https://images.unsplash.com/photo-1534274988754-8d431a23a958?auto=format&fit=crop&w=600&q=80", # Rainy Window
        "https://images.unsplash.com/photo-1594156596782-fa8205b823dc?auto=format&fit=crop&w=600&q=80", # Drought
        "https://images.unsplash.com/photo-1454789476662-bdd710d05b35?auto=format&fit=crop&w=600&q=80", # Clouds
        "https://images.unsplash.com/photo-1530563885674-66db50a1af19?auto=format&fit=crop&w=600&q=80", # Monsoon
        "https://images.unsplash.com/photo-1516912481808-3406841bd33c?auto=format&fit=crop&w=600&q=80", # Weather Vane
    ],
    "market": [
        "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=600&q=80", # Veggies Market
        "https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=600&q=80", # Fruit Stall
        "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=600&q=80", # Money/Business
        "https://images.unsplash.com/photo-1610348725531-843dff563e2c?auto=format&fit=crop&w=600&q=80", # Grain Market
        "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80", # Shopping
        "https://images.unsplash.com/photo-1573485868688-46643699b0c7?auto=format&fit=crop&w=600&q=80", # Handshake
        "https://images.unsplash.com/photo-1556742049-0cfed4f7a07d?auto=format&fit=crop&w=600&q=80", # Economics
        "https://images.unsplash.com/photo-1611974765270-ca12586343bb?auto=format&fit=crop&w=600&q=80", # Stock Chart
    ],
    "tech": [
        "https://images.unsplash.com/photo-1508614589041-895b88991e3e?auto=format&fit=crop&w=600&q=80", # Drone
        "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=600&q=80", # Laptop/Field
        "https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=600&q=80", # Smart Farm
        "https://images.unsplash.com/photo-1535376472810-5d229c6bda3c?auto=format&fit=crop&w=600&q=80", # Server Farm
        "https://images.unsplash.com/photo-1563770095-39d468a9239c?auto=format&fit=crop&w=600&q=80", # Satellite
        "https://images.unsplash.com/photo-1518152006812-edab29b069ac?auto=format&fit=crop&w=600&q=80", # Robot arm
        "https://images.unsplash.com/photo-1589254065878-42c9da9e2cd6?auto=format&fit=crop&w=600&q=80", # Automation
        "https://images.unsplash.com/photo-1526628953301-3e589a6a8b74?auto=format&fit=crop&w=600&q=80", # Lab Analysis
    ],
    "policy": [
        "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80", # Grain Sacks
        "https://images.unsplash.com/photo-1625246333195-5848c4281413?auto=format&fit=crop&w=600&q=80", # Farmer Check
        "https://images.unsplash.com/photo-1532629345422-7515f3d16bb6?auto=format&fit=crop&w=600&q=80", # Hands/Plant
        "https://images.unsplash.com/photo-1555447405-05842c375627?auto=format&fit=crop&w=600&q=80", # Meeting
        "https://images.unsplash.com/photo-1577962917302-cd874c4e31d2?auto=format&fit=crop&w=600&q=80", # Documents
        "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?auto=format&fit=crop&w=600&q=80", # Government Building
        "https://images.unsplash.com/photo-1521791136064-7986c2920216?auto=format&fit=crop&w=600&q=80", # Handshake
        "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=600&q=80", # Paperwork
    ],
    "general": [
        "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=600&q=80", # Sunset Field
        "https://images.unsplash.com/photo-1605000797499-95a51c5269ae?auto=format&fit=crop&w=600&q=80", # Tractor
        "https://images.unsplash.com/photo-1499529112042-449518cf8141?auto=format&fit=crop&w=600&q=80", # Green Field
        "https://images.unsplash.com/photo-1500937386664-56d1dfef3854?auto=format&fit=crop&w=600&q=80", # Wheat
        "https://images.unsplash.com/photo-1530507629858-e4976987d297?auto=format&fit=crop&w=600&q=80", # Corn
        "https://images.unsplash.com/photo-1523348837708-15d4a09cfac2?auto=format&fit=crop&w=600&q=80", # Cows
        "https://images.unsplash.com/photo-1595841696677-6489ff3f8cd1?auto=format&fit=crop&w=600&q=80", # Fruit Basket
        "https://images.unsplash.com/photo-1628352081506-83c43123ed6d?auto=format&fit=crop&w=600&q=80", # Seedling
    ]
}

# Fallback data in case AI fails (Detailed Content included)
FALLBACK_NEWS = [
    {
        "title": "New Subsidy for Organic Fertilizers Announced",
        "source": "AgriNews Daily",
        "category": "Policy",
        "imageUrl": IMAGE_LIBRARY["policy"][0],
        "content": "The government has announced a new subsidy scheme aimed at promoting organic farming across the state. Under this new initiative, farmers will receive a 50% subsidy on the purchase of certified organic fertilizers and bio-pesticides.\n\nThe goal is to reduce dependency on chemical fertilizers which degrade soil health over time. Applications for the subsidy can be submitted through the local Krishi Kendra starting next month.\n\nExperts believe this move will not only improve soil quality but also fetch premium prices for organic produce in international markets."
    },
    {
        "title": "Monsoon Forecast: Heavy Rains Expected in Nashik",
        "source": "Weather Bureau",
        "category": "Weather",
        "imageUrl": IMAGE_LIBRARY["weather"][0],
        "content": "Farmers in the Nashik district are advised to take precautionary measures as heavy rainfall is predicted over the next 48 hours. The meteorological department has issued an orange alert for the region.\n\nEnsure drainage channels are clear to prevent waterlogging in fields, especially for crops like onions and grapes which are sensitive to excess moisture. It is also recommended to delay any planned spraying of pesticides until the weather clears up."
    },
    {
        "title": "Tomato Prices Surge by 20% in Local Markets",
        "source": "Market Watch",
        "category": "Market",
        "imageUrl": IMAGE_LIBRARY["market"][0],
        "content": "Tomato prices have seen a sharp increase of 20% in local APMC markets due to supply shortages caused by unseasonal rains in major growing belts.\n\nTraders expect prices to remain high for the next two weeks until fresh arrivals from neighboring states stabilize the supply. Farmers with ready stock are advised to bring their produce to market gradually to maximize returns."
    }
]

async def get_agri_news():
    """
    Uses AI to generate fresh, realistic agricultural news on the fly.
    """
    print("📰 NEWS: Generating fresh content via AI...")
    
    try:
        # Prompt Gemini to be a news aggregator with CONTENT field
        prompt = """
        Generate 5 realistic agricultural news items for India.
        Mix categories like 'Policy', 'Weather', 'Market', 'Technology'.
        
        Return a valid JSON array of objects with these keys:
        - title: Headline
        - source: Agency name (e.g. AgriWire)
        - category: One word category (Policy, Weather, Market, Technology)
        - content: A detailed 2-3 paragraph description of the news event.
        
        Do not use markdown. Just the raw JSON.
        """
        
        # Non-blocking call
        loop = asyncio.get_event_loop()
        response = await loop.run_in_executor(None, model.generate_content, prompt)
        
        text = response.text.replace("```json", "").replace("```", "").strip()
        ai_news = json.loads(text)
        
        # Add Images & Times dynamically
        final_news = []
        for idx, item in enumerate(ai_news):
            
            # --- SIMPLE & SAFE IMAGE SELECTION (REVERTED TO WORKING LOGIC) ---
            category = item.get('category', 'General')
            cat_lower = category.lower()
            
            if "weather" in cat_lower:
                image_url = random.choice(IMAGE_LIBRARY["weather"])
            elif "market" in cat_lower:
                image_url = random.choice(IMAGE_LIBRARY["market"])
            elif "tech" in cat_lower:
                image_url = random.choice(IMAGE_LIBRARY["tech"])
            elif "policy" in cat_lower or "govt" in cat_lower:
                image_url = random.choice(IMAGE_LIBRARY["policy"])
            else:
                image_url = random.choice(IMAGE_LIBRARY["general"])

            # Random time ago
            time_val = random.randint(1, 10)
            
            final_news.append({
                "id": idx + 1,
                "title": item.get('title', 'News Headline'),
                "source": item.get('source', 'AgriNext News'),
                "timeAgo": f"{time_val}h ago",
                "category": category,
                "imageUrl": image_url,
                "content": item.get('content', "Read more details in the full article.")
            })
            
        # --- DEBUG ---
        print("\n" + "="*40)
        print("📰 GENERATED NEWS FEED:")
        for news in final_news:
            print(f"🔹 [{news['category']}] {news['title']}")
            print(f"   📝 Content Snippet: {news['content'][:50]}...")
            print(f"   🖼️ Image: {news['imageUrl']}")
        print("="*40 + "\n")
            
        return final_news

    except Exception as e:
        print(f"⚠️ News AI Failed: {e}")
        # Return fallback with simulated content
        formatted_fallback = []
        for idx, item in enumerate(FALLBACK_NEWS):
            formatted_fallback.append({
                "id": idx + 1,
                "title": item["title"],
                "source": item["source"],
                "timeAgo": "4h ago",
                "category": item["category"],
                "imageUrl": item["imageUrl"],
                "content": item["content"]
            })
        return formatted_fallback