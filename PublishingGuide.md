# URL Player Beta - Publishing Guide

## Steps to Package and Publish on Google Play Store

### 1. Prepare Your App
- Ensure your app is fully functional and tested.
- Remove any unnecessary logs or debug information.
- Optimize performance and fix any known bugs.

### 2. Set Up Your Package Name
- The package name should be unique and follow the convention: `com.yourcompany.urlplayerbeta`.
- In your `AndroidManifest.xml`, set the package name correctly:
  ```xml
  <manifest xmlns:android="http://schemas.android.com/apk/res/android"
      package="com.yourcompany.urlplayerbeta">
  ```
- In `build.gradle.kts`, set the application ID:
  ```kotlin
  android {
      namespace = "com.yourcompany.urlplayerbeta"
      defaultConfig {
          applicationId = "com.yourcompany.urlplayerbeta"
      }
  }
  ```
- Once published, the package name **cannot be changed**.

### 3. Generate a Signed APK or AAB
- Open Android Studio.
- Go to **Build > Generate Signed Bundle/APK**.
- Select **Android App Bundle (.aab)** (recommended) or **APK**.
- Create or use an existing keystore for signing.
- Complete the required fields:
  - Key alias
  - Key password
  - Keystore password
- Click **Finish** to generate the file.

### 4. Set Up a Google Play Developer Account
- Sign up at [Google Play Console](https://play.google.com/console/).
- Pay the one-time registration fee.
- Complete your developer profile.

### 5. Create a New App Listing
- In Google Play Console, click **Create App**.
- Enter app details such as name, default language, and category.
- Set permissions and privacy policies.

### 6. Upload Your AAB or APK
- Navigate to **Release Management > Production**.
- Click **Create a New Release**.
- Upload the generated **.aab** or **.apk** file.

### 7. Fill Out Store Listing Details
- Provide a detailed description of the app.
- Add high-quality screenshots (minimum 4 required).
- Upload a feature graphic and app icon.
- Set the content rating via Google’s questionnaire.

### 8. Set Pricing and Distribution
- Choose whether your app is **Free** or **Paid**.
- Select the countries where the app will be available.
- Enable or disable family-friendly options as needed.

### 9. Review and Submit
- Check for any missing requirements in **App Content**.
- Run a final review for compliance with Google Play policies.
- Click **Submit for Review**.

### 10. Wait for Approval
- Google reviews apps within a few hours to a few days.
- Once approved, your app will be live on the Play Store.

## Additional Notes
- Regularly update your app for security and performance improvements.
- Monitor feedback and analytics to improve user experience.

For any issues, refer to [Google Play Developer Support](https://support.google.com/googleplay/android-developer).

