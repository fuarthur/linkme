package com.ams.linkme.data

import android.content.Context
import com.ams.linkme.domain.WikiTopicIndex
import com.ams.linkme.domain.WikiTopicReferences

/** Public packaged data only. AAPT expands source .gz assets and strips their extension. */
object WikiTopicRepository {
    @Volatile private var cached: WikiTopicIndex? = null

    fun load(context: Context): WikiTopicIndex {
        cached?.let {
            return it
        }
        return synchronized(this) {
            cached
                ?: context.applicationContext.assets
                    .open("wiki-topics/topics.tsv")
                    .use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).use(WikiTopicIndex::read)
                    }
                    .also {
                        check(it.size == WikiTopicReferences.TOPIC_COUNT) {
                            "Wiki topic snapshot does not match this app"
                        }
                        cached = it
                    }
        }
    }
}
