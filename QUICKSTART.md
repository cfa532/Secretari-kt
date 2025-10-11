# Quick Start Guide - Secretari Android

Get up and running with Secretari Android in minutes!

## Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17 or later
- **Android SDK**: API level 26+ (Android 8.0+)
- **Device/Emulator**: Android 8.0+ with microphone

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/Seretari-kt.git
cd Seretari-kt
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned directory
4. Click "OK"

### 3. Sync Project

Android Studio will automatically:
- Download Gradle wrapper
- Sync Gradle dependencies
- Index project files

**If sync fails:**
```bash
./gradlew --refresh-dependencies
```

### 4. Configure SDK

If prompted, install:
- Android SDK 34
- Build Tools 34.0.0
- Android Emulator (if testing on emulator)

## Running the App

### On Physical Device

1. Enable Developer Options on your Android device:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times
   
2. Enable USB Debugging:
   - Go to Settings > Developer Options
   - Enable "USB Debugging"

3. Connect device via USB

4. Click "Run" (▶️) in Android Studio

5. Select your device from the list

### On Emulator

1. Create AVD (Android Virtual Device):
   - Tools > Device Manager
   - Click "Create Device"
   - Select device (e.g., Pixel 5)
   - Select system image (API 34 recommended)
   - Click "Finish"

2. Click "Run" (▶️) in Android Studio

3. Select emulator from the list

## First Run

### Grant Permissions

When the app launches:
1. Tap "Allow" when prompted for microphone permission
2. This is required for speech recognition

### Setup

1. **Language Selection**:
   - Tap menu (⋮) > Settings
   - Select your preferred recognition language
   - Tap back

2. **Test Recording**:
   - Tap the red "Start" button
   - Speak clearly: "This is a test recording"
   - Tap "Stop"
   - Wait for AI summarization

### Account Setup (Optional)

The app automatically creates a temporary account with trial balance.

**To register a permanent account:**
1. Tap menu (⋮) > Register
2. Fill in username, password, and email
3. Tap "Register"

**To login with existing account:**
1. Tap menu (⋮) > Login
2. Enter credentials
3. Tap "Login"

## Project Structure Overview

```
Seretari-kt/
├── app/
│   ├── src/main/
│   │   ├── java/com/secretari/app/
│   │   │   ├── data/           # Models, database, network
│   │   │   ├── ui/             # Screens, ViewModels, theme
│   │   │   ├── service/        # Speech recognition
│   │   │   ├── util/           # Managers and utilities
│   │   │   ├── MainActivity.kt
│   │   │   └── SecretariApplication.kt
│   │   ├── res/                # Resources (strings, layouts)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts        # App-level Gradle config
├── build.gradle.kts            # Project-level Gradle config
├── settings.gradle.kts
└── README.md
```

## Key Files to Know

### Configuration
- `app/build.gradle.kts` - Dependencies and build settings
- `app/src/main/AndroidManifest.xml` - Permissions and app components

### Entry Points
- `MainActivity.kt` - App entry point
- `ui/navigation/NavGraph.kt` - Screen navigation

### State Management
- `ui/viewmodel/MainViewModel.kt` - App state and business logic

### Data
- `data/database/AppDatabase.kt` - Room database setup
- `data/network/ApiService.kt` - REST API
- `data/network/WebSocketClient.kt` - AI streaming

## Development Workflow

### Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Cleaning

```bash
# Clean build artifacts
./gradlew clean
```

## Common Issues

### Issue: Gradle Sync Failed

**Solution:**
```bash
./gradlew --refresh-dependencies
# Or: File > Invalidate Caches / Restart
```

### Issue: "SDK location not found"

**Solution:**
Create `local.properties`:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### Issue: Speech recognition not working

**Possible causes:**
1. Microphone permission not granted
2. Device has no speech recognition service
3. No internet connection (Google Speech API requires network)

**Solutions:**
- Check app permissions in device settings
- Install Google app on device
- Verify internet connection

### Issue: App crashes on startup

**Check:**
1. Logcat for error messages
2. All dependencies downloaded
3. Correct SDK version installed

**Debug:**
```bash
adb logcat | grep SecretariApplication
```

### Issue: WebSocket connection fails

**Possible causes:**
1. Backend server unavailable
2. Network firewall blocking WSS
3. Invalid authentication token

**Solutions:**
- Verify backend URL in `WebSocketClient.kt`
- Test with curl: `curl https://secretari.leither.uk/secretari/notice`
- Check authentication token in encrypted preferences

## Development Tips

### Hot Reload (Compose)

Jetpack Compose supports hot reload:
1. Make UI changes in Composable functions
2. No need to rebuild - changes apply instantly

### Debugging

**Set breakpoints:**
- Click left margin in code editor
- Run with debugger (🐛)

**View database:**
- Tools > Device Explorer
- Navigate to: `/data/data/com.secretari.app/databases/`

**Network traffic:**
- Check Logcat for HTTP/WebSocket logs
- Use Charles Proxy or Fiddler for detailed inspection

### Code Style

The project follows Kotlin official style guide:
```bash
# Format code
./gradlew ktlintFormat
```

## Next Steps

### Learn More

1. **Architecture**: Read `ARCHITECTURE.md` for detailed design
2. **API**: Check `ApiService.kt` for backend endpoints
3. **UI**: Explore `ui/screens/` for Compose examples

### Contribute

1. Fork the repository
2. Create feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -am 'Add my feature'`
4. Push branch: `git push origin feature/my-feature`
5. Open Pull Request

### Customize

**Change app name:**
- Edit `app/src/main/res/values/strings.xml`
- Update `<string name="app_name">`

**Change backend URL:**
- Edit `WebSocketClient.kt` - `baseUrl`
- Edit `ApiService.kt` - `BASE_URL`

**Add new language:**
1. Create `values-{locale}/strings.xml`
2. Add locale to `RecognizerLocale.kt`
3. Add translations to all string resources

## Resources

- [Android Developer Guide](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Material Design 3](https://m3.material.io/)

## Getting Help

- **Issues**: Open an issue on GitHub
- **Discussions**: GitHub Discussions
- **Stack Overflow**: Tag `secretari-android`

## License

MIT License - See LICENSE file for details

---

**Happy Coding! 🚀**

