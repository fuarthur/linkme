package com.ams.linkme.domain

import org.junit.Assert.*
import org.junit.Test

class InterestCatalogTest {
    @Test
    fun catalogProvidesConcreteStartingPointsAcrossTwelveGroups() {
        assertEquals(12, InterestCatalog.groups.size)
        assertTrue(InterestCatalog.topics.count { !it.isBroad } >= 100)
        assertEquals(
            InterestCatalog.topics.size,
            InterestCatalog.topics.map { it.id }.distinct().size,
        )
        assertTrue(
            InterestCatalog.groups.all { group ->
                InterestCatalog.topics.any { it.groupId == group.id }
            }
        )
        assertTrue(
            InterestCatalog.topics.all { topic ->
                InterestCatalog.groups.any { it.id == topic.groupId }
            }
        )
        val required =
            mapOf(
                "indie-games" to "games",
                "live-music" to "music",
                "cooking" to "food",
                "photography" to "creative",
                "hiking" to "outdoors",
                "vinyl-records" to "music",
                "astrophotography" to "tech",
                "anime" to "screen",
                "movie" to "screen",
                "indie-rock" to "music",
            )
        for ((id, group) in required) {
            assertEquals(id, group, InterestCatalog.topics.singleOrNull { it.id == id }?.groupId)
        }
    }

    @Test
    fun normalizationPreservesCustomDisplayWhileResolvingEnglishAndChineseAliases() {
        assertEquals(
            listOf("陶艺 Club", "Indie games", "Sport", "Movie", "Live music"),
            InterestCatalog.normalize(
                listOf(" 陶艺　Club ", "陶艺 CLUB", "独立游戏", "indie games", "ＳＰＯＲＴＳ", "Films", "现场音乐")
            ),
        )
        assertEquals("Sports", InterestCatalog.label("sport"))
        assertEquals("Movies", InterestCatalog.label("films"))
        assertEquals("陶艺 Club", InterestCatalog.label("  陶艺　Club  "))
    }

    @Test
    fun normalizationDropsInvalidLabelsAndCapsAfterDeduplication() {
        val values = listOf("", "x\u0000", "Music", "music") + (1..20).map { "Local club $it" }
        val result = InterestCatalog.normalize(values)
        assertEquals(16, result.size)
        assertEquals("Music", result.first())
        assertEquals("Local club 15", result.last())
    }

    @Test
    fun validationUsesNormalizedUtf16LengthAndRejectsRawControlsOrInvisibleOnlyLabels() {
        for (value in
            listOf(
                "",
                "　\u00a0",
                "\u200b",
                "\u200d",
                "x\n",
                "x\t",
                "x\u0000",
                "x\u007f",
                "x\u0085",
                "x".repeat(41),
                "😀".repeat(21),
            )) {
            assertNotNull(value, InterestCatalog.validateLabel(value))
        }
        for (value in
            listOf("  Local　Club  ", "x".repeat(40), "😀".repeat(20), "陶艺", "CAFÉ", "👩‍🍳")) {
            assertNull(value, InterestCatalog.validateLabel(value))
        }
    }

    @Test
    fun searchFindsAliasesAndSpecificTopicsBeforeSubstringMatches() {
        assertEquals("photography", InterestCatalog.search("photo").first().id)
        assertEquals("indie-games", InterestCatalog.search("独立游戏").first().id)
        assertEquals("live-music", InterestCatalog.search("现场音乐").first().id)
        assertEquals("movie", InterestCatalog.search("films").first().id)
        assertTrue(
            InterestCatalog.search("indie")
                .map { it.id }
                .containsAll(listOf("indie-games", "indie-rock"))
        )
        assertTrue(InterestCatalog.search("", "music", 4).all { it.groupId == "music" })
        assertEquals(4, InterestCatalog.search("", "music", 4).size)
        assertTrue(InterestCatalog.search("hiking", "music").isEmpty())
        assertTrue(InterestCatalog.search("", "missing").isEmpty())
        assertTrue(InterestCatalog.search("", limit = 0).isEmpty())
    }

    @Test
    fun customQueryKeysUseStableNfkcWhitespaceAndCaseInsensitiveSha256() {
        val expected = "c:d5739910299210bb8662fdbb3680783ec8284fd586463bd7cc46d082ae068267"
        assertEquals(listOf(expected), InterestCatalog.indexKeys(listOf("Local Club")))
        assertEquals(
            listOf(expected),
            InterestCatalog.indexKeys(listOf("  Ｌｏｃａｌ　ＣＬＵＢ  ", "local club")),
        )
        assertEquals(listOf("t:indie-games", "g:games"), InterestCatalog.indexKeys(listOf("独立游戏")))
    }

    @Test
    fun queryKeysAreBoundedUniqueAndDerivedOnlyFromActualLabels() {
        val values =
            InterestCatalog.groups.map { group ->
                InterestCatalog.topics.first { it.groupId == group.id }.label
            } + (1..20).map { "My interest $it" }
        val keys = InterestCatalog.indexKeys(values)
        assertTrue(keys.size <= 30)
        assertEquals(keys.size, keys.distinct().size)
        assertTrue(keys.all { it.length <= 72 })
        assertFalse(InterestCatalog.indexKeys(listOf("t:indie-games")).contains("t:indie-games"))
        assertTrue(InterestCatalog.indexKeys(emptyList()).isEmpty())
    }

    @Test
    fun legacyQueryIncludesRelevantRootsAliasesAndCustomLabelsWithBoundedTerms() {
        val values =
            InterestCatalog.legacyQueryValues(
                listOf("Indie games", "Live music", "Cooking", "Hiking", "Movies", "Custom club")
            )
        assertTrue(
            values.containsAll(listOf("Games", "Music", "Food", "Movie", "Movies", "Custom club"))
        )
        assertFalse(values.contains("Dating"))
        assertTrue(
            InterestCatalog.legacyQueryValues(InterestCatalog.topics.map { it.label }).size <= 30
        )
        assertTrue(InterestCatalog.legacyQueryValues(emptyList()).isEmpty())
    }

    @Test
    fun matchUsesExactAndCategoryEvidenceAndSpecificExactAlwaysWins() {
        val specific =
            InterestCatalog.match(listOf("Indie games", "Live music", "Cooking"), listOf("独立游戏"))
        val related =
            InterestCatalog.match(
                listOf("Indie games", "Live music", "Cooking"),
                listOf("Games", "Music", "Food"),
            )
        assertEquals(listOf("Indie games"), specific.exact)
        assertTrue(specific.score > related.score)
        assertTrue(related.exact.isEmpty())
        assertTrue(related.relatedGroups.containsAll(listOf("Games", "Music", "Food & drink")))
        val custom = InterestCatalog.match(listOf(" City　Sketchers "), listOf("ＣＩＴＹ sketchers"))
        assertEquals(listOf("City Sketchers"), custom.exact)
        assertTrue(custom.relatedGroups.isEmpty())
        assertTrue(InterestCatalog.match(listOf("Indie games"), listOf("t:indie-games")).score == 0)
        assertEquals(0, InterestCatalog.match(listOf("Hiking"), listOf("Cooking")).score)
    }

    @Test
    fun explicitIntroductionMentionsYieldCandidatesWithoutBroadSubstringDuplicates() {
        assertEquals(
            listOf("Indie games", "Photography", "Live music"),
            InterestCatalog.fromText("I enjoy indie games, photography, and live music."),
        )
        assertEquals(listOf("Hiking", "Photography"), InterestCatalog.fromText("我喜欢徒步，也喜欢摄影。"))
        assertTrue(InterestCatalog.fromText("I am studying and looking to meet people.").isEmpty())
        assertTrue(InterestCatalog.fromText("A playground near the foodcourt.").isEmpty())
    }

    @Test
    fun obviousNegativeIntroductionPhrasesDoNotBecomePositiveCandidates() {
        assertEquals(
            listOf("Photography"),
            InterestCatalog.fromText("I don't like hiking, but I love photography."),
        )
        assertEquals(
            listOf("Cooking"),
            InterestCatalog.fromText("Not into indie games or live music. I enjoy cooking."),
        )
        assertTrue(
            InterestCatalog.fromText("I hate anime. No sports. I do not enjoy hiking.").isEmpty()
        )
        assertEquals(listOf("Photography"), InterestCatalog.fromText("我不喜欢徒步，但我喜欢摄影。"))
        assertTrue(
            InterestCatalog.fromText("I used to play basketball, but I no longer do.").isEmpty()
        )
    }

    @Test
    fun directNegationDoesNotNeedAnInterestVerb() {
        assertTrue(InterestCatalog.fromText("Not hiking, not camping. No anime.").isEmpty())
    }

    @Test
    fun negationKeepsItsScopeAcrossInfinitivesUntilAnExplicitNewClause() {
        assertEquals(
            listOf("Photography"),
            InterestCatalog.fromText("I don't like to play games. I enjoy photography."),
        )
        assertEquals(
            listOf("Photography"),
            InterestCatalog.fromText("I don't like hiking, I enjoy photography."),
        )
    }

    @Test
    fun ambiguousEverydayWordsDoNotImplySpecializedHobbies() {
        assertTrue(InterestCatalog.fromText("My ultimate goal is to meet people.").isEmpty())
    }

    @Test
    fun categorySuggestionsDoNotResurrectExplicitlyDislikedTopics() {
        val suggestions =
            InterestCatalog.suggestions(
                listOf("Hiking"),
                "I don't like camping or travel. I enjoy photography.",
                setOf("afternoon-outdoors"),
                limit = 16,
            )
        assertTrue(suggestions.any { it.label == "Photography" })
        assertFalse(suggestions.any { it.label == "Camping" || it.label == "Travel" })
    }

    @Test
    fun threeSceneQuestionsOfferEditableConcreteSuggestions() {
        assertEquals(3, InterestCatalog.questions.size)
        val choices = InterestCatalog.questions.flatMap { it.choices }
        assertEquals(choices.size, choices.map { it.id }.distinct().size)
        assertTrue(InterestCatalog.questions.all { it.choices.size >= 3 })
        assertTrue(
            choices.all { choice ->
                choice.topics.isNotEmpty() &&
                    choice.topics.all { name -> InterestCatalog.topics.any { it.label == name } }
            }
        )
        val selected = listOf("Hiking")
        val suggestions = InterestCatalog.suggestions(selected, "", setOf("afternoon-outdoors"))
        assertFalse(suggestions.any { it.label == "Hiking" })
        assertTrue(
            suggestions.any {
                it.label == "Cycling" && it.reason.contains("scene", ignoreCase = true)
            }
        )
        assertEquals(listOf("Hiking"), selected)
    }

    @Test
    fun relatedSuggestionsCycleThroughEachSelectedCategoryBeforeRepeating() {
        val suggestions =
            InterestCatalog.suggestions(listOf("Indie games", "Cooking", "Live music"), "")
        assertEquals(
            listOf("games", "food", "music", "games", "food", "music", "games", "food"),
            suggestions.map { suggestion ->
                InterestCatalog.topics.single { it.label == suggestion.label }.groupId
            },
        )
    }

    @Test
    fun relatedSuggestionsSkipDislikedAndSelectedTopicsWithoutLosingCategoryTurns() {
        val suggestions =
            InterestCatalog.suggestions(
                listOf("Indie games", "Cooking", "Live music"),
                "I do not like board games or baking or indie rock.",
                limit = 3,
            )
        assertEquals(listOf("Co-op games", "Coffee", "Vinyl records"), suggestions.map { it.label })
    }

    @Test
    fun introductionAndSceneSuggestionsKeepPriorityAheadOfBalancedRelatedTopics() {
        val suggestions =
            InterestCatalog.suggestions(
                listOf("Indie games", "Cooking", "Live music"),
                "I enjoy photography.",
                setOf("afternoon-outdoors"),
                limit = 7,
            )
        assertEquals(
            listOf("Photography", "Hiking", "City walks", "Cycling"),
            suggestions.take(4).map { it.label },
        )
        assertTrue(suggestions.first().reason.contains("introduction"))
        assertTrue(suggestions.drop(1).take(3).all { it.reason.contains("scene") })
        assertEquals(
            listOf("games", "food", "music"),
            suggestions.drop(4).map { suggestion ->
                InterestCatalog.topics.single { it.label == suggestion.label }.groupId
            },
        )
    }

    @Test
    fun photographyPhrasalMentionsRespectWordBoundariesAndNegation() {
        assertEquals(
            listOf("Photography", "Indie games"),
            InterestCatalog.fromText("I take photos and enjoy indie games."),
        )
        assertEquals(listOf("Photography"), InterestCatalog.fromText("I enjoy taking photos."))
        assertTrue(InterestCatalog.fromText("I study photosynthesis.").isEmpty())
        assertTrue(InterestCatalog.fromText("I do not like taking photos.").isEmpty())
        assertTrue(InterestCatalog.fromText("I do not like to take photos.").isEmpty())
    }

    @Test
    fun suggestionsExplainTheirEvidenceExcludeSelectedAndRemainBounded() {
        val suggestions =
            InterestCatalog.suggestions(
                listOf("Hiking"),
                "I enjoy live music and photography.",
                limit = 3,
            )
        assertEquals(3, suggestions.size)
        assertEquals(listOf("Live music", "Photography"), suggestions.take(2).map { it.label })
        assertTrue(
            suggestions.take(2).all { it.reason.contains("introduction", ignoreCase = true) }
        )
        assertTrue(suggestions.last().reason.contains("Outdoors", ignoreCase = true))
        assertFalse(suggestions.any { it.label == "Hiking" })
        assertTrue(suggestions.all { it.reason.isNotBlank() })
        assertTrue(InterestCatalog.suggestions(emptyList(), "", setOf("not-a-choice")).isEmpty())
        assertTrue(InterestCatalog.suggestions(emptyList(), "Hiking", limit = 0).isEmpty())
    }
}
