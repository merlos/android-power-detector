# Release Signing and GitHub Secrets

This document explains how to configure signing for the GitHub Actions release workflow.

## 1. Generate a keystore

Run this on a secure machine:

```bash
keytool -genkeypair \
  -v \
  -keystore power-detector-release.jks \
  -alias powerdetector \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Keep the resulting `.jks` file private.

## 2. Convert the keystore to base64

### macOS

```bash
base64 -i power-detector-release.jks | pbcopy
```

### Linux

```bash
base64 -w 0 power-detector-release.jks
```

Copy the single-line base64 output.

## 3. Add repository secrets

In GitHub, open:

`Settings -> Secrets and variables -> Actions`

Create these repository secrets:

- `ANDROID_KEYSTORE_BASE64`: base64 content of the `.jks` file
- `ANDROID_KEYSTORE_PASSWORD`: keystore password
- `ANDROID_KEY_ALIAS`: key alias, for example `powerdetector`
- `ANDROID_KEY_PASSWORD`: password for the signing key

## 4. How the workflow uses them

On release tags such as `v1.0.0`, the workflow:

1. Decodes `ANDROID_KEYSTORE_BASE64` into a temporary keystore file
2. Exports these environment variables for Gradle:
   - `ANDROID_KEYSTORE_PATH`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
3. Runs:

```bash
./gradlew assembleRelease
```

4. Uploads the signed APK to the GitHub Release page

## 5. Release process

1. Commit your changes to `main`.
2. Create a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

3. GitHub Actions builds the release APK.
4. The workflow publishes the APK as a release asset.
5. The marketing website download button can point to the latest release automatically.

## 6. Notes

- Never commit the keystore file to the repository.
- Never put signing passwords in source files.
- Rotate the keystore only if necessary, because changing signing keys affects upgrade compatibility.
- If your repository owner is not `merlos`, update the GitHub Pages site metadata in [docs/index.html](docs/index.html).
