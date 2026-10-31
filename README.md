# LinkMe 2.1

Native Android app for meeting people through shared interests and continuing in a private one-to-one conversation. Kotlin Views, Material 3, Firebase Authentication and Cloud Firestore.

**Rollout status:** The approved2.1 Firestore policy was published and verified October31,2026 at00:52 America/Chicago. All eight indexes are Enabled, existing data is retained and Spark remains $0/month. The local APKs and verification evidence are ready.

## What's included

- A searchable148-topic catalog in12 groups, custom interests, removable16-topic mixes and selected English/Chinese search aliases.
- Explicit suggestions from an introduction and three optional scene questions; each suggestion explains its source and requires selection.
- Discovery separates exact shared topics from related categories, with introductions and evidence-based conversation starters. Profile interests and discovery filters are edited separately.
- Real-time text chat, named inbox, server timestamps, bounded history pagination and recovery of older legacy conversations.
- Profile editing with acknowledged saves, an optional public Discover profile, private contact details and block/unblock controls.
- Email/password registration, sign-in, password recovery and sign-out.
- Light/dark themes, large-text layouts, phone bottom navigation and a navigation rail on windows at least 600dp wide.

Discovery merges two bounded server queries (up to60 new-key profiles plus60 legacy-label profiles), deduplicates people, ranks actual label overlap and shows at most60. It is not exhaustive global ranking or a compatibility score. Old tags remain supported; aliases normalize to stable identities. Custom labels retain their wording and match across case, Unicode normalization and whitespace. Suggestions run locally without remote AI or chat/contact profiling.

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

The ignored `artifacts/2.1/` directory holds `LinkMe-2.1-debug.apk`, an unsigned optimized release APK, `SHA256SUMS.txt`, native screenshots and `verification/summary.json`. The installable normal APK connects to the existing production Firebase project and contains no seeded sample users. Preview screenshots are explicitly labeled sample data. Earlier2.0 artifacts are retained.

Verification for this refactor covers54 unit tests,177 security cases and15 device tests covering the phone flows. Additional capture workflows cover dark mode, font scale1.3, short landscape and tablet. See the current review for final build, signature, lint and rollout evidence.

## Backend and migration

The existing Firebase project remains active on **Spark, $0/month**. No billing account, paid service, Cloud Storage, SMS auth or Functions was added. Free quotas still apply; exhausted quotas produce a recoverable error instead of an automatic billing upgrade.

The approved restrictive policy was deployed October30,2026. The2.1 interest schema was explicitly approved, published and verified October31 at00:52; all eight indexes, including the new interest-key index, are Enabled. The security document records the exact deployed hash. Existing data remains in place. Version1.0 clients must update because their unbounded/private-directory queries are denied; version2.0 clients must update to edit profiles already saved with2.1 interest keys.

`Users/{uid}` is private. Users explicitly publish only their safe name, introduction and interests to `PublicProfiles/{uid}` when saving their profile. There is no automatic public migration of old accounts. Historical private messages remain readable to their sender and recipient; a peer without a public profile appears as “LinkMe member.” New messages and their inbox summaries are written atomically and validated together.

See [security and deployment details](docs/firebase-security.md), [interest refactor review and verification](docs/interest-redesign-review-2026-10-31.md), [product scope](PRODUCT.md) and [design system](DESIGN.md).
