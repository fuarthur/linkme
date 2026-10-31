# Firebase security and deployment verification

The latest production verification on **October31,2026 at00:53 America/Chicago** confirmed the explicitly approved2.1 policy in `linkme-e0b8d`, all8 indexes Enabled, four denied anonymous probes and Spark at $0/month. The deployed rule text is10,984 bytes and exactly matches the177-test candidate, SHA-256 `298b5b565ce300da32c0753bac087e004e7852030b089b4f54e2995460a97f8d`. Existing production documents are retained. The final rollout record is at the end; the October30 snapshot below is historical. No billing, paid Storage, Functions or SMS dependency was added.

## Previous production deployment — October30 historical snapshot

The main task published and verified this production snapshot at **2026-10-30 23:04 America/Chicago**:

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

The2.1 policy requires the six existing public fields: `uid`, `username`, `bio`, `interests`, `discoverable`, and `updatedAt`. The only optional field is `interestKeys`. Email, phone, gender, arbitrary roles, and every other additional field remain denied. Names have 1–40 UTF-16 code units and bios 0–240. Publication uses a server timestamp. Legacy accounts publish only after saving their own profile; there is no blanket copy from private `Users` documents.

Interests are now 1–16 unique string labels, each 1–40 UTF-16 code units. Custom topics are accepted. Every possible element is checked; C0/C1 controls and labels made entirely of Unicode whitespace or format characters (including zero-width text) are denied. The app performs NFKC, Unicode whitespace, alias, and case normalization before publication; the rules independently enforce structural safety and exact-string uniqueness without attempting to reproduce the catalog.

Optional `interestKeys` must contain 1–30 unique strings, with every element checked. Accepted keys are ASCII `t:<slug>` and `g:<slug>` (a 1–70-character lowercase alphanumeric/hyphen slug with alphanumeric endpoints), or `c:<64 lowercase hexadecimal characters>`. Thus every key is nonempty and at most 72 UTF-16 code units. Unicode custom labels remain supported through their ASCII SHA-256 keys. These values are candidate lookup hints; rules do not enforce an exact catalog mapping or label/hash relationship. Keys grant no authorization and cannot be trusted as evidence of overlap. The app derives match explanations from normalized labels.

Legacy six-field public documents remain valid and can be read or updated by the same authorized actors. Once a document contains `interestKeys`, its resulting update document must retain valid keys. Old 2.0 replacement saves that omit them are denied; a private/public profile batch then fails atomically, retaining both previous documents. Partial edits that preserve existing keys remain possible. Private owner access, contact separation, visibility, message participants, linked summaries, and blocking are unchanged.

New messages contain exactly `senderId`, `receiveId`, `content`, `timestamp`, `conversationId`, and `members`. Sender identity must match authentication, the recipient must be a different user with a public profile, the two member IDs must match those identities, and the message must contain at least one non-whitespace character with at most 2,000 characters. The Android client trims input. Message timestamps use `FieldValue.serverTimestamp()`.

A send atomically writes the message and its `Chats/{conversationId}` summary. Summaries contain exactly `members`, `lastMessage`, `lastSenderId`, `updatedAt`, and `lastMessageId`. The client puts the new `Messages` document ID in `lastMessageId`; the existing message schema does not change. Summary members must be in ascending UID order and cannot be reordered or replaced.

Both sides verify the link. `freshMessageForSummary` requires the referenced message not to exist before the write and reads its post-batch document with `getAfter()`. Its conversation ID, members, content, sender, and request timestamp must match the summary. Conversely, `matchingSummary` requires `summary.lastMessageId` to equal the actual new message document ID in addition to matching participants/content/sender/time. Therefore summary-only creation or updates, reusing an old message, and authorizing two different messages with one summary are denied.

`conversationKey` independently recomputes the same key as Android: sort the two UIDs with string comparison, concatenate each UTF-16 length plus `:` plus that UID, hash the UTF-8 string with SHA-256, and encode lowercase hexadecimal. Both the `Chats` document path and each message’s `conversationId` must equal that key. A caller cannot occupy somebody else’s pair key using different members or flood duplicate summaries under arbitrary IDs.

Existing four-field legacy messages remain readable by their original sender/recipient without migration or rewriting. Older four-field summaries also remain participant-readable. The next valid message can replace an older summary with the five-field linked schema while retaining the same sorted members and the same canonical ID; no ID migration is required. Private `Users` and public profile read/write authorization is unchanged.

## Query indexes

`firestore.indexes.json` now defines eight composite indexes. The candidate adds only `PublicProfiles(discoverable ASC, interestKeys ARRAY_CONTAINS)` and retains all seven existing definitions.

- Interest-key discovery (new candidate): `discoverable ASC`, `interestKeys ARRAY_CONTAINS`.
- Legacy visible discovery (retained): `discoverable ASC`, `interests ARRAY_CONTAINS`.
- A conversation in both directions: `senderId ASC`, `receiveId ASC`, `timestamp DESC`.
- Legacy sent inbox: `senderId ASC`, `timestamp DESC`.
- Legacy received inbox: `receiveId ASC`, `timestamp DESC`.
- Durable summaries: `members ARRAY_CONTAINS`, `updatedAt DESC`.

Conversation and legacy inbox queries use descending `timestamp`, then descending document ID, and `limit(100)`. The final timestamp direction supplies the matching implicit `__name__ DESC` index ordering. Discovery uses a visible `interestKeys array-contains-any ...` query and a separate visible legacy `interests array-contains-any ...` query, each with `discoverable == true` and `limit(60)`. Each filter contains at most 30 terms; empty filters short-circuit in the app. Results are merged by UID and ranked from actual label overlap, with at most 60 shown. This bounded search is not exhaustive global ranking. Chat-summary queries use `members array-contains currentUid`, descending `updatedAt`, and `limit(100)`.

The retained definitions are `Messages(receiveId ASC, senderId ASC, timestamp ASC)` and `Users(interests ARRAY_CONTAINS, uid ASC)`.

The Firestore emulator verifies security authorization for those query shapes but does not enforce production composite-index availability. The October31 live console check confirmed all eight indexes Enabled, including the retained seven and new `CICAgJiUsZIK` interest-key index. This does not imply the new policy is published; see the checkpoint below.

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


## Interest-discovery candidate verification (local only)

The October 31, 2026 candidate has **10,984 ASCII characters/bytes**, SHA-256 **`298b5b565ce300da32c0753bac087e004e7852030b089b4f54e2995460a97f8d`**. The root policy and `app/src/androidTest/assets/firestore.rules` are byte-identical. The previously verified production snapshot above is historical; this security task did not deploy this candidate, create a production index, access production data, or change billing.

Before changing the canonical 7,194-character policy (`e989f86356cc6a6fb068eee64328dbfc75a1a0bd4c024cd51c09176e829087c6`), the expanded suite ran **164 cases: 156 passed and 8 failed** for the intended gaps. Six accepted custom/key-bearing/maximum-size or partial-edit cases were denied by the old schema, while two old-client downgrade denial assertions failed because the writes incorrectly succeeded. The original 79 cases are retained apart from replacing the superseded unknown-interest denial with the 17-label bound; accepting that formerly unknown custom interest is now covered explicitly.

An initial generic per-element validator exceeded Firestore's 1,000-expression limit at maximum size. The final flat validator uses one string-only ASCII pattern per key to enforce the approved prefix/length/type contract, including custom hash keys, and preserves Unicode display labels. The 13 malformed canonical-key cases were first observed incorrectly succeeding against the generic validator; that RED run completed **177 cases: 163 passed and 14 failed**, including the still-failing maximum-size write. `List.join` was investigated but rejected: the emulator demonstrated that it converts number, boolean and null elements into strings. It is not used to validate this schema.

The final full verification completed **177 passed, 0 failed, 0 skipped**, exit code 0; the test process reported **11,601.767 ms**. The accepted boundary case executes all three real paths: a maximum-size replacement, a new document creation, and the Android-style atomic `Users` merge plus `PublicProfiles` set with 16 labels and 30 keys. It also verifies that contact fields survive and both batch timestamps agree. Coverage includes every one of the 16 interest and 30 key positions, 40-unit Unicode labels, 72-unit keys, malformed types/lengths/control characters/blank text, exact public allowlisting, partial edits, explicit key deletion, old-client replacement and whole-batch rollback, legacy profiles and discovery fallback, and visible/bounded/authenticated key queries. Existing private-access, identity, participant, fresh-summary, blocking and visibility policy segments were checked byte-for-byte against the canonical policy and are unchanged.

The final local session then ran `seed-preview.mjs --reset` and an immediate normal rerun. Both seeded seven synthetic profiles and two conversations successfully. Readback verified all requested specific tags and stable keys, six visible profiles plus the hidden Indie games profile, the expected Indie games/Cooking/Astrophotography and broader music query results, hidden-profile denial and private-contact separation. Synthetic profile publication now merges private fields and writes the public document in one batch. All of this used only `demo-linkme` on the two loopback emulators; the CLI shut them down after verification.

The index file was checked to contain exactly the seven previous definitions plus the single new `PublicProfiles(discoverable ASC, interestKeys ARRAY_CONTAINS)` definition, with unchanged field overrides. The emulator does not prove production index readiness. Rollout verification of that eighth index and the exact candidate hash remains a separate step.

Reproduce the complete security suite with the existing `npm test` command documented above. Local ignored evidence is retained in `firebase-tests/interest-red.log`, `firebase-tests/interest-key-shape-red.log` and `firebase-tests/interest-final-local.log`. Rebuild the Android test APK so the fixture loaded by `RegistrationFlowTest` includes this exact policy.

## October31 pre-approval checkpoint — historical

The root task created `PublicProfiles(discoverable ASC, interestKeys ARRAY_CONTAINS)` in the existing project. The console reported creation complete at00:33:53 America/Chicago, and its table then showed all8 indexes Enabled. No earlier index was removed. The project continues to display Spark; billing and production documents were not changed.

The complete rule text was copied back from the visible editor and exactly matched the10,984-byte tested file, SHA-256 `298b5b565ce300da32c0753bac087e004e7852030b089b4f54e2995460a97f8d`. The Publish action was rejected by automatic approval review: the prior explicit69-test approval did not cover this177-test interest-key and old-client compatibility policy. No CLI or indirect deployment was attempted. The editor still shows unpublished changes, with the prior published version starred at Yesterday •11:04PM. The canonical production snapshot recorded above remains the last verified published policy.

All local work is ready:54 logic tests,177 authorization cases,15 device tests covering the phone flows, four2-flow viewport runs and final compact/tablet save-feedback regressions pass. Both normal APK variants use `linkme-e0b8d` and disable emulators; the debug APK signature is verified. At that checkpoint, production2.1 profile saves containing `interestKeys` remained unavailable until publication.

Approval covers allowing owner-saved custom interests (up to16) and validated lookup keys, plus denying2.0 replacement saves that omit keys on an already-upgraded profile. Existing data is retained, private profile/message access is unchanged, and Spark remains the free plan. Local proof images and machine-readable state are in ignored `artifacts/2.1/verification/`. The explicitly approved publication and final live verification are recorded next.

## Final2.1 production verification — October31

After the user explicitly approved the exact177-test rule file and its compatibility impact, the same console Publish action succeeded at **00:52 America/Chicago on October31,2026**. The console now stars Today •12:52AM and no longer shows unpublished changes. The entire published editor text was copied back through the visible UI and exactly matches the approved source:10,984 bytes, SHA-256 `298b5b565ce300da32c0753bac087e004e7852030b089b4f54e2995460a97f8d`.

| Check | Final live result |
| --- | --- |
| Project | `linkme-e0b8d` |
| Rule candidate | Exact approved177-test hash published |
| Composite indexes | All8 Enabled, zero Building; seven previous indexes retained |
| New interest-key index | `CICAgJiUsZIK`, Enabled |
| Billing | Spark, No-cost ($0/month), unchanged |
| Anonymous `_linkme_health_probe/status` | HTTP403 |
| Anonymous `Users/_linkme_health_probe` | HTTP403 |
| Anonymous `Messages/_linkme_health_probe` | HTTP403 |
| Anonymous `PublicProfiles/_linkme_health_probe` | HTTP403 |
| Production QA document/account writes | None |
| Existing data | Preserved; no migration or deletion |

The anonymous checks were single-document reads of nonexistent probe paths, without credentials or production data retrieval. The successful owner custom-save paths are covered by the177-case emulator suite and15-test device suite; no test profiles were inserted into production. The2.1 schema and index prerequisites are now live. Older2.0 clients must update to edit a profile already containing2.1 keys; this prevents silent replacement with the older schema.

Proof images, anonymous response codes, exact package flags, signatures, hashes and the combined verification summary are preserved in ignored `artifacts/2.1/verification/`. The earlier automatic-review rejection was resolved by the user's explicit approval; no alternate deployment path was used.
