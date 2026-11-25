# YouTube Vault — Native Android (Kotlin + Jetpack Compose)

A standalone native Android app written in Kotlin & Jetpack Compose with all features of YouTube Vault:
- **Native Google Sign-In** with Google Drive scopes & email whitelist
- **Google Drive Vault** (`videos/` folder + `links.txt` + `playlists.json` two-way sync)
- **YouTube Data API v3** (Popular feeds, categories, search, channel uploads)
- **Dual Video Player**:
  - **Media3 ExoPlayer** for hardware-accelerated Google Drive streaming
  - **Android YouTube Player** for YouTube videos
  - **Picture-in-Picture (PiP)** & watch progress resume
- **Vault / Private Mode**, **Dark & Light Themes**, **Favourites**, and **Playlists**

## Setup

1. Open `android-app/local.properties` and add your API keys (matching your web `.env`):
   ```properties
   sdk.dir=/Users/corewixteam08/Library/Android/sdk
   YOUTUBE_API_KEY=your_youtube_api_key
   GOOGLE_CLIENT_ID=your_google_client_id
   ALLOWED_EMAILS=your_email@gmail.com
   ```

2. In **Google Cloud Console > APIs & Services > Credentials**:
   - Create an **Android** OAuth 2.0 Client ID:
     - **Package name**: `com.youtubevault.app`
     - **SHA-1 certificate fingerprint**: run `./gradlew signingReport` inside `android-app/` to get your debug SHA-1.

3. Build & run in **Android Studio**, or via terminal:
   ```bash
   ./gradlew assembleDebug
   ```

## Separate GitHub Repository

This folder has its own `.git` repository and is ignored by the parent `youtube` repository.
To push it to its own GitHub repository:
```bash
cd android-app
git add .
git commit -m "Initial commit: Native Kotlin YouTube Vault Android app"
git remote add origin https://github.com/YOUR_USERNAME/youtube-vault-android.git
git push -u origin main
```
