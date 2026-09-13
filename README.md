# BlazeIn

A modern Android app for browsing, streaming, and downloading videos from Telegram channels — built with Jetpack Compose, TDLib, and ExoPlayer.

BlazeIn connects directly to Telegram's MTProto network via TDLib, letting you discover video content across your subscribed channels, groups, and private chats, then stream or download it with a full-featured video player.

## Features

### 🔐 Telegram Authentication
- Direct connection to Telegram MTProto servers via TDLib (no bot API)
- Multi-step login: API credentials → phone number → OTP verification → optional 2FA password
- Persistent encrypted session — stay logged in across app restarts

### 📺 Video Discovery
- Browse subscribed channels, supergroups, groups, and private chats
- Smart video extraction from three sources:
  - Native Telegram videos (`MessageVideo`)
  - Video files sent as documents — MP4, MKV, WebM, AVI, MOV, FLV, and more (`MessageDocument`)
  - Animations and video clips (`MessageAnimation`)
- Pagination with "Load More Videos" support
- Video cards with thumbnails, duration, file size, and download status

### ▶️ Progressive Video Streaming
- Custom ExoPlayer `DataSource` that streams directly from TDLib chunk-by-chunk
- No need to wait for the full file to download before playback
- Random seeking support — jump anywhere in the timeline instantly

### 🎬 Immersive Video Player
- Full-screen landscape mode with hidden system bars
- 10-second quick seek forward/backward
- Auto-hiding overlay controls (4-second timeout)
- Streaming status badge ("Live Streaming" vs "Offline (Downloaded)")
- Seamless switch to local playback when download completes

### 📝 External Subtitle Support
- Load `.srt` and `.vtt` subtitle files via file picker
- Multi-encoding BOM normalization (UTF-8, UTF-16LE, UTF-16BE)
- High-performance binary search for subtitle timing
- Styled high-contrast overlay (white text on semi-transparent dark background)

### ⬇️ Download Manager
- Download videos for offline playback with real-time progress tracking
- Cancel ongoing downloads
- Automatic local playback when file is fully downloaded

### 🎨 Material You
- Material 3 with dynamic color theming on Android 12+
- Automatic dark/light mode based on system setting

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow + UDF) |
| Navigation | Navigation Compose |
| Telegram SDK | [TDLib Android](https://github.com/nicegram/TDLib-Android) (native JNI) |
| Video Player | AndroidX Media3 / ExoPlayer |
| Image Loading | Coil Compose |
| Build System | Gradle with Kotlin DSL |
| Min SDK | 26 (Android 8.0 Oreo) |
| Target SDK | 37 |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     UI LAYER (Compose)                      │
│   HomeScreen  │  SettingsScreen  │  VideoPlayerScreen        │
└──────────────────────────────▲──────────────────────────────┘
                               │ Observes StateFlow
┌──────────────────────────────┴──────────────────────────────┐
│                    VIEWMODEL LAYER                          │
│                   TelegramViewModel                         │
│          (Single source of truth for UI state)              │
└──────────────────────────────▲──────────────────────────────┘
                               │ Calls suspend functions
┌──────────────────────────────┴──────────────────────────────┐
│                      DATA LAYER                             │
│  TelegramRepository ─── Singleton, TDLib ResultHandler      │
│  TelegramDataSource ─── Media3 BaseDataSource (streaming)   │
│  UserPreferences ─────── SharedPreferences manager          │
│  TelegramModels ──────── AuthState, VideoItem, ChatSummary  │
└──────────────────────────────▲──────────────────────────────┘
                               │ Native JNI (libtdjni.so)
┌──────────────────────────────┴──────────────────────────────┐
│                   TDLib Native Client                       │
│                 Telegram MTProto Servers                     │
└─────────────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/java/mn/blazeapps/blazein/
├── MainActivity.kt                    # Single Activity with NavHost (home → settings → player)
├── data/
│   ├── model/
│   │   └── TelegramModels.kt          # AuthState, ChatSummary, ChatType, VideoItem
│   ├── TelegramRepository.kt          # TDLib client lifecycle, chat/video queries, downloads
│   ├── TelegramDataSource.kt          # Media3 BaseDataSource for progressive TDLib streaming
│   └── UserPreferences.kt             # SharedPreferences for API credentials & phone
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt              # Auth prompt, chat selector, video grid with actions
│   │   ├── SettingsScreen.kt          # API setup, phone auth, OTP, 2FA, session management
│   │   └── VideoPlayerScreen.kt       # Immersive ExoPlayer, subtitles, overlay controls
│   ├── viewmodel/
│   │   └── TelegramViewModel.kt       # App state (StateFlow), user actions, coroutine bridge
│   └── theme/
│       ├── Color.kt                   # Purple/Pink color palette (light & dark)
│       ├── Theme.kt                   # Material 3 + Dynamic Color support
│       └── Type.kt                    # Typography definitions
```

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- Android SDK 37
- A Telegram account
- Telegram API credentials from [my.telegram.org](https://my.telegram.org)

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/nicegram/BlazeIn.git
   cd BlazeIn
   ```

2. Open the project in Android Studio and sync Gradle.

3. Run on a device or emulator (min API 26).

4. On first launch, go to **Settings** and enter your Telegram API ID and API Hash.

5. Complete phone number verification and start browsing videos.

### APK Variants

The build produces split APKs by architecture:

| ABI | Target |
|---|---|
| `arm64-v8a` | Modern 64-bit ARM devices |
| `armeabi-v7a` | Older 32-bit ARM devices |
| `x86_64` | Emulators and x86 devices |
| `universal` | All architectures (larger size) |

## Dependencies

| Library | Version |
|---|---|
| Compose BOM | 2026.02.01 |
| Activity Compose | 1.8.0 |
| Core KTX | 1.10.1 |
| Lifecycle | 2.8.7 |
| Navigation Compose | 2.8.5 |
| Media3 ExoPlayer | 1.5.1 |
| Coil Compose | 2.7.0 |
| TDLib Android | capullo-tech/lib-tdlib-android |

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Telegram API communication and video streaming |
| `ACCESS_NETWORK_STATE` | Network connectivity checks |

## License

This project is licensed under the **GNU Affero General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
