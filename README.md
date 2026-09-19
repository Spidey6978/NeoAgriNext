<div align="center">

# 🌾 AgriNext

## 🎥 Demo Video

Watch the full demo here:  
👉 https://drive.google.com/drive/folders/1P_c8xT5calfFbLno6ex74H9rzJmeV9nC?usp=drive_link


### Bridging the Gap in Digital Agriculture

![AgriNext](https://img.shields.io/badge/Version-1.0.0-brightgreen?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-12+-green?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple?style=for-the-badge&logo=kotlin)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**Empowering Indian farmers with real-time market data, weather insights, and AI-driven agriculture recommendations**

[Features](#-features) • [Tech Stack](#-tech-stack) • [Installation](#-installation) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## 🎯 About AgriNext

AgriNext is a comprehensive mobile application designed to solve information fragmentation in Indian agriculture. By unifying real-time weather data, market prices from Agmarknet, expert advice, and AI-powered insights into one intuitive interface, AgriNext empowers farmers to make better decisions and connect directly with buyers.

**Mission**: Democratize access to agricultural information and create fair marketplace connections for Indian farmers.

---

## ✨ Key Features

### 🌤️ Real-Time Weather Integration
- Hyperlocal weather forecasts tailored to farm location
- Agricultural alerts based on weather patterns
- Historical weather data for crop planning

### 💹 Live Market Price Data
- Real-time prices from Agmarknet
- Price trend analysis and predictions
- Multi-commodity price tracking
- Direct integration with government agricultural databases

### 🤖 AI-Powered Recommendations
- Personalized crop recommendations based on:
  - Local climate conditions
  - Soil type and characteristics
  - Market demand trends
  - Historical yields
- Smart market timing suggestions
- Predictive analytics for better planning

### 🏪 Local Digital Marketplace
- Direct farmer-to-buyer connections
- Eliminates middleman overhead
- Fair pricing for all stakeholders
- Verified user accounts and secure transactions
- Rating and review system

### 📱 Beautiful Mobile Experience
- Modern Material Design 3 UI
- Responsive and intuitive navigation
- Offline-first architecture
- Multi-language support ready
- Optimized for all Android devices (API 24+)

---

## 🔧 Tech Stack

### Frontend
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Minimum SDK**: Android 12+ (API 24 minimum)
- **Target SDK**: Android 13 (API 36)

### Backend & Services
- **Authentication**: Firebase Authentication
- **Database**: Firebase Realtime Database
- **Maps**: Google Maps API
- **Location**: Google Play Services Location
- **Translation**: ML Kit for on-device translation

### Build & Deployment
- **Build System**: Gradle with Kotlin DSL
- **CI/CD**: GitHub Actions
- **Distribution**: APK + AAB releases

### Key Dependencies
- Retrofit 2.9.0 for API calls
- Coil 2.4.0 for image loading
- Jetpack Navigation Compose
- Google Services integration

---

## 📥 Installation

### Prerequisites
- Android 12 or higher
- Internet connection for real-time features
- Location permissions enabled

### Steps

1. **Download the APK**
   - Go to [Releases](https://github.com/Spidey6978/AgriNext/releases)
   - Download the latest `app-release.apk` or `app-debug.apk`

2. **Enable Installation from Unknown Sources**
   - Go to Settings → Security
   - Enable "Install from unknown sources"

3. **Install the APK**
   - Open your Downloads folder
   - Tap the AgriNext APK file
   - Tap "Install"

4. **Launch & Setup**
   - Open AgriNext from your app drawer
   - Sign up with your email or phone number
   - Grant necessary permissions (location, storage)
   - Allow location access for weather and market data

5. **Start Using**
   - Set your farm location
   - Configure crop preferences
   - Start receiving personalized recommendations

---

## 🏗️ Architecture

```
AgriNext/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/agrinext/
│   │   │   │   ├── ui/screens/       # Jetpack Compose screens
│   │   │   │   ├── viewmodel/        # MVVM ViewModels
│   │   │   │   ├── repository/       # Data repositories
│   │   │   │   ├── service/          # Firebase & API services
│   │   │   │   └── model/            # Data models
│   │   │   └── res/                  # Resources
│   │   └── androidTest/              # Integration tests
│   └── build.gradle.kts              # App dependencies
├── gradle/                           # Gradle wrapper
└── .github/workflows/                # CI/CD automation
```

---

## 🚀 Getting Started (Development)

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11 or higher
- Android SDK API 36
- Git

### Setup

```bash
# Clone the repository
git clone https://github.com/Spidey6978/AgriNext.git
cd AgriNext

# Check out the development branch
git checkout Frontend

# Build the project
./gradlew build

# Run on emulator or device
./gradlew installDebug
```

### Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires keystore)
./gradlew assembleRelease

# Android App Bundle
./gradlew bundleRelease

# Run tests
./gradlew test
```

---

## 📊 Current Features Status

| Feature | Status | Notes |
|---------|--------|-------|
| Weather Integration | ✅ Complete | Real-time data from API |
| Market Prices | ✅ Complete | Live Agmarknet integration |
| User Authentication | ✅ Complete | Firebase Auth |
| Crop Recommendations | 🔄 In Progress | ML model being trained |
| Marketplace | 🔄 In Progress | Phase 2 development |
| Multi-language | 📋 Planned | ML Kit ready, translation pending |
| Push Notifications | 📋 Planned | Firebase Cloud Messaging |
| Advanced Analytics | 📋 Planned | User behavior tracking |

---

## 🐛 Known Issues & Limitations

- 🔴 Translation features require internet connectivity
- 🔴 Market data updates may have slight delays (5-10 minutes)
- 🟡 Offline marketplace functionality still in development
- 🟡 Some screens optimize better for landscape on tablets

---

## 🗺️ Roadmap

### v1.1.0 (Q1 2026)
- [ ] Push notifications for price alerts
- [ ] Crop calendar with farming schedule
- [ ] Extended weather forecasts (14 days)
- [ ] Offline mode for previously viewed data

### v1.2.0 (Q2 2026)
- [ ] Marketplace phase 1 (direct connections)
- [ ] Payment integration
- [ ] Invoice & receipt management
- [ ] Multi-language support (Hindi, Tamil, Telugu, Kannada)

### v2.0.0 (Q3 2026)
- [ ] Advanced analytics dashboard
- [ ] Crop insurance integration
- [ ] Government subsidy tracker
- [ ] Video tutorials and guides
- [ ] Expert consultation booking

---

## 📚 Documentation

- [API Documentation](https://drive.google.com/drive/folders/1i0gDeXLD3cMh0nrHnw9r4BkcCRNssn6B?usp=drive_link)
- [Architecture Guide](https://github.com/Spidey6978/AgriNext/wiki/Architecture)
- [Contributing Guidelines](https://github.com/Spidey6978/AgriNext/blob/Frontend/CONTRIBUTING.md)
- [Release Notes](https://github.com/Spidey6978/AgriNext/releases)

---

## 🤝 Contributing

We love contributions from the community! Whether you're fixing bugs, adding features, or improving documentation, your help is valuable.

### How to Contribute

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Kotlin style guide
- Write meaningful commit messages
- Add tests for new features
- Update documentation

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Team

**Built by the AgriNext Team**
- A collaborative project developed as part of AgriTech innovation initiatives
- Contributors: [Team Members]

---

## 💬 Support & Feedback

- **Report Issues**: [GitHub Issues](https://github.com/Spidey6978/AgriNext/issues)
- **Feature Requests**: [Discussions](https://github.com/Spidey6978/AgriNext/discussions)
- **Email**: [contact info if available]

---

## 🙏 Acknowledgments

- Agmarknet for agricultural data
- Google for Maps and ML Kit services
- Firebase for backend infrastructure
- Material Design team for UI guidelines
- Indian farming community for inspiration

---

<div align="center">

### Made with ❤️ for Indian Farmers

⭐ Star us on GitHub if you find AgriNext useful!

[⬆ back to top](#-agrinext)

</div>
