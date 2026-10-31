package com.ams.linkme.domain

import com.ams.linkme.model.Message
import com.ams.linkme.model.User
import java.security.MessageDigest
import java.util.Locale

object Interests {
    val all = listOf("Sport", "Food", "Dating", "Games", "Music", "Anime", "Movie", "Other")

    fun normalize(values: Collection<String>): List<String> =
        values
            .mapNotNull { raw ->
                val normalized =
                    when (raw.trim().lowercase(Locale.ROOT)) {
                        "sports",
                        "sport" -> "Sport"
                        "movies",
                        "movie",
                        "film",
                        "films" -> "Movie"
                        else -> all.firstOrNull { it.equals(raw.trim(), ignoreCase = true) }
                    }
                normalized
            }
            .distinct()
            .take(8)

    fun label(value: String): String =
        when (value) {
            "Sport" -> "Sports"
            "Movie" -> "Movies"
            else -> value
        }
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
        val selected = Interests.normalize(interests).toSet()
        return users
            .filter { user ->
                !user.uid.isNullOrBlank() &&
                    user.uid != currentUid &&
                    user.uid !in blocked &&
                    !user.username.isNullOrBlank() &&
                    Interests.normalize(user.interests.orEmpty()).any { it in selected }
            }
            .distinctBy { it.uid }
            .sortedWith(
                compareByDescending<User> {
                        Interests.normalize(it.interests.orEmpty()).count { interest ->
                            interest in selected
                        }
                    }
                    .thenBy { it.username.orEmpty().lowercase(Locale.ROOT) }
                    .thenBy { it.uid }
            )
    }

    fun validateProfile(name: String, bio: String, interests: List<String>): String? =
        when {
            name.trim().isEmpty() -> "Add a name so people know what to call you."
            name.trim().length > 40 -> "Keep your name to 40 characters."
            bio.trim().length > 240 -> "Keep your introduction to 240 characters."
            interests.isEmpty() -> "Choose at least one interest."
            interests.size > 8 ||
                interests.distinct().size != interests.size ||
                interests.any { it !in Interests.all } -> "Choose interests from the list."
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
