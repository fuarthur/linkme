package com.ams.linkme.domain

import java.io.File
import java.util.Locale
import java.util.zip.GZIPInputStream
import org.junit.Assert.*
import org.junit.Test

class WikiInterestCompatibilityTest {
    private fun library() =
        GZIPInputStream(File("src/main/assets/wiki-topics/topics.tsv.gz").inputStream())
            .bufferedReader()
            .use(WikiTopicIndex::read)

    @Test
    fun longReferencesKeepTheirIdentityAndRestoreTheCompleteTitle() {
        val topic = library().topics.first { it.title.length > 40 }
        val normalized = InterestCatalog.normalize(listOf(topic.value.uppercase(Locale.ROOT)))
        assertEquals(listOf(topic.value), normalized)
        assertEquals(topic.title, InterestCatalog.label(normalized.single()))
        assertTrue(normalized.single().length <= 40)
        assertNull(InterestCatalog.validateLabel(normalized.single()))
    }

    @Test
    fun broaderWikiMeaningsDoNotBecomeNarrowerStarterAliases() {
        for ((value, title, narrower) in
            listOf(
                Triple("wiki:Q6607", "Guitar", "Playing guitar"),
                Triple("wiki:Q1081491", "Football", "Soccer"),
                Triple("wiki:Q189900", "Information security", "Cybersecurity"),
                Triple("wiki:Q114397581", "Weightlifting", "Strength training"),
                Triple("wiki:Q39162", "Open source", "Open-source software"),
            )) {
            assertEquals(listOf(value), InterestCatalog.normalize(listOf(value)))
            assertEquals(title, InterestCatalog.label(value))
            assertTrue(InterestCatalog.match(listOf(value), listOf(narrower)).exact.isEmpty())
        }
        assertEquals(listOf("Playing guitar"), InterestCatalog.normalize(listOf("Guitar")))
        assertEquals(listOf("Soccer"), InterestCatalog.normalize(listOf("Football")))
    }

    @Test
    fun existingLiteralWikiLabelsKeepTheirPriorCustomKey() {
        val expected = "c:c92057f601d9d87516049e0af0bbded93ff7d80a26ae0f9fad4ab6ae9eb8e5bc"
        assertEquals(listOf(expected), InterestCatalog.indexKeys(listOf("Quantum mechanics")))
        assertEquals(listOf(expected), InterestCatalog.indexKeys(listOf("quantum MECHANICS")))
    }

    @Test
    fun aFullWikiMixStaysWithinThePublishedValueAndQueryBounds() {
        val values =
            library().topics.filter { it.value.startsWith("wiki:") }.take(16).map { it.value }
        val normalized = InterestCatalog.normalize(values)
        assertEquals(16, normalized.size)
        assertTrue(normalized.all { it.length in 1..40 })
        val keys = InterestCatalog.indexKeys(normalized)
        assertEquals(16, keys.size)
        assertTrue(keys.all { it.matches(Regex("c:[0-9a-f]{64}")) })
    }

    @Test
    fun unknownReferenceLookingCustomTextIsNotInventedAsAKnownTopic() {
        val custom = "wiki:Q999999999999999"
        assertEquals(listOf(custom), InterestCatalog.normalize(listOf(custom)))
        assertEquals(custom, InterestCatalog.label(custom))
    }
}
