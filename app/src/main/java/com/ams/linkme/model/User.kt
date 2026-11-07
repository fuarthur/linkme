package com.ams.linkme.model

/** Public, deliberately contact-free profile. Identity always comes from the document path. */
data class User(
    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val discoverable: Boolean = false,
)
