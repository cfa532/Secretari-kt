# Changelog

All notable changes to Secretari Android will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-10-11

### Added
- Initial Android release
- Real-time speech recognition with Android SpeechRecognizer
- AI-powered summarization via WebSocket
- Support for 8 languages (English, Japanese, Chinese, Korean, Spanish, Indonesian, Vietnamese, Thai)
- Local database storage with Room
- User authentication (temporary users, registration, login)
- Settings management with DataStore
- Multiple prompt types:
  - Summary: Comprehensive text summary
  - Checklist: Bullet-point extraction
  - Subscription: Clean transcript formatting
- Material Design 3 UI with Jetpack Compose
- Multi-language UI localization
- Encrypted token storage
- Audio silence threshold configuration
- Share recordings functionality
- Account management (view balance, tokens, usage)

### Technical Features
- MVVM architecture
- Kotlin Coroutines and Flow
- Retrofit for REST API
- OkHttp for WebSocket streaming
- Room database for persistence
- EncryptedSharedPreferences for security
- Navigation Compose
- ViewModels for state management

## [Unreleased]

### Planned
- In-app purchases and subscriptions
- Background recording with foreground service
- Export to PDF/DOCX/TXT
- Cloud backup and sync
- Offline mode with local AI
- Translation feature
- Voice activity detection improvement
- Widget support
- Wear OS companion app

### Known Issues
- Speech recognition requires internet connection
- Background recording has limitations
- Some languages may have lower recognition accuracy
- WebSocket reconnection needs improvement

## Version History

### Comparison with iOS Version
The Android version 1.0.0 achieves feature parity with iOS version with the following differences:

| Feature | Status | Notes |
|---------|--------|-------|
| Core recording | ✅ Complete | Using Android SpeechRecognizer |
| AI summarization | ✅ Complete | WebSocket streaming |
| Database | ✅ Complete | Room instead of SwiftData |
| User auth | ✅ Complete | REST API integration |
| Multi-language | ✅ Complete | 8 languages supported |
| IAP | ⏳ Planned | Google Play Billing |
| Notifications | ⏳ Planned | Push notifications |
| Background mode | ⏳ Planned | Foreground service |
| Export | ⏳ Planned | Multiple formats |

---

## Notes

- This changelog follows [Keep a Changelog](https://keepachangelog.com/) principles
- Dates are in YYYY-MM-DD format
- All versions are tagged in git
- Breaking changes are clearly marked

