from fastapi import FastAPI
from fastapi.responses import HTMLResponse, RedirectResponse
import sqlite3
import json
import datetime
import os

# We point to the same database file your app is using
DB_NAME = "agrinext_cache.db"

app = FastAPI(title="AgriNext Visualizer")

def get_db_connection():
    try:
        conn = sqlite3.connect(DB_NAME)
        conn.row_factory = sqlite3.Row
        return conn
    except Exception as e:
        print(f"Database connection error: {e}")
        return None

@app.get("/", response_class=HTMLResponse)
async def read_root():
    """
    Reads the SQLite cache and renders a live HTML dashboard.
    """
    # Check if DB exists
    if not os.path.exists(DB_NAME):
        return """
        <div style="font-family:sans-serif; text-align:center; padding:50px; color:#555;">
            <h1>📭 Database Not Found</h1>
            <p>I couldn't find <b>agrinext_cache.db</b> in the root folder.</p>
            <p>Run your main app (AgriNext) and search for a crop first to create it!</p>
        </div>
        """

    conn = get_db_connection()
    if not conn:
        return "<h1>Error connecting to database</h1>"

    try:
        cursor = conn.cursor()
        
        # 1. Discover Tables (Check for both known schemas)
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        all_tables = [r['name'] for r in cursor.fetchall()]
        
        target_tables = [t for t in all_tables if t in ('crop_cache', 'api_cache')]
        
        rows = []
        total_count = 0
        table_status = []
        
        # 2. Fetch Data from ALL found tables
        if not target_tables:
            debug_msg = "⚠️ CRITICAL: No cache tables found! (Expected 'crop_cache' or 'api_cache')"
        else:
            for t in target_tables:
                # Count
                cursor.execute(f"SELECT Count(*) as count FROM {t}")
                c = cursor.fetchone()['count']
                total_count += c
                table_status.append(f"{t} ({c})")
                
                # Fetch Rows
                try:
                    cursor.execute(f"SELECT *, '{t}' as source_table FROM {t} ORDER BY timestamp DESC LIMIT 50")
                    rows.extend(cursor.fetchall())
                except:
                    pass # Schema mismatch might cause error, skip
            
            # Sort combined rows by timestamp desc
            rows.sort(key=lambda x: x['timestamp'], reverse=True)
            rows = rows[:50]
            
            debug_msg = f"Tables: {', '.join(table_status)}"

    finally:
        conn.close()

    # --- GENERATE HTML ---
    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>AgriNext Brain</title>
        <meta http-equiv="refresh" content="5"> <!-- Auto-Refresh every 5s -->
        <style>
            body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f3f4f6; padding: 20px; color: #1f2937; }}
            .container {{ max_width: 1200px; margin: 0 auto; }}
            .header {{ display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }}
            .card {{ background: white; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); overflow: hidden; }}
            table {{ width: 100%; border-collapse: collapse; text-align: left; }}
            th {{ background: #f9fafb; padding: 12px 16px; font-size: 12px; font-weight: 600; text-transform: uppercase; color: #6b7280; border-bottom: 1px solid #e5e7eb; }}
            td {{ padding: 16px; border-bottom: 1px solid #e5e7eb; vertical-align: top; font-size: 14px; }}
            tr:hover {{ background: #f9fafb; }}
            .tag {{ display: inline-block; padding: 2px 8px; border-radius: 99px; font-size: 11px; font-weight: 600; }}
            .tag-crop {{ background: #d1fae5; color: #065f46; }}
            .tag-market {{ background: #dbeafe; color: #1e40af; }}
            .tag-weather {{ background: #ffedd5; color: #9a3412; }}
            .tag-gemini {{ background: #f3e8ff; color: #6b21a8; }}
            .json {{ font-family: monospace; background: #111827; color: #a5b4fc; padding: 10px; border-radius: 6px; font-size: 12px; max-height: 150px; overflow-y: auto; white-space: pre-wrap; }}
            .btn {{ background: #ef4444; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; }}
            .debug {{ font-size: 12px; color: #666; margin-top: 5px; }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <div>
                    <h1>🧠 AgriNext Cache <span style="font-size:16px; color:#6b7280; font-weight:400">({total_count} records)</span></h1>
                    <div class="debug">{debug_msg}</div>
                </div>
                <form action="/clear" method="post" onsubmit="return confirm('Clear all data?');">
                    <button class="btn">🗑️ Wipe Cache</button>
                </form>
            </div>
            <div class="card">
                <table>
                    <thead>
                        <tr>
                            <th width="10%">Type</th>
                            <th width="20%">Key / Time</th>
                            <th width="70%">Data</th>
                        </tr>
                    </thead>
                    <tbody>
    """
    
    for row in rows:
        key = row['location_key']
        ts = row['timestamp']
        data = row['data']
        
        # Determine Tag
        if "crop_" in key: tag = '<span class="tag tag-crop">CROP</span>'
        elif "market_" in key: tag = '<span class="tag tag-market">MARKET</span>'
        elif "weather_" in key: tag = '<span class="tag tag-weather">WEATHER</span>'
        elif "gemini_" in key: tag = '<span class="tag tag-gemini">AI ADVICE</span>'
        else: tag = '<span class="tag">OTHER</span>'
        
        time_str = datetime.datetime.fromtimestamp(ts).strftime('%H:%M:%S')
        
        try:
            parsed = json.loads(data)
            pretty_json = json.dumps(parsed, indent=2)
        except:
            pretty_json = data

        html_content += f"""
        <tr>
            <td>{tag}</td>
            <td>
                <div><b>{key}</b></div>
                <div style="color:#9ca3af; font-size:12px; margin-top:4px">{time_str}</div>
            </td>
            <td><div class="json">{pretty_json}</div></td>
        </tr>
        """

    html_content += """
                    </tbody>
                </table>
            </div>
        </div>
    </body>
    </html>
    """
    return html_content

@app.post("/clear")
async def clear_cache():
    conn = get_db_connection()
    if conn:
        try:
            # Try clearing both possible table names
            conn.execute("DELETE FROM crop_cache")
        except: pass
        
        try:
            conn.execute("DELETE FROM api_cache")
        except: pass
        
        conn.commit()
        conn.close()
    return RedirectResponse(url="/", status_code=303)