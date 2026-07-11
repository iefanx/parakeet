# Google Play release checklist

## Status before first upload

- [ ] Create and securely back up the upload keystore. Never commit it or `keystore.properties`.
- [ ] Set the real keystore values in `keystore.properties`; the release build intentionally has no signing configuration until these are supplied.
- [ ] Publish `PRIVACY_POLICY.md` at a public, non-geofenced HTTPS URL and replace its Contact placeholder with the legal developer name and monitored email.
- [ ] Add that URL to Play Console and verify the in-app Privacy policy text matches it.
- [ ] Complete Data safety. Voice and transcription stay on-device. Network access downloads public model files from Hugging Face and optional ML Kit language packs; user content is not sent for cloud processing. Recheck this if analytics, crash reporting, ads, sync, or another network SDK is added.
- [ ] Complete the Accessibility declaration as a non-accessibility tool. State that the service detects focused editable fields, displays the dictation control, and inserts only user-requested transcription. Upload a short demo video that shows onboarding, consent, enabling the service, and dictation in another app.
- [ ] Mention AccessibilityService use in the store listing; do not claim that Paraflow is an accessibility tool.
- [ ] Complete the content-rating questionnaire, app-content declarations, contact details, data-safety form, store listing, screenshots, 512px icon, and 1024×500 feature graphic.
- [ ] Upload first to Internal testing. If this is a new personal developer account, complete the required closed-test period before production access.

## Store-listing copy

**Short description**

Private, offline dictation that writes wherever you do.

**Accessibility disclosure for long description**

Paraflow uses Android's AccessibilityService API to show a dictation button when an editable text field is focused and to insert the transcription you request at your cursor. Voice and text are processed on-device and are never sent to a server.

## Build the upload bundle

1. Create an upload keystore if you do not already have one:

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

2. Copy `keystore.properties.example` to `keystore.properties` and fill in the real values.

3. Build the Android App Bundle:

```bash
./gradlew :app:bundleRelease
```

The upload artifact will be at:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Play Console declarations

- Accessibility API: declare that Paraflow uses accessibility to detect editable text fields, show a floating dictation control, and paste user-requested transcription at the cursor. Provide the required demo video.
- Data safety: audio is processed offline on device. The initial public model download comes from Hugging Face and contains no user content. Transcription history is stored locally unless you add sync/export features later. A public privacy-policy URL is still required.
- Permissions: microphone is required for dictation. Battery unrestricted access is requested so cross-app dictation remains reliable in the background.
