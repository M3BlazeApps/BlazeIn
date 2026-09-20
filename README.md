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

#### ⬇️ Download Manager & Downloads Screen (New in v2.0.0)
- Download videos for offline playback with real-time progress tracking
- **Dedicated Downloads Screen**: Browse, manage, and play all offline downloaded media
- **One-tap Playback & File Deletion**: Instant playback using the built-in video player or delete files to free up disk space
- **Open-source Movie Database Details**: Automatically parses video file names and fetches rich metadata (movie/show title, release year, genre, plot synopsis, and high-res poster artwork) via open-source iTunes Search API with no API key needed
- Automatic local playback when file is fully downloaded

### 🎨 Figma Make Glassmorphism & UI Redesign
- **Obsidian Mesh Canvas**: Deep obsidian dark background (`#080B1A`) with ambient 4-point radial mesh glow (indigo, azure, violet, warm orange)
- **Electric Dual-Accent System**: Vivid Blue-Violet (`#6366F1`) and high-energy Electric Orange (`#F97316`) for primary actions, buttons, and badges
- **Refined Glass Components**: 16dp / 12dp frosted glass cards (`rgba(255,255,255,0.04)`), ultra-thin glass borders (`rgba(255,255,255,0.08)`), glowing squircle icon boxes, and pill badges
- **Figma Make Layouts**: Overhauled Home, Settings, Downloads, and Video Player screens matching the modern Figma Make prototype specification

## Tech Stack

| Component | Technology |
|---|---|
| Version | 2.0.0 |
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
┌─────────────────────────────────────────────────────────────────────────┐
│                           UI LAYER (Compose)                            │
│   HomeScreen  │  DownloadsScreen  │  SettingsScreen  │  VideoPlayerScreen│
└────────────────────────────────────▲────────────────────────────────────┘
                                     │ Observes StateFlow
┌────────────────────────────────────┴────────────────────────────────────┐
│                          VIEWMODEL LAYER                                │
│               TelegramViewModel  │  DownloadsViewModel                  │
│                (Single source of truth for UI state)                    │
└────────────────────────────────────▲────────────────────────────────────┘
                                     │ Calls suspend functions
┌────────────────────────────────────┴────────────────────────────────────┐
│                            DATA LAYER                                   │
│  TelegramRepository ─── Singleton, TDLib ResultHandler                  │
│  DownloadsRepository ── File scanner & iTunes Movie Metadata fetcher    │
│  TelegramDataSource ─── Media3 BaseDataSource (streaming)               │
│  UserPreferences ─────── SharedPreferences manager                      │
│  TelegramModels ──────── AuthState, VideoItem, ChatSummary, Downloads   │
└────────────────────────────────────▲────────────────────────────────────┘
                                     │ Native JNI (libtdjni.so)
┌────────────────────────────────────┴────────────────────────────────────┐
│                         TDLib Native Client                             │
│                       Telegram MTProto Servers                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/java/mn/blazeapps/blazein/
├── MainActivity.kt                    # Single Activity with NavHost (home → downloads → settings → player)
├── data/
│   ├── model/
│   │   └── TelegramModels.kt          # AuthState, ChatSummary, ChatType, VideoItem
│   ├── TelegramRepository.kt          # TDLib client lifecycle, chat/video queries, downloads
│   ├── TelegramDataSource.kt          # Media3 BaseDataSource for progressive TDLib streaming
│   ├── DownloadsRepository.kt         # Recursive file scanner & iTunes movie metadata fetcher
│   └── UserPreferences.kt             # SharedPreferences for API credentials & phone
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt              # Auth prompt, chat selector, video grid with actions
│   │   ├── DownloadsScreen.kt         # Downloaded videos manager, playback, deletion & metadata card
│   │   ├── SettingsScreen.kt          # API setup, phone auth, OTP, 2FA, session management
│   │   └── VideoPlayerScreen.kt       # Immersive ExoPlayer, subtitles, overlay controls
│   ├── viewmodel/
│   │   ├── TelegramViewModel.kt       # Telegram state, chats, streaming playback bridge
│   │   └── DownloadsViewModel.kt      # Downloaded videos state, deletion & metadata enrichment
│   └── theme/
│       ├── Color.kt                   # Glassmorphic Apple-inspired color palette
│       ├── Glassmorphism.kt           # Custom glass cards, buttons, chips, and backgrounds
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
   git clone https://github.com/M3BlazeApps/BlazeIn.git
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
