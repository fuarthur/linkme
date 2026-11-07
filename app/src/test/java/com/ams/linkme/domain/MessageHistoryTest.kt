package com.ams.linkme.domain

import com.ams.linkme.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageHistoryTest {
    private fun message(number: Int) =
        Message(
            id = "message-$number",
            content = "Message $number",
            timestampSeconds = number.toLong(),
            timestampNanoseconds = 123,
        )

    @Test
    fun advancingLiveWindowKeepsTheBoundaryOfLoadedHistory() {
        val history = MessageHistory()
        history.updateRecent((101..200).map(::message))
        history.addOlder((1..100).map(::message))
        history.updateRecent((102..201).map(::message))
        assertEquals((1..201).map { "message-$it" }, history.messages.map { it.id })
    }

    @Test
    fun multipleArrivalsDoNotCutHolesBetweenPagedAndLiveMessages() {
        val history = MessageHistory()
        history.updateRecent((101..200).map(::message))
        history.addOlder((1..100).map(::message))
        history.updateRecent((151..250).map(::message))
        history.updateRecent((201..300).map(::message))
        assertEquals((1..300).map { "message-$it" }, history.messages.map { it.id })
    }

    @Test
    fun rejectedPendingMessageDisappearsWhenItLeavesTheLiveSnapshot() {
        val history = MessageHistory()
        val pending = message(2).copy(pending = true)
        history.updateRecent(listOf(message(1), pending))
        history.addOlder(listOf(message(0)))
        assertEquals(listOf("message-0", "message-1", "message-2"), history.messages.map { it.id })
        history.updateRecent(listOf(message(1)))
        assertEquals(listOf("message-0", "message-1"), history.messages.map { it.id })
    }

    @Test
    fun acknowledgementReplacesPendingAndSurvivesLaterWindowAdvances() {
        val history = MessageHistory()
        val pending = message(1).copy(pending = true, timestampNanoseconds = 999)
        val acknowledged = message(1).copy(timestampNanoseconds = 400)
        history.updateRecent(listOf(pending))
        history.updateRecent(listOf(acknowledged))
        assertEquals(listOf(acknowledged), history.messages)
        history.updateRecent(listOf(message(2)))
        assertEquals(listOf(acknowledged, message(2)), history.messages)
        assertFalse(history.messages.first().pending)
    }

    @Test
    fun aPendingCopyInAnOlderPageCannotSurviveRollback() {
        val history = MessageHistory()
        val pending = message(2).copy(pending = true)
        history.updateRecent(listOf(message(1), pending))
        history.addOlder(listOf(message(0), pending))
        history.updateRecent(listOf(message(1)))
        assertEquals(listOf("message-0", "message-1"), history.messages.map { it.id })
    }

    @Test
    fun pagedAcknowledgementWinsOverAnOlderPendingSnapshot() {
        val history = MessageHistory()
        val acknowledged = message(2)
        history.updateRecent(listOf(acknowledged.copy(pending = true)))
        history.addOlder(listOf(acknowledged))
        assertEquals(listOf(acknowledged), history.messages)
    }

    @Test
    fun disjointResumedWindowKeepsLoadedHistoryAndExposesTheMissingRangeCursor() {
        val history = MessageHistory()
        history.updateRecent((101..200).map(::message))
        history.addOlder((1..100).map(::message))
        history.updateRecent((301..400).map(::message))
        assertTrue(history.hasGap)
        assertEquals("message-301", history.gapCursor?.id)
        assertEquals(
            (1..200).map { "message-$it" } + (301..400).map { "message-$it" },
            history.messages.map { it.id },
        )
    }

    @Test
    fun repeatedBoundedGapPagesContinueUntilTheyOverlapKnownHistory() {
        val history = MessageHistory()
        history.updateRecent((1..100).map(::message))
        history.updateRecent((401..500).map(::message))
        assertTrue(history.hasGap)
        history.addGapPage(history.gapCursor!!, (301..400).map(::message))
        assertEquals("message-301", history.gapCursor?.id)
        history.addGapPage(history.gapCursor!!, (201..300).map(::message))
        assertEquals("message-201", history.gapCursor?.id)
        history.addGapPage(history.gapCursor!!, (101..200).map(::message))
        assertTrue(history.hasGap)
        history.addGapPage(history.gapCursor!!, (1..100).map(::message))
        assertFalse(history.hasGap)
        assertNull(history.gapCursor)
        assertEquals((1..500).map { "message-$it" }, history.messages.map { it.id })
    }

    @Test
    fun overlappingLiveUpdatesPreserveTheInProgressGapCursorAndPendingRollback() {
        val history = MessageHistory()
        history.updateRecent((1..100).map(::message))
        history.updateRecent((401..500).map(::message))
        assertTrue(history.hasGap)
        history.addGapPage(history.gapCursor!!, (301..400).map(::message))
        history.updateRecent((403..501).map(::message) + message(502).copy(pending = true))
        assertEquals("message-301", history.gapCursor?.id)
        history.updateRecent((402..501).map(::message))
        assertEquals("message-301", history.gapCursor?.id)
        assertFalse(history.messages.any { it.id == "message-502" })
    }

    @Test
    fun anotherDisjointWindowDoesNotOverwriteAnEarlierUnfinishedGap() {
        val history = MessageHistory()
        history.updateRecent((1..100).map(::message))
        history.updateRecent((201..300).map(::message))
        assertTrue(history.hasGap)
        val firstCursor = history.gapCursor!!
        history.updateRecent((401..500).map(::message))
        assertTrue(history.hasGap)
        // A previously requested page can finish after a newer gap has appeared.
        history.addGapPage(firstCursor, (101..200).map(::message))
        assertEquals("message-401", history.gapCursor?.id)
        history.addGapPage(history.gapCursor!!, (301..400).map(::message))
        history.addGapPage(history.gapCursor!!, (201..300).map(::message))
        assertEquals("message-101", history.gapCursor?.id)
        history.addGapPage(history.gapCursor!!, (1..100).map(::message))
        assertFalse(history.hasGap)
        assertEquals((1..500).map { "message-$it" }, history.messages.map { it.id })
    }

    @Test
    fun anExhaustedGapPageClosesTheGapWithoutDiscardingLoadedMessages() {
        val history = MessageHistory()
        history.updateRecent((1..100).map(::message))
        history.updateRecent((301..400).map(::message))
        assertTrue(history.hasGap)
        history.addGapPage(history.gapCursor!!, emptyList())
        assertFalse(history.hasGap)
        assertEquals(200, history.messages.size)
    }

    @Test
    fun overlappingPagesDedupeAndPreserveExactNanosecondOrder() {
        val history = MessageHistory()
        val earlier = message(1).copy(id = "z", timestampNanoseconds = 100)
        val later = message(1).copy(id = "a", timestampNanoseconds = 200)
        history.updateRecent(listOf(later))
        history.addOlder(listOf(later, earlier))
        assertEquals(listOf(earlier, later), history.messages)
    }
}
