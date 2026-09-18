# Android Virtual Camera Studio

A production-ready, fully functional Android application that combines real-time physical camera hardware integration with a hardware-accelerated virtual video feed playback engine and system camera intent integration.

---

## 📌 Android Architecture & System-Wide Camera Feeds

### The Reality of "Virtual Cameras" on Unrooted Android

On desktop operating systems (e.g., Windows or macOS with OBS Studio), software can install a virtual device driver at the kernel/DirectShow/CoreMedia level that appears to every application as a standard webcam.

On standard, unrooted Android:
1. **Camera HAL Isolation**: Camera hardware is mediated exclusively by Android's **Hardware Abstraction Layer (HAL)** and the system `cameraserver` process.
2. **SELinux & Process Sandboxing**: Third-party applications (such as WhatsApp, Instagram, Google Meet, Zoom, Telegram) connect directly to `CameraManager` and `cameraserver` via Binder IPC. SELinux policy prevents regular third-party applications from registering camera devices or intercepting another application's camera session.
3. **No Public System Virtual Camera API**: Standard Android does **not** provide a public API for user-space applications to create a device-wide `/dev/video*` node or inject frames into other applications' private camera streams. Such capability on Android strictly requires:
   - Device **Root** access with a kernel module (`v4l2loopback`), or
   - **Xposed/LSPosed** hooking into `cameraserver` / app ART runtimes, or
   - A custom AOSP ROM / OEM system signature.

### The Legitimate Android-Supported Solution

This project implements the closest, most complete technically possible solution supported by official Android APIs:

1. **Dual-Mode Studio Pipeline**: Seamlessly switch between physical camera hardware (via Jetpack CameraX) and a custom virtual video stream.
2. **Smooth Hardware-Accelerated Video Engine**: Ingest any local video file (MP4, MKV, WebM) via the system file picker and render it smoothly with full transport controls (Play, Pause, Stop, Seek, Seamless Loop, Mute/Unmute, Playback Speed, and Fit/Crop Aspect Ratio).
3. **System Camera Intent Provider (`CameraCaptureActivity`)**: When external Android applications (such as browsers, messaging apps, and form uploaders) trigger `MediaStore.ACTION_IMAGE_CAPTURE`, `ACTION_IMAGE_CAPTURE_SECURE`, or `ACTION_VIDEO_CAPTURE`, this application responds as an authorized camera provider, allowing the user to supply snapshot frames or video clips directly from their virtual video feed.

---

## ✨ Features

- **Physical Camera Preview**:
  - Powered by AndroidX **CameraX 1.5.0**.
  - Dynamic camera lens toggle (Front facing / Back facing).
  - Lifecycle-aware binding (`bindToLifecycle`) that automatically halts sensor capture when backgrounded.
- **Virtual Video Feed**:
  - System Photo/Video picker (`ActivityResultContracts.PickVisualMedia` with fallback to `GetContent`).
  - Asynchronous metadata extraction (resolution, duration, file size, display name) on `Dispatchers.IO` to ensure the main UI thread never stutters.
  - Hardware-accelerated `MediaPlayer` connected to `TextureView` with dynamic matrix transformations.
- **Comprehensive Transport Controls**:
  - **Play / Pause** toggle.
  - **Stop** button (rewinds and resets position).
  - **Interactive Scrubbing Slider**: Smooth seek bar with current position and total duration in `mm:ss`.
  - **Seamless Loop** toggle with active indicator.
  - **Mute / Unmute** toggle with zero-audio gain mode.
  - **Playback Speed**: Cycle between 0.5x, 1.0x, 1.5x, and 2.0x speeds.
  - **Aspect Ratio Control**: Switch between **Fit (Letterbox)** and **Fill / Crop** modes.
- **Robust Error & Lifecycle Handling**:
  - Gracefully handles permission denials without crashing.
  - Validates selected files and safely surfaces errors via Material 3 Snackbars.
  - Automatically pauses playback when screen turns off or app is sent to background.
  - Fully releases `MediaPlayer`, `TextureView`, and `ProcessCameraProvider` resources.

---

## 🛠️ Tech Stack & Requirements

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow
- **Camera**: AndroidX CameraX (Core, Camera2, Lifecycle, View 1.5.0)
- **Minimum SDK**: API 24 (Android 7.0 Nougat)
- **Target / Compile SDK**: API 36 (Android 16)
- **JDK**: Java 17

---

## 📂 Project Structure

```
├── .github/workflows/
│   └── build.yml               # GitHub Actions APK build & test workflow
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                  # Main Studio UI & lifecycle
│   │   │   │   ├── CameraCaptureActivity.kt         # System Camera Intent responder
│   │   │   │   ├── model/
│   │   │   │   │   └── VirtualCameraModel.kt        # State models & data classes
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── VirtualCameraViewModel.kt    # StateFlow & business logic
│   │   │   │   └── ui/
│   │   │   │       ├── components/
│   │   │   │       │   ├── PhysicalCameraPreview.kt # CameraX PreviewView integration
│   │   │   │       │   ├── VirtualVideoPreview.kt   # Hardware MediaPlayer & TextureView
│   │   │   │       │   ├── PlaybackControlsBar.kt   # Transport controls & seekbar
│   │   │   │       │   ├── StudioTopBar.kt          # Mode switch & status indicators
│   │   │   │       │   └── SystemArchitectureDialog.kt # Technical limitation explainer
│   │   │   │       └── theme/                       # M3 Material Theme & Color
│   │   │   ├── res/                                 # Drawables, mipmaps, strings, XML
│   │   │   └── AndroidManifest.xml                  # Permissions & activity declarations
│   │   └── test/                                    # Unit & Robolectric test suites
│   ├── build.gradle.kts                             # Module build configuration
│   └── proguard-rules.pro                           # R8 / ProGuard optimization rules
├── gradle/libs.versions.toml                        # Gradle version catalog
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Building & Testing

### Command Line

```bash
# Build the debug APK
./gradlew assembleDebug

# Run unit and Robolectric tests
./gradlew testDebugUnitTest
```

### Android Studio

1. Open Android Studio (Ladybug or newer).
2. Select **Open** and point to the project root directory.
3. Allow Gradle to sync dependencies.
4. Select the `app` configuration and click **Run** (or press Shift+F10).

---

## 🔐 Permissions Declared

| Permission | Protection Level | Purpose |
|------------|------------------|---------|
| `android.permission.CAMERA` | Runtime Dangerous | Streaming live video from the device's physical lens. |
| `android.permission.READ_EXTERNAL_STORAGE` | Normal / Runtime (API <= 32) | Accessing video files on older Android versions. |
| *Zero-Permission Photo Picker* | System Contract | API 33+ securely loads videos without requiring broad storage permission. |

---

## 📄 License

This project is licensed under the Apache License 2.0.
