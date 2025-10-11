# Secretari - Android

An Android voice recording and AI-powered transcription and summarization app, ported from the iOS version.

## Overview

Secretari is a powerful speech-to-text application that allows users to:
- Record audio using Android's built-in speech recognition
- Get real-time transcription of spoken words
- Generate AI-powered summaries of recordings
- Support multiple languages (English, Japanese, Chinese, Korean, Spanish, Indonesian, Vietnamese, Thai)
- Store recordings locally with Room database
- Sync with cloud backend for AI processing

## Features

### Core Features
- **Speech Recognition**: Real-time speech-to-text using Android's SpeechRecognizer API
- **AI Summarization**: Connect to AI backend via WebSocket for intelligent text summarization
- **Multiple Summary Types**:
  - Summary: Comprehensive text summary
  - Checklist: Extract key points as a checklist
  - Subscription: Clean transcription with formatting
- **Multi-language Support**: 8+ languages for recognition and summarization
- **Local Storage**: All recordings stored locally using Room database
- **User Management**: Registration, login, and account management
- **Settings**: Customizable recognition language, prompt types, and audio thresholds

### Technical Features
- Built with Kotlin and Jetpack Compose
- MVVM architecture with ViewModels and Repositories
- Room Database for local data persistence
- Retrofit for HTTP API calls
- OkHttp WebSocket for real-time AI streaming
- DataStore for preferences
- Encrypted SharedPreferences for sensitive data
- Material Design 3 UI

## Architecture

```
app/
├── data/
│   ├── model/          # Data models (User, AudioRecord, Settings)
│   ├── database/       # Room database and DAOs
│   ├── network/        # API service and WebSocket client
│   └── repository/     # Data repositories
├── service/            # Speech recognition service
├── ui/
│   ├── screens/        # Composable screens
│   ├── components/     # Reusable UI components
│   ├── viewmodel/      # ViewModels
│   ├── navigation/     # Navigation graph
│   └── theme/          # Material theme
└── util/               # Utility classes (SettingsManager, UserManager)
```

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 26+ (Android 8.0+)
- Kotlin 1.9.20+

### Building the Project

1. Clone the repository:
```bash
git clone https://github.com/yourusername/Seretari-kt.git
cd Seretari-kt
```

2. Open the project in Android Studio

3. Sync Gradle dependencies

4. Run the app on an emulator or physical device

### Backend Configuration

The app connects to the backend server at `https://secretari.leither.uk`. To use your own backend:

1. Update the base URLs in:
   - `WebSocketClient.kt` - WebSocket URL
   - `ApiService.kt` - REST API URL

## Usage

### First Launch
1. Grant microphone permission when prompted
2. Select your preferred recognition language in Settings
3. Tap the "Start" button to begin recording
4. Speak clearly into the device
5. Tap "Stop" when finished
6. The app will automatically send the transcript to AI for summarization

### Recording
- Tap **Start** to begin recording
- Real-time transcription appears on screen
- Change language mid-recording if needed
- Tap **Stop** to finish and generate summary

### Managing Recordings
- All recordings appear in the main list
- Tap a record to view details
- Swipe to delete unwanted recordings
- Share recordings via the share button

### Settings
- **Language**: Select recognition language
- **Prompt Type**: Choose summary style
- **Audio Threshold**: Adjust silence detection sensitivity

### Account
- **Unregistered**: App creates temporary account with trial balance
- **Register**: Create permanent account
- **Login**: Access existing account
- View account balance and usage statistics

## Dependencies

### Core
- androidx.core:core-ktx:1.12.0
- androidx.lifecycle:lifecycle-runtime-ktx:2.6.2
- androidx.activity:activity-compose:1.8.1

### Compose
- androidx.compose.ui:ui
- androidx.compose.material3:material3
- androidx.navigation:navigation-compose:2.7.5

### Database
- androidx.room:room-runtime:2.6.1
- androidx.room:room-ktx:2.6.1

### Network
- com.squareup.okhttp3:okhttp:4.12.0
- com.squareup.retrofit2:retrofit:2.9.0
- org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0

### Security
- androidx.security:security-crypto:1.1.0-alpha06

## Localization

The app supports the following languages:
- English (en)
- Spanish (es)
- Japanese (ja)
- Chinese (zh)
- Korean (ko)

String resources are located in `app/src/main/res/values-{locale}/strings.xml`

## API Integration

### Authentication
All API calls require authentication token obtained from login/registration.

### WebSocket Protocol
The AI backend uses WebSocket for streaming responses:

```json
// Send message
{
  "input": {
    "prompt": "...",
    "prompt_type": "summary",
    "rawtext": "...",
    "subscription": false
  },
  "parameters": {
    "llm": "openai",
    "temperature": "0.0"
  }
}

// Receive stream
{"type": "stream", "data": "..."}

// Receive result
{"type": "result", "answer": "...", "cost": 0.01, "tokens": 100, "eof": true}

// Receive error
{"type": "error", "message": "..."}
```

## Permissions

The app requires the following permissions:
- `RECORD_AUDIO` - For speech recognition
- `INTERNET` - For API communication
- `ACCESS_NETWORK_STATE` - For network status

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Known Limitations

- Speech recognition requires active internet connection
- Background recording support is limited
- Some advanced iOS features not yet implemented:
  - In-app purchases/subscriptions
  - Push notifications
  - Advanced audio level monitoring

## Differences from iOS Version

This Android version maintains feature parity with the iOS app with some platform-specific differences:

| Feature | iOS | Android |
|---------|-----|---------|
| Speech Recognition | Apple Speech Framework | Android SpeechRecognizer |
| Database | SwiftData | Room |
| UI Framework | SwiftUI | Jetpack Compose |
| Data Storage | UserDefaults + Keychain | DataStore + EncryptedSharedPreferences |
| Networking | URLSession | Retrofit + OkHttp |

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Original iOS version by 超方
- AI backend powered by OpenAI/Gemini APIs
- Material Design by Google

## Support

For issues and questions:
- Open an issue on GitHub
- Contact: [your-email@example.com]

## Version History

### 1.0.0 (Current)
- Initial Android release
- Core features: recording, transcription, summarization
- Multi-language support
- User authentication
- Local database storage
- WebSocket AI integration

