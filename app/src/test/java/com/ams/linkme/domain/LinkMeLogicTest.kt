package com.ams.linkme.domain

import com.ams.linkme.model.User
import org.junit.Assert.*
import org.junit.Test

class LinkMeLogicTest {
    @Test
    fun conversationIdentityIsSymmetric() {
        assertEquals(
            LinkMeLogic.conversationId("alice", "bob"),
            LinkMeLogic.conversationId("bob", "alice"),
        )
    }

    @Test
    fun conversationIdentityCannotCollideOnSeparators() {
        assertNotEquals(
            LinkMeLogic.conversationId("a_b", "c"),
            LinkMeLogic.conversationId("a", "b_c"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotChatWithSelf() {
        LinkMeLogic.conversationId("a", "a")
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotChatWithMissingRecipient() {
        LinkMeLogic.conversationId("a", " ")
    }

    @Test
    fun interestsNormalizeLegacyPluralAndCaseAndKeepMovies() {
        assertEquals(
            listOf("Sport", "Music", "Movie", "unknown"),
            Interests.normalize(listOf("sports", " Music ", "music", "MOVIES", "unknown")),
        )
    }

    @Test
    fun discoveryExcludesSelfBlockedAndUnrelatedAndRanksSharedInterests() {
        val people =
            listOf(
                User(uid = "self", username = "Me", interests = arrayListOf("Music", "Games")),
                User(uid = "one", username = "Zoe", interests = arrayListOf("Music")),
                User(uid = "two", username = "Ari", interests = arrayListOf("Music", "Games")),
                User(uid = "blocked", username = "Ben", interests = arrayListOf("Music", "Games")),
                User(uid = "other", username = "May", interests = arrayListOf("Food")),
            )
        assertEquals(
            listOf("two", "one"),
            LinkMeLogic.rankProfiles(people, "self", listOf("Music", "Games"), setOf("blocked"))
                .map { it.uid },
        )
    }

    @Test
    fun discoveryTieOrderIsStable() {
        val people =
            listOf(
                User(uid = "z", username = "Alex", interests = arrayListOf("Music")),
                User(uid = "a", username = "Alex", interests = arrayListOf("Music")),
            )
        assertEquals(
            listOf("a", "z"),
            LinkMeLogic.rankProfiles(people, "me", listOf("Music")).map { it.uid },
        )
    }

    @Test
    fun validProfileAcceptsTrimmedValues() {
        assertNull(LinkMeLogic.validateProfile("Ari", "I like live music", listOf("Music")))
    }

    @Test
    fun profileRequiresNameAndInterest() {
        assertNotNull(LinkMeLogic.validateProfile(" ", "", listOf("Music")))
        assertNotNull(LinkMeLogic.validateProfile("Ari", "", emptyList()))
    }

    @Test
    fun profileRejectsLongValues() {
        assertNotNull(LinkMeLogic.validateProfile("A".repeat(41), "", listOf("Music")))
        assertNotNull(LinkMeLogic.validateProfile("Ari", "x".repeat(241), listOf("Music")))
    }

    @Test
    fun interestsPreserveCustomUnicodeDisplayAndDeduplicateNormalizedVariants() {
        assertEquals(
            listOf("陶艺 Club", "Live music"),
            Interests.normalize(listOf("  陶艺　Club  ", "陶艺 CLUB", "Ｌｉｖｅ　ｍｕｓｉｃ", "live music")),
        )
    }

    @Test
    fun profileSupportsSixteenCustomLabels() {
        val sixteen = (1..16).map { "My topic $it" }
        assertNull(LinkMeLogic.validateProfile("Ari", "", sixteen))
        assertNotNull(LinkMeLogic.validateProfile("Ari", "", sixteen + "One too many"))
        assertNull(LinkMeLogic.validateProfile("Ari", "", listOf("  陶艺 Club  ")))
    }

    @Test
    fun profileRejectsAliasAndCustomDuplicates() {
        assertNotNull(LinkMeLogic.validateProfile("Ari", "", listOf("Movies", "film")))
        assertNotNull(LinkMeLogic.validateProfile("Ari", "", listOf("陶艺 Club", " 陶艺　CLUB ")))
    }

    @Test
    fun profileRejectsEmptyLongAndControlCharacterLabels() {
        for (label in listOf(" ", "x".repeat(41), "Live\nmusic", "Art\u0000", "x\u007f")) {
            assertNotNull(label, LinkMeLogic.validateProfile("Ari", "", listOf(label)))
        }
        assertNull(LinkMeLogic.validateProfile("Ari", "", listOf("x".repeat(40))))
    }

    @Test
    fun discoveryRanksExactSpecificTopicBeforeMultipleBroadMatches() {
        val people =
            listOf(
                User(
                    uid = "related",
                    username = "Ari",
                    interests = listOf("Music", "Games", "Food"),
                ),
                User(uid = "exact", username = "Zoe", interests = listOf("Indie games")),
            )
        assertEquals(
            listOf("exact", "related"),
            LinkMeLogic.rankProfiles(people, "me", listOf("Indie games", "Live music", "Cooking"))
                .map { it.uid },
        )
    }

    @Test
    fun discoveryMatchesCustomLabelsByNormalizedName() {
        val people =
            listOf(
                User(uid = "same", username = "Zoe", interests = listOf("Ｃｉｔｙ　Sketchers")),
                User(uid = "other", username = "Ari", interests = listOf("Different club")),
            )
        assertEquals(
            listOf("same"),
            LinkMeLogic.rankProfiles(people, "me", listOf(" city sketchers ")).map { it.uid },
        )
    }

    @Test
    fun messageRejectsWhitespaceAndOversizedContent() {
        assertNotNull(LinkMeLogic.validateMessage(" \n\t "))
        assertNotNull(LinkMeLogic.validateMessage("x".repeat(2001)))
        assertNull(LinkMeLogic.validateMessage("Hey, what are you listening to?"))
    }

    @Test
    fun acknowledgementClearsOnlyTheSubmittedDraft() {
        assertEquals("", LinkMeLogic.remainingDraft("First message", "First message"))
        assertEquals("New draft", LinkMeLogic.remainingDraft("New draft", "First message"))
        assertEquals(
            "First message plus more",
            LinkMeLogic.remainingDraft("First message plus more", "First message"),
        )
    }
}
