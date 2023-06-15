package com.ams.linkme.model

data class Message(
    val messageId: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val content: String? = null,
    val timestamp: Long? = null
)

