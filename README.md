<div align="center">

# 🌾 NeoAgriNext (Hackathon Edition)

### Code for Communities 2 Hackathon Submission

![Android](https://img.shields.io/badge/Android-13+-green?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack_Compose-purple?style=for-the-badge&logo=kotlin)
![Python](https://img.shields.io/badge/Python-FastAPI-blue?style=for-the-badge&logo=python)
![Google Cloud](https://img.shields.io/badge/Google_Cloud-Gemini_AI-orange?style=for-the-badge&logo=googlecloud)

**An Interoperable Digital Agriculture Network empowering Indian farmers with real-time AI advisory, dynamic farm scheduling, and DPG-compliant Open APIs.**

</div>

---

## 🎯 About NeoAgriNext

NeoAgriNext was built to solve the fragmentation of agricultural data by acting as a unified **Digital Public Good (DPG)** layer. It bridges the gap between raw data (satellite imagery, weather sensors, market rates) and actionable, highly contextual AI advice for farmers via a seamless mobile experience.

---

## ✨ Hackathon Features & Completeness

Based on our initial 5-Day Task Allocation matrix, we successfully achieved **~85% of our targeted architecture**:

### ✅ Completed & Fully Functional
- **Google AI & Vision (Dev 1/2):** Integrated **Google Gemini AI** to power the crop advisory engine and dynamic news generation. 
- **Dynamic Jetpack Compose UI (Dev 1/2):** Built a beautiful, edge-to-edge Android UI using Material Design 3. The `HomeScreen` is fully wired to the backend, displaying dynamic real-time AI farm health scores and automatically generated agricultural news.
- **FastAPI Agro-Intelligence Backend (Dev 2):** Built a high-performance Python 3.11 backend. It features intelligent caching (SQLite) to minimize API latency and token limits during analysis.
- **DPG Open API Compliance (Dev 3):** Successfully implemented standard Indian AgriStack schema endpoints (`/api/v1/dpg/advisory`, `/api/v1/dpg/models`) via OpenAPI 3.0 (Swagger UI).
- **Cloud Infrastructure CI/CD (Dev 3):** Created a production-ready `Dockerfile` and GitHub Actions pipeline for Google Cloud Run deployment.

### 🔄 Simulated or Mocked (Due to Time Constraints)
- **BigQuery / Firebase Sync:** We pivoted to a local SQLite cache on the FastAPI backend for extreme speed and demo reliability.
- **Earth Engine Telemetry:** Satellite telemetry logic is written but currently falls back to simulation mode due to library dependency limits on Windows.

---

## 🔧 Google Tech Stack Used

### 1. Android Mobile App Layer
- **UI:** Kotlin + Jetpack Compose (Material Design 3)
- **Networking:** Retrofit 2.9.0 + OkHttp3
- **Image/Camera:** CameraX + Coil 2.4.0

### 2. Agro-Intelligence Backend Service
- **Framework:** Python 3.11 + FastAPI
- **Generative AI:** Google Gemini API (via `google-generativeai`)
- **Data Integrations:** OpenWeatherMap API, Local SQLite Caching

### 3. Interoperable DPG Layer
- **API Specs:** OpenAPI 3.0 via FastAPI Swagger UI
- **Containerization:** Docker (Ready for Google Cloud Run)

---

## 🚀 Running the Project

### Start the FastAPI Backend
1. Make sure you have your API keys in a `.env` file (`GEMINI_API_KEY`, `OPENWEATHER_API_KEY`).
2. Run the server:
```powershell
# Set encoding to prevent Windows emoji crashes
$env:PYTHONIOENCODING="utf-8"
python -m uvicorn app.main:app --port 8000
```
3. View the **DPG Open API Swagger Docs**: `http://localhost:8000/docs`

### Build the Android App
1. Ensure your `app/src/main/java/com/example/agrinext/Config.kt` points to your backend URL (e.g., an ngrok tunnel).
2. Build and install via Gradle:
```powershell
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 👥 Team
Built with ❤️ for the **Code for Communities 2 Hackathon**.
