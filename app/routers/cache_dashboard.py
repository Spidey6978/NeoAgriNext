from fastapi import APIRouter
from fastapi.responses import HTMLResponse
import sqlite3
import json
import datetime
from app.services.cache_service import DB_NAME
from app.services.schedule_service import generate_farm_schedule

router = APIRouter()

@router.get("/cache-dashboard", response_class=HTMLResponse)
async def view_cache():
    rows = []
    latest_crop = None
    latest_timestamp = 0
    
    try:
        conn = sqlite3.connect(DB_NAME)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [r[0] for r in cursor.fetchall()]
        
        target_table = "crop_cache" if "crop_cache" in tables else "api_cache"
        
        if target_table in tables:
            cursor.execute(f"SELECT * FROM {target_table} ORDER BY timestamp DESC LIMIT 20")
            rows = cursor.fetchall()
            
            # Smart logic to find the 'Context' for the schedule
            for row in rows:
                try:
                    data = json.loads(row['data'])
                    # Check if this is an AI Analysis result (contains roadmap)
                    if "roadmap" in data and "name" in data["roadmap"]:
                        latest_crop = data["roadmap"]["name"]
                        latest_timestamp = row['timestamp']
                        break # Found the most recent valid analysis
                except: continue
    except: pass
    finally:
        try: conn.close()
        except: pass

    # Generate Schedule Data
    if latest_crop:
        # Simulate sowing date based on the timestamp of the analysis
        analysis_date = datetime.datetime.fromtimestamp(latest_timestamp).strftime("%Y-%m-%d")
        schedule_data = generate_farm_schedule(latest_crop, analysis_date)
        farm_score = schedule_data['farm_score']
        schedule_title = f"GENERATED SCHEDULE: {latest_crop.upper()}"
        schedule_visible = "block"
        empty_state = "none"
    else:
        schedule_data = {"tasks": []}
        farm_score = 0
        schedule_title = "Awaiting Data..."
        schedule_visible = "none"
        empty_state = "block"

    html_content = f"""
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>AgriNext Intelligence Hub</title>
        <meta http-equiv="refresh" content="10">
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600&family=JetBrains+Mono:wght@400&display=swap" rel="stylesheet">
        <style>
            :root {{
                --bg-dark: #0f1115;
                --panel-dark: #181b21;
                --border-color: #2a2e35;
                --accent-green: #00ff9d;
                --accent-blue: #00bcd4;
                --accent-purple: #bb86fc;
                --text-main: #e0e0e0;
                --text-muted: #8b949e;
            }}
            
            * {{ box-sizing: border-box; }}

            body {{
                font-family: 'Inter', sans-serif;
                background-color: var(--bg-dark);
                color: var(--text-main);
                margin: 0;
                padding: 20px;
                height: 100vh;
                display: flex;
                flex-direction: column;
            }}
            
            /* HEADER */
            .header {{
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding-bottom: 20px;
                border-bottom: 1px solid var(--border-color);
                margin-bottom: 20px;
            }}
            
            h1 {{ margin: 0; font-size: 1.2rem; letter-spacing: 1px; color: var(--accent-green); }}
            
            /* MAIN LAYOUT */
            .main-grid {{
                display: grid;
                grid-template-columns: 1fr 1fr; /* Split 50/50 */
                gap: 20px;
                flex: 1;
                overflow: hidden; /* Prevent body scroll */
            }}
            
            .column {{
                display: flex;
                flex-direction: column;
                overflow: hidden;
                background: var(--panel-dark);
                border: 1px solid var(--border-color);
                border-radius: 8px;
            }}
            
            .col-header {{
                padding: 15px;
                background: rgba(255,255,255,0.03);
                border-bottom: 1px solid var(--border-color);
                font-weight: 600;
                display: flex;
                justify-content: space-between;
            }}
            
            .col-content {{
                flex: 1;
                overflow-y: auto; /* Internal Scroll */
                padding: 15px;
            }}

            /* LOG CARDS */
            .log-card {{
                background: #0d0f12;
                border: 1px solid var(--border-color);
                border-radius: 6px;
                padding: 10px;
                margin-bottom: 10px;
                font-size: 0.85em;
                min-width: 0; /* Prevent overflow */
            }}
            
            .log-meta {{
                display: flex;
                justify-content: space-between;
                margin-bottom: 8px;
                font-family: 'JetBrains Mono', monospace;
                font-size: 0.8em;
                color: var(--accent-blue);
            }}
            
            .json-dump {{
                font-family: 'JetBrains Mono', monospace;
                color: var(--text-muted);
                white-space: pre-wrap; /* Wrap text so it doesn't cut off */
                word-break: break-all; /* Break long tokens */
                max-height: 150px;
                overflow-y: auto; /* Scrollable Y */
                overflow-x: auto; /* Scrollable X if needed */
                background: rgba(0,0,0,0.3);
                padding: 5px;
                border-radius: 4px;
            }}

            /* SCHEDULE ITEMS */
            .timeline-item {{
                display: flex;
                gap: 15px;
                margin-bottom: 20px;
                position: relative;
            }}
            
            .timeline-item::before {{
                content: '';
                position: absolute;
                left: 7px; top: 20px; bottom: -25px;
                width: 2px; background: var(--border-color);
            }}
            .timeline-item:last-child::before {{ display: none; }}
            
            .dot {{
                width: 16px; height: 16px;
                border-radius: 50%;
                background: var(--panel-dark);
                border: 2px solid var(--text-muted);
                z-index: 2;
                margin-top: 2px;
                flex-shrink: 0;
            }}
            
            .dot.active {{ border-color: var(--accent-green); background: var(--accent-green); }}
            
            .task-info {{ flex: 1; min-width: 0; }}
            .task-info h4 {{ margin: 0 0 4px 0; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }}
            .task-info p {{ margin: 0; color: var(--text-muted); font-size: 0.9em; }}
            .task-date {{ font-size: 0.8em; color: var(--accent-purple); font-family: 'JetBrains Mono', monospace; }}

        </style>
    </head>
    <body>
        <div class="header">
            <div>
                <h1>AGRINEXT DASHBOARD</h1>
                <small style="color:var(--text-muted)">Real-time Data Processing Pipeline</small>
            </div>
            <div>
                <form action="/clear" method="post" onsubmit="return confirm('Clear all data?');">
                    <button style="background:transparent; border:1px solid #ff5252; color:#ff5252; padding:5px 10px; border-radius:4px; cursor:pointer;">RESET SYSTEM</button>
                </form>
            </div>
        </div>

        <div class="main-grid">
            
            <!-- COLUMN 1: RAW DATA INGESTION -->
            <div class="column">
                <div class="col-header">
                    <span>STEP 1: RAW SENSOR DATA</span>
                    <span style="color:var(--accent-blue)">{len(rows)} LOGS</span>
                </div>
                <div class="col-content">
    """

    for row in rows:
        key = row['location_key'] if 'location_key' in row.keys() else row['cache_key']
        timestamp = row['timestamp']
        data_str = row['data']
        
        try:
            time_str = datetime.datetime.fromtimestamp(timestamp).strftime('%H:%M:%S')
            parsed = json.loads(data_str)
            pretty_json = json.dumps(parsed, indent=2)
        except:
            time_str = "Unknown"
            pretty_json = data_str

        html_content += f"""
            <div class="log-card">
                <div class="log-meta">
                    <span>{key}</span>
                    <span>{time_str}</span>
                </div>
                <div class="json-dump">{pretty_json}</div>
            </div>
        """

    html_content += f"""
                </div>
            </div>

            <!-- COLUMN 2: PROCESSED INTELLIGENCE -->
            <div class="column">
                <div class="col-header">
                    <span>STEP 2: AI GENERATED SCHEDULE</span>
                    <span style="color:var(--accent-green)">SCORE: {farm_score}%</span>
                </div>
                <div class="col-content">
                    <div style="display:{empty_state}; text-align:center; padding:40px; color:var(--text-muted);">
                        Waiting for Analysis Data...<br>
                        <small>Use the App to generate a plan.</small>
                    </div>
                    
                    <div style="display:{schedule_visible}">
                        <div style="margin-bottom:20px; padding:10px; background:rgba(0,255,157,0.1); border:1px solid var(--accent-green); border-radius:6px; color:var(--accent-green); font-weight:bold; text-align:center;">
                            {schedule_title}
                        </div>
    """

    if latest_crop:
        for task in schedule_data['tasks']:
            active_class = "active" if task['isCompleted'] else ""
            
            html_content += f"""
                <div class="timeline-item">
                    <div class="dot {active_class}"></div>
                    <div class="task-info">
                        <div class="task-date">{task['date']} (Day {task['daysOffset']})</div>
                        <h4>{task['stage']}</h4>
                        <p>👉 {task['actionTitle']}</p>
                        <p style="opacity:0.6; font-size:0.8em">{task['triggerInfo']}</p>
                    </div>
                </div>
            """

    html_content += """
                    </div>
                </div>
            </div>
        </div>
    </body>
    </html>
    """
    
    return html_content