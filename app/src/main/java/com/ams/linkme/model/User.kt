package com.ams.linkme.model

data class User (
    val username: String? = null,
    val email: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val interests: ArrayList<String>? = null,
    val uid: String? = null
)