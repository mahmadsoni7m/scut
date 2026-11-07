# Build instructions (beginner)
## On GitHub (no PC needed)
1. Create a new empty repository on github.com.
2. Upload ALL files from this ZIP (keep the `.github` folder; on a phone use "Add file -> Upload files" or Termux `git push`).
3. Open the **Actions** tab -> **Android Build** -> **Run workflow**.
4. When it is green, open the run -> **Artifacts** -> download `app-debug-apk`, unzip, install `app-debug.apk`.
5. If it is red, open the failed step, copy the error text and fix/report it.

## Locally
Install JDK 17, Android SDK 34 and Gradle 8.9, then: `gradle assembleDebug`.
The Gradle wrapper jar is not included (it could not be generated offline). Run `gradle wrapper --gradle-version 8.9` once if you want `./gradlew`.
