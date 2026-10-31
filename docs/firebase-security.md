# Firebase security and deployment verification

The canonical restrictive Firestore policy is deployed to the existing `linkme-e0b8d` project. The final production check on October 30, 2026 at 11:04 PM America/Chicago confirmed the published rule source, enabled indexes and Email/Password authentication, and denied anonymous probes. Firebase remains on Spark at $0. The implementation uses Authentication, Firestore, local test emulators and initial avatars, with no Cloud Functions or paid Storage dependency.

## Verified production deployment

The main task published and verified this final state at **2026-10-30 23:04 America/Chicago**:

| Check | Verified result |
| --- | --- |
| Firebase project | `linkme-e0b8d` |
| Published rule length | 7,194 characters |
| Published rule SHA-256 | `e989f86356cc6a6fb068eee64328dbfc75a1a0bd4c024cd51c09176e829087c6` |
| Firestore composite indexes | All 7 Enabled: 5 new indexes and 2 retained indexes |
| Firebase Authentication | Email/Password Enabled |
| Billing plan | Spark, $0; billing unchanged |
| Anonymous `_linkme_health_probe` request | HTTP 403 |
| Anonymous `Users` request | HTTP 403 |
| Anonymous `Messages` request | HTTP 403 |

The published policy includes canonical conversation identity and reciprocal fresh-message/summary linkage. Existing private user data and legacy messages were preserved. This record distinguishes the live deployment checks above from the local regression evidence below.

## Access contract

| Path | Reads | Writes |
| --- | --- | --- |
| `Users/{uid}` | Owner document get only; collection lists denied | Owner only; existing contact/profile fields can be preserved with merge writes |
| `Users/{uid}/Blocked/{peerUid}` | Owner only, including lists | Owner create/delete; exact `createdAt` server timestamp; no self-block or update |
| `PublicProfiles/{uid}` | Signed-in readers may get visible profiles; owner may also get their hidden profile; lists must select visible profiles and limit to 60 | Owner create/update using the exact safe schema; deletion denied |
| `Messages/{id}` | Existing sender or recipient only; lists limited to 100 | Valid new sends only; updates and deletions denied |
| `Chats/{id}` | Members only; lists limited to 100 | Canonical pair ID, sorted immutable members, current sender/server timestamp and a linked newly created message; deletion denied |

Unmatched documents are denied. Private profile ownership does not grant access to another user’s private document or block list. Blocking prevents both directions of new message and summary writes; it retains participant access to existing history.

Public profiles contain exactly `uid`, `username`, `bio`, `interests`, `discoverable`, and `updatedAt`. They cannot contain email, phone, gender, arbitrary roles, or additional fields. Names have 1–40 characters, bios 0–240, and interests are a unique list of 1–8 values from `Sport`, `Food`, `Dating`, `Games`, `Music`, `Anime`, `Movie`, and `Other`. Publication uses a server timestamp. Legacy accounts publish only after saving their own profile; there is no blanket copy from private `Users` documents.

New messages contain exactly `senderId`, `receiveId`, `content`, `timestamp`, `conversationId`, and `members`. Sender identity must match authentication, the recipient must be a different user with a public profile, the two member IDs must match those identities, and the message must contain at least one non-whitespace character with at most 2,000 characters. The Android client trims input. Message timestamps use `FieldValue.serverTimestamp()`.

A send atomically writes the message and its `Chats/{conversationId}` summary. Summaries contain exactly `members`, `lastMessage`, `lastSenderId`, `updatedAt`, and `lastMessageId`. The client puts the new `Messages` document ID in `lastMessageId`; the existing message schema does not change. Summary members must be in ascending UID order and cannot be reordered or replaced.

Both sides verify the link. `freshMessageForSummary` requires the referenced message not to exist before the write and reads its post-batch document with `getAfter()`. Its conversation ID, members, content, sender, and request timestamp must match the summary. Conversely, `matchingSummary` requires `summary.lastMessageId` to equal the actual new message document ID in addition to matching participants/content/sender/time. Therefore summary-only creation or updates, reusing an old message, and authorizing two different messages with one summary are denied.

`conversationKey` independently recomputes the same key as Android: sort the two UIDs with string comparison, concatenate each UTF-16 length plus `:` plus that UID, hash the UTF-8 string with SHA-256, and encode lowercase hexadecimal. Both the `Chats` document path and each message’s `conversationId` must equal that key. A caller cannot occupy somebody else’s pair key using different members or flood duplicate summaries under arbitrary IDs.

Existing four-field legacy messages remain readable by their original sender/recipient without migration or rewriting. Older four-field summaries also remain participant-readable. The next valid message can replace an older summary with the five-field linked schema while retaining the same sorted members and the same canonical ID; no ID migration is required. Private `Users` and public profile read/write authorization is unchanged.

## Query indexes

`firestore.indexes.json` defines seven composite indexes: five new v2 indexes below plus the two original indexes retained for compatibility.

- Visible discovery: `discoverable ASC`, `interests ARRAY_CONTAINS`.
- A conversation in both directions: `senderId ASC`, `receiveId ASC`, `timestamp DESC`.
- Legacy sent inbox: `senderId ASC`, `timestamp DESC`.
- Legacy received inbox: `receiveId ASC`, `timestamp DESC`.
- Durable summaries: `members ARRAY_CONTAINS`, `updatedAt DESC`.

Conversation and legacy inbox queries use descending `timestamp`, then descending document ID, and `limit(100)`. The final timestamp direction supplies the matching implicit `__name__ DESC` index ordering. Discovery uses `discoverable == true`, `interests array-contains-any ...`, and `limit(60)`. Chat-summary queries use `members array-contains currentUid`, descending `updatedAt`, and `limit(100)`.

The retained definitions are `Messages(receiveId ASC, senderId ASC, timestamp ASC)` and `Users(interests ARRAY_CONTAINS, uid ASC)`.

The Firestore emulator verifies security authorization for those query shapes but does not enforce production composite-index availability. The final live console check confirmed all seven production indexes Enabled, including the five versioned definitions above and two retained indexes.

## Isolated emulator workflow

Requirements: Node 24.12 or later and Java 21. The lockfile pins official npm packages: Firebase SDK 13.1.0, rules-unit-testing 6.0.0 and firebase-tools 15.34.2. Install from the official npm registry with `npm ci` in `firebase-tests`.

The root `.firebaserc` defaults to `demo-linkme`; the original `linkme-e0b8d` project exists only as the explicit `production` alias. `firebase.json` configures Authentication on `127.0.0.1:9099` and Firestore on `127.0.0.1:8080`, with the emulator UI disabled. Tests explicitly require those loopback emulator endpoints and project `demo-linkme`. No credentials or production data are needed.

From `firebase-tests`, the exact verification command used on this Mac is:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home npm test
```

To repeat the historical-policy regression demonstration locally:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home npm run test:baseline
```

The baseline fixture is explicitly unsafe and is loaded only by the test harness when `LINKME_RULES_MODE=open`. Production configuration references `firestore.rules`, never the fixture. Running baseline changes only the ephemeral `demo-linkme` emulator policy.

For Android debug builds using `-PuseEmulators=true`, both Firebase initialization and endpoint selection must target the demo project: project ID `demo-linkme`, Authentication `10.0.2.2:9099`, Firestore `10.0.2.2:8080`. Changing endpoints while retaining project ID `linkme-e0b8d` would test a different emulator project. Release builds must never enable emulator endpoints. To keep local emulators running for a device session, run from `firebase-tests`:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home npx firebase emulators:start --project demo-linkme --only auth,firestore --config ../firebase.json
```

## Regression evidence

The first run against the actual historical `allow read, write: if true` policy completed 69 tests: 8 passed and 61 correctly failed denial assertions with “Expected request to fail, but it succeeded.” The first restrictive-rules run passed all 69 tests. The final verification below additionally exercises explicit document-ID pagination, all eight canonical interests, field-length boundaries, multiline message acceptance, and chat-list limits.

The suite covers anonymous and cross-user reads/writes, private contact protection, exact public fields, malformed profiles/messages, sender forgery, timestamp spoofing, missing or mismatched summaries, member replacement/addition, immutable history, blocking in both directions, valid atomic sends, legacy participant and inbox queries, bounded discovery and chat queries, and a real email/password sign-up/sign-out/sign-in flow through the Auth emulator. Expected rejected attacks produce Firebase SDK permission-denied diagnostics; those are assertions, not test failures.

Initial clean verification:

```sh
cd /Users/arthurfu/Documents/linkme/firebase-tests
npm ci --no-audit --no-fund
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home npm test
```

Result: clean installation succeeded; **69 tests passed, 0 failed, 0 skipped**, exit code 0. The final test process reported 5,471 ms. `npm ls --depth=0` confirms only the three pinned top-level test dependencies. Baseline output is retained locally in ignored `firebase-tests/baseline.log`; the final successful output is in ignored `firebase-tests/secure.log`. The official CLI downloads Firestore emulator 1.22.0. npm emitted deprecation notices for transitive CLI packages; none prevented installation or testing.


## Canonical identity and fresh-summary fix

A focused review found that the initial member-only summary policy did not bind the `Chats` document ID to its participants and allowed summary-only writes. Six new exploit regressions were run against that exact 5,880-character policy before implementation. All six failed with “Expected request to fail, but it succeeded”: another participant occupying the Alice/Bob key, arbitrary duplicate pair IDs, summary-only create/update, old-message reuse, and two new message IDs sharing one summary.

The strengthened rules retain the existing access model and legacy reads, add server-verified canonical identity, and add only `lastMessageId` to the new summary schema. The original 69 authorization tests remain, including merge preservation of private legacy fields and a valid first send that upgrades an old summary without `lastMessageId`. Four additional accepted-send cases verify case ordering, accented/CJK IDs, emoji length, and emoji versus U+E000 ordering. These demonstrate that rules comparison and `size()` agree with Android UTF-16 sorting/length for the tested cases.

Primary references: [Firebase rules hashing](https://firebase.google.com/docs/reference/rules/rules.hashing) documents SHA-256 string input and UTF-8 encoding; [rules Bytes](https://firebase.google.com/docs/reference/rules/rules.Bytes) shows uppercase hexadecimal output; [rules String](https://firebase.google.com/docs/reference/rules/rules.String) documents `lower()`, `size()`, and integer-to-string conversion. The implementation uses `hashing.sha256(key).toHexString().lower()` and `string(uid.size())`. These operations and the comparisons were also executed in the official local Firestore emulator before changing the policy.

The Android QA emulators were already running, so the final focused verification reused their explicitly isolated demo endpoints:

```sh
cd /Users/arthurfu/Documents/linkme
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 node --test --test-concurrency=1 firebase-tests/security.test.mjs
```

Result: **79 passed, 0 failed, 0 skipped**, exit code 0; 5,208 ms for the test process. The six new denial regressions are green, all 69 earlier tests pass, and four canonical-key compatibility tests pass. Red/green outputs are retained locally in ignored `firebase-tests/canonical-red.log` and `firebase-tests/canonical-green.log`. The strengthened 7,194-character policy was subsequently published and verified live at the time recorded in Verified production deployment; Spark billing remained unchanged. The preview seed now supplies `lastMessageId` and creates only correctly linked local demo batches.
