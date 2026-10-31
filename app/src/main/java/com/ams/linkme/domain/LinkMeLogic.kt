package com.ams.linkme.domain

import com.ams.linkme.model.Message
import com.ams.linkme.model.User
import java.security.MessageDigest
import java.util.Locale

/** Compatibility facade for existing profile and chat callers. */
object Interests {
    val all: List<String> = InterestCatalog.topics.map { it.label }

    fun normalize(values: Collection<String>): List<String> = InterestCatalog.normalize(values)

    fun label(value: String): String = InterestCatalog.label(value)
}

object LinkMeLogic {
    fun conversationId(first: String, second: String): String {
        require(first.isNotBlank() && second.isNotBlank() && first != second) {
            "Choose another person to chat with."
        }
        val key = listOf(first, second).sorted().joinToString("") { "${it.length}:$it" }
        return MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun rankProfiles(
        users: List<User>,
        currentUid: String,
        interests: List<String>,
        blocked: Set<String> = emptySet(),
    ): List<User> {
        val selected = InterestCatalog.normalize(interests)
        if (selected.isEmpty()) return emptyList()
        return users
            .asSequence()
            .filter { user ->
                user.uid.isNotBlank() &&
                    user.uid != currentUid &&
                    user.uid !in blocked &&
                    user.username.isNotBlank()
            }
            .map { it to InterestCatalog.match(selected, it.interests) }
            .filter { it.second.score > 0 }
            .distinctBy { it.first.uid }
            .sortedWith(
                compareByDescending<Pair<User, InterestMatch>> { it.second.score }
                    .thenBy { it.first.username.lowercase(Locale.ROOT) }
                    .thenBy { it.first.uid }
            )
            .map { it.first }
            .toList()
    }

    fun validateProfile(name: String, bio: String, interests: List<String>): String? =
        when {
            name.trim().isEmpty() -> "Add a name so people know what to call you."
            name.trim().length > 40 -> "Keep your name to 40 characters."
            bio.trim().length > 240 -> "Keep your introduction to 240 characters."
            interests.isEmpty() -> "Choose at least one interest."
            interests.size > InterestCatalog.MAX_INTERESTS -> "Choose up to 16 interests."
            interests.any { InterestCatalog.validateLabel(it) != null } ->
                "Use an interest name with 1–40 characters and no control characters."
            InterestCatalog.normalize(interests).size != interests.size ->
                "Choose each interest only once."
            else -> null
        }

    fun validateMessage(content: String): String? =
        when {
            content.isBlank() -> "Write a message first."
            content.trim().length > 2000 -> "Keep your message to 2,000 characters."
            else -> null
        }

    val messageComparator: Comparator<Message> =
        compareBy<Message> {
                if (it.timestampSeconds == null && it.timestamp == null) Long.MAX_VALUE
                else paginationCursor(it).first
            }
            .thenBy { paginationCursor(it).second }
            .thenBy { it.id }

    fun orderMessages(messages: Collection<Message>): List<Message> =
        messages.sortedWith(messageComparator)

    fun paginationCursor(message: Message): Pair<Long, Int> =
        message.timestampSeconds?.let { it to message.timestampNanoseconds }
            ?: ((message.timestamp ?: 0L) / 1000) to
                (((message.timestamp ?: 0L) % 1000) * 1_000_000).toInt()

    fun remainingDraft(current: String, submitted: String): String =
        if (current == submitted) "" else current
}
