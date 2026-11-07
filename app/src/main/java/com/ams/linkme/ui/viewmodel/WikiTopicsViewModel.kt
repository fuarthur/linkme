package com.ams.linkme.ui.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.ams.linkme.R
import com.ams.linkme.data.WikiTopicRepository
import com.ams.linkme.domain.InterestCatalog
import com.ams.linkme.domain.WikiTopic
import java.util.concurrent.Executors
import java.util.concurrent.Future

data class WikiTopicsState(
    val selected: List<String>,
    val query: String,
    val domain: String?,
    val domains: List<String>,
    val topics: List<WikiTopic>,
    val loading: Boolean,
    val loadFailed: Boolean = false,
    val feedback: String? = null,
)

/** Search runs off the main thread; a completed older request cannot replace a newer draft. */
class WikiTopicsViewModel(application: Application, private val saved: SavedStateHandle) :
    AndroidViewModel(application) {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var pending: Future<*>? = null
    private var generation = 0
    private var closed = false
    private var domains = emptyList<String>()
    private var topics = emptyList<WikiTopic>()
    val state = MutableLiveData(snapshot(loading = true))

    init {
        search()
    }

    private fun selected() =
        InterestCatalog.normalize(saved.get<ArrayList<String>>("selectedInterests").orEmpty())

    private fun snapshot(
        loading: Boolean = false,
        failed: Boolean = false,
        feedback: String? = null,
    ) =
        WikiTopicsState(
            selected(),
            saved.get<String>("initialQuery").orEmpty().take(120),
            saved["wikiDomain"],
            domains,
            topics,
            loading,
            failed,
            feedback,
        )

    fun query(value: String) {
        val bounded = value.take(120)
        if (bounded == state.value!!.query) return
        saved["initialQuery"] = bounded
        search()
    }

    fun domain(value: String?) {
        if (value != null && value !in domains) return
        if (value == state.value!!.domain) return
        saved["wikiDomain"] = value
        search()
    }

    fun retry() = search()

    private fun search() {
        val version = ++generation
        val query = saved.get<String>("initialQuery").orEmpty().take(120)
        val domain = saved.get<String>("wikiDomain")
        pending?.cancel(false)
        topics = emptyList()
        state.value = snapshot(loading = true)
        pending =
            worker.submit {
                try {
                    val index = WikiTopicRepository.load(getApplication())
                    val result = index.search(query, domain)
                    main.post {
                        if (!closed && version == generation) {
                            domains = index.domains
                            topics = result
                            state.value = snapshot()
                        }
                    }
                } catch (_: Exception) {
                    main.post {
                        if (!closed && version == generation) state.value = snapshot(failed = true)
                    }
                }
            }
    }

    fun toggle(topic: WikiTopic) {
        val current = state.value!!
        if (current.loading || current.topics.none { it.wikidataId == topic.wikidataId }) return
        val existing =
            current.selected.firstOrNull {
                InterestCatalog.match(listOf(it), listOf(topic.value)).exact.isNotEmpty()
            }
        if (existing != null) {
            saved["selectedInterests"] = ArrayList(current.selected.filterNot { it == existing })
        } else {
            if (current.selected.size >= InterestCatalog.MAX_INTERESTS) {
                state.value =
                    snapshot(
                        feedback = getApplication<Application>().getString(R.string.wiki_mix_full)
                    )
                return
            }
            saved["selectedInterests"] =
                ArrayList(InterestCatalog.normalize(current.selected + topic.value))
        }
        state.value = snapshot()
    }

    override fun onCleared() {
        closed = true
        generation++
        pending?.cancel(true)
        worker.shutdownNow()
        main.removeCallbacksAndMessages(null)
        super.onCleared()
    }
}
