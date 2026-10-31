# LinkMe code review and v2 upgrade

Reviewed baseline `ce640c9e6a87fd21952b0214f42a93978e655815` and the complete replacement on `codex/linkme-v2`. The repository was absent from the local locations searched, then cloned to `/Users/arthurfu/Documents/linkme`. This is a native Android upgrade preserving the interest-discovery and one-to-one chat idea.

## Findings and repairs

| Priority | Reproduced problem | Resulting behavior / evidence |
|---|---|---|
| P0 | Production Firestore allowed anonymous reads and writes across all collections. | Published approved participant/owner rules; anonymous nonexistent-path probes for Users, Messages and a health collection now return403. Existing data is retained. |
| P1 | Registration announced success before profile persistence; recreation could route away while that write was pending. | Registration waits for backend acknowledgement. A setup failure signs out with recovery instructions. A recreated activity retains the in-flight setup screen. |
| P1 | Discovery read private user records containing contact data. | Safe opt-in PublicProfiles collection, strict field allowlist, private Users documents; no automatic publication of old contacts. |
| P1 | Client-only message identity and arbitrary conversation summaries allowed spoofing or conversation-key squatting. | Sender equals authenticated UID; canonical SHA256 conversation ID, immutable sorted members, fresh lastMessageId and reciprocal atomic Message/Chat validation. Six final-review exploit cases fail under the earlier candidate and pass under the final rules. |
| P1 | Inbound chat did not update live; rapid or delayed sends could duplicate a message or erase a newer draft. | Lifecycle-managed realtime listeners, one pending send, stable retry ID, server timestamps and draft comparison on acknowledgement. |
| P2 | Live query windows displaced already loaded chat history; resuming after 100+ unseen messages could leave a gap. | Retained immutable history, rejected-pending cleanup, exact timestamp ordering and bounded “Load missed messages” recovery. Twelve focused history regressions. |
| P2 | A busy conversation could hide all older legacy contacts from the inbox. | Manual bounded inbox pagination recovers pre-v2 conversations without rewriting historical data. |
| P2 | Millisecond-only cursors skipped messages sharing a timestamp with finer precision. | Preserve Firestore seconds/nanoseconds plus document ID for exact pagination and deterministic order. Two failing-then-passing regressions. |
| P2 | Profile editing did not load existing values, silently failed for missing documents, or replaced unrelated fields. | Load/retry state, merge private fields, atomic private/public updates and acknowledged success; synthetic contact-field preservation check. |
| P2 | Duplicate launchers, forced Login startup, null auth/recipient dereferences and recursive navigation selection. | Single launcher, restored sessions, safe routing and separated programmatic selection from navigation callbacks. |
| P2 | Tab navigation was obscured by a save Snackbar; inbox names could be squeezed to zero width. | Snackbar anchored above navigation; timestamp label measures to its content. Native flow tests cover profile-to-Discover-to-Chats navigation. |
| P1 | A short landscape window with 1.3 font scaling left almost no discovery list viewport. | A native horizontal filter row and compact header preserve the list; the original capture regression failed before the change and passed after it. |
| P2 | Static layouts and weak state handling made empty, offline and error cases hard to recover from. | Consistent native forms, actionable empty/error states, retry, filter chips, server save feedback, private profile and block controls. |

## Verification

- Baseline: one example unit test, debug build and lint completed; 46 lint warnings.
- Domain: 27 unit tests pass, including 12 history and two exact-timestamp regressions.
- Security: 79 local emulator cases pass. The original public-rules fixture failed 61 denial checks; final-review hardening added six exploit regressions and four Unicode/hash compatibility checks.
- Android lint/debug build pass. Twenty remaining warnings are primarily newer available dependency/target versions, unused resources, one deliberate always-visible overflow control and one English-only string warning. No lint errors. Full localization and current Play policy certification are outside this local build.
- Native API34 emulator: the complete 11-test device suite passed; a separate short-window/font1.3 run verifies the repaired discovery viewport. Empty-chat tests exercise the starter, a real software keyboard and draft preservation. Android may use its native full-screen extract editor in landscape; the test accepts that documented system behavior and checks the restored app composer. See [Android InputMethodManager](https://developer.android.com/reference/android/view/inputmethod/InputMethodManager#isFullscreenMode()).
- Independent rendered review found the short-window discovery defect above, then confirmed its repair. Light/dark text pairs checked from source exceed4.5:1; final captures wait for opaque rows and rendering frames.

## Backend changes

Existing `linkme-e0b8d`, Spark no-cost plan; Email/Password enabled. First restrictive rule publication at10:44PM; final canonical/message-reference hardening published at11:04PM October30,2026 America/Chicago. Final source SHA256: `e989f86356cc6a6fb068eee64328dbfc75a1a0bd4c024cd51c09176e829087c6`. All five new composite indexes and two retained indexes show Enabled. No production users or messages were created for QA. No bulk migration, deletion, billing upgrade or replacement project.

## Product limits and follow-up

- This version keeps eight original interests, text conversations and initial avatars. Photos/Storage, SMS and Functions were omitted to keep the existing free-only constraint.
- Discovery fetches up to60 candidates before local common-interest ranking; it does not claim exhaustive or personalized matching quality. Inbox and chat read at most100 per request with explicit history pagination.
- Block management currently fetches the first100 blocked IDs. Server rules enforce every individual block regardless of UI pagination; a dedicated larger-list management flow remains future work.
- Private history is immutable from the client. Account deletion/export, moderation/report review, email verification requirements, groups, push notifications and Play Store release are separate product work.
- Actual two-way Firebase behavior was exercised using the local Auth/Firestore emulators. Production Auth was inspected, rules deployed and anonymous reads probed; no real-account login/send was fabricated.

## Delivered artifacts and limits

`artifacts/LinkMe-2.0-debug.apk` is installable and its Android v2 signature was verified. Its SHA256 is `a18e9efa75557c5e09316c3af2dcaaad80106cdb5627069d318cf80ba4b18b53`. Both debug and unsigned minified release builds use `linkme-e0b8d` with `USE_EMULATORS=false`. Detailed results, hashes, lint report, device XML and screenshots are in the ignored local `artifacts/` directory. The release build completes but needs a private distribution signing key before release; it was not installed as a signed release.

The independent UI review passed after the compact-window repair. Its practical limits are physical-device performance, interactive TalkBack testing and hardware gestures; those were not tested. Production Auth/sign-in/send were not exercised using a real user account. The confirmed live checks are the console state, index readiness, published rules and anonymous403 probes.

Release resource shrinking/R8 completed. Upstream Google/Android dependency keep rules emit forward-looking default-constructor warnings; no release build error occurred. No measured startup, battery or recommendation-quality improvement is claimed.
