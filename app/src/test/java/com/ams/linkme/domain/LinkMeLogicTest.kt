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
            listOf("Sport", "Music", "Movie"),
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
    fun profileRejectsUnsupportedAndLongValues() {
        assertNotNull(LinkMeLogic.validateProfile("A".repeat(41), "", listOf("Music")))
        assertNotNull(LinkMeLogic.validateProfile("Ari", "x".repeat(241), listOf("Music")))
        assertNotNull(LinkMeLogic.validateProfile("Ari", "", listOf("Unknown")))
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
