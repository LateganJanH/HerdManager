# Fixing "configuration_not_found" / Auth errors

If you see **"An internal error has occurred - configuration_not_found"** (or "API key not valid") when signing in or creating an account, work through these steps in order.

## 1. Enable Authentication in Firebase Console

1. Open [Firebase Console](https://console.firebase.google.com/) and select your project (**herdmanager-b06d8**).
2. Go to **Build → Authentication**.
3. If you see **"Get started"**, click it. This initializes Authentication for the project.
4. Open the **Sign-in method** tab.
5. Click **Email/Password**, turn **Enable** on, and **Save**.

## 2. Enable Identity Toolkit API (required for Auth)

Firebase Auth uses the Identity Toolkit API. It must be enabled for your project:

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select the **same** project as Firebase (e.g. **herdmanager-b06d8**).
   - Or from Firebase: Project settings (gear) → under "Your project" click the Google Cloud project link.
2. Go to **APIs & Services → Library** (or open: https://console.cloud.google.com/apis/library).
3. Search for **"Identity Toolkit API"**.
4. Open it and click **Enable**.
5. Wait 1–2 minutes for the change to apply.

## 3. Confirm Android app and package name

1. In Firebase: **Project settings** (gear) → **Your apps**.
2. Your Android app should have package name **`com.herdmanager.app`** (no trailing period).
3. If it’s wrong or missing, add an Android app with package name **`com.herdmanager.app`**, then download the new **google-services.json** and replace `android/app/google-services.json`.

## 4. Clean build and reinstall

From the `android` folder:

```bash
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
```

Uninstall the app from the device/emulator, then install the new APK so the old config isn’t cached.

## 5. If it still fails

- Check that **API key restrictions** (Google Cloud Console → APIs & Services → Credentials → your API key) are not blocking the app. For debugging you can leave the key unrestricted; restrict by package name later.
- Try creating a **new** Firebase project, add one Android app with package `com.herdmanager.app`, enable Email/Password and Identity Toolkit API, then use the new **google-services.json**.
