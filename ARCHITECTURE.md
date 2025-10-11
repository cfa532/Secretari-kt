# Secretari Android - Architecture Documentation

## Overview

Secretari Android follows the MVVM (Model-View-ViewModel) architecture pattern with a clean separation of concerns. The app is built using modern Android development practices with Jetpack Compose, Room, and Kotlin Coroutines.

## Architecture Layers

### 1. Presentation Layer (UI)
**Location**: `app/src/main/java/com/secretari/app/ui/`

#### Components:
- **Screens**: Composable functions for each screen
  - `MainScreen`: List of audio recordings
  - `DetailScreen`: Recording/streaming/summary view
  - `SettingsScreen`: App configuration
  - `AccountScreen`: User authentication and profile
  
- **ViewModels**: State management and business logic
  - `MainViewModel`: Manages app state, coordinates between UI and data layers

- **Navigation**: Screen routing
  - `NavGraph`: Defines navigation structure using Compose Navigation

- **Theme**: Material Design 3 theming
  - `Theme.kt`: Color schemes and typography

### 2. Domain Layer
**Location**: `app/src/main/java/com/secretari/app/data/`

#### Models:
- `User`: User account data
- `AudioRecord`: Recording with transcript and summary
- `Settings`: App settings and preferences
- `RecognizerLocale`: Supported languages enum
- `PromptType`: AI summarization types

#### Business Logic:
Encapsulated in ViewModels and Repository classes

### 3. Data Layer

#### Database (Room)
**Location**: `app/src/main/java/com/secretari/app/data/database/`

- `AppDatabase`: Room database configuration
- `AudioRecordDao`: Data access object for recordings
- `Converters`: Type converters for complex types

#### Network
**Location**: `app/src/main/java/com/secretari/app/data/network/`

- `ApiService`: Retrofit interface for REST API
- `WebSocketClient`: OkHttp WebSocket for AI streaming
- Request/Response models

#### Repository
**Location**: `app/src/main/java/com/secretari/app/data/repository/`

- `AudioRecordRepository`: Abstraction over database operations

### 4. Service Layer
**Location**: `app/src/main/java/com/secretari/app/service/`

- `SpeechRecognitionService`: Android SpeechRecognizer wrapper with Flow API

### 5. Utility Layer
**Location**: `app/src/main/java/com/secretari/app/util/`

- `SettingsManager`: Persistent settings using DataStore
- `UserManager`: User authentication and profile management with encrypted storage

## Data Flow

### Recording Flow
```
User taps Start
    ↓
MainViewModel.startRecording()
    ↓
SpeechRecognitionService.startRecognition()
    ↓
Flow<RecognitionResult> → transcript updates
    ↓
DetailScreen displays real-time transcript
    ↓
User taps Stop
    ↓
MainViewModel.stopRecording() → sendToAI()
    ↓
WebSocketClient connects and sends transcript
    ↓
Flow<WebSocketMessage> → streamed text updates
    ↓
Final result → AudioRecord created
    ↓
AudioRecordRepository.insert()
    ↓
Room Database persists record
```

### Authentication Flow
```
App Launch
    ↓
UserManager.init()
    ↓
Check DataStore for user
    ↓
If no user: createTempUser()
    ↓
API call to /secretari/users/temp
    ↓
Store encrypted token in SharedPreferences
    ↓
Store user in DataStore
```

## State Management

### Reactive State with Flows
All state is exposed as Kotlin `Flow` and `StateFlow`:

```kotlin
// ViewModel
val records: Flow<List<AudioRecord>> = repository.allRecords
val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

// Composable
val records by viewModel.records.collectAsState(initial = emptyList())
val isRecording by viewModel.isRecording.collectAsState()
```

### State Hoisting
State is hoisted to the appropriate level:
- Screen-level state: In ViewModels
- Component state: In parent Composables
- Local UI state: In individual Composables using `remember`

## Threading Model

### Coroutines and Dispatchers
- **Main**: UI updates, Compose recomposition
- **IO**: Database operations, network calls
- **Default**: Heavy computations (JSON parsing)

```kotlin
viewModelScope.launch {
    // Runs on Main by default
    val settings = withContext(Dispatchers.IO) {
        settingsManager.getSettings()
    }
    // Back to Main
    updateUI(settings)
}
```

### Flow-based Asynchrony
- Database queries return `Flow` for reactive updates
- Network operations use callbacks converted to Flows
- Speech recognition uses `callbackFlow`

## Dependency Injection

Currently using manual DI with singleton managers:
- `SettingsManager.getInstance(context)`
- `UserManager.getInstance(context)`
- `AppDatabase.getDatabase(context)`

**Future Enhancement**: Migrate to Hilt or Koin for proper DI

## Security

### Data Protection
1. **Encrypted SharedPreferences**: Sensitive data (tokens, device ID)
   - Uses Android's `EncryptedSharedPreferences`
   - Master key managed by Android Keystore

2. **DataStore**: User preferences (non-sensitive)
   - Plain text, but app-private

3. **Room Database**: Local recordings
   - App-private storage
   - Encrypted at device level (FDE/FBE)

### Network Security
- HTTPS for REST API
- WSS for WebSocket
- Bearer token authentication
- Certificate pinning (recommended for production)

## Error Handling

### Network Errors
```kotlin
try {
    val response = apiService.login(username, password)
    if (response.isSuccessful) {
        // Handle success
    } else {
        // Handle HTTP error
    }
} catch (e: Exception) {
    // Handle network/parsing error
}
```

### Speech Recognition Errors
```kotlin
sealed class RecognitionResult {
    data class Success(val text: String) : RecognitionResult()
    data class Error(val code: Int, val message: String) : RecognitionResult()
    object Ready : RecognitionResult()
}
```

### UI Error Display
Errors are exposed as StateFlow and displayed with Snackbar/AlertDialog

## Performance Optimizations

### Database
- Limit stored records to 30 (configurable)
- Automatic cleanup of old records
- Indexed queries by date

### UI
- LazyColumn for efficient list rendering
- State hoisting to minimize recomposition
- Stable keys for list items

### Memory
- Speech recognizer properly destroyed when not in use
- WebSocket connections closed after streaming
- Flow collection scoped to lifecycle

## Testing Strategy

### Unit Tests
- ViewModels: Test state transitions
- Repositories: Test data operations
- Utilities: Test business logic

### Integration Tests
- Database: Test DAO operations
- Network: Mock API responses

### UI Tests (Future)
- Compose UI testing
- Navigation testing
- End-to-end user flows

## Build Variants

### Debug
- Logging enabled
- Network interceptor with full logging
- Debug database inspector

### Release
- ProGuard/R8 enabled
- Optimized and minified
- No logging

## Future Enhancements

### Planned Features
1. **In-app Purchases**: Subscriptions and one-time purchases
2. **Background Recording**: Service with foreground notification
3. **Export**: PDF, DOCX, TXT formats
4. **Sync**: Cloud backup and multi-device sync
5. **Offline Mode**: Local AI models for basic summarization

### Technical Improvements
1. **Dependency Injection**: Migrate to Hilt
2. **Testing**: Comprehensive test coverage
3. **Modularization**: Feature modules
4. **CI/CD**: Automated builds and testing
5. **Analytics**: Crash reporting and usage analytics

## Dependencies Graph

```
MainActivity
    ↓
NavGraph
    ↓
Screens ←→ MainViewModel
              ↓
         ┌────┴────┬──────────┬──────────────┐
         ↓         ↓          ↓              ↓
    Repository  Settings  UserManager  WebSocket
         ↓      Manager                   Client
    AudioRecordDao
         ↓
    AppDatabase
```

## Configuration

### Build Configuration
- `build.gradle.kts`: Dependencies and build settings
- `gradle.properties`: Gradle JVM settings
- `proguard-rules.pro`: Code obfuscation rules

### Resources
- `strings.xml`: Localized strings
- `themes.xml`: Material theme configuration
- `AndroidManifest.xml`: App permissions and components

## Version Control

### Branching Strategy
- `main`: Stable releases
- `develop`: Integration branch
- `feature/*`: New features
- `bugfix/*`: Bug fixes

### Commit Messages
Follow conventional commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `refactor:` Code refactoring
- `test:` Tests
- `chore:` Maintenance

## Deployment

### Release Process
1. Update version in `build.gradle.kts`
2. Update CHANGELOG.md
3. Create release tag
4. Build release APK/AAB
5. Test on physical devices
6. Submit to Google Play Console

### Versioning
Following semantic versioning: MAJOR.MINOR.PATCH
- MAJOR: Breaking changes
- MINOR: New features
- PATCH: Bug fixes

