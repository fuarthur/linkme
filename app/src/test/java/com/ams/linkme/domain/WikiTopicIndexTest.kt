package com.ams.linkme.domain

import java.io.File
import java.io.StringReader
import java.util.zip.GZIPInputStream
import org.junit.Assert.*
import org.junit.Test

class WikiTopicIndexTest {
    private val header = "wikidata_id\ttitle\tdomain\tdescription\tvalue\n"

    private fun read(vararg rows: String): WikiTopicIndex =
        WikiTopicIndex.read(StringReader(header + rows.joinToString("\n") + "\n"))

    @Test
    fun readsEverySenseAndKeepsFullTitlesAndLiteralValues() {
        val index = read(
            "Q2\tJava (programming language)\tComputing\ta general purpose language\tJava (programming language)",
            "Q1\tJava\tGeography\tan island in Indonesia\tJava",
            "Q3\t陶艺 — Café\tArts\ta Unicode topic description\t陶艺 — Café",
        )
        assertEquals(3, index.size)
        assertEquals(listOf("Arts", "Computing", "Geography"), index.domains)
        assertEquals(listOf("Java", "Java (programming language)", "陶艺 — Café"), index.topics.map { it.title })
        assertEquals("Java (programming language)", index.search("Java")[1].value)
        assertEquals(listOf("Q1", "Q2"), index.search("ＪＡＶＡ").map { it.wikidataId })
    }

    @Test
    fun ranksExactThenPrefixThenWordThenSubstringThenDescriptionStably() {
        val index = read(
            "Q6\tZulu\tOther\tjava is mentioned here\tZulu",
            "Q5\tSuperjava\tOther\ta short description\tSuperjava",
            "Q4\tLearning Java\tComputing\ta short description\tLearning Java",
            "Q3\tJavaScript\tComputing\ta short description\tJavaScript",
            "Q2\tJava (programming language)\tComputing\ta short description\tJava (programming language)",
            "Q1\tJava\tGeography\ta short description\tJava",
        )
        assertEquals(listOf("Q1", "Q2", "Q3", "Q4", "Q5", "Q6"), index.search(" java ").map { it.wikidataId })
        assertEquals(listOf("Q1", "Q2"), index.search("java", limit = 2).map { it.wikidataId })
        assertEquals(index.topics, index.search(""))
        assertEquals(index.search("java"), index.search("java"))
    }

    @Test
    fun ambiguousQueriesRetainDistinctGoPythonAndRustSenses() {
        val index = read(
            "Q1\tGo (game)\tGames\ta board game\tGo (game)",
            "Q2\tGo (programming language)\tComputing\ta programming language\tGo (programming language)",
            "Q3\tPython (genus)\tNature\ta genus of snakes\tPython (genus)",
            "Q4\tPython (programming language)\tComputing\ta programming language\tPython (programming language)",
            "Q5\tRust (fungus)\tNature\ta plant disease\tRust (fungus)",
            "Q6\tRust (programming language)\tComputing\ta programming language\tRust (programming language)",
        )
        assertEquals(listOf("Q1", "Q2"), index.search("go").map { it.wikidataId })
        assertEquals(listOf("Q3", "Q4"), index.search("python").map { it.wikidataId })
        assertEquals(listOf("Q5", "Q6"), index.search("rust").map { it.wikidataId })
    }

    @Test
    fun filtersDomainsAndBoundsLimitsWithoutHidingUnqueriedRows() {
        val index = read(
            "Q1\tJava\tGeography\tan island\tJava",
            "Q2\tJava (programming language)\tComputing\ta language\tJava (programming language)",
        )
        assertEquals(listOf("Q2"), index.search("java", "Computing").map { it.wikidataId })
        assertEquals(listOf("Q1"), index.search("", "Geography").map { it.wikidataId })
        assertTrue(index.search("java", "Missing").isEmpty())
        assertTrue(index.search("java", limit = 0).isEmpty())
        assertTrue(index.search("java", limit = -1).isEmpty())
        assertTrue(index.search("unmatched").isEmpty())
    }

    @Test
    fun longAndCollidingTitlesRoundTripWithoutTruncation() {
        val title = "International Red Cross and Red Crescent Movement"
        val index = read(
            "Q7178\t$title\tSociety\tan international humanitarian movement\twiki:Q7178",
            "Q1\tJAVA\tGeography\tan island\twiki:Q1",
            "Q2\tJava\tComputing\ta language\twiki:Q2",
            "Q3\t${"😀".repeat(20)}\tOther\tforty UTF16 units\t${"😀".repeat(20)}",
        )
        assertEquals(title, index.search("Red Crescent").single().title)
        assertEquals("wiki:Q7178", index.search("Red Crescent").single().value)
        assertEquals(2, index.search("java").size)
        assertEquals(40, index.search("😀").single().value.length)
    }

    @Test
    fun broaderFootballAndGuitarMeaningsKeepReferencesSeparateFromStarterAliases() {
        val index = read(
            "Q1081491\tFootball\tGames\ta family of team sports\twiki:Q1081491",
            "Q6607\tGuitar\tMusic\ta stringed musical instrument\twiki:Q6607",
        )
        assertEquals("wiki:Q1081491", index.search("football").single().value)
        assertEquals("wiki:Q6607", index.search("guitar").single().value)
    }

    @Test
    fun deniesMalformedHeadersRowsIdentitiesAndValues() {
        val bad = listOf(
            "wrong\n",
            header + "Q1\tTitle\tDomain\tdescription\n",
            header + "Q1\tTitle\tDomain\tdescription\tTitle\textra\n",
            header + "Q0\tTitle\tDomain\tdescription\tTitle\n",
            header + "Q1\tTitle\t\tdescription\tTitle\n",
            header + "Q1\tTitle\tDomain\t\tTitle\n",
            header + "Q1\tTitle\tDomain\tdescription\twiki:Q2\n",
            header + "Q1\t${"x".repeat(41)}\tDomain\tdescription\t${"x".repeat(41)}\n",
            header + "Q1\tTitle\tDomain\tdescription\tTitle\nQ1\tOther\tDomain\tdescription\tOther\n",
            header + "Q1\tJava\tDomain\tdescription\tJava\nQ2\tＪＡＶＡ\tDomain\tdescription\tＪＡＶＡ\n",
            header + "Q1\tTitle\u0000\tDomain\tdescription\tTitle\u0000\n",
            header + "Q1\t\u200b\tDomain\tdescription\t\u200b\n",
            header + "Q1\t\u0301\tDomain\tdescription\t\u0301\n",
            header + "Q1\tBad\ud800\tDomain\tdescription\tBad\ud800\n",
            header,
            header + "\n",
        )
        for (input in bad) {
            try {
                WikiTopicIndex.read(StringReader(input))
                fail("Malformed Wiki data must be denied: $input")
            } catch (_: IllegalArgumentException) {
                // Corrupt assets produce one recoverable error instead of a partial library.
            }
        }
    }

    @Test
    fun fullSnapshotRetainsAll30919IdentitiesAnd152References() {
        val asset = File("src/main/assets/wiki-topics/topics.tsv.gz")
        assertTrue("The full offline snapshot must exist", asset.isFile)
        val index = GZIPInputStream(asset.inputStream()).bufferedReader(Charsets.UTF_8).use(WikiTopicIndex::read)
        assertEquals(30919, index.size)
        assertEquals(23, index.domains.size)
        assertEquals(30919, index.topics.map { it.wikidataId }.distinct().size)
        assertEquals(152, index.topics.count { it.value.startsWith("wiki:") })
        assertEquals(30919, index.search("").size)
        assertTrue(index.topics.all { it.value.length in 1..40 })
        for (topic in index.topics.filter { it.value.startsWith("wiki:") }) {
            assertEquals(topic.title, WikiTopicReferences.label(topic.value.lowercase()))
            assertEquals(topic.value, WikiTopicReferences.canonical(topic.value.uppercase()))
        }
        assertEquals(30919, WikiTopicReferences.TOPIC_COUNT)
        assertNull(WikiTopicReferences.label("wiki:Q9999999999999"))
        assertNull(WikiTopicReferences.canonical("Java"))
    }
}
