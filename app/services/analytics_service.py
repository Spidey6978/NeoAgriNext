import pandas as pd
import numpy as np
import sqlite3
import json
import base64
import io
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
from collections import Counter
import warnings

# Use non-interactive backend for server environments
import matplotlib
matplotlib.use('Agg')

from app.services.cache_service import DB_NAME

# =============================================================================
# SECTION A: SETUP & CONFIGURATION
# =============================================================================
warnings.filterwarnings('ignore')  # Suppress warnings for cleaner logs

# Configuration for plots
plt.style.use('dark_background')  # Match your neon theme
SNS_PALETTE = "viridis"

class AgriAnalyticsEngine:
    """
    Advanced Data Science Engine for Agricultural Intelligence.
    Performs automated EDA, feature engineering, and insight generation.
    """
    
    def __init__(self, db_path=DB_NAME):
        self.db_path = db_path
        self.raw_data = None
        self.processed_data = None
        self.insights = {}
        self.figures = {}

    # =========================================================================
    # SECTION B: DATA LOADING & INGESTION
    # =========================================================================
    def load_data(self):
        """
        Connects to the SQLite Data Lake and ingests raw sensor/cache logs.
        """
        print("📊 ANALYTICS: Connecting to Data Lake...")
        try:
            conn = sqlite3.connect(self.db_path)
            
            # Fetch from the ML-ready table if populated
            query_ml = "SELECT * FROM smart_farm_data"
            try:
                df_ml = pd.read_sql_query(query_ml, conn)
            except:
                df_ml = pd.DataFrame() # Table might not exist yet

            # Fetch from the raw cache for broader context
            query_cache = "SELECT * FROM crop_cache"
            df_cache = pd.read_sql_query(query_cache, conn)
            
            conn.close()
            
            # If ML table is empty, try to construct it from cache blobs
            if df_ml.empty and not df_cache.empty:
                print("⚠️ ANALYTICS: ML Table empty. Constructing dataset from Raw Logs...")
                self.raw_data = self._parse_cache_to_dataframe(df_cache)
            else:
                self.raw_data = df_ml

            print(f"✅ Data Loaded: {len(self.raw_data)} records found.")
            return self.raw_data

        except Exception as e:
            print(f"❌ Data Loading Failed: {e}")
            return pd.DataFrame()

    def _parse_cache_to_dataframe(self, df_cache):
        """
        Helper to parse raw JSON blobs into structured features.
        """
        extracted = []
        for _, row in df_cache.iterrows():
            try:
                key_parts = row['location_key'].split('_')
                data = json.loads(row['data'])
                
                record = {
                    'timestamp': datetime.fromtimestamp(row['timestamp']),
                    'type': row['data_type'],
                    'key': row['location_key']
                }
                
                # Feature Extraction based on type
                if "crop_" in row['location_key']:
                    record['category'] = 'Crop Recommendation'
                    # Extract Lat/Lon from key if possible
                    if len(key_parts) >= 3:
                        record['lat'] = float(key_parts[1])
                        record['lon'] = float(key_parts[2])
                
                elif "market_" in row['location_key']:
                    record['category'] = 'Market Analysis'
                    if len(key_parts) >= 4:
                        record['crop_name'] = key_parts[3]
                    # Extract price data
                    if 'prices' in data:
                        record['market_price'] = data['prices'].get('city_mandi', np.nan)
                        record['local_price'] = data['prices'].get('local_mandi', np.nan)
                
                elif "gemini_" in row['location_key']:
                    record['category'] = 'AI Advisory'
                    if 'roadmap' in data:
                        record['crop_name'] = data['roadmap'].get('name', 'Unknown')
                        record['duration'] = data['roadmap'].get('duration_days', 0)

                extracted.append(record)
            except:
                continue
                
        return pd.DataFrame(extracted)

    # =========================================================================
    # SECTION C: DATA CLEANING & FEATURE ENGINEERING
    # =========================================================================
    def clean_and_engineer(self):
        """
        Prepares raw data for analysis: handling missing values, typing, and feature creation.
        """
        if self.raw_data is None or self.raw_data.empty:
            return

        df = self.raw_data.copy()

        # 1. Date Handling
        if 'timestamp' in df.columns:
            df['date'] = pd.to_datetime(df['timestamp'])
            df['hour'] = df['date'].dt.hour
            df['day_of_week'] = df['date'].dt.day_name()
        
        # 2. Categorical Standardization
        if 'crop_name' in df.columns:
            df['crop_name'] = df['crop_name'].str.title().fillna('Unknown')

        # 3. Numeric Cleaning
        numeric_cols = ['market_price', 'local_price', 'duration']
        for col in numeric_cols:
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')

        # 4. Feature Creation: Profit Margin
        if 'market_price' in df.columns and 'local_price' in df.columns:
            df['profit_spread'] = df['market_price'] - df['local_price']
            df['profit_margin_pct'] = (df['profit_spread'] / df['local_price']) * 100

        self.processed_data = df
        print("✅ Data Cleaning & Engineering Complete.")

    # =========================================================================
    # SECTION D: EXPLORATORY DATA ANALYSIS (EDA)
    # =========================================================================
    def perform_eda(self):
        """
        Executes core statistical analysis on the processed dataset.
        """
        df = self.processed_data
        if df is None or df.empty:
            return {"error": "No data available for analysis"}

        insights = {
            "total_records": len(df),
            "time_range": {
                "start": str(df['date'].min()),
                "end": str(df['date'].max())
            } if 'date' in df else "N/A",
            "distributions": {},
            "trends": {}
        }

        # 1. Crop Popularity Analysis
        if 'crop_name' in df.columns:
            counts = df['crop_name'].value_counts().to_dict()
            insights['distributions']['top_crops'] = counts

        # 2. Market Value Analysis
        if 'market_price' in df.columns:
            stats = df['market_price'].describe().to_dict()
            insights['distributions']['price_stats'] = stats
            
            # Anomaly Detection (Z-Score > 2)
            mean = df['market_price'].mean()
            std = df['market_price'].std()
            anomalies = df[np.abs(df['market_price'] - mean) > (2 * std)]
            insights['anomalies'] = {
                "count": len(anomalies),
                "high_value_crops": anomalies['crop_name'].unique().tolist() if 'crop_name' in anomalies else []
            }

        self.insights = insights
        return insights

    # =========================================================================
    # SECTION E: VISUALIZATION GENERATION
    # =========================================================================
    def generate_visualizations(self):
        """
        Creates professional plots and encodes them as Base64 strings for web display.
        """
        df = self.processed_data
        if df is None or df.empty:
            return

        plots = {}

        # Plot 1: Crop Frequency Distribution (Bar Chart)
        if 'crop_name' in df.columns:
            plt.figure(figsize=(10, 6))
            top_crops = df['crop_name'].value_counts().head(10)
            sns.barplot(x=top_crops.values, y=top_crops.index, palette=SNS_PALETTE)
            plt.title('Top Analyzed Crops (User Interest)', fontsize=14, color='white')
            plt.xlabel('Count', color='white')
            plt.ylabel('Crop', color='white')
            plots['crop_dist'] = self._save_plot_to_base64()

        # Plot 2: Market Price Distribution (Histogram)
        if 'market_price' in df.columns:
            plt.figure(figsize=(10, 6))
            sns.histplot(df['market_price'].dropna(), kde=True, color='#00ff9d')
            plt.title('Market Price Distribution', fontsize=14, color='white')
            plt.xlabel('Price (INR/Quintal)', color='white')
            plots['price_dist'] = self._save_plot_to_base64()

        # Plot 3: Activity Heatmap (Day vs Hour) - If enough data
        if 'day_of_week' in df.columns and 'hour' in df.columns and len(df) > 5:
            plt.figure(figsize=(10, 6))
            pivot = df.pivot_table(index='day_of_week', columns='hour', aggfunc='size', fill_value=0)
            sns.heatmap(pivot, cmap='viridis', annot=True, fmt='d')
            plt.title('User Activity Heatmap', fontsize=14, color='white')
            plots['activity_map'] = self._save_plot_to_base64()

        self.figures = plots
        print(f"✅ Generated {len(plots)} visualization assets.")

    def _save_plot_to_base64(self):
        """Helper to convert Matplotlib plots to Base64 strings."""
        buffer = io.BytesIO()
        plt.tight_layout()
        plt.savefig(buffer, format='png', transparent=True)
        plt.close()
        buffer.seek(0)
        image_png = buffer.getvalue()
        graph = base64.b64encode(image_png)
        return graph.decode('utf-8')

    # =========================================================================
    # SECTION F: PIPELINE EXECUTION
    # =========================================================================
    def run_pipeline(self):
        """
        Orchestrates the full analysis pipeline.
        Returns: Tuple(Insights Dict, Plots Dict)
        """
        self.load_data()
        self.clean_and_engineer()
        insights = self.perform_eda()
        self.generate_visualizations()
        
        return {
            "meta": {
                "generated_at": datetime.now().isoformat(),
                "status": "success"
            },
            "insights": insights,
            "visualizations": self.figures
        }

# --- STANDALONE TESTING ---
if __name__ == "__main__":
    engine = AgriAnalyticsEngine()
    result = engine.run_pipeline()
    print(json.dumps(result['insights'], indent=2))