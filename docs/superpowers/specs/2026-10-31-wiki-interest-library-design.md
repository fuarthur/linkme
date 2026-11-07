# OtherWise Wiki topic library for LinkMe

The user requested the Wiki word pool already used in local OtherWise. Prior instructions delegate product choices and direct implementation. This extends the Android interest editor and preserves Spark-only operation, private data and the live2.1 rule hash.

## Chosen approach

Bundle a reproducible offline snapshot of the30,919 Wikidata-sourced records in OtherWise/data/topics.json (23 domains). Retain the148 LinkMe starters and custom interests. The718 separately authored OtherWise records and personal histories are outside this Wiki import.

Alternatives considered: calling the local OtherWise Python/embedding backend would make the phone depend on the computer/service; a live Wikimedia autocomplete would add network failures and would not be the same reviewed pool. A bundled snapshot is the agent's choice under delegated execution, supports offline use and adds no paid service.

## Data and compatibility

A fresh importer reads only the public topics and catalog metadata, emits deterministic gzip TSV plus provenance/credit metadata and a small generated reference map. Preserve Wikidata IDs, titles, descriptions, domain selection and attribution. Data selection/grouping stays CC BY-SA4.0; Wikidata structured labels/descriptions remain CC0. Do not copy OtherWise's implementation, private cache, user interests or browse history.

Ordinary titles up to40 UTF-16 units keep their literal text, so existing custom label hashes and starter aliases remain compatible. The147 longer titles, five broader legacy-alias meaning conflicts (or any normalized-label collisions found during import) use short `wiki:Q<id>` values, with a generated synchronous pure lookup restoring full display labels. Never silently truncate or collapse distinct meanings. All stored values still satisfy the existing1–40-unit,16-interest,30-key schema; keep current Firestore rules/indexes unchanged. Wiki IDs are lookup identities, never match percentages or personal inference.

## Native flow

The existing editor adds a visible Browse Wiki topics entry and a search-result handoff carrying the typed query. A dedicated multi-select native picker shows full titles, short descriptions, fields and checked selections. A23-field chooser narrows results. It retains the148/custom mix passed by the parent, caps total selection at16, returns an explicit local draft and cancels on Back. Parent Use/Profile Save retain publication ownership.

Use a RecyclerView for the31k library, background load/search and stale-result suppression; never construct31k native controls or scan31k terms during every starter suggestion render. Preserve query/domain/selection across recreation. Show loading, retry, empty, full-mix and source-credit states. Keep the existing purple Material light/dark world, stable search, keyboard handling and a usable compact-height list. Sources/credits must be accessible in the picker.

## Interface contract

Pure package com.ams.linkme.domain:
- `data class WikiTopic(val wikidataId:String, val title:String, val domain:String, val description:String, val value:String)`.
- `WikiTopicIndex.read(reader:java.io.Reader):WikiTopicIndex`, `val topics:List<WikiTopic>`, `val domains:List<String>`, `val size:Int`.
- `search(query:String, domain:String?=null, limit:Int=Int.MAX_VALUE):List<WikiTopic>`; valid unknown domain returns empty, stable exact/prefix/word search, bounded requested limit, no silent sense resolution.
- Generated `WikiTopicReferences.TOPIC_COUNT`, `label(value:String):String?`, `canonical(value:String):String?`; case-insensitive known-reference lookup, null for unknown values.
- Asset `app/src/main/assets/wiki-topics/topics.tsv.gz`, five columns header `wikidata_id\ttitle\tdomain\tdescription\tvalue`; importer owns all data artifacts and provenance.

Android picker result: `selectedInterests` ArrayList<String>; input additionally `initialQuery` String. Return RESULT_OK only on explicit confirmation. Back leaves parent unchanged. Wiki source references display through InterestCatalog.label; parent normalization preserves known reference identity. Exact matching continues from actual normalized stored selections, not arbitrary external hints.

## Evidence required

Importer/parser real-snapshot counts, unique identities, Unicode, long-title round trips, deterministic rebuild and corrupt-input denial. Search disambiguation and legacy key compatibility tests. Native multi-select/recreation/cancel,16-limit recovery, offline full-library access and parent save-versus-filter regression. Render phone/dark/font1.3/compact/tablet picker, including long title, sources and empty state. Fresh independent code and rendered reviews; production-config APK build/signature/flags/hashes. Record the unchanged live rule hash; no production rule deployment is needed.
