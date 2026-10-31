package com.ams.linkme.domain

import com.ams.linkme.model.Message
import org.junit.Assert.*
import org.junit.Test

class MessageOrderingTest {
    @Test
    fun sameMillisecondMessagesKeepServerNanosecondOrder() {
        val earlier =
            Message(
                id = "z",
                timestamp = 123456,
                timestampSeconds = 123,
                timestampNanoseconds = 456123000,
            )
        val later =
            Message(
                id = "a",
                timestamp = 123456,
                timestampSeconds = 123,
                timestampNanoseconds = 456987000,
            )
        assertEquals(
            listOf("z", "a"),
            LinkMeLogic.orderMessages(listOf(later, earlier)).map { it.id },
        )
    }

    @Test
    fun paginationPreservesExactServerTimestamp() {
        val boundary =
            Message(
                id = "boundary",
                timestamp = 123456,
                timestampSeconds = 123,
                timestampNanoseconds = 456789012,
            )
        assertEquals(123L to 456789012, LinkMeLogic.paginationCursor(boundary))
    }
}
