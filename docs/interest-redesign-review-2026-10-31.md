# LinkMe2.1 interest redesign — review and verification

Scope: the user-authorized interest refactor on `codex/interest-discovery`, based on `edc028e` (LinkMe2.0). Existing authentication, private profiles, messaging, history and blocking are retained. The historical full-codebase review is in `code-review-2026-10-30.md`.

## Result

The old eight-choice taxonomy is replaced by a local catalog of148 topics (141 concrete topics) in12 groups, selected English/Chinese aliases, and free custom labels. A person can keep16 topics, each1–40 UTF-16 units. Search, normalization, suggestions, stable lookup keys and matching live behind the pure `InterestCatalog` module.

Introductions and three optional scene questions offer ideas with visible source labels. Suggestions remain unselected until tapped. Scene answers stay local to the editor; only explicitly selected labels are returned. There is no remote AI, paid API, chat/contact profiling or background inference.

Profile editing receives a local draft and publishes only on Save profile. Discover editing changes only its search filters. Back cancels the draft; rotation preserves search, choices and the selected mix.

## Findings addressed

| Finding or limitation | Resulting behavior |
| --- | --- |
| Fixed broad interests excluded specific and custom identities | Specific catalog topics and freely named custom interests are searchable, removable and combinable. |
| Entering interests required starting from a blank self-description | Introduction mentions, optional scenes and related categories offer explicit suggestions; selected groups contribute diverse ideas. |
| Alias/case/Unicode variants could fragment matching | Canonical topic identities and normalized custom SHA-256 keys deduplicate equivalent labels while retaining custom display wording. |
| Shared custom topics with different case appeared again under Also into | Subtraction now uses the same normalized match identity as the shared-topic explanation. |
| Interest lookup hints could be mistaken for match evidence | Ranking, exact-topic explanations, related-group explanations and starters derive from labels. Stored keys grant neither evidence nor authorization. |
| Old clients could overwrite a new interest profile without keys | Once keys exist, a replacement omitting them is denied; the private/public batch rolls back together. Legacy six-field profiles remain valid. |
| Fixed search/header/footer left about49dp of usable short-screen body | Short-height search and count now share the content ScrollView; toolbar/footer remain reachable. Scrolled captures prove complete selected chips and Create controls. |
| Guided exploration was buried below recommendations | Help me find my interests appears immediately after the selected mix, before recommendations. |
| Profile saved feedback was clipped above a navigation rail | Only bottom navigation anchors the snackbar; rail windows use the native bottom position. |

The independent whole-change code review found no P0–P2 issues. Its P3 shared-custom-topic duplication finding was verified and repaired. The rendered review requested the short-height correction, earlier exploration entry and design documentation, then found the rail feedback issue in newly supplied integration captures. Final rendered disposition: **ship**, limited to the scored fixes and reviewed interest-related editor/Profile/Discover surfaces. All five requested fix/evidence checks were resolved, including readable rail feedback and settled compact chips/Edit controls.

## Validation

- 54 JVM tests pass: catalog/custom validation, aliases, Unicode, literal/negated mentions, scene suggestions, group diversity, key bounds, exact-vs-related ranking and existing message/history logic.
- 177 official Firebase emulator cases pass (root verification10,654.605ms), including every16-label/30-key position, accepted maximum-size create/replace/atomic private+public saves, malformed keys, legacy fallback and old-client rollback. All prior access, message identity, fresh-summary and blocking regressions remain covered.
- 15 complete phone flows pass after the final navigation-feedback change, including custom search, rotation/cancel, publication-versus-filter separation and existing authentication/chat/history/offline/blocking flows.
- Additional two-flow runs pass on normal phone, font1.3, short landscape at font1.3 and tablet. Final compact/tablet save-and-filter regressions also pass1/1 each. Normal debug and optimized unsigned release builds pass; both select production project `linkme-e0b8d` with emulator endpoints disabled. Debug signature and version2.1/code3 are verified. Lint reports0 errors and20 warnings (existing target/dependency/resource/localization notices).

Meaningful RED runs preceded the domain and schema implementation. The canonical-policy expansion first failed8 of164 cases; stricter canonical-key and maximum-size coverage first failed14 of177 before the final177/177 pass. A rendered short-height regression and independent native review exposed the viewport issue. The native visibility matcher alone did not detect the rail snackbar's clipped glyphs; the saved before/after frames and independent rendered verdict establish that correction.

The test-only rules fixture is byte-identical to `firestore.rules`:10,984 bytes, SHA-256 `298b5b565ce300da32c0753bac087e004e7852030b089b4f54e2995460a97f8d`.

## Backend and delivery

The new interest-key composite index is Enabled; all seven earlier indexes are retained. The exact tested rule candidate is prepared in the existing `linkme-e0b8d` console but **has not been published**. Automatic approval review rejected publication because the earlier explicit approval covered the old69-test rules, not this exact177-test interest-key/compatibility policy. Explicit approval for this file and its production impact is required; no alternate deployment route was attempted. Spark is unchanged. There are no production QA account/message writes, no data migration/deletion and no billing upgrade.

Until that policy is published, the production backend rejects2.1 profile saves containing interest keys. The installable package is ready for the approved rollout. Private-profile/message authorization is unchanged by the candidate; older2.0 replacement saves are denied only after a profile contains2.1 keys, preventing silent loss.

The final normal debug/release configuration, signature, APK hashes and complete check results are recorded in ignored `artifacts/2.1/verification/summary.json`. The debug APK is installable; the optimized release APK is unsigned. Earlier2.0 deliverables are preserved. No GitHub push or store publication is part of this delivery.

## Practical limits

Discovery merges two server queries capped at60 each, deduplicates by UID, ranks actual overlap and shows at most60 people. Each query has at most30 terms. This is a bounded candidate sample, not exhaustive ranking or a compatibility percentage.

The catalog and suggestions are deterministic and local. Unknown custom topics match through normalized exact wording; related-category connections require a known catalog category. Selected Chinese aliases support search; the interface itself remains English. No preference-learning or recommendation-quality improvement is claimed without user evidence.

Users choose whether to publish a Discover profile. Private contact fields remain private; existing accounts are not automatically copied into the public directory. Version2.0 must update to edit a profile already saved with2.1 keys; existing data is retained. Spark quotas still apply and do not trigger an automatic paid upgrade.
