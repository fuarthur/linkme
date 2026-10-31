# Wiki Interest Library Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for Android integration, with superpowers:dispatching-parallel-agents for the independent import/index deliverable. Review the final change independently.

**Goal:** Reuse OtherWise's30,919 Wiki topics offline in LinkMe's interest picker, preserving existing profiles and free Firebase.

**Architecture:** A deterministic public-data snapshot and pure search index feed a virtualized native multi-select picker. Existing interest drafts and the save boundary remain authoritative; compact references represent long labels within the deployed schema.

**Tech Stack:** Existing Kotlin/Android Material/RecyclerView, Java gzip IO and standard-library Python importer; no paid service or new runtime dependency.

**Spec:** docs/superpowers/specs/2026-10-31-wiki-interest-library-design.md

## Global Constraints

- Android, Spark only; no OtherWise personal data, server startup or modifications to OtherWise.
- Existing16-interest/40-unit stored-label/30-key bounds and live rules hash298b5b...97f8d remain unchanged.
- Import only public Wikidata-sourced rows; preserve source/CC0 and derived-selection CC BY-SA4.0 credits.
- All ordinary stored labels and legacy key behavior stay compatible; long/colliding identities cannot be truncated.
- Picker returns a draft, never publishes. Existing Profile Save and discovery filter separation remain intact.

## Review Focus

- Same-name or ambiguous concepts must remain visibly distinct, especially Java/Go/Python/Rust.
- Long Unicode titles must survive selecting, recreation, saving and displaying without violating40-unit storage.
- Quick query/filter changes and late background responses cannot replace current results or a destroyed model.
- A16-item inherited mix, empty results, offline load and corrupt assets must remain recoverable.
- Short/large-text/keyboard windows retain enough list space, with native checked state and accessible sources.

### Task1 — Public snapshot and pure index (independent data agent)

Files: scripts/import_otherwise_wiki.py; tests/test_wiki_import.py; domain/WikiTopicIndex.kt, generated WikiTopicReferences.kt; test/domain/WikiTopicIndexTest.kt; assets/wiki-topics/*; docs/wiki-topic-library.md.

Consumes exact interfaces from spec. Produces deterministic snapshot, all30,919 records and ref map, validated pure reader/search, provenance/credit evidence.

- [x] Write tests for public-only import, deterministic bytes, invalid fields/IDs, normalized collisions, >40-unit references, ambiguous title search, domains and real-snapshot counts. Observe RED.
- [x] Implement importer, generated references and bounded stable search; preserve all Wiki IDs and ordinary literal stored labels. Observe GREEN.
- [x] Rebuild/check with the actual OtherWise source; verify source hashes, count, unique stored values and generated references. Do not modify OtherWise or run its build.

### Task2 — Native picker and existing editor integration (root)

Files: new data/WikiTopicRepository.kt, ui/viewmodel/WikiTopicsViewModel.kt, ui/activity/WikiTopicsActivity.kt, adapter/WikiTopicsAdapter.kt; existing InterestsActivity/InterestsViewModel, InterestCatalog, ProfileScreen, manifest/ids/strings and version.

Consumes Task1 types/assets. Produces `selectedInterests` result after explicit confirmation; supports `initialQuery` and retains inherited mix.

- [x] Write failing native tests for launch/query/description/selection/recreate/cancel and >40-unit title selection. Observe missing-screen RED.
- [x] Load/cache index off main; search with bounded query and generation guards; preserve SavedStateHandle draft/query/domain. Clear owned work when model is destroyed.
- [x] Implement virtualized multi-select UI with search, field picker, clear counts, error/retry/empty/full-mix states and source credits. Keep compact controls in one row and list weighted; use native light/dark roles.
- [x] Add browse/search handoff in existing editor, local mix callback, pure long-label display/normalization and long-chip wrapping safety. Existing starters/custom choices remain available.
- [x] Verify unit/lint/device flows; add actual Wiki reference save coverage against unchanged emulator rules.

### Task3 — Review and delivery

- [x] Independently review the full code/data integration and actual phone/dark/large/compact/tablet captures; resolve material findings.
- [x] Verify deterministic data regeneration, complete JVM/security/device checks, unchanged backend rules and no OtherWise edits.
- [x] Build normal production-config debug/release APKs (2.2/code4); verify signature, flags, version and SHA256.
- [x] Update PRODUCT/DESIGN/sidecar/README/review/progress and local artifacts/2.2; keep earlier2.1 artifacts and save the local branch without pushing.
