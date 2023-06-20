package com.ams.linkme.model


data class Message(
    val receiveId: String? = null,
    val senderId: String? = null,
    val content: String? = null,
    val timestamp: Long? = null,
)

