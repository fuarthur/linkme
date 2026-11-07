# Interest discovery redesign

User request: interests depend too much on manual entry and lack customization; substantial refactoring is authorized. The user previously delegated product choices and execution to the agent. The choices below are implementation decisions, not new user-stated preferences.

## Intent and approach

Replace the fixed eight-checkbox model with an editable interest space: specific searchable topics, custom labels, optional scene-based exploration and suggestions from an introduction. Keep native Android and the existing Firebase Spark project. The chosen approach combines a bundled catalog with local deterministic suggestions and owner-controlled public tags. A tags-only free-text field would retain the manual-entry burden; a hosted AI profiler would add external processing/cost and unverifiable inference. Neither is needed for this release.

## Experience

A dedicated native interest editor is launched from Profile or Discover. It supports search, category browsing, aliases (including selected Chinese aliases), a selected mix with removable chips, custom labels and a three-question scene explorer. Exploring yields suggestions; it never silently selects or saves them. Suggestions from the provided introduction and selected topics are explicitly labeled and require a tap to select. The editor returns a draft on Done; Back cancels. Profile changes publish only after Save profile acknowledges. Discover edits affect current filters, not the public profile.

Keep 1–16 selected interests, each 1–40 UTF-16 code units after NFKC/whitespace normalization. Deduplicate case/alias variants; retain custom display text. Do not read private chat messages, contacts or other apps to infer interests. No paid API, remote model or analytics is added. A curated catalog of at least100 topics across12 groups provides concrete, editable starting points. Custom topics remain fully usable even if absent from the catalog.

Discover shows the active interest mix, opens the same editor and explains each result: exact shared topics first; otherwise the common broader category. An exact specific topic outranks broad-category overlap. Never present a compatibility percentage. Conversation starters use actual exact/shared-category evidence. Interest-only candidate filtering stays bounded; this is not exhaustive global ranking.

## Domain interface (pure Kotlin)

`InterestCatalog` owns catalog, aliases, normalization, custom validation, search, suggestions, index keys and overlap. Types: `InterestTopic(id,label,groupId,aliases,isBroad)`, `InterestGroup(id,label)`, `InterestSuggestion(label,reason)`, `InterestMatch(exact,relatedGroups,score)`, `ExplorationChoice(id,title,detail,topics)`, `ExplorationQuestion(title,choices)`.

Public members:
- `MAX_INTERESTS=16`, `MAX_LABEL_LENGTH=40`; `groups`, `topics`, `questions`.
- `normalize(Collection<String>):List<String>`; `label(String):String`; `validateLabel(String):String?`.
- `search(query:String, groupId:String?=null, limit:Int=12):List<InterestTopic>`.
- `suggestions(selected:List<String>, bio:String, explorationChoices:Set<String>=emptySet(), limit:Int=8):List<InterestSuggestion>`.
- `fromText(text:String):List<String>` returns candidates, not inferred profile facts; obvious negative phrases must not generate positive suggestions.
- `indexKeys(Collection<String>):List<String>`; `legacyQueryValues(Collection<String>):List<String>`.
- `match(left:Collection<String>,right:Collection<String>):InterestMatch`.

Keys: known topic `t:<stable-id>`; group `g:<group-id>`; custom `c:<SHA256 of NFKC/whitespace-normalized lowercase UTF-8 label>`. The same custom name differing only in case/spacing has the same key. Known aliases resolve before custom hashing. Maximum query keys30 (16 tags plus at most12 groups =28). Required fixture topics: Indie games (`indie-games`,games), Live music (`live-music`,music), Cooking (`cooking`,food), Photography (`photography`,creative), Hiking (`hiking`,outdoors), Vinyl records (`vinyl-records`,music), Astrophotography (`astrophotography`,tech), Anime (`anime`,screen), Movie (`movie`,screen). Legacy Sport/Games/Food/Music/Dating/Anime/Movie/Other remain recognized; display Sport as Sports and Movie as Movies.

`Interests` remains a compatibility facade; `LinkMeLogic` delegates ranking/validation through the module. Match results are recomputed from labels, not trusted index keys. An arbitrary index key on another user's profile cannot fabricate a shared-topic label.

## Persistence and privacy

`User.interests` remains a list of display/canonical topic labels, now supporting custom values. No new sensitive user attributes. PublicProfiles optionally adds `interestKeys`, a bounded list of at most30 unique canonical ASCII keys of at most72 code units (`t:`/`g:` plus a1–70-character lowercase alphanumeric/hyphen slug, or `c:` plus64 lowercase hex characters). New saves populate it in both private and public documents atomically; unrelated private fields remain merged. Legacy public profiles without keys remain readable/queryable. A new-schema public profile cannot be overwritten by an old client that omits keys; the whole save batch is denied to protect custom tags from old normalization.

New discovery query: visible PublicProfiles + interestKeys array-contains-any keys + limit60. A second bounded legacy interests query includes appropriate legacy roots/aliases; merge by UID, rank actual label overlap and show at most60. Add one composite index (discoverable ASC, interestKeys ARRAY_CONTAINS), retaining all seven existing indexes. Empty query lists short-circuit; query failure is surfaced rather than silently reported as an empty success. Each array filter has at most30 terms. See [official Firestore query limits](https://firebase.google.com/docs/firestore/query-data/queries).

Keep owner/participant access, message integrity and blocking unchanged. Rules validate every interest/key element, bounds and duplicate values. No collection migration or bulk rewrite. Only owners opt into new profiles by saving. Production rollout follows local tests and a concrete reviewable rule candidate; any required per-action approval is handled at that final step.

## Verification and finish

Pure regressions: custom normalization/Unicode/aliases, invalid labels, max16, exact-before-broad ranking, custom key consistency, search, conservative bio suggestions, scene suggestions, <=30 query keys, legacy fallback. Security regressions: new valid shape,16 vs17 labels, per-element types/length/control chars, optional legacy shape, old-client downgrade denial, owner isolation and visibility/limit checks on new queries. Android: search/create/select/remove/Done/cancel/recreate, suggestions remain unselected, profile roundtrip with custom tags, current-filter edits do not overwrite public tags, discovery explanation and all existing auth/chat regressions. Inspect phone/dark/font1.3/compact captures; obtain independent code/render review. Deliver version2.1 APK, updated docs and verified deployment state.
