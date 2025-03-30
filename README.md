# Aura Security

<div align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="Aura Security Logo" width="200"/>
  <br>
  <p><strong>Event Entry & Security Management System</strong></p>
</div>

## 📋 Overview

Aura Security is an Android application designed for secure event entry/exit management using QR code scanning. The app provides security checkpoint management, attendee verification, and attendance tracking for events.

### ✨ Key Features

- **🔐 Secure Authentication** - Token-based login with encrypted credential storage
- **🚪 Multi-Checkpoint Support** - Manage different security checkpoints (Main Gate, Concert Area)
- **📱 QR Code Scanning** - Fast and reliable scanning for attendee verification
- **📊 Entry/Exit Tracking** - Record and monitor attendee movement between event areas
- **🔄 Real-time Verification** - Instantly verify attendee credentials and permissions
- **👤 Role-based Access** - Different functionality based on user permissions

## 🏗️ Architecture

This application follows modern Android development practices:

- **Kotlin** - 100% Kotlin implementation
- **Jetpack Compose** - Declarative UI toolkit
- **MVVM Architecture** - Clear separation of concerns
- **Hilt** - Dependency injection
- **Repository Pattern** - Clean data access abstraction
- **Coroutines & Flow** - Asynchronous programming
- **EncryptedSharedPreferences** - Secure credential storage

## 📁 Project Structure

```
app/src/main/java/com/techelites/attendacemarkingv1/
├── data/
│   ├── preferences/          # Secure data storage
│   └── repository/           # Data access layer
├── di/                       # Dependency injection modules
├── network/                  # API services and models
├── ui/
│   ├── auth/                 # Authentication screens
│   ├── components/           # Reusable UI components
│   ├── main/                 # Main app screens
│   │   ├── home/             # Home screen
│   │   ├── profile/          # User profile
│   │   └── scanner/          # QR scanner implementation
│   ├── splash/               # App startup screen
│   └── theme/                # App theming
└── utils/                    # Utility classes
```

## 🛠️ Setup & Installation

### Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 21+
- Gradle 7.0+

### Getting Started

1. Clone the repository:
   ```
   git clone https://github.com/your-username/aura-security.git
   ```

2. Open the project in Android Studio

3. Configure the API endpoint in `NetworkModule.kt`:
   ```kotlin
   private const val BASE_URL = "https://your-api-url.com/v1/api"
   ```

4. Build and run the application on your device or emulator

## 📱 Screenshots

<div align="center">
  <img src="screenshots/login.png" alt="Login Screen" width="200"/>
  <img src="screenshots/home.png" alt="Home Screen" width="200"/>
  <img src="screenshots/scanner.png" alt="Scanner Screen" width="200"/>
  <img src="screenshots/profile.png" alt="Profile Screen" width="200"/>
</div>

## 🔑 Key Components

### Secure Authentication

```kotlin
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { getEncryptedSharedPreferences() }
    
    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // Token Management
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
}
```

### QR Code Scanner Integration

```kotlin
private fun setupScanner() {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.TYPE_TEXT
        )
        .enableAutoZoom()
        .build()

    scanner = GmsBarcodeScanning.getClient(this, options)
}
```

## 📝 API Documentation

The application communicates with a backend server for authentication and data verification:

```kotlin
interface AuthApiService {
    // Login API
    @Headers("Content-Type: application/json")
    @POST("admin/admin/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Main Gate Entry
    @POST("entry/entry/main-gate")
    suspend fun mainGateEntry(
        @Header("Authorization") token: String, 
        @Body request: EntryExitRequest
    ): Response<ResponseBody>
    
    // Concert Gate Entry
    @POST("entry/entry/concert-area")
    suspend fun concertGateEntry(
        @Header("Authorization") token: String,
        @Body request: EntryExitRequest
    ): Response<ResponseBody>
    
    // ...other endpoints
}
```

## 🔒 Security Features

- **🔐 Token-based Authentication**: Secure API access
- **🛡️ Encrypted Storage**: All sensitive information is encrypted using AES256
- **📱 Permission Handling**: Runtime permissions for camera access
- **👥 Role-based Access Control**: Different user roles have tailored access to features

## 🚀 Future Enhancements

- **📶 Offline Support**: Function with limited connectivity
- **📊 Analytics Dashboard**: Track entry/exit statistics
- **🌐 Multi-language Support**: Internationalization
- **🌙 Dark Mode**: Enhanced UI theming options
- **📲 Push Notifications**: Real-time alerts for security staff

## 📄 License

```
© 2025 Tech Elites. All rights reserved.
```

## 👥 Contributors

- Avinash P - Developer

---

<div align="center">
  <p>Made with ❤️ by Tech Elites</p>
</div>
