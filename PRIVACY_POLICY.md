# Privacy Policy for Paraflow

**Effective Date:** July 10, 2026

Paraflow ("we", "our", or "us") is committed to protecting your privacy. This Privacy Policy explains how we handle information in connection with the Paraflow Android application (the "App").

## 1. Private On-Device Speech Processing
Paraflow downloads its speech-recognition model from Hugging Face during initial setup. After the model is installed, speech recognition runs entirely offline.
* **No Cloud Transmission:** All speech-to-text recognition is processed locally on your device. Your audio recordings, voice data, and transcribed text are never transmitted to the cloud, our servers, or any third party.
* **Model Download Only:** Internet access downloads public Parakeet ONNX files from `huggingface.co`. These requests contain no voice, transcription, history, or other user content. Partial downloads may resume and completed files are verified before use.

## 2. Information We Access and How We Use It
To provide offline dictation, the App requests specific system permissions. We use these permissions strictly for the following purposes:

* **Microphone Access (`RECORD_AUDIO`):** Required to capture your voice input so the on-device speech-to-text engine can transcribe it. Audio is processed in temporary memory and is immediately discarded after transcription is complete.
* **Accessibility Service (`AccessibilityService`):** Used solely to detect when a text field is focused, show a floating dictation button, and paste the transcribed text directly at your cursor. We do not monitor, log, or store any text or window content other than to execute the copy-paste action at your command.
* **Battery Optimization Exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`):** Used to keep cross-app dictation available reliably while the app is in the background.
* **Internet and Network State (`INTERNET`, `ACCESS_NETWORK_STATE`):** Used to download the public model files and report download progress. No user content is included in these requests.

## 3. Data Storage
* **Local History:** Your transcription history is stored locally on your device in the App's private directory. 
* **User Control:** You can view, copy, or permanently clear your transcription history at any time from the "Usage" tab in the App. If you uninstall the App, all local history is permanently deleted by the Android system.

## 4. On-Device Translation, Grammar Correction, and No Tracking
Optional translation uses Google ML Kit language packs, and optional grammar correction uses local intermediate translation loops (using Spanish, French, and German). All translation, grammar correction, and transcription processing are performed entirely offline on your device. No text is sent to any cloud services. Paraflow does not include advertising, analytics, accounts, or telemetry.

## 5. Children's privacy
The App is not directed specifically to children. Paraflow does not transmit personal information off-device or operate user accounts.

## 6. Changes to This Privacy Policy
We may update this Privacy Policy from time to time. Changes may be published on the public policy page and included with future App updates.

## 7. Contact Us
If you have any questions or feedback about this Privacy Policy, please contact us at:
* **Developer/Support Email:** realiefan@gmail.com
* **GitHub Repository:** https://github.com/iefanx/parakeet
