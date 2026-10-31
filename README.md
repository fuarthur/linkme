# LinkMe 2.0

Native Android app for meeting people through shared interests and continuing in a private one-to-one conversation. Kotlin Views, Material 3, Firebase Authentication and Cloud Firestore.

## What's included

- Multiple-interest discovery with explicit common-interest counts, introductions and conversation starters.
- Real-time text chat, named inbox, server timestamps, bounded history pagination and recovery of older legacy conversations.
- Profile editing with acknowledged saves, an optional public Discover profile, private contact details and block/unblock controls.
- Email/password registration, sign-in, password recovery and sign-out.
- Light/dark themes, large-text layouts, phone bottom navigation and a navigation rail on windows at least 600dp wide.

Discovery is a bounded sample of up to 60 eligible profiles, ranked by shared interests within that sample. It is not a global recommendation or compatibility score. Existing interests remain the eight original categories; old Sport/Sports and Movie/Movies values are normalized.

## Build

Use JDK 17, Android SDK 35 and Android Studio or the included Gradle wrapper. Set `sdk.dir` in your local, ignored `local.properties`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew assembleRelease
```

The normal debug/release builds use the existing `linkme-e0b8d` project. `assembleRelease` produces an unsigned release APK; configure a private release signing key separately for distribution. Do not commit signing keys or passwords.

## Local preview and tests

The emulator variant is opt-in and selects a separate, synthetic `demo-linkme` Google Services configuration **before** Android initializes Firebase. Release always keeps emulators disabled. No production accounts or documents are needed for the device tests.

Use Node 24.12+ and JDK 21 for the Firebase emulators:

```sh
cd firebase-tests
npm ci
npm test
```

For native preview, start the emulators from the repository root:

```sh
firebase-tests/node_modules/.bin/firebase emulators:start --only auth,firestore --project demo-linkme --config firebase.json
```

In another terminal, seed synthetic data, then run against an Android emulator (replace the device serial with your own):

```sh
node firebase-tests/seed-preview.mjs --reset
ANDROID_SERIAL=emulator-5592 ./gradlew -PuseEmulators=true connectedDebugAndroidTest
./gradlew -PuseEmulators=true assembleDebug
```

`--reset` clears only the literal localhost demo emulator data. Preview sign-in: `alex@linkme.example.test`; password is ` linkme-preview-pass `, including both spaces. The preview build displays a sample-data banner. Never use these accounts or this password in production.

The device suite temporarily changes the **local demo** rules for registration failure cases, then restores the bundled restrictive fixture. Do not run security tests, seeding and device tests simultaneously against the same emulator ports.

## Verified local delivery

The `artifacts/` directory is ignored by Git and contains the installable `LinkMe-2.0-debug.apk`, an unsigned optimized release APK, `SHA256SUMS.txt`, native screenshots and `verification/summary.json`. The delivered normal APK connects to the existing production Firebase project; it contains no seeded sample users. Local preview screenshots are explicitly labeled sample data.

Verified:27 unit tests,79 security cases and11 device flows pass. Android lint has no errors and20 warnings, documented in the review. Debug signing and both variants' production project/emulator flags were checked.

## Backend and migration

The existing Firebase project remains active on **Spark, $0/month**. No billing account, paid service, Cloud Storage, SMS auth or Functions was added. Free quotas still apply; exhausted quotas produce a recoverable error instead of an automatic billing upgrade.

The approved restrictive rules and five additional indexes were deployed October 30, 2026; all seven indexes, including the two original indexes, are enabled. Existing data remains in place. Version 1.0 clients must update because their unbounded/private-directory queries are now denied.

`Users/{uid}` is private. Users explicitly publish only their safe name, introduction and interests to `PublicProfiles/{uid}` when saving their profile. There is no automatic public migration of old accounts. Historical private messages remain readable to their sender and recipient; a peer without a public profile appears as “LinkMe member.” New messages and their inbox summaries are written atomically and validated together.

See [security and deployment details](docs/firebase-security.md), [review and verification](docs/code-review-2026-10-30.md), [product scope](PRODUCT.md) and [design system](DESIGN.md).
