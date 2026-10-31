package com.ams.linkme.domain

import java.io.Reader
import java.text.Normalizer
import java.util.Locale

/** One public Wikidata entity; [value] is its schema-compatible, user-selected stored value. */
data class WikiTopic(
    val wikidataId: String,
    val title: String,
    val domain: String,
    val description: String,
    val value: String,
)

/** Immutable offline library. No Android, network, profile inference, or starter-alias resolution. */
class WikiTopicIndex private constructor(entries: List<Entry>) {
    private val entries = entries.sortedWith(compareBy<Entry> { it.title }.thenBy { it.topic.title }.thenBy { it.topic.wikidataId })
    val topics: List<WikiTopic> = this.entries.map { it.topic }
    val domains: List<String> = topics.map { it.domain }.distinct().sortedWith(compareBy<String> { folded(it) }.thenBy { it })
    val size: Int get() = topics.size
    private val knownDomains = domains.toSet()

    /**
     * Every matching sense remains a separate result. Ties use the library's deterministic
     * title/ID order. The default limit deliberately returns the full virtualized list.
     */
    fun search(query: String, domain: String? = null, limit: Int = Int.MAX_VALUE): List<WikiTopic> {
        if (limit <= 0 || (domain != null && domain !in knownDomains)) return emptyList()
        val needle = folded(query)
        val candidates = if (domain == null) entries else entries.filter { it.topic.domain == domain }
        if (needle.isEmpty()) return candidates.asSequence().take(limit).map { it.topic }.toList()
        val words = needle.split(' ').filter { it.isNotEmpty() }
        // Buckets retain pre-sorted order without sorting 31k pairs for each keystroke.
        val ranked = Array(7) { mutableListOf<WikiTopic>() }
        for (entry in candidates) {
            val rank = entry.rank(needle, words) ?: continue
            ranked[rank].add(entry.topic)
        }
        val result = ArrayList<WikiTopic>(minOf(limit, candidates.size))
        for (bucket in ranked) {
            for (topic in bucket) {
                result.add(topic)
                if (result.size == limit) return result
            }
        }
        return result
    }

    private class Entry(val topic: WikiTopic) {
        val title = folded(topic.title)
        private val description = folded(topic.description)
        private val domain = folded(topic.domain)

        fun rank(needle: String, words: List<String>): Int? =
            when {
                title == needle -> 0
                title.startsWith(needle) -> 1
                hasWordPrefix(title, needle) -> 2
                title.contains(needle) -> 3
                words.size > 1 && words.all { title.contains(it) } -> 4
                description.contains(needle) || words.all { title.contains(it) || description.contains(it) } -> 5
                domain.contains(needle) -> 6
                else -> null
            }
    }

    companion object {
        private const val HEADER = "wikidata_id\ttitle\tdomain\tdescription\tvalue"
        private val QID = Regex("Q[1-9][0-9]*")

        /**
         * Reads the five-column TSV after decompression. Invalid data throws one
         * IllegalArgumentException; it never returns a silently incomplete library.
         * Ownership of the supplied Reader remains with the caller.
         */
        fun read(reader: Reader): WikiTopicIndex {
            val input = reader.buffered()
            require(input.readLine() == HEADER) { "Unsupported Wiki topic header" }
            val ids = hashSetOf<String>()
            val values = hashSetOf<String>()
            val entries = ArrayList<Entry>()
            var lineNumber = 1
            while (true) {
                val line = input.readLine() ?: break
                lineNumber++
                val columns = line.split('\t')
                require(columns.size == 5) { "Wiki row $lineNumber must have five columns" }
                val (id, title, domain, description, value) = columns
                require(QID.matches(id)) { "Invalid Wikidata ID at row $lineNumber" }
                require(ids.add(id)) { "Duplicate Wikidata ID at row $lineNumber" }
                require(listOf(title, domain, description, value).all(::validText)) {
                    "Invalid Wiki text at row $lineNumber"
                }
                require(value == title || value == "wiki:$id") { "Mismatched Wiki identity at row $lineNumber" }
                require(value.length in 1..40 && cleaned(value).length in 1..40) {
                    "Wiki stored value exceeds schema bounds at row $lineNumber"
                }
                require(values.add(folded(value))) { "Duplicate normalized Wiki value at row $lineNumber" }
                entries.add(Entry(WikiTopic(id, title, domain, description, value)))
            }
            require(entries.isNotEmpty()) { "The Wiki topic library is empty" }
            return WikiTopicIndex(entries)
        }

        private fun validText(value: String): Boolean {
            var hasVisible = false
            var offset = 0
            while (offset < value.length) {
                val char = value[offset]
                if (Character.isHighSurrogate(char)) {
                    if (offset + 1 >= value.length || !Character.isLowSurrogate(value[offset + 1])) return false
                } else if (Character.isLowSurrogate(char)) {
                    return false
                }
                val cp = Character.codePointAt(value, offset)
                val type = Character.getType(cp)
                if (Character.isISOControl(cp)) return false
                if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp) &&
                    type != Character.FORMAT.toInt() && type != Character.NON_SPACING_MARK.toInt() &&
                    type != Character.ENCLOSING_MARK.toInt()) hasVisible = true
                offset += Character.charCount(cp)
            }
            return hasVisible
        }

        private fun cleaned(value: String): String {
            val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
            val clean = StringBuilder()
            var pendingSpace = false
            var offset = 0
            while (offset < normalized.length) {
                val cp = Character.codePointAt(normalized, offset)
                if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                    if (clean.isNotEmpty()) pendingSpace = true
                } else {
                    if (pendingSpace) clean.append(' ')
                    clean.appendCodePoint(cp)
                    pendingSpace = false
                }
                offset += Character.charCount(cp)
            }
            return clean.toString()
        }

        private fun folded(value: String): String = cleaned(value).lowercase(Locale.ROOT)

        private fun hasWordPrefix(text: String, needle: String): Boolean {
            var from = 0
            while (from < text.length) {
                val index = text.indexOf(needle, from)
                if (index < 0) return false
                if (index == 0 || !Character.isLetterOrDigit(text.codePointBefore(index))) return true
                from = index + 1
            }
            return false
        }
    }
}
