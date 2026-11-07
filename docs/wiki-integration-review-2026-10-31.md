# LinkMe2.2 — OtherWise Wiki library integration review

The user requested the Wiki word pool already used in local OtherWise. Work is on `codex/wiki-interest-library`, based on `243d5e6`. The implementation bundles a public snapshot and extends the existing Android editor; OtherWise remains read-only.

## Shipped behavior

- 30,919 Wikidata-sourced topics across23 fields are searchable offline, with title, short definition, field and full details/source access. The718 separately authored OtherWise rows are outside this Wiki import.
- The existing148 starters and custom wording remain available. Browse Wiki topics and the search handoff open a dedicated multi-select picker. Query, field and mix survive recreation; Back cancels; explicit confirmation returns a draft to the parent. Profile Save owns publication and Discover filters remain separate.
- RecyclerView binds only visible rows. A device assertion checks30919 available items with at most30 attached child views. Parsing and search run on an owned background executor; generation checks reject older results and clearing the model cancels owned callbacks/work.
- 147 long titles and five broader legacy-alias meanings use short Wiki references. Complete labels render through a small synchronous map without loading the full index into profile screens. The five distinctions preserve Guitar versus Playing guitar, Football versus Soccer, Information security versus Cybersecurity, Weightlifting versus Strength training, and Open source versus Open-source software.
- All stored values retain the deployed40-unit/16-interest/30-query-key bounds. Ordinary literal labels retain prior custom hashes and safe starter synonyms. There is no new Firebase schema, rule, index, paid API, embedding service or OtherWise server dependency.

## Source and integrity

Only the public OtherWise `data/topics.json`, `data/catalog_metadata.json` and attribution README are read. Importer validation finishes before output replacement; invalid IDs/fields/provenance or duplicate identities fail rather than silently shrinking the pool. Output paths inside OtherWise are denied.

The deterministic topic gzip is1,157,819 bytes, SHA-256 `a9ce59f9e1b88f3c43be42c2160849593e68e0987d5f05f3dbec1cc66cb9a910`; it expands to3,495,903 UTF-8 TSV bytes. Per-record source/list revisions and the modified selection's credit/license notices are preserved alongside it. Metadata records the input/output hashes and transformations. Wikidata structured names/descriptions are credited as CC0; derived selection/grouping is distributed under CC BY-SA4.0. The app's Sources dialog exposes these credits and links; details expose each public Wikidata identity.

Android AAPT expands source `.gz` assets and removes their suffix in the actual APK. Inspection of the first device package exposed a loader-path mismatch. The Android repository now reads packaged `assets/wiki-topics/topics.tsv`; Python/JVM checks read the compressed source file. Both normal APK variants must pass the same package-entry/data-integrity checks at delivery.

## Review and verification

An independent whole-change review covered importer/provenance, index, reference identities, lifecycle/query guards, picker, parent callback, credits and tests. It found no actionable P0–P2 issues. A scoped follow-up cleared quantity-aware status, paragraph spacing and the stable loading footer.

Confirmed for the final delivery:

- 13 Python importer tests pass; exact-source `--check` and independent reviewer regeneration match byte-for-byte.
- 67 JVM tests pass, including eight index/data cases and five compatibility cases on top of the54 earlier tests.
- 21 complete-suite device tests pass. These include explicit Wiki drafts, cancellation, recreation, full16-item recovery, long-title parent round trips and an actual demo-backend save of a compact reference under unchanged rules. Before Save the public profile is unchanged; after Save the reference/key exists and email/phone remain absent.
- Final picker capture workflows pass for phone, font1.3, short landscape/font1.3 and tablet. Captures cover all-topic virtualization, search/keyboard, field filtering, checked selections, sources, empty, dark, full details, long title and parent entry.
- All177 security tests pass against the unchanged rule fixture. Normal debug and optimized unsigned release builds pass at version2.2/code4, production project `linkme-e0b8d`, emulators disabled. The debug APK signature and both packaged topic/provenance assets match their source bytes.
- Lint reports0 errors and21 warnings. Two Wiki advisories concern the fixed snapshot count in source copy and whole-list notifications when replacing search results; the actual list remains virtualized. Other warnings concern existing platform/dependency/resources/profile copy. No blocking issue remains.
- An independent native reviewer inspected45 actual captures across the four configurations and returned `ship`, with no material fixes. Existing Material typography, colors, touch targets, draft ownership, sources and compact behavior match the recorded design system.

The first native test was observed RED because the screen did not exist. The importer/index had meaningful failing assertions before implementation. Device verification then exposed the packaging mismatch; test-only corrections selected off-screen dialog data, scoped visible profile chips and waited for the visible loaded profile before scrolling. Native publication assertions themselves succeeded; no production account/profile/message QA writes were made.

Final independent rendered disposition and normal package/signature/flag/hash evidence are recorded in `artifacts/2.2/verification/summary.json`; the detailed native verdict is in `verification/native-review.md`. All captures use an isolated API34 emulator and clearly labeled local preview data; physical-device timing/gesture performance is not claimed.

## Backend and practical scope

The live2.1 rules/indexes are unchanged from `243d5e6`, including rule SHA-256 `298b5b565ce300da32c0753bac087e004e7852030b089b4f54e2995460a97f8d`. The existing Spark-only project stays in use; no production deployment is needed for this client extension. Existing profiles and messages are not migrated or deleted.

This is a versioned offline vocabulary snapshot, not a live account/personal-history synchronization or the OtherWise embedding recommender. Current Wiki titles follow OtherWise's English pool. Existing starter Chinese aliases/custom text remain available. Refresh the public snapshot with the documented importer command before rebuilding; no app startup requires a local OtherWise service.

Use2.2 to display the full names behind the new compact Wiki references. An older2.1 client preserves such values as custom labels but may display the identifier. Unknown reference-looking custom text is never fabricated into a known concept.

The installable debug APK and optimized unsigned release APK belong in `artifacts/2.2/`; earlier releases remain intact. Debug signing does not replace a private distribution key. No GitHub push, store publication or OtherWise extension delivery is performed by this LinkMe task.
