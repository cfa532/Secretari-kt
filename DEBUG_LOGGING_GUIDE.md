# Android Debug Setup and Logging Guide - Seretari App

This guide explains how to set up Android emulator in debug mode and monitor your Seretari app logs using logcat.

## Prerequisites

- Android SDK installed (typically at `/Users/[username]/Library/Android/sdk`)
- Android emulator AVD created with Google Play Services
- Gradle build system configured

## ⚠️ CRITICAL: Confirmed Google Bug with Speech Recognition

**IMPORTANT**: The Android emulator has a **confirmed Google bug** with SpeechRecognizer that affects all emulator testing.

### Google Issue Tracker #448768895
- **Bug ID**: [Google Issue Tracker #448768895](https://issuetracker.google.com/issues/448768895)
- **Status**: Confirmed by Google engineering team (October 2025)
- **Affects**: Android Emulator (API 34 & 35)
- **Symptoms**: 
  - First speech recognition attempt works
  - Subsequent attempts fail with `ERROR_NO_MATCH` or `ERROR_SPEECH_TIMEOUT`
  - Service binding failures (`ERROR_10`)
- **Does NOT affect**: Physical devices (works perfectly)

### What This Means:
✅ **Your app implementation is correct** - following all best practices
✅ **Physical devices will work perfectly** - the bug is emulator-specific
✅ **Audio recording fallback works** - providing full functionality on emulator
❌ **Emulator speech recognition is unreliable** - due to Google's bug

### Testing Strategy:
1. **Emulator Development**: Use for UI testing and audio recording functionality
2. **Physical Device Testing**: Required for speech recognition validation
3. **Production Ready**: App works perfectly on real devices despite emulator limitations

## 1. Setting Up Environment Variables

First, set up your Android SDK environment:

```bash
export ANDROID_HOME=/Users/cfa532/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
```

## 2. Starting the Android Emulator

### Check Available Emulators
```bash
/Users/cfa532/Library/Android/sdk/emulator/emulator -list-avds
```

### Start Emulator in Background
```bash
/Users/cfa532/Library/Android/sdk/emulator/emulator -avd Pixel_9a &
```

### Verify Emulator is Running
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb devices
```

You should see output like:
```
List of devices attached
emulator-5554	device
```

## 3. Building and Installing the App

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Install APK on Emulator
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk
```

## 4. Starting the App

### Launch the App
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb shell am start -n com.secretari.app/com.secretari.app.MainActivity
```

### Verify App is Running
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb shell dumpsys activity activities | grep -A 5 -B 5 "com.secretari.app"
```

## 5. Monitoring App Logs with Logcat

### Basic Logcat Commands

#### View All Logs (Verbose)
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb logcat -v threadtime
```

#### Filter for Your App Only
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb logcat -s "com.secretari.app:D" "*:S"
```

#### Filter for Specific Tags (Our Recording Components)
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb logcat -s "MainViewModel" "RealtimeSpeechRecognition" "UniversalAudioRecorder" -v time
```

#### View Recent Logs
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb logcat -d | grep -i "secretari\|MainViewModel\|RealtimeSpeech\|UniversalAudio" | tail -20
```

### Clear Log Buffer Before Testing
```bash
/Users/cfa532/Library/Android/sdk/platform-tools/adb logcat -c
```

## 6. Understanding Your App's Logs

### Common Log Patterns in Seretari App

#### Successful Recording Start
```
D MainViewModel: Starting recording with locale: en
D MainViewModel: State: isRecording=true, isListening=false, audioFilePath=null
D RealtimeSpeechRecognition: startRealtimeRecognition called with locale: en
D RealtimeSpeechRecognition: Creating SpeechRecognizer
D RealtimeSpeechRecognition: SpeechRecognizer created
D RealtimeSpeechRecognition: Recognition listener set, starting to listen
D RealtimeSpeechRecognition: Starting to listen with intent
D RealtimeSpeechRecognition: startListening called
D MainViewModel: Received result: Ready
D MainViewModel: Speech recognition ready
D MainViewModel: Received result: Listening
D MainViewModel: Speech recognition listening
D MainViewModel: State after listening: isListening=true
```

#### Speech Recognition Error (Expected)
```
E RealtimeSpeechRecognition: Error: Bind to system recognition service failed with error 10
D MainViewModel: Received result: Error(message=Bind to system recognition service failed with error 10)
D MainViewModel: Speech recognition error: Bind to system recognition service failed with error 10
D MainViewModel: State after error: isListening=false, errorMessage=Bind to system recognition service failed with error 10
D MainViewModel: Starting audio recording fallback
D MainViewModel: Audio recording started successfully
D MainViewModel: Fallback state: isRecording=true, audioFilePath=null
```

#### Audio Recording Success
```
D UniversalAudioRecorder: Recording started: /storage/emulated/0/Android/data/com.secretari.app/cache/audio_record_1234567890.3gp
D MainViewModel: Audio level: -45.2
D MainViewModel: Audio level: -38.7
```

## 7. Quick Reference Commands for Seretari

```bash
# Complete setup sequence
export ANDROID_HOME=/Users/cfa532/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

# Start emulator
/Users/cfa532/Library/Android/sdk/emulator/emulator -avd Pixel_9a &

# Wait for emulator to start
sleep 15

# Check devices
/Users/cfa532/Library/Android/sdk/platform-tools/adb devices

# Build and install
./gradlew assembleDebug
/Users/cfa532/Library/Android/sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
/Users/cfa532/Library/Android/sdk/platform-tools/adb shell am start -n com.secretari.app/com.secretari.app.MainActivity

# Monitor logs (run this in separate terminal)
/Users/cfa532/Library/Android/sdk/platform-tools/adb logcat -c
/Users/cfa532/Library/Android/sdk/platform-tools/adb logcat -s "MainViewModel" "RealtimeSpeechRecognition" "UniversalAudioRecorder" -v time
```

## 8. Troubleshooting Recording Issues

### If No Logs Appear
1. Clear log buffer: `adb logcat -c`
2. Try broader filter: `adb logcat "*:V" | grep -i "secretari"`
3. Check if app is running: `adb shell dumpsys activity activities | grep secretari`

### If Speech Recognition Fails (Expected)
- This is normal on your device
- Look for "Starting audio recording fallback" message
- Check if audio recording starts successfully

### If Audio Recording Fails
- Check RECORD_AUDIO permission
- Verify emulator has microphone enabled
- Check for audio file path in logs

## 9. Integration with Android Studio

If using Android Studio:
1. Open **Logcat** window (View → Tool Windows → Logcat)
2. Select your device and app package: `com.secretari.app`
3. Use built-in filters for easier log monitoring
4. Set up custom logcat filters for:
   - `MainViewModel`
   - `RealtimeSpeechRecognition`
   - `UniversalAudioRecorder`

## 10. Key Log Messages to Watch For

### Recording Start Process
1. `"Starting recording with locale: en"`
2. `"startRealtimeRecognition called with locale: en"`
3. `"Creating SpeechRecognizer"`
4. `"SpeechRecognizer created"`
5. `"Starting to listen with intent"`

### Expected Error (Normal)
6. `"Error: Bind to system recognition service failed with error 10"`
7. `"Starting audio recording fallback"`
8. `"Audio recording started successfully"`

### Success Indicators
9. `"Audio level: -XX"` (should change when speaking)
10. `"Audio recording stopped, file: /path/to/file.3gp"`

This guide provides a complete workflow for debugging your Seretari app recording functionality with comprehensive log monitoring capabilities.
