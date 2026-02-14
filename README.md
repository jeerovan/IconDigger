# IconDigger

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**IconDigger** is an Android utility application designed to reverse-engineer installed icon packs. It allows developers and themers to inspect other icon packs by extracting their core resources—including the `appfilter.xml` configuration, raw drawables, and rasterized bitmaps—directly to your device's storage.

## Features

- **Icon Pack Detection**: Automatically scans and lists all installed icon packs on your device.
- **One-Click Extraction**: Simple UI with dedicated buttons for different extraction modes.
- **Resource Types**:
  - **AppFilter**: Extracts the `appfilter.xml` file which maps app components to icon resources.
  - **Drawables**: Extracts the raw drawable resources (XML vectors or original images).
  - **Bitmaps**: Extracts icons converted into high-quality Bitmap (PNG) format.
- **Organized Output**: All extracted files are neatly saved to your device's `Downloads` folder.

## Build and Install

To build and install this project, you will need **Android Studio** and a device/emulator running **Android 10.0 (API 29)** or higher.

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17+

### Steps

1.  **Clone the repository**:
    
    ```bash
    git clone https://github.com/jeerovan/IconDigger.git
    ```

2.  **Open in Android Studio**:
    - Launch Android Studio.
    - Select **File > Open** and navigate to the cloned directory.

3.  **Sync and Build**:
    - Wait for Gradle to sync dependencies.
    - Connect your Android device via USB (ensure USB Debugging is on).
    - Click the green **Run** (▶) button.

> **Note**: This app requires the `QUERY_ALL_PACKAGES` permission to detect installed icon packs

## How to Use

1. **Launch IconDigger**: Open the app from your launcher.
2. **Select an Icon Pack**: The main screen lists all compatible icon packs installed on your device. Tap on the card of the icon pack you wish to inspect.
3. **Choose Extraction Method**:
   - Tap **Extract AppFilter** to save the `appfilter.xml` file.
   - Tap **Extract Drawables** to get the raw resource files (useful for inspecting VectorDrawables).
   - Tap **Extract Bitmaps** to get the icons as rasterized PNG images.
4. **Locate Files**:
   - Open your file manager.
   - Navigate to `Internal Storage > Downloads`.
   - You will find appfilter.xml or the zip file containing icon files.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
