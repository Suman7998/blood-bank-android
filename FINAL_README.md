# Blood Bank Android Application

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Technical Stack](#technical-stack)
- [Installation](#installation)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Screens](#screens)
- [API Integration](#api-integration)
- [Firebase Integration](#firebase-integration)
- [Permissions](#permissions)
- [Testing](#testing)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

## Overview

Blood Bank is a comprehensive Android application designed to connect blood donors with recipients in need. The app serves as a platform to facilitate blood donation by providing a seamless way to find donors, request blood, and locate nearby blood donation centers. The application is built with modern Android development practices and follows Material Design guidelines for an optimal user experience.

## Features

### Core Features
- **User Authentication**: Secure sign-up and login system
- **Donor Search**: Find blood donors by blood type and location
- **Blood Requests**: Create and manage blood donation requests
- **Donation Centers**: Locate nearby blood banks and donation centers
- **User Profiles**: Manage personal information and donation history
- **Real-time Updates**: Get instant notifications for blood requests
- **Offline Support**: Access critical information without internet connection
- **Multimedia Support**: Attach photos, videos, and voice notes to requests

### Advanced Features
- **Push Notifications**: Receive alerts for nearby blood requests
- **Maps Integration**: Interactive maps to locate donors and centers
- **News Feed**: Stay updated with the latest blood donation news
- **Dark Mode**: Enhanced visibility in low-light conditions
- **Multi-language Support**: Available in multiple languages
- **Data Backup & Sync**: Cloud synchronization of user data

## Technical Stack

### Core Components
- **Language**: Kotlin
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Asynchronous Programming**: Kotlin Coroutines with Flow
- **Database**: SQLite with Room
- **Authentication**: Firebase Authentication
- **Cloud Storage**: Firebase Firestore
- **Analytics**: Firebase Analytics

### Libraries
- **UI**: Material Design Components, ConstraintLayout, RecyclerView
- **Networking**: Retrofit, OkHttp, Gson
- **Image Loading**: Glide
- **Maps**: Google Maps SDK for Android
- **Background Processing**: WorkManager
- **Testing**: JUnit, Espresso, MockK
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Component
- **Camera**: CameraX
- **Media**: ExoPlayer

## Installation

### Prerequisites
- Android Studio Flamingo (2022.2.1) or later
- Android SDK 34
- Java Development Kit (JDK) 11 or later
- Google Play Services (for Maps and Location)

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/blood-bank-android.git
   cd blood-bank-android
   ```

2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Create a `google-services.json` file from Firebase Console and place it in the `app/` directory
5. Update the `google_maps_key` in `res/values/google_maps_api.xml`
6. Build and run the application

## Configuration

### Environment Variables
Create a `local.properties` file in the root directory with the following content:
```
# Google Maps API Key
MAPS_API_KEY=your_google_maps_api_key

# NewsAPI Key
NEWS_API_KEY=your_news_api_key
```

### Firebase Setup
1. Create a new project in Firebase Console
2. Add an Android app to your Firebase project
3. Download the `google-services.json` file
4. Place the file in the `app/` directory
5. Enable the following Firebase services:
   - Authentication
   - Firestore Database
   - Cloud Messaging
   - Storage
   - Analytics

## Architecture

The application follows the MVVM (Model-View-ViewModel) architecture pattern with the following key components:

### Data Layer
- **Repositories**: Handle data operations and business logic
- **Data Sources**: Local (Room) and Remote (Firebase) data sources
- **Models**: Data classes representing the app's data

### Domain Layer
- **Use Cases**: Contain business logic
- **Repositories Interfaces**: Define data operations

### Presentation Layer
- **Activities/Fragments**: UI components
- **ViewModels**: Manage UI-related data
- **State Management**: UI state handling with StateFlow

## Screens

### Authentication
- Splash Screen
- Login Screen
- Registration Screen
- Forgot Password

### Main Screens
- Home Dashboard
- Donor List/Map View
- Blood Request Form
- Donation Centers Map
- User Profile
- Settings

### Additional Screens
- Donation History
- Notifications
- Search Results
- Blood Compatibility Guide
- News Feed

## API Integration

The application integrates with the following APIs:

### NewsAPI
- **Base URL**: `https://newsapi.org/v2/`
- **Endpoints**:
  - `everything?q=blood%20donation` - Get latest blood donation news
  - `top-headlines?category=health` - Get health-related headlines

### Google Maps API
- **Features**:
  - Display nearby donors
  - Show route to donation centers
  - Location-based search

## Firebase Integration

### Authentication
- Email/Password authentication
- Phone number authentication
- Google Sign-In
- Anonymous authentication

### Cloud Firestore
- **Collections**:
  - `users`: User profiles and preferences
  - `donors`: Blood donor information
  - `requests`: Blood donation requests
  - `donation_centers`: Blood bank locations

### Cloud Messaging
- Push notifications for:
  - New blood requests
  - Donation reminders
  - Emergency alerts

### Storage
- User profile pictures
- Document uploads
- Media attachments

## Permissions

The app requires the following permissions:
- `INTERNET`: For network requests
- `ACCESS_FINE_LOCATION`: For location-based services
- `CAMERA`: For capturing photos
- `RECORD_AUDIO`: For voice notes
- `READ_EXTERNAL_STORAGE`: For accessing media files
- `CALL_PHONE`: For calling donors directly

## Testing

The application includes the following test suites:

### Unit Tests
- ViewModel tests
- Repository tests
- Use case tests
- Utility function tests

### Instrumentation Tests
- UI tests with Espresso
- Navigation tests
- Database tests

### Test Coverage
- Minimum 70% code coverage
- UI test coverage for critical user flows
- Integration tests for API and database operations

## Security

### Data Protection
- Data encryption at rest and in transit
- Secure SharedPreferences for sensitive data
- Biometric authentication support

### Secure Coding Practices
- Input validation
- Secure API key management
- Certificate pinning
- ProGuard/R8 obfuscation

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments
- Material Design Components
- Android Jetpack
- Firebase
- Google Maps Platform
- NewsAPI
- Open Source Community

---

*This application is developed for educational purposes. Always follow proper medical guidelines for blood donation.*
