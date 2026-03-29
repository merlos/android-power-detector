# Power Detector

Power Detector is an Android application written in Kotlin for devices running Android 8.1 or newer. It detects when the phone switches between external power and battery, shows the current state on the home screen, and executes a configurable list of actions whenever that state changes.

## Features

- Large centered status on the home screen: `AC Power` or `Battery`
- Action list in the lower panel with tap-to-edit cards
- Add actions with the in-panel Add button, empty-state button, or floating `+` button
- Supported actions:
  - Send an SMS to one phone number
  - Send a Telegram message to a chat using a bot token
- Go command line tool to generate Telegram setup QR codes
- Background power change detection with WorkManager-backed execution
- Manual test execution from the action form before relying on automation
- GitHub Actions workflow to build and publish the APK
- GitHub Pages marketing website with a download button for the latest release

## Requirements

- Android Studio Iguana or newer, or a compatible Gradle/Android SDK setup
- JDK 17
- Android SDK 34
- A device or emulator running Android 8.1+ (API 27+)

## Build locally

1. Install Android Studio and ensure the Android SDK for API 34 is installed.
2. Open the repository in Android Studio.
3. Let Gradle sync and install any missing SDK components.
4. Build the debug APK:

```bash
./gradlew assembleDebug
```

5. The APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run locally

1. Connect an Android device or start an emulator running API 27 or higher.
2. Install the debug build:

```bash
./gradlew installDebug
```

3. Open Power Detector.
4. The home screen will show the current state in the center:
   - `AC Power` when the device is plugged in
   - `Battery` when the device is unplugged

## How to add actions

### Add an SMS action

1. Tap the `+` button.
2. Choose `Send an SMS`.
3. Select when it should run:
   - `When AC power is connected`
   - `When running on battery`
4. Enter the destination phone number.
5. Enter the message.
6. Save the action.
7. Grant the SMS permission when Android prompts for it.
8. Use `Run test action now` in the form if you want to verify delivery immediately.

### Add a Telegram action

1. Create a Telegram bot with BotFather and copy the bot token.
2. Find the chat ID where the message should be delivered.
3. Tap the `+` button.
4. Choose `Send a Telegram message`.
5. Select the trigger.
6. Enter the Telegram chat ID.
7. Enter the bot token.
8. Enter the message.
9. Save the action.
10. Use `Run test action now` in the form to verify the bot token and chat ID.

### Import Telegram setup from a QR image

1. Generate a QR image with the Go tool in [tools/telegramqr/main.go](tools/telegramqr/main.go).
2. Open the Telegram action form in the Android app.
3. Tap `Import Telegram QR`.
4. Choose the QR image from storage.
5. The app fills the Telegram bot token and chat ID automatically.

### Generate a Telegram setup QR code

The repository includes a Go command line tool that generates a QR image containing the Telegram setup payload understood by the Android app.

Run it from the repository root:

```bash
cd tools/telegramqr
go run . <botid> <chatid> [output.png]
```

Example:

```bash
cd tools/telegramqr
go run . 123456:ABCDEF -100123456789 telegram-setup.png
```

This generates a QR image whose payload looks like this:

```text
powerdetector://telegram?botid=123456%3AABCDEF&chatid=-100123456789
```

### Message placeholders

The app supports these placeholders in action messages:

- `{status}` → `AC Power` or `Battery`
- `{time}` → current local timestamp

Example:

```text
Power state changed to {status} at {time}
```

## Permissions and behavior

- `SEND_SMS` is only used for SMS actions.
- `INTERNET` is only used for Telegram messages.
- Power state changes are detected through Android power broadcast intents.
- SMS sending depends on device capabilities and granted permission.
- Telegram delivery depends on network availability and valid bot credentials.

## Release builds

Release APKs are built by GitHub Actions on version tags like `v1.0.0`.

See [docs/release-signing.md](docs/release-signing.md) for:

- keystore generation
- GitHub repository secrets
- release workflow behavior
- APK publishing steps

## GitHub Pages site

The repository includes a static marketing website in [docs/index.html](docs/index.html). A GitHub Pages workflow deploys it automatically from the `docs` directory.

After enabling Pages in the repository, the site will expose a download button that points to the latest GitHub Release APK.

## Recommended test flow

1. Add one SMS action for `AC Power`.
2. Add one Telegram action for `Battery`.
3. Plug and unplug the device.
4. Confirm the centered status changes and the actions execute.
5. Tap an action card to edit or disable it.
6. Use the manual test button in the form to verify each action without unplugging the device.
