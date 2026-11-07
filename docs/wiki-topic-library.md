# Offline Wiki topic library

LinkMe bundles the 30,919 records marked `source == "Wikidata"` in the public
OtherWise catalog snapshot dated 2026-10-05. They cover 23 domains. All 718
separately authored records are excluded. The importer reads only
`data/topics.json`, `data/catalog_metadata.json`, and `data/README.md`; it does not
import OtherWise code, traverse caches, access user histories, use embeddings, or
make network/API requests. OtherWise stays read-only, and generated output paths
inside its project are rejected.

## Rebuild and check

Run from the LinkMe project root with Python 3.9 or later:

```sh
python3 scripts/import_otherwise_wiki.py --source ../OtherWise
python3 scripts/import_otherwise_wiki.py --source ../OtherWise --check
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests -p 'test_wiki_import.py' -v
./gradlew :app:testDebugUnitTest
```

`--check` validates the public source again and compares every generated file
byte-for-byte without writing. The importer finishes validation before replacing
outputs. It rejects malformed fields, invalid/duplicate IDs, unexpected source
types, inconsistent source counts, duplicate normalized storage identities, and
invalid public provenance. It never truncates a title or silently drops a Wiki
record. gzip headers have a zero timestamp and no filename; source hashes and
revision numbers, rather than a machine path or generation time, identify the
snapshot.

Generated assets are under `app/src/main/assets/wiki-topics/`:

| File | Purpose | Bytes |
|---|---|---:|
| `topics.tsv.gz` | Full offline title, field, description, and stored identity | 1,157,819 |
| `record-provenance.jsonl.gz` | Every entity's public source/list revisions and sections | 1,452,496 |
| `provenance.json` | Input/output hashes, counts, revision map, licenses, transformations | 6,038 |
| `NOTICE.txt` | Redistributable contributor credits and license notices | 1,648 |

The topic TSV decompresses to 3,495,903 bytes. Its UTF-8 header is exactly
`wikidata_id\ttitle\tdomain\tdescription\tvalue`, with five columns per record.
Descriptions collapse whitespace to one space for TSV; titles remain intact.
Per-record provenance preserves source URLs and revisions, list revisions,
sections, canonical article URLs where present, and the retained guide title
where present. The 2,734 retained guide entries lack a recorded list URL in the
source: their known Wikimedia expanded-guide URL is attached with an explicit
`list_url_inferred_from_retained_guide` flag. Their recorded revisions are retained.
The other 28,185 entries retain their actual Vital Articles source URLs.

## Storage identities and full labels

The existing 40-UTF-16-unit storage bound stays in place. Ordinary source titles
remain literal stored values, preserving existing custom-label keys and safe
starter synonyms. There are 152 compact references:

- 147 titles exceed 40 UTF-16 units; the longest is 79 units.
- Five short titles would otherwise be narrowed by an existing starter alias.
- There are zero normalized-title collision groups in this snapshot.

The five meaning conflicts use these identities:

| Wikidata identity | Full Wiki title | Meaning preserved |
|---|---|---|
| `wiki:Q189900` | Information security | Broader than Cybersecurity |
| `wiki:Q6607` | Guitar | The instrument, rather than Playing guitar |
| `wiki:Q1081491` | Football | The family of codes, rather than Soccer alone |
| `wiki:Q114397581` | Weightlifting | The sport/exercise, rather than generic Strength training |
| `wiki:Q39162` | Open source | Practices broader than Open-source software |

For a future snapshot, all members of an NFKC/whitespace/case-normalized title
collision receive distinct `wiki:Q…` values. Titles that enter the reserved
reference namespace also receive their own identity, and titles whose NFKC form
expands beyond the storage bound receive references. Neither case occurs in the
current snapshot. This policy preserves every entity without choosing a sense
from a bare word. Meaningful qualifiers such as `(programming language)` remain
visible. Java, Go, Python, and Rust searches can return multiple distinct senses.

`WikiTopicReferences.kt` is a generated 12,284-byte synchronous map for these 152
references. Profile rendering can recover complete labels without loading the
31k-row index. Known references are looked up case-insensitively; unknown values
return `null` and are not silently treated as known Wiki concepts.

```kotlin
package com.ams.linkme.domain

data class WikiTopic(
    val wikidataId: String,
    val title: String,
    val domain: String,
    val description: String,
    val value: String,
)

WikiTopicIndex.read(reader: java.io.Reader): WikiTopicIndex
val index.topics: List<WikiTopic>
val index.domains: List<String>
val index.size: Int
index.search(query: String, domain: String? = null, limit: Int = Int.MAX_VALUE): List<WikiTopic>

WikiTopicReferences.TOPIC_COUNT // 30919
WikiTopicReferences.label(value: String): String?
WikiTopicReferences.canonical(value: String): String?
```

The pure reader consumes an already decompressed TSV Reader, leaves closing it
to the caller, and throws `IllegalArgumentException` for corrupt data instead of
returning a partial library. The immutable index precomputes normalized search
fields and sorts topics by normalized title, literal title, then ID. Search
priority is exact title, title prefix, word prefix, title substring, all query
words in the title, description/combined title-description matches, then domain
matches. Ties retain library order. Unknown domains and nonpositive limits return
empty results; a blank query with the default limit returns all 30,919 rows for a
virtualized list. The index never resolves starter aliases or writes selections.

## Provenance, credit, and licenses

Credit goes to Wikidata contributors, Wikipedia and Wikimedia contributors, and
OtherWise's public topic selection and domain grouping. Wikidata structured
labels and short descriptions are [CC0](https://www.wikidata.org/wiki/Wikidata:Licensing).
The derived selection and grouping, including LinkMe's changes, are distributed
under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Public list
sources are [Wikipedia Vital Articles Level 5](https://en.wikipedia.org/wiki/Wikipedia:Vital_articles/Level/5)
and the [Wikimedia expanded guide](https://meta.wikimedia.org/wiki/List_of_articles_every_Wikipedia_should_have/Expanded).
No article bodies or separately authored OtherWise descriptions are copied.
`NOTICE.txt` and both provenance assets ship with the library; downstream
redistribution should retain them and the same license for the derived selection.

The exact public inputs are pinned by SHA256:

| Input | SHA256 |
|---|---|
| `data/topics.json` | `210e2d4a080a90cd0acbb7116dd547e1573971102c60651f728527c0df3a995f` |
| `data/catalog_metadata.json` | `2e194afccd0461a9bb43392e17dc1f9a2ff7565ba9d64b34a9b61724a528beb7` |
| `data/README.md` | `f407be83cc8fa315ede6cb4f09310d1d8d97fe89ee1c63bc86106b9a3eca0699` |

Generated SHA256 hashes:

| Output | SHA256 |
|---|---|
| `topics.tsv.gz` | `a9ce59f9e1b88f3c43be42c2160849593e68e0987d5f05f3dbec1cc66cb9a910` |
| `record-provenance.jsonl.gz` | `6be262056901d14865b1490912fbffa681aa06f29b44c8595cf06d92ddfc8570` |
| `WikiTopicReferences.kt` | `5150b26876e403d5d6bb4f6de885e7d2b96b9e744a9accf688aa71fc6ecdfc87` |

Domain coverage reflects the source selection, rather than equal representation:

| Domain | Imported Wiki topics |
|---|---:|
| Biology & nature | 4,304 |
| Chemistry & materials | 1,022 |
| Computing & information | 620 |
| Earth & environment | 1,028 |
| Economics & organizations | 602 |
| Engineering & transport | 2,068 |
| Food & agriculture | 883 |
| Games & sports | 840 |
| Geography & travel | 5,009 |
| Health & medicine | 1,094 |
| History & culture | 3,094 |
| Home & crafts | 431 |
| Learning & language | 721 |
| Literature & storytelling | 325 |
| Mathematics | 1,102 |
| Mind & behavior | 224 |
| Music & performance | 878 |
| Philosophy | 240 |
| Physics & astronomy | 2,202 |
| Politics & law | 1,157 |
| Religion & spirituality | 1,139 |
| Society & relationships | 1,317 |
| Visual arts & design | 619 |

## Verification evidence

On 2026-10-31, the initial Python RED run had seven assertion failures across ten
tests against a deliberate not-implemented importer and absent snapshot. The
Kotlin RED run had eight failing tests against the deliberately empty reader
stub. Additional importer regressions demonstrated three failures for reserved
reference collisions, source-tree output paths, and invisible-only titles.
After implementation, all 13 Python tests passed. The full JVM suite passed
62 tests (including all eight Wiki index tests), with zero failures, errors, or
skips. `--check` confirmed byte-identical regeneration from the actual public
snapshot. Tests verify all 30,919 unique identities, all 23 domains, every one of
the 152 reference label/canonical round trips, stable rankings, distinct ambiguous
senses, Unicode boundaries, corruption denial, source hashes, and drift detection.

## Android runtime packaging

The checked-in sources retain deterministic gzip files. AAPT expands these assets while building an APK and removes `.gz`; the packaged paths are `assets/wiki-topics/topics.tsv` and `assets/wiki-topics/record-provenance.jsonl`. The Android repository opens the plain packaged TSV. Importer and JVM tests continue to open the compressed source. Delivery verifies these actual APK entries against the decompressed source; this avoids mistaking a source filename for a runtime asset path.

Ordinary starter aliases still use LinkMe's existing canonicalization when selected; the five broader Wiki meanings above use distinct references to avoid narrowing them. The picker returns only a local draft. All stored values still fit the already-published2.1 policy, so vocabulary updates do not require publishing different access rules.
