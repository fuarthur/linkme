"""Behavioral checks for the public-data-only, offline Wiki snapshot importer."""
import csv
import gzip
import hashlib
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/import_otherwise_wiki.py"
HEADER = ["wikidata_id", "title", "domain", "description", "value"]


def wiki(qid="Q1", title="Java (programming language)", **changes):
    row = {
        "wikidata_id": qid,
        "topic": title,
        "domain": "Computing & information",
        "description": "a general purpose programming language",
        "source": "Wikidata",
        "source_url": f"https://www.wikidata.org/wiki/{qid}",
        "source_revision": 123456,
        "list_url": "https://en.wikipedia.org/wiki/Wikipedia:Vital_articles/Level_5/Technology",
        "list_revision": 234567,
        "list_section": "Technology",
        "subsection": "Computing / Programming",
        "wikipedia_url": "https://en.wikipedia.org/wiki/Java_(programming_language)",
    }
    row.update(changes)
    return row


class WikiImportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.source = self.base / "OtherWise"
        self.data = self.source / "data"
        self.data.mkdir(parents=True)
        self.output = self.base / "assets"
        self.references = self.base / "WikiTopicReferences.kt"

    def write_source(self, rows):
        (self.data / "topics.json").write_text(json.dumps(rows, ensure_ascii=False), encoding="utf-8")
        wiki_rows = [row for row in rows if isinstance(row, dict) and row.get("source") == "Wikidata"]
        counts = {}
        for row in wiki_rows:
            domain = row.get("domain", "invalid")
            if isinstance(domain, str):
                counts[domain] = counts.get(domain, 0) + 1
        metadata = {
            "snapshot_date": "2026-10-05",
            "topic_count": len(rows),
            "source_counts": {"Wikidata": len(wiki_rows), "ProductSpace": len(rows) - len(wiki_rows)},
            "domain_count": len(counts),
            "domains": counts,
            "list_url": "https://en.wikipedia.org/wiki/Wikipedia:Vital_articles/Level_5",
            "list_revisions": {"Wikipedia:Vital articles/Level 5/Technology": 234567},
        }
        (self.data / "catalog_metadata.json").write_text(json.dumps(metadata), encoding="utf-8")
        (self.data / "README.md").write_text("Public catalog selection documentation.\n", encoding="utf-8")

    def run_import(self, *extra, success=True):
        result = subprocess.run(
            [sys.executable, str(SCRIPT), "--source", str(self.source), "--output-dir", str(self.output),
             "--references", str(self.references), *extra],
            capture_output=True, text=True,
        )
        if success:
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        else:
            self.assertNotEqual(0, result.returncode, "Malformed public data must not be silently accepted")
        return result

    def rows(self):
        text = gzip.decompress((self.output / "topics.tsv.gz").read_bytes()).decode("utf-8")
        reader = csv.DictReader(io.StringIO(text), delimiter="\t")
        self.assertEqual(HEADER, reader.fieldnames)
        return list(reader)

    def manifest(self):
        return json.loads((self.output / "provenance.json").read_text(encoding="utf-8"))

    def test_imports_only_wikidata_without_touching_source_or_reading_personal_files(self):
        self.write_source([wiki(), {"source": "ProductSpace", "topic": "Authored private prose is not imported"}])
        original = {p.name: p.read_bytes() for p in self.data.iterdir()}
        (self.source / ".cache").mkdir()
        (self.source / ".cache" / "personal.json").write_text("DO NOT COPY THIS PERSONAL SENTINEL")
        self.run_import()
        self.assertEqual(["Q1"], [row["wikidata_id"] for row in self.rows()])
        self.assertEqual("Java (programming language)", self.rows()[0]["value"])
        self.assertEqual(original, {p.name: p.read_bytes() for p in self.data.iterdir()})
        for path in self.output.iterdir():
            content = gzip.decompress(path.read_bytes()) if path.suffix == ".gz" else path.read_bytes()
            self.assertNotIn(b"PERSONAL SENTINEL", content)
            self.assertNotIn(b"Authored private prose", content)

    def test_preserves_unicode_titles_and_collapses_only_description_whitespace(self):
        self.write_source([wiki(title="陶艺 — Café", description="  a\tshort\n  public\r\n description  ")])
        self.run_import()
        row = self.rows()[0]
        self.assertEqual("陶艺 — Café", row["title"])
        self.assertEqual("陶艺 — Café", row["value"])
        self.assertEqual("a short public description", row["description"])

    def test_long_utf16_or_nfkc_expanding_titles_use_identity_references(self):
        self.write_source([
            wiki("Q1", "😀" * 20), wiki("Q2", "😀" * 21),
            wiki("Q3", "x" * 39 + "ﬃ"), wiki("Q4", "x" * 40),
        ])
        self.run_import()
        rows = {row["wikidata_id"]: row for row in self.rows()}
        self.assertEqual("😀" * 20, rows["Q1"]["value"])
        self.assertEqual("wiki:Q2", rows["Q2"]["value"])
        self.assertEqual("😀" * 21, rows["Q2"]["title"])
        self.assertEqual("wiki:Q3", rows["Q3"]["value"])
        self.assertEqual("x" * 40, rows["Q4"]["value"])
        self.assertEqual(2, self.manifest()["reference_count"])

    def test_every_normalized_title_collision_gets_a_distinct_reference(self):
        self.write_source([wiki("Q1", "Java"), wiki("Q2", "ＪＡＶＡ"), wiki("Q3", " java "), wiki("Q4", "Java island")])
        self.run_import()
        values = {row["wikidata_id"]: row["value"] for row in self.rows()}
        self.assertEqual({"Q1": "wiki:Q1", "Q2": "wiki:Q2", "Q3": "wiki:Q3", "Q4": "Java island"}, values)
        self.assertEqual(1, self.manifest()["normalized_title_collision_groups"])
        self.assertEqual(3, self.manifest()["reference_count"])

    def test_short_titles_with_broader_legacy_alias_meanings_keep_wiki_identities(self):
        self.write_source([
            wiki("Q189900", "Information security"), wiki("Q6607", "Guitar"),
            wiki("Q1081491", "Football"), wiki("Q114397581", "Weightlifting"),
            wiki("Q39162", "Open source"),
        ])
        self.run_import()
        self.assertEqual(
            {"Q189900": "wiki:Q189900", "Q6607": "wiki:Q6607", "Q1081491": "wiki:Q1081491",
             "Q114397581": "wiki:Q114397581", "Q39162": "wiki:Q39162"},
            {row["wikidata_id"]: row["value"] for row in self.rows()},
        )
        self.assertEqual(5, self.manifest()["legacy_starter_meaning_collision_count"])

    def test_literal_titles_in_the_reference_namespace_do_not_collide_with_long_entities(self):
        self.write_source([wiki("Q1", "wiki:Q2"), wiki("Q2", "x" * 41)])
        self.run_import()
        self.assertEqual(
            {"Q1": "wiki:Q1", "Q2": "wiki:Q2"},
            {row["wikidata_id"]: row["value"] for row in self.rows()},
        )

    def test_refuses_generated_outputs_inside_the_read_only_source_project(self):
        self.write_source([wiki()])
        self.run_import("--references", str(self.source / "Generated.kt"), success=False)
        self.assertFalse((self.source / "Generated.kt").exists())
        self.assertFalse(self.output.exists())

    def test_generated_outputs_are_byte_reproducible_and_check_detects_drift(self):
        self.write_source([wiki("Q2", "Rust (programming language)"), wiki("Q1", "Rust (fungus)")])
        self.run_import()
        first = {p.name: p.read_bytes() for p in self.output.iterdir()}
        references = self.references.read_bytes()
        self.run_import()
        self.assertEqual(first, {p.name: p.read_bytes() for p in self.output.iterdir()})
        self.assertEqual(references, self.references.read_bytes())
        self.run_import("--check")
        (self.output / "topics.tsv.gz").write_bytes(b"stale")
        self.run_import("--check", success=False)
        self.assertEqual(b"stale", (self.output / "topics.tsv.gz").read_bytes())

    def test_records_source_hashes_domain_counts_and_per_record_public_revisions(self):
        self.write_source([wiki(), wiki("Q2", "Java", domain="Geography & travel")])
        self.run_import()
        manifest = self.manifest()
        self.assertEqual(2, manifest["topic_count"])
        self.assertEqual({"Computing & information": 1, "Geography & travel": 1}, manifest["domains"])
        for name in ("topics.json", "catalog_metadata.json", "README.md"):
            self.assertEqual(hashlib.sha256((self.data / name).read_bytes()).hexdigest(), manifest["sources"][f"data/{name}"]["sha256"])
        sources = [json.loads(line) for line in gzip.decompress((self.output / "record-provenance.jsonl.gz").read_bytes()).decode().splitlines()]
        self.assertEqual({"Q1", "Q2"}, {row["wikidata_id"] for row in sources})
        self.assertTrue(all(row["source_revision"] == 123456 and row["list_revision"] == 234567 for row in sources))
        self.assertEqual("CC0-1.0", manifest["licenses"]["labels_and_descriptions"])
        self.assertEqual("CC-BY-SA-4.0", manifest["licenses"]["selection_and_grouping"])

    def test_rejects_malformed_wiki_rows_without_partial_output(self):
        cases = [
            {"wikidata_id": "Q0"}, {"wikidata_id": "q12"}, {"wikidata_id": "Q1\t"},
            {"topic": "x\tbroken"}, {"topic": "\u200b"}, {"domain": ""},
            {"description": " \t\n "}, {"description": "bad\u0000text"}, {"topic": 12},
            {"source_revision": "old"}, {"list_revision": 0},
            {"source_url": "file:///private/history"}, {"topic": "\u0301"},
        ]
        for changes in cases:
            with self.subTest(changes=changes):
                self.write_source([wiki(**changes)])
                self.run_import(success=False)
                self.assertFalse(self.output.exists())
                self.assertFalse(self.references.exists())

    def test_rejects_duplicate_entity_ids_instead_of_dropping_a_sense(self):
        self.write_source([wiki(), wiki(title="A second source title")])
        self.run_import(success=False)
        self.assertFalse(self.output.exists())

    def test_rejects_source_metadata_count_mismatch(self):
        self.write_source([wiki()])
        path = self.data / "catalog_metadata.json"
        metadata = json.loads(path.read_text())
        metadata["source_counts"]["Wikidata"] = 2
        path.write_text(json.dumps(metadata))
        self.run_import(success=False)
        self.assertFalse(self.output.exists())

    def test_shipped_snapshot_contains_every_reviewed_wiki_identity_and_no_authored_rows(self):
        asset = ROOT / "app/src/main/assets/wiki-topics/topics.tsv.gz"
        self.assertTrue(asset.is_file(), "The offline public Wiki snapshot must be shipped")
        data = list(csv.DictReader(io.StringIO(gzip.decompress(asset.read_bytes()).decode("utf-8")), delimiter="\t"))
        self.assertEqual(30919, len(data))
        self.assertEqual(30919, len({row["wikidata_id"] for row in data}))
        self.assertEqual(23, len({row["domain"] for row in data}))
        self.assertEqual(152, sum(row["value"].startswith("wiki:") for row in data))
        self.assertTrue(all(len(row["value"].encode("utf-16-le")) // 2 <= 40 for row in data))
        self.assertEqual(30919, len({row["value"].lower() for row in data}))


if __name__ == "__main__":
    unittest.main()
