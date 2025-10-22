# Seretari Android Debug Logging Guide

This guide explains how to monitor your Seretari app logs using logcat to debug issues with anonymous user accounts, login status, and other app functionality.

## Prerequisites

- Android SDK installed (typically at `/Users/[username]/Library/Android/sdk`)
- Android emulator or device connected
- Seretari app built and installed

## 1. Setting Up Environment Variables

First, set up your Android SDK environment:

```bash
export ANDROID_HOME=/Users/[username]/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
```

For your setup:
```bash
export ANDROID_HOME=/Users/cfa532/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
```

## 2. Checking Connected Devices

### List Available Devices
```bash
adb devices
```

You should see output like:
```
List of devices attached
emulator-5554	device
emulator-5556	device
```

### Use Specific Device
If multiple devices are connected, specify which one to use:
```bash
adb -s emulator-5556 [command]
```

## 3. Building and Installing the App

### Build Debug APK
```bash
cd /Users/cfa532/Documents/GitHub/Seretari-kt
./gradlew assembleDebug
```

### Install APK on Device
```bash
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch the App
```bash
adb -s emulator-5556 shell am start -n com.secretari.app/.MainActivity
```

## 4. Monitoring App Logs with Logcat

### Basic Logcat Commands

#### Clear Log Buffer and Monitor
```bash
adb -s emulator-5556 logcat -c
adb -s emulator-5556 logcat -s MainViewModel UserManager
```

#### View Recent Logs
```bash
adb -s emulator-5556 logcat -d | grep -E "MainViewModel|UserManager" | tail -20
```

#### Filter for Specific Tags
```bash
# MainViewModel only
adb -s emulator-5556 logcat -s MainViewModel

# UserManager only  
adb -s emulator-5556 logcat -s UserManager

# Both tags
adb -s emulator-5556 logcat -s MainViewModel UserManager
```

### Logcat Output Formats

#### Thread Time Format (Default)
```bash
adb -s emulator-5556 logcat -v threadtime
```
Output: `10-22 10:11:03.075 27017 27017 D MainViewModel: Setting status to SIGNED_IN (has token)`

#### Brief Format
```bash
adb -s emulator-5556 logcat -v brief
```
Output: `D/MainViewModel(27017): Setting status to SIGNED_IN (has token)`

#### Color-Coded Output
```bash
adb -s emulator-5556 logcat -v color
```

## 5. Understanding Seretari App Logs

### Anonymous User Creation Flow

#### Successful Anonymous User Creation
```
✅ Good flow:
UserManager: Creating temp user with device ID: 49a1cfaa96b9d8a40a015faf8477a78a
UserManager: Temp user created successfully, token: eyJhbGciOi
UserManager: Created user with username: '49a1cfaa96b9d8a40a015faf8477a78a' (length: 32)
MainViewModel: updateLoginStatus - token: eyJhbGciOi, user: 49a1cfaa96b9d8a40a015faf8477a78a
MainViewModel: Setting status to SIGNED_IN (has token)
```

#### Anonymous User Detection
```
✅ Anonymous user detected:
MainViewModel: User username: '49a1cfaa96b9d8a40a015faf8477a78a' (length: 32), isAnonymous: true
MainViewModel: Setting status to SIGNED_IN (anonymous user)
```

#### Registered User Detection
```
✅ Registered user detected:
MainViewModel: User username: 'john_doe' (length: 8), isAnonymous: false
MainViewModel: Setting status to SIGNED_IN (has token)
```

### Login Status Flow

#### Anonymous User (Should show "Account" in dropdown)
```
MainViewModel: updateLoginStatus - token: [token], user: [32+ char device ID]
MainViewModel: User username: '[32+ chars]' (length: 32), isAnonymous: true
MainViewModel: Setting status to SIGNED_IN (anonymous user)
```

#### Registered User with Token
```
MainViewModel: updateLoginStatus - token: [token], user: [username]
MainViewModel: Setting status to SIGNED_IN (has token)
```

#### Registered User without Token (Should show "Login" in dropdown)
```
MainViewModel: updateLoginStatus - token: null, user: [username]
MainViewModel: User username: '[username]' (length: [<20]), isAnonymous: false
MainViewModel: Setting status to SIGNED_OUT (registered user without token)
```

#### No User (Should show "Register" in dropdown)
```
MainViewModel: updateLoginStatus - token: null, user: null
MainViewModel: Setting status to UNREGISTERED (no user)
```

### Common Error Patterns

#### Failed User Creation
```
❌ Bad flow:
UserManager: Creating temp user with device ID: [device ID]
UserManager: Failed to create temp user, response code: [error code]
MainViewModel: Anonymous account initialization result: false
```

#### Empty Username Issue
```
❌ Bad flow:
UserManager: Existing user found: 
MainViewModel: User username: '' (length: 0), isAnonymous: false
MainViewModel: Setting status to SIGNED_OUT (registered user without token)
```

## 6. Advanced Logcat Features

### Save Logs to File
```bash
adb -s emulator-5556 logcat -s "MainViewModel:D UserManager:D" "*:S" > seretari_logs.txt
```

### Clear Log Buffer
```bash
adb -s emulator-5556 logcat -c
```

### Monitor Logs in Real-time
```bash
adb -s emulator-5556 logcat -s MainViewModel UserManager | grep -E "(SIGNED_IN|SIGNED_OUT|UNREGISTERED)"
```

### View All App Logs
```bash
adb -s emulator-5556 logcat | grep "com.secretari.app"
```

## 7. Troubleshooting Common Issues

### App Not Installing
- Check if app is already installed: `adb -s emulator-5556 shell pm list packages | grep com.secretari.app`
- Uninstall previous version: `adb -s emulator-5556 uninstall com.secretari.app`
- Rebuild the APK: `./gradlew assembleDebug`

### No App Logs Appearing
- Verify app is running: `adb -s emulator-5556 shell dumpsys activity activities | grep com.secretari.app`
- Check if app has logging enabled in code
- Try broader filters: `adb -s emulator-5556 logcat "*:V"`

### Anonymous User Issues
- Clear app data: `adb -s emulator-5556 shell pm clear com.secretari.app`
- Reinstall app and check logs
- Verify device ID generation: Look for "Creating temp user with device ID" logs

### Login Status Issues
- Check username length: Should be 32+ chars for anonymous users
- Verify token presence: Anonymous users should have tokens
- Look for "isAnonymous: true/false" in logs

## 8. Quick Reference Commands

```bash
# Complete debugging sequence
export ANDROID_HOME=/Users/cfa532/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator
adb devices
cd /Users/cfa532/Documents/GitHub/Seretari-kt
./gradlew assembleDebug
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -n com.secretari.app/.MainActivity
adb -s emulator-5556 logcat -s MainViewModel UserManager
```

## 9. Integration with Android Studio

If using Android Studio:
- Open **Logcat** window (View → Tool Windows → Logcat)
- Select your device and app package: `com.secretari.app`
- Use built-in filters for easier log monitoring
- Set up custom logcat filters for MainViewModel and UserManager tags

## 10. Key Debug Information

### What to Look For in Logs:

1. **Anonymous User Creation**: Device ID length (should be 32+ chars)
2. **Username Length**: Should match device ID for anonymous users
3. **Anonymous Detection**: `isAnonymous: true` for device ID usernames
4. **Login Status**: `SIGNED_IN` for anonymous users with tokens
5. **Dropdown Menu Behavior**: Based on login status

### Expected Behavior:
- **Anonymous users**: Show "Account" in dropdown menu
- **Registered users with token**: Show "Account" in dropdown menu  
- **Registered users without token**: Show "Login" in dropdown menu
- **No users**: Show "Register" in dropdown menu

This guide provides a complete workflow for debugging your Seretari app with comprehensive log monitoring capabilities.