# Secure Eventify

<div align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="Secure Eventify Logo" width="200"/>
  <br>
  <p><strong>Event Attendance & ID Card Management System</strong></p>
</div>

## 📋 Overview

Secure Eventify is an Android application designed for attendance management and ID card tracking at events. The app provides event staff with an intuitive interface for marking attendance and tracking ID card collection using QR code scanning.

### ✨ Key Features

- **🔐 Secure Authentication** - Token-based login with encrypted credential storage
- **📱 QR Code Scanning** - Fast and reliable scanning for attendee verification
- **📊 Attendance Tracking** - Mark and unmark attendance with real-time verification
- **🆔 ID Card Management** - Track collection and return of ID cards
- **👤 Role-based Access** - Different functionality based on user permissions
- **🔄 Real-time Verification** - Instantly verify attendee credentials with visual feedback
- **🔔 Audio Feedback** - Sound notifications for successful and failed scans

## 🏗️ Architecture

This application follows modern Android development practices:

- **Kotlin** - 100% Kotlin implementation
- **Jetpack Compose** - Declarative UI toolkit
- **MVVM Architecture** - Clear separation of concerns
- **Hilt** - Dependency injection
- **Repository Pattern** - Clean data access abstraction
- **Coroutines & Flow** - Asynchronous programming
- **EncryptedSharedPreferences** - Secure credential storage
- **ML Kit Vision** - QR code scanning capabilities

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
│   │   └── scanner/          # QR scanner implementations
│   ├── splash/               # App startup screen
│   └── theme/                # App theming
└── utils/                    # Utility classes
```

## 🛠️ Setup & Installation

### Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 31+ (Android 12)
- Gradle 7.0+

### Getting Started

1. Clone the repository:
   ```
   git clone https://github.com/your-username/secure-eventify.git
   ```

2. Open the project in Android Studio

3. Configure the API endpoint in `NetworkModule.kt`:
   ```kotlin
   private const val BASE_URL = "https://xyz.com/v1/api"
   ```

4. Build and run the application on your device or emulator


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
private suspend fun startScanning(scanner: GmsBarcodeScanner, viewModel: MainViewModel) {
    try {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val scannedData = when (barcode.valueType) {
                    Barcode.TYPE_URL -> barcode.url?.url
                    Barcode.TYPE_TEXT -> barcode.displayValue
                    else -> barcode.displayValue
                }

                scannedData?.let {
                    viewModel.processScannedData(it)
                }
            }
    } catch (e: Exception) {
        // Handle scanning errors
    }
}
```

## 📝 API Documentation

The application communicates with a backend server for authentication and data verification:

```kotlin
interface AuthApiService {
    // Login API
    @Headers("Content-Type: application/json")
    @POST("scan/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Mark Attendance
    @POST("scan/attendance/mark")
    suspend fun markAttendance(
        @Body request: MainRequest
    ): Response<ResponseBody>

    // Unmark Attendance
    @POST("scan/attendance/unmark")
    suspend fun unmarkAttendance(
        @Body request: MainRequest
    ): Response<ResponseBody>

    // ID Collection APIs
    @POST("scan/mark-id-card/collect")
    suspend fun collectId(
        @Body request: MainRequest
    ): Response<ResponseBody>

    @POST("scan/mark-id-card/return")
    suspend fun returnId(
        @Body request: MainRequest
    ): Response<ResponseBody>
}
```

## 🔒 Security Features

- **🔐 Token-based Authentication**: Secure API access
- **🛡️ Encrypted Storage**: All sensitive information is encrypted using AES256
- **📱 Permission Handling**: Runtime permissions for camera access
- **👥 Role-based Access Control**: Different user roles have tailored access to features

## 🚀 Future Enhancements

- **📊 Analytics Dashboard**: Track attendance and ID card statistics
- **🌙 Dark Mode**: Enhanced UI theming options
- **📲 Push Notifications**: Real-time alerts for event staff

## 📄 License

```
© 2025 Tech Team KLS GIT. All rights reserved.
```

## 👥 Contributors

- Avinash Pauskar - Developer
- Ganesh Kugaji - Developer

---

<div align="center">
  <p>Made with ❤️ by Tech Team KLS GIT</p>
</div>
