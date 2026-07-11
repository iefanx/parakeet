# Paraflow

Paraflow is private, offline speech-to-text for Android. It uses an on-device
Parakeet model and an opt-in Accessibility Service to place user-requested
dictation into the focused text field.

## Privacy

Audio and transcription stay on the device. Internet access is used only to
download the public speech model during initial setup. The app contains no
advertising, analytics, accounts, or cloud sync.

Public privacy policy: <https://iefanx.github.io/paraflow/>

## Local development

Requirements:

- Android Studio or Android SDK 35
- JDK 17

The app downloads the Parakeet TDT 0.6B v3 INT8 ONNX model from its canonical
Hugging Face repository on first launch. Downloads resume after interruption,
and each file is verified with a pinned SHA-256 digest before loading.

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

Build a Play upload bundle after creating a local `keystore.properties` from
`keystore.properties.example`:

```bash
./gradlew :app:bundleRelease
```

Never commit signing keys, `keystore.properties`, `local.properties`, build
outputs, or ONNX model binaries. These are covered by `.gitignore`.

## Google Play

Release and declaration notes are in [PLAY_STORE_RELEASE.md](PLAY_STORE_RELEASE.md).

## License

No license has been granted yet. Add an explicit license before accepting
external contributions or reuse.
