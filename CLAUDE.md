# Typewriter keyboard: build rules (root)

Read this whole file before every task.

## 1. What it is

An Android keyboard (an IME) that looks, sounds and feels like a manual typewriter: round
black keys with chrome rims, cream serif legends, typebar rods behind the keys, a red
backspace, and a paper strip where a held key "stamps" its letter. Built by Lester as a
side venture. The look comes from a concept render (not ours: never copy it into the app).

## 2. Non-negotiables

1. **No INTERNET permission, ever.** Nothing typed can leave the phone; this is the product's
   privacy promise. CI fails the build if the APK asks for it. Before adding any library,
   check the merged manifest: SDKs like Firebase and ad networks add it quietly. So: no ads,
   no analytics, no crash reporters that phone home.
2. **Never log, store or send typed text.** The only log allowed is the debug-build key
   position log (tag `TWKEYS`) used by the virtual phone: positions only.
3. **Password fields and `IME_FLAG_NO_PERSONALIZED_LEARNING`:** no suggestions, no learning.
4. **Nothing is allocated while drawing or touching.** Paints, Paths and Shaders are built in
   `KeyArt.prepare` (on size change); `onDraw` and `onTouchEvent` only reuse them.
5. **Cheap phones first.** minSdk 26. Debug APK under 15 MB, shrunk APK under 4 MB.
6. **Pure logic has no Android imports and has unit tests** (layout maths, shift state, and
   later suggestions). Add a test with every change to them.
7. Keep the welcome screen and the keyboard working on gesture and three-button navigation:
   Android 15+ draws keyboards edge to edge, so the keyboard pads itself by the system-bar inset.

## 3. How we build (the Linda way)

No Android Studio: GitHub Actions is the build machine.
- Claude works on a branch and opens a pull request; Lester reviews and merges.
- `build-apk.yml` (every push): unit tests, debug APK, shrunk APK, the no-internet check and
  the size budget. On the default branch it publishes the `latest-build` pre-release
  (`typewriter.apk`) that Lester installs on his phone. The committed `android/debug.keystore`
  makes every build install over the last. versionCode = the GitHub run number.
- `virtual-phone.yml` (pull requests that change the app, or by hand): an Android 15 emulator
  runs `tools/virtual-phone/tour.py`, which switches the phone to the keyboard, taps real keys,
  checks the typed text and screenshots each step into the `virtual-phone-latest` pre-release.
  When the look changes, open the screenshots and look at them before saying it works.
  The emulator has no sound or vibration.
- Google Play (later) needs a private upload key stored as a GitHub secret and a workflow that
  builds a signed `.aab`. The debug key never goes to Play.

## 4. Code layout

```
android/app/src/main/java/com/lester/typewriter/
  core/theme/            Machines.kt: colour palettes (themes are data)
  features/keyboard/     TypewriterIME (service + typing logic), TypewriterKeyboardView
                         (touch + drawing), KeyArt (painting), KeyboardLayout (rows as data),
                         KeyboardGeometry (positions + nearest key), ShiftState, KeyFeedback
  features/onboarding/   MainActivity + WelcomeScreen (Compose): turn on, pick, try it
android/app/src/test/    JVM unit tests for the pure logic
tools/virtual-phone/     emulator tour (adb + Python standard library only)
```

## 5. Design decisions so far

- Keys are drawn in code (gradients for the rim, enamel, gloss and shadow), not images.
- Round caps, but touches go to the nearest key edge, so the gaps between caps never swallow a tap.
- A pressed key lights its typebar rod and stamps its character on the paper strip: that is the
  key preview, above the finger.
- Legends are always capitals; the shift key shows the state (outline = off, filled = next
  letter, filled + bar and held down = shift lock).
- Letters type on release (a slide to a neighbour changes the choice); delete acts on press and
  repeats while held.
- Enter does the box's action (Search, Send, Next...) and falls back to a new line.
- Sound is Android's own key click for now; recorded typewriter sounds come later.

Planned next: a night theme, numbers and symbols page, real typewriter sounds, long-press
accents, word completion (no auto-replacing autocorrect in v1), emoji.

## 6. Toolchain (pinned; do not upgrade unless a build fails because of it)

AGP 8.13.2 · Gradle 8.14.3 (wrapper) · Kotlin 2.2.21 · JDK 17 · compileSdk/targetSdk 36 ·
minSdk 26 · Compose BOM 2024.12.01.
