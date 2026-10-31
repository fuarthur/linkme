# Interest Discovery Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for the integrated native app; independent domain and security work may run through superpowers:dispatching-parallel-agents under the fixed interface below. Review the final branch independently.

**Goal:** Ship editable, discoverable and explainable interests, preserving privacy and free Firebase.

**Architecture:** A pure InterestCatalog module hides catalog/alias/search/suggestion/matching details. An activity-scoped editor returns explicit drafts; the existing repository publishes normalized tags/index hints atomically. Two bounded queries cover new and legacy profiles.

**Tech Stack:** Existing Kotlin/Material Views, AndroidX SavedStateHandle, Firebase Spark; no new paid dependencies.

**Spec:** docs/superpowers/specs/2026-10-31-interest-discovery-design.md

## Global Constraints

- Keep project linkme-e0b8d on Spark; no billing, remote AI or chat-derived profiling.
- Max16 tags,40 code units/label,30 index keys,72 code units/key; per-query limit60.
- Owner/private/contact separation, atomic message validation and all existing history behavior remain enforced.
- Existing data stays intact; legacy profiles remain readable, new-schema downgrade saves are rejected.
- Suggestions require selection; editor Back cancels and Profile Save owns publication.

## Review Focus

- Alias/Unicode duplicates and arbitrary custom names must survive load/save and match consistently.
- Out-of-order search/query callbacks cannot overwrite newer selections; rotation preserves the editor draft.
- Old clients cannot silently erase new custom labels by omitting index keys.
- Index hints cannot manufacture displayed overlap, and >30 terms never reach Firestore.
- Short windows, keyboard, large text and an already-full16-tag mix must remain recoverable.

### Task1: Pure interest module

Files: create domain/InterestCatalog.kt, test/domain/InterestCatalogTest.kt; update domain/LinkMeLogic.kt and LinkMeLogicTest.kt. The exact interface/types, required fixture IDs and key scheme are in the spec.
- [x] Add meaningful failing tests for custom preservation, aliases, Unicode, validation, search, suggestions, scenes, key bounds and exact-vs-category ranking.
- [x] Observe RED, implement the module, adjust only intentionally superseded fixed-eight expectations, then run all unit tests GREEN.

### Task2: Rule schema and compatibility

Files: firestore.rules, firestore.indexes.json, androidTest/assets/firestore.rules, firebase-tests/security.test.mjs, seed-preview.mjs, docs/firebase-security.md.
- [x] Add tests showing the current rules reject valid new custom profiles and lookup queries; retain all existing access/message tests.
- [x] Add optional validated interestKeys and validated16-tag schema; forbid new-to-legacy downgrade updates; preserve legacy reads/writes on legacy docs.
- [x] Add the single composite index and update local synthetic fixtures; run the complete emulator suite GREEN. Do not deploy from a subagent.

### Task3: Native editor and integration

Files: create ui/activity/InterestsActivity.kt, ui/viewmodel/InterestsViewModel.kt; update ProfileScreen/DiscoverScreen, MainActivity/ProfileViewModel/MainViewModel/PeopleAdapter, repository, manifest/ids and version.
- [x] Write failing native workflow tests for search/custom/selection/rotation/cancel and saving vs filtering separation.
- [x] Build one editor with selected mix, typeahead/create, categories, labeled suggestions and three scene questions. Use SavedStateHandle for selection/query/scene state and explicit activity results.
- [x] Replace fixed chips in Profile/Discover with editable mix; publish interestKeys from canonical labels; dual bounded queries rank actual overlap; update conversation starter evidence.
- [x] Run unit/lint/device suite; fix root causes of failures.

### Task4: Review, rollout and delivery

- [x] Independent whole-change code review and native rendered review; resolve material findings.
- [x] Capture phone, dark, font1.3 and compact layouts with stable rendered frames.
- [x] Verify all security/device/unit checks; build normal production-config debug and release APKs; validate flags/signatures/hashes.
- [ ] Publish the exact tested schema candidate and enable the new index, retaining Spark and existing data; verify live state and handle any required final action approval.
- [x] Update PRODUCT/DESIGN/README/review/progress and artifacts; save local branch, preserving old2.0 artifacts and existing production data.

Final rollout checkpoint: all local implementation/review/build/artifact steps are complete. The interest-key index is Enabled; publication of rule hash298b5b...97f8d awaits explicit approval after automatic approval review rejected it. Existing production documents and free Spark billing are unchanged.
