import random
import json
import asyncio
import re
from google import genai
from app.config import settings

# Initialize new Google GenAI Client
client = genai.Client(api_key=settings.GEMINI_API_KEY)

# Updated list of valid, active models for google-genai
MODEL_CANDIDATES = [
    getattr(settings, "GEMINI_MODEL", None),
    "gemini-2.5-flash",
    "gemini-2.0-flash",
    "gemini-1.5-flash",
]
MODEL_CANDIDATES = [m for m in dict.fromkeys(MODEL_CANDIDATES) if m]

async def _generate_news_with_failover(prompt, loop):
    """Failover generator that cycles through available models on 429/404 errors."""
    backoffs = [5, 10]
    for model_name in MODEL_CANDIDATES:
        for attempt in range(len(backoffs) + 1):
            try:
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
    raise RuntimeError("All Gemini models failed for News Generation.")

IMAGE_LIBRARY = {
    "weather": [
        "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1504608524841-42fe6f032b4b?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1561470508-fd4df1ed90b2?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1534274988754-8d431a23a958?auto=format&fit=crop&w=600&q=80",
    ],
    "market": [
        "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=600&q=80",
    ],
    "tech": [
        "https://images.unsplash.com/photo-1508614589041-895b88991e3e?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=600&q=80",
    ],
    "policy": [
        "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1625246333195-5848c4281413?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1532629345422-7515f3d16bb6?auto=format&fit=crop&w=600&q=80",
    ],
    "general": [
        "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1605000797499-95a51c5269ae?auto=format&fit=crop&w=600&q=80",
        "https://images.unsplash.com/photo-1499529112042-449518cf8141?auto=format&fit=crop&w=600&q=80",
    ]
}

FALLBACK_NEWS = [
    {
        "title": "New Subsidy for Organic Fertilizers Announced",
        "source": "AgriNews Daily",
        "category": "Policy",
        "imageUrl": IMAGE_LIBRARY["policy"][0],
        "content": "The government has announced a new subsidy scheme aimed at promoting organic farming across the state."
    },
    {
        "title": "Monsoon Forecast: Heavy Rains Expected in Nashik",
        "source": "Weather Bureau",
        "category": "Weather",
        "imageUrl": IMAGE_LIBRARY["weather"][0],
        "content": "Farmers in the Nashik district are advised to take precautionary measures as heavy rainfall is predicted."
    },
    {
        "title": "Tomato Prices Surge by 20% in Local Markets",
        "source": "Market Watch",
        "category": "Market",
        "imageUrl": IMAGE_LIBRARY["market"][0],
        "content": "Tomato prices have seen a sharp increase of 20% in local APMC markets due to supply shortages."
    }
]

async def get_agri_news():
    print("📰 NEWS: Generating fresh content via AI...")
    
    try:
        prompt = """
        Generate 5 realistic agricultural news items for India.
        Mix categories like 'Policy', 'Weather', 'Market', 'Technology'.
        
        Return a valid JSON array of objects with these keys:
        - title: Headline
        - source: Agency name (e.g. AgriWire)
        - category: One word category (Policy, Weather, Market, Technology)
        - content: A detailed description of the news event.
        
        Do not use markdown formatting.
        """
        
        loop = asyncio.get_running_loop()
        raw_text = await _generate_news_with_failover(prompt, loop)
        
        cleaned_json = raw_text.replace("```json", "").replace("```", "").strip()
        match = re.search(r'\[.*\]', cleaned_json, re.DOTALL)
        if match:
            cleaned_json = match.group(0)

        ai_news = json.loads(cleaned_json)
        
        final_news = []
        for idx, item in enumerate(ai_news):
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
            
        return final_news

    except Exception as e:
        print(f"⚠️ News AI Failed: {e}")
        return [
            {
                "id": idx + 1,
                "title": item["title"],
                "source": item["source"],
                "timeAgo": "4h ago",
                "category": item["category"],
                "imageUrl": item["imageUrl"],
                "content": item["content"]
            }
            for idx, item in enumerate(FALLBACK_NEWS)
        ]