# MEMENTO — Android starter project

A local-first Android app and home-screen widget for time awareness and one daily execution target.

## Personal defaults
- Date of birth: 20 March 2006
- No death-date countdown or fabricated lifespan estimate
- Dark charcoal / bone / blood-red visual system

## Build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync to download dependencies.
4. Run the `app` configuration on your Android phone.
5. On the home screen, long-press an empty area → Widgets → MEMENTO.

## Notes
- The elapsed-days counter is calculated from the device's local date.
- The app stores the daily mission and completion state locally in SharedPreferences.
- The widget refreshes when Android updates it or when the app saves a mission.
- Android may batch widget refreshes to conserve battery.
- This starter does not include unlock interception, notification spam, health tracking, or a death countdown.
- For a true daily reset and reliable scheduled widget refresh, add a WorkManager periodic worker in a later iteration.

## Build an APK with GitHub Actions

This repository includes `.github/workflows/android-apk.yml`.

1. Create a GitHub repository and upload the contents of this project.
2. Open the repository's **Actions** tab.
3. Select **Build MEMENTO APK** and choose **Run workflow** (or push to `main`).
4. When the run completes, open the run and download the `MEMENTO-debug-apk` artifact.
5. Extract the artifact to get `app-debug.apk`, then install it on Android. You may need to allow installation from your browser/file manager.

The workflow builds a debug APK. It is suitable for personal testing, not Play Store publication.
