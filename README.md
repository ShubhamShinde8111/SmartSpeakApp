# 🗣️ Smart SpeakUp — AI-Powered English Learning App

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/AI-Groq%20%7C%20Gemini-8B5CF6?style=for-the-badge&logo=openai&logoColor=white" />
  <img src="https://img.shields.io/badge/Voice-Agora%20RTC-4F86F7?style=for-the-badge" />
</p>

> **Smart SpeakUp** is a feature-rich, gamified Android application that helps Hindi-speaking students learn English through structured grammar missions, real-time voice conversations, AI-powered answer evaluation, and a Duolingo-style progression system.

---

## 📋 Table of Contents

- [Features](#-features)
- [App Architecture](#-app-architecture)
- [Tech Stack](#-tech-stack)
- [Module Breakdown](#-module-breakdown)
- [Gamification System](#-gamification-system)
- [AI & Speech Integration](#-ai--speech-integration)
- [Database Structure](#-database-structure)
- [Project Structure](#-project-structure)
- [Setup & Configuration](#-setup--configuration)
- [API Keys Required](#-api-keys-required)
- [Build Instructions](#-build-instructions)
- [Screens & Navigation](#-screens--navigation)
- [Contributing](#-contributing)

---

## ✨ Features

### 🎓 Structured Learning Path
- **10 Grammar Chapters** organized into 3 difficulty sections:
  - 🌱 **Section 1 – Foundations**: Time Prepositions, Simple Past, Simple Present
  - 🚀 **Section 2 – Progressing**: Present Continuous, Simple Future, Past Continuous
  - 👑 **Section 3 – Advanced**: Articles, Prepositions of Place, Modal Verbs, Passive Voice
- Each chapter contains **4 sequential missions**: Learn → Practice → Translate → Speak

### 🤖 AI-Powered Missions
- **Learn**: Grammar theory with visual sections and examples
- **Practice**: Hindi-to-English sentence building via drag-and-drop word bank
- **Translate**: Listen to Hindi, speak/type the English translation with AI evaluation
- **Speak**: Open-ended Q&A with real-time speech recognition and AI scoring

### 🎮 Full Gamification Engine
- **XP & Leveling System** with an exponential progression curve
- **Coin Economy** earned through activities
- **Daily Login Bonus** (awarded once per day)
- **Badge System**: First Steps, On Fire, Rocket, Diamond, Supernova
- **Level-Up Celebration Dialog** with animations
- **Supernova Mode** (2× XP when on a 14-day streak)
- **Mission Manager** with daily missions and streak tracking

### 🎙️ Live Voice Conversation
- **Peer-to-peer audio calls** powered by **Agora RTC SDK**
- Automatic matchmaking via Firebase Firestore lobby
- Mute, speaker toggle, call timer, and connection diagnostics
- Token-based authentication with automatic token refresh and retry logic

### 📊 Progress Dashboard
- **Animated fluency score ring** (CircularProgressIndicator)
- **7-day activity bar chart** with color-coded bars (green/orange/red by score)
- **Strengths & weaknesses analysis** pulled from Firebase
- **Weekly insight messages** based on learning trends

### 📖 Built-in Dictionary
- Look up English words via **Retrofit + Dictionary API**
- XP reward for dictionary usage (up to 10 lookups/day)
- Rich results with multiple meanings, part of speech, and phonetics

### 👤 User Profile & Auth
- Firebase Authentication (Email/Password)
- Editable profile with Firebase Firestore sync
- Splash screen with auto-login via `SharedPreferences`
- Forgot password via Firebase email reset

### 🧠 Adaptive Engine
- Tracks per-category accuracy using **Exponential Moving Average**
- Categories: basic, tense, preposition, article, conversation, speech
- Automatically recommends easier content if accuracy < 60%
- Unlocks bonus challenges if accuracy > 90%

---

## 🏗️ App Architecture

```
Smart SpeakUp
│
├── Authentication Layer
│   └── SplashScreen → Login / Register / ForgotPassword
│
├── Home Dashboard (HomeActivity)
│   ├── XP Bar & Level Badge
│   ├── Daily Mission Card
│   ├── Quick-access Feature Cards (Dictionary, Learning, Call, etc.)
│   └── Side Drawer Navigation (Profile, Feedback, Logout)
│
├── Mission Hub (MissionHubActivity)
│   ├── Section Headers (Foundations / Progressing / Advanced)
│   ├── Chapter Cards with progress pills
│   └── Mission Launch → Learn / Practice / Translate / Speak
│
├── Gamification Layer (GamificationEngine)
│   ├── XP Awards, Coin Economy
│   ├── Level Calculation (sqrt curve)
│   ├── Badge System & Supernova Mode
│   └── Firestore Persistence
│
├── AI Layer (MissionAIEngine)
│   ├── Groq API (LLaMA 3.3-70B) for content generation
│   ├── Answer evaluation with word-level highlighting
│   └── Fallback fuzzy matching evaluator
│
├── Voice Layer (AudioCallActivity)
│   ├── Agora RTC SDK v4.5.0
│   ├── Firebase lobby + matchmaking
│   └── Token management (AgoraTokenService)
│
└── Progress Layer
    ├── Firebase Firestore (cloud)
    ├── SQLite local DB (LearningProgressDBHelper)
    └── ProgressDashboardActivity
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java (Android SDK 34, min SDK 24) |
| **Build System** | Gradle (with version catalog) |
| **UI** | XML Layouts, Material Design 3, ViewBinding |
| **Backend / Auth** | Firebase Authentication, Firestore |
| **AI Engine** | Groq API (LLaMA 3.3-70B), Google Generative AI SDK |
| **Voice Calls** | Agora RTC Full SDK 4.5.0 |
| **HTTP Client** | Retrofit 2.11 + Gson converter |
| **Networking** | Volley, HttpURLConnection |
| **Local Storage** | SQLite (via LearningProgressDBHelper) |
| **Speech** | Android SpeechRecognizer, TextToSpeech |
| **Animations** | ObjectAnimator, ValueAnimator, ViewPropertyAnimator |

---

## 📦 Module Breakdown

### Core Activities

| Activity | Description |
|---|---|
| `SplashScreenActivity` | Auto-login check, animated splash |
| `MainActivity` | Entry point / AI Speak practice hub |
| `HomeActivity` | Main dashboard with gamification UI |
| `MissionHubActivity` | Chapter progression & mission launcher |
| `LearnMissionActivity` | Grammar theory with swipeable cards |
| `PracticeMissionActivity` | Hindi→English word-bank drag & drop |
| `TranslateMissionActivity` | Hindi→English speech/type translation |
| `SpeakMissionActivity` | Open Q&A with voice input + AI evaluation |
| `AudioCallActivity` | Live peer-to-peer voice call (Agora) |
| `DictionaryActivity` | Word lookup with meanings and XP reward |
| `ProgressDashboardActivity` | Animated stats, chart, and feedback |
| `ProfileActivity` / `EditProfileActivity` | User profile management |
| `ResultActivity` | Test/quiz result screen with scoring |
| `SelectRole` | Role-selection screen (e.g., student/teacher) |

### Engine Classes

| Class | Responsibility |
|---|---|
| `GamificationEngine` | XP, coins, levels, badges, Supernova mode |
| `MissionManager` | Daily mission assignment and completion tracking |
| `MissionAIEngine` | Groq API calls: content generation & answer evaluation |
| `AdaptiveEngine` | Per-category difficulty tracking using exponential moving average |
| `GrammarDataProvider` | Static content: all chapters, practice sentences, translate exercises, speak questions |
| `StreakHelper` | Daily streak calculation and milestone rewards |
| `FluencyScorer` | Computes fluency score from speech analysis results |
| `EnglishAnalyzer` | Text analysis helper for grammar checks |
| `FirebaseProgressHelper` | Firebase Firestore read/write for user milestone progress |
| `FirebaseProgressRepository` | Realtime listener-based stats aggregation |
| `LearningProgressDBHelper` | SQLite DB for offline session/conversation logging |
| `DBHelper` | Core SQLite helper for user data |

### Grammar Tests (Legacy Quiz Module)

| Class | Description |
|---|---|
| `BasicTest` | Basic English vocabulary quiz |
| `TenseTest` | Grammar tense identification quiz |
| `PrepositionTest` | Preposition-usage multiple choice |
| `ArticleTest` | Article (a/an/the) placement quiz |

---

## 🎮 Gamification System

### XP Award Table

| Activity | XP | Coins |
|---|---|---|
| Daily Login | 15 XP | 3 🪙 |
| Dictionary Lookup | 5 XP | 1 🪙 |
| Speech Practice | 25 XP | 5 🪙 |
| Voice Conversation | 40 XP | 10 🪙 |
| Quiz Complete | 50 XP | 10 🪙 |
| Daily Mission | 75 XP | 20 🪙 |
| Quiz Perfect Score | +100 XP | +25 🪙 |

### Level Formula

```
Level = floor(√(totalXP / 100)) + 1
```

| Level | XP Required |
|---|---|
| 1 | 0 XP |
| 2 | 100 XP |
| 3 | 400 XP |
| 5 | 1,600 XP |
| 10 | 8,100 XP |
| 20 | 36,100 XP |

### Rank System (Mission Hub)

| Rank | Level Range | Emoji |
|---|---|---|
| Meteor | 1–4 | ☄️ |
| Comet | 5–9 | 💫 |
| Star | 10–14 | ⭐ |
| Supernova | 15+ | 🌟 |

### Badges

| Badge ID | Trigger |
|---|---|
| `first_steps` | Complete Day 1 |
| `on_fire` | 3-day streak |
| `rocket` | Reach Level 5 |
| `diamond` | Reach Level 10 |
| `supernova` | 14-day streak (activates 2× XP) |

---

## 🤖 AI & Speech Integration

### Groq API (LLaMA 3.3-70B)
The `MissionAIEngine` uses Groq's OpenAI-compatible endpoint:

```
POST https://api.groq.com/openai/v1/chat/completions
Model: llama-3.3-70b-versatile
```

**Functions:**
1. **Generate Learn Content** – Grammar rules and bilingual examples
2. **Generate Practice Sentences** – Hindi→English with word banks and distractors
3. **Generate Translate Exercises** – Hindi sentence + expected English answer
4. **Generate Speak Questions** – Open-ended grammar-topic questions
5. **Evaluate Answers** – Word-level highlighting, score (0–100), correction, tips

**Fallback:** When the API is unavailable, a local fuzzy matcher (1-character edit distance + word coverage scoring) is used.

### Android Speech Services
- `SpeechRecognizer` — On-device ASR (Google Speech Services)
- Auto-detects silence and stops recording (1.5s silence timeout)
- Partial results used as fallback if final result is empty
- `TextToSpeech` — Reads questions aloud before user speaks

### Agora RTC (Voice Calls)
- **SDK**: `io.agora.rtc:full-sdk:4.5.0`
- **Token Strategy**: Intent token → Token server fetch → Temp token → Tokenless join
- **Retry Logic**: Max 2 retries on token error (codes 109/110)
- **Audio Config**: `AUDIO_PROFILE_SPEECH_STANDARD`, full duplex, speaker/earpiece toggle

---

## 🗄️ Database Structure

### Firebase Firestore Collections

```
users/{userId}/
├── profile              → name, email, photoUrl
├── gamification/
│   ├── profile          → totalXP, level, coins, xpMultiplier, unlockedBadges
│   ├── chapter_progress → {chapterId: {completedMissions, completedAt}}
│   ├── adaptive         → {category_avg, category_count, category_latest}
│   ├── xp_log/events/   → {xp, coins, source, timestamp}
│   └── missions         → {dayIndex, missionId, completed, assignedAt}
└── progress/
    ├── stats            → {avgScore, totalSessions, advancedCount}
    ├── history/{date}   → {avgScore}
    └── feedback         → {strengths[], weaknesses[]}
```

### SQLite (Local DB)

| Table | Purpose |
|---|---|
| `conversations` | Session logs (userId, timestamp, score) |
| `strengths` | Per-session grammar strength notes |
| `mistakes` | Per-session grammar mistake notes |
| `learning_progress` | Module completion records |

---

## 📁 Project Structure

```
Smart-SpeakUp-Application-main/
├── app/
│   ├── build.gradle                    # App dependencies
│   ├── google-services.json            # Firebase configuration
│   └── src/main/
│       ├── java/com/example/registration_login_module/
│       │   ├── HomeActivity.java
│       │   ├── MissionHubActivity.java
│       │   ├── SpeakMissionActivity.java
│       │   ├── TranslateMissionActivity.java
│       │   ├── PracticeMissionActivity.java
│       │   ├── LearnMissionActivity.java
│       │   ├── AudioCallActivity.java
│       │   ├── GamificationEngine.java
│       │   ├── MissionAIEngine.java
│       │   ├── AdaptiveEngine.java
│       │   ├── GrammarDataProvider.java
│       │   ├── MissionManager.java
│       │   ├── StreakHelper.java
│       │   ├── FirebaseProgressHelper.java
│       │   ├── FirebaseProgressRepository.java
│       │   ├── LearningProgressDBHelper.java
│       │   ├── DBHelper.java
│       │   ├── ProgressDashboardActivity.java
│       │   ├── DictionaryActivity.java
│       │   ├── ProfileActivity.java
│       │   ├── LoginActivity.java
│       │   ├── RegisterActivity.java
│       │   └── ... (50 total Java files)
│       └── res/
│           ├── layout/                 # XML UI layouts
│           ├── drawable/               # Shapes, gradients, icons
│           ├── anim/                   # Entry/pulse/level-up animations
│           ├── values/                 # Colors, strings, themes
│           └── menu/                  # Bottom nav & drawer menus
├── build.gradle                        # Project-level Gradle
├── settings.gradle
└── README.md
```

---

## ⚙️ Setup & Configuration

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 11**
- **Android SDK** API 34 (target), API 24 (minimum)
- A **Firebase project** with Firestore and Authentication enabled
- An active **Groq API** key
- An **Agora** account and App ID

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/Smart-SpeakUp-Application.git
   cd Smart-SpeakUp-Application
   ```

2. **Set up Firebase**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a project, add an Android app with package `com.example.registration_login_module`
   - Download `google-services.json` and place it in `app/`
   - Enable **Email/Password Authentication** and **Cloud Firestore**

3. **Configure API Keys** in `app/src/main/res/values/strings.xml`:
   ```xml
   <!-- Groq AI API Key -->
   <string name="groq_api_key">YOUR_GROQ_API_KEY</string>

   <!-- Agora RTC -->
   <string name="agora_app_id">YOUR_AGORA_APP_ID</string>
   <string name="agora_channel">smarttalk</string>
   <string name="agora_temp_token">YOUR_TEMP_TOKEN_OR_EMPTY</string>
   <string name="agora_token_server_base">YOUR_TOKEN_SERVER_URL_OR_EMPTY</string>
   <string name="agora_token_uid">0</string>
   ```

4. **Sync Gradle** and **Build** the project in Android Studio.

---

## 🔑 API Keys Required

| Service | Where to Get | Usage |
|---|---|---|
| **Firebase** | [console.firebase.google.com](https://console.firebase.google.com) | Auth, Firestore, Analytics |
| **Groq API** | [console.groq.com](https://console.groq.com) | AI answer evaluation & content generation |
| **Agora** | [console.agora.io](https://console.agora.io) | Real-time voice calls |
| **Dictionary API** | [dictionaryapi.dev](https://dictionaryapi.dev) | Free, no key needed |

> ⚠️ **Never commit your `google-services.json` or `strings.xml` with real API keys to a public repository.**

---

## 🏗️ Build Instructions

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

Or use **Android Studio → Build → Make Project / Run**.

---

## 📱 Screens & Navigation

```
SplashScreen
    └─► LoginActivity ◄─► RegisterActivity
                              │
                              ▼
                        HomeActivity  (bottom nav: Learn / Dictionary / Progress / Profile)
                        │   ├── MissionHubActivity
                        │   │   ├── LearnMissionActivity
                        │   │   ├── PracticeMissionActivity
                        │   │   ├── TranslateMissionActivity
                        │   │   └── SpeakMissionActivity
                        │   ├── DictionaryActivity
                        │   ├── AudioCallActivity (Agora voice call)
                        │   ├── LearningActivity (legacy quiz path)
                        │   │   ├── BasicTest / TenseTest / PrepositionTest / ArticleTest
                        │   │   └── ResultActivity
                        │   └── ProgressDashboardActivity
                        │
                        └── Side Drawer
                            ├── ProfileActivity → EditProfileActivity
                            ├── FeedbackActivity
                            └── Logout
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -m "Add my feature"`
4. Push to branch: `git push origin feature/my-feature`
5. Open a Pull Request

### Code Style
- Follow standard Java naming conventions
- Use ViewBinding (enabled) — avoid `findViewById` in new code where possible
- Add `@Override` annotations and Javadoc to all public methods in engine classes
- Keep activities lean — move business logic to Engine/Helper classes

---

## 📄 License

This project is for educational purposes. All third-party SDKs and APIs are subject to their respective licenses:
- [Agora SDK License](https://www.agora.io/en/terms-of-use/)
- [Firebase Terms of Service](https://firebase.google.com/terms)
- [Groq Terms of Service](https://groq.com/terms-of-service/)

---

<p align="center">
  Built with ❤️ to help Hindi-speaking students speak English confidently.
</p>
