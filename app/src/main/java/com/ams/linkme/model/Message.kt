package com.ams.linkme.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiveId: String = "",
    val content: String = "",
    val timestamp: Long? = null,
    val pending: Boolean = false,
    val timestampSeconds: Long? = null,
    val timestampNanoseconds: Int = 0,
) {
    fun peerUid(currentUid: String): String = if (senderId == currentUid) receiveId else senderId
}

data class Conversation(val peerUid: String, val message: Message, val profile: User? = null)

data class StreamUpdate<T>(val items: List<T>, val fromCache: Boolean)
