# AI-Based Alerts and Notifications System

## Overview
This document describes the AI-powered alerts and notifications system implemented for the Blood Bank Android application. The system intelligently analyzes blood inventory data and generates contextual notifications to help maintain optimal blood supply levels.

## Features

### 🤖 AI-Powered Analysis
- **Blood Demand Prediction**: Uses real-world blood group distribution data to identify shortages
- **Seasonal Adjustments**: Accounts for seasonal demand variations (higher in winter, lower in summer)
- **Smart Priority Calculation**: Automatically assigns priority levels based on shortage severity
- **Contextual Recommendations**: Provides actionable insights for blood bank management

### 📱 Intelligent Notifications
- **Multiple Alert Types**: Blood shortages, donation reminders, emergency requests, health tips
- **Priority-Based Channels**: Different notification channels for different priority levels
- **User Preferences**: Customizable notification settings including quiet hours
- **Rich Notifications**: Detailed notifications with blood group, location, and contact information

### 🎯 Targeted Alerts
- **Blood Group Specific**: Notifications targeted to specific blood groups
- **Location-Based**: Alerts can include location information for nearby centers
- **Time-Sensitive**: Alerts with expiry times for time-critical requests
- **Emergency Override**: Critical alerts bypass quiet hours and user preferences

## System Architecture

### Core Components

1. **AINotificationService**: Main service for generating and displaying notifications
2. **BloodDemandPredictor**: AI engine for analyzing blood inventory and predicting shortages
3. **AlertRepository**: Database management for storing and retrieving alerts
4. **NotificationManager**: WorkManager integration for background processing
5. **NotificationPreferenceManager**: User preference management

### Data Models

- **BloodAlert**: Core alert entity with priority, type, and metadata
- **AlertPriority**: 5-level priority system (LOW to EMERGENCY)
- **AlertType**: 8 different alert types for various scenarios
- **NotificationPreference**: User customization options

## How It Works

### 1. Data Analysis
```kotlin
val analysis = bloodDemandPredictor.analyzeBloodInventory(donors)
```
- Analyzes current donor distribution
- Compares against expected global distribution
- Applies seasonal adjustment factors
- Identifies critical shortages

### 2. Smart Alert Generation
```kotlin
val smartAlert = bloodDemandPredictor.generateSmartAlert(analysis)
```
- Generates contextual alerts based on analysis
- Calculates required blood units
- Sets appropriate priority levels
- Includes actionable recommendations

### 3. Notification Display
```kotlin
aiNotificationService.showNotification(alert)
```
- Respects user preferences and quiet hours
- Uses appropriate notification channels
- Displays rich content with actions
- Stores in local database for history

### 4. Background Processing
```kotlin
notificationManager.startPeriodicNotifications()
```
- Runs every 4 hours using WorkManager
- Performs analysis in background
- Generates alerts automatically
- Handles device sleep and battery optimization

## Usage Instructions

### For Users

1. **Initial Setup**: Grant notification permissions when prompted
2. **View Alerts**: Check the AI Alerts section on the home screen
3. **Interact with Alerts**: Tap alerts to view detailed information and AI insights
4. **Test System**: Long-press the BERT Model card to generate test alerts
5. **Manage Preferences**: Customize notification settings (coming soon)

### For Developers

1. **Dependencies Added**:
   - WorkManager for background tasks
   - Firebase Messaging for push notifications
   - Gson for JSON processing

2. **Permissions Added**:
   - POST_NOTIFICATIONS (Android 13+)
   - VIBRATE
   - WAKE_LOCK

3. **Key Classes**:
   - `AINotificationService`: Main notification logic
   - `BloodDemandPredictor`: AI analysis engine
   - `AlertRepository`: Data persistence
   - `NotificationWorker`: Background processing

## Testing the System

### Manual Testing
1. Open the app and navigate to the home screen
2. Long-press the "BERT Model Analysis" card to generate test alerts
3. Check the notification panel for system notifications
4. View alerts in the AI Alerts section
5. Tap alerts to see detailed information and AI insights

### Automated Testing
The system includes:
- Background workers that run every 4 hours
- Automatic cleanup of expired alerts
- Preference-based filtering
- Error handling for edge cases

## AI Intelligence Features

### Blood Group Analysis
- Compares current inventory against global distribution patterns
- Identifies shortages using statistical analysis
- Accounts for blood group compatibility requirements

### Seasonal Intelligence
- Adjusts demand predictions based on time of year
- Higher demand factors in winter months
- Lower demand factors in summer months

### Priority Intelligence
- Emergency: >25% shortage with seasonal adjustment
- Critical: >15% shortage with seasonal adjustment
- High: >10% shortage with seasonal adjustment
- Medium: >5% shortage with seasonal adjustment
- Low: <5% shortage

### Recommendation Engine
- Targeted donor recruitment suggestions
- Hospital coordination recommendations
- Emergency campaign triggers
- Seasonal adjustment advice

## Future Enhancements

1. **Machine Learning Integration**: Train models on historical data
2. **Location Services**: GPS-based nearby donation center alerts
3. **Push Notifications**: Server-side notification triggers
4. **Analytics Dashboard**: Comprehensive reporting and insights
5. **Donor Engagement**: Personalized donor communication

## Technical Notes

- Uses SQLite for local data storage
- Firebase integration ready for cloud sync
- WorkManager ensures reliable background execution
- Notification channels for Android 8.0+ compatibility
- Preference system for user customization

## Error Handling

The system includes comprehensive error handling:
- Graceful degradation when permissions are denied
- Safe fallbacks for database operations
- Silent error handling to prevent app crashes
- Logging for debugging and monitoring

## Performance Considerations

- Background processing limited to prevent battery drain
- Efficient database queries with proper indexing
- Lazy loading of UI components
- Memory-conscious alert storage with automatic cleanup

---

**Note**: This AI notification system is designed to be robust, intelligent, and user-friendly while maintaining the existing app functionality without any breaking changes.
