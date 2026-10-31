package com.ams.linkme.domain

import com.ams.linkme.model.Message

/** Retains loaded immutable history while pending writes follow the current live snapshot. */
class MessageHistory {
    private val confirmed = mutableMapOf<String, Message>()
    private var pending = emptyMap<String, Message>()

    private data class Gap(var cursor: Message, val knownTail: Message)

    private val gaps = mutableListOf<Gap>()

    fun updateRecent(messages: List<Message>) {
        val incoming = messages.filterNot { it.pending }
        val previousTail = confirmed.values.maxWithOrNull(LinkMeLogic.messageComparator)
        val first = incoming.minWithOrNull(LinkMeLogic.messageComparator)
        if (
            messages.size >= 100 &&
                previousTail != null &&
                first != null &&
                incoming.none { it.id in confirmed } &&
                LinkMeLogic.messageComparator.compare(first, previousTail) > 0
        ) {
            // A bounded listener can skip unseen messages after a long pause.
            gaps.add(Gap(first, previousTail))
        }
        addOlder(messages)
        // A pending message absent from the next snapshot may have been rejected.
        pending = messages.filter { it.pending }.associateBy { it.id }
    }

    fun addOlder(messages: List<Message>) {
        messages.filterNot { it.pending }.forEach { confirmed[it.id] = it }
    }

    val hasGap: Boolean
        get() = gaps.isNotEmpty()

    val gapCursor: Message?
        get() = gaps.lastOrNull()?.cursor

    fun addGapPage(before: Message, messages: List<Message>) {
        addOlder(messages)
        // Match the requested cursor: another live gap may appear while its page is loading.
        val gap = gaps.firstOrNull { it.cursor.id == before.id } ?: return
        val first = messages.filterNot { it.pending }.minWithOrNull(LinkMeLogic.messageComparator)
        if (
            messages.size < 100 ||
                first == null ||
                LinkMeLogic.messageComparator.compare(first, gap.knownTail) <= 0
        ) {
            gaps.remove(gap)
        } else {
            gap.cursor = first
        }
    }

    val messages: List<Message>
        get() = LinkMeLogic.orderMessages((pending + confirmed).values)
}
