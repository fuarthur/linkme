package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.data.userMessage
import com.ams.linkme.domain.LinkMeLogic
import com.ams.linkme.domain.MessageHistory
import com.ams.linkme.model.Conversation
import com.ams.linkme.model.Message
import com.google.firebase.firestore.ListenerRegistration

data class InboxState(
    val loading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null,
    val offline: Boolean = false,
    val hasOlder: Boolean = false,
    val loadingOlder: Boolean = false,
)

class InboxViewModel : ViewModel() {
    private val repo = LinkMeRepository.instance
    val state = MutableLiveData(InboxState())
    private var historyListener: ListenerRegistration? = null
    private var chatsListener: ListenerRegistration? = null
    private val history = MessageHistory()
    private var summaries = emptyList<Conversation>()
    private val profiles = mutableMapOf<String, com.ams.linkme.model.User?>()
    private val requestedProfiles = mutableSetOf<String>()
    private var blocked = emptySet<String>()
    private var epoch = 0
    private var cursor: Message? = null
    private var firstServerPage = true
    private var historyError: String? = null
    private var summaryError: String? = null
    private var pagingError: String? = null

    fun start() {
        if (historyListener != null) return
        val token = ++epoch
        firstServerPage = true
        requestedProfiles.clear()
        historyError = null
        summaryError = null
        pagingError = null
        state.value =
            state.value!!.copy(
                loading = state.value!!.conversations.isEmpty(),
                loadingOlder = false,
                error = null,
            )
        repo.blocked { result ->
            if (token == epoch)
                result.onSuccess {
                    blocked = it
                    render()
                }
        }
        historyListener =
            repo.observeInbox { result ->
                if (token != epoch) return@observeInbox
                result.fold(
                    { update ->
                        history.updateRecent(update.items)
                        if (firstServerPage && !update.fromCache) {
                            firstServerPage = false
                            cursor = update.items.lastOrNull { !it.pending }
                            state.value = state.value!!.copy(hasOlder = update.items.size == 100)
                        }
                        historyError = null
                        state.value = state.value!!.copy(offline = update.fromCache)
                        render()
                    },
                    {
                        historyError = it.userMessage()
                        render()
                    },
                )
            }
        chatsListener =
            repo.observeChats { result ->
                if (token != epoch) return@observeChats
                result.fold(
                    {
                        summaries = it
                        summaryError = null
                        render()
                    },
                    {
                        summaryError = it.userMessage()
                        render()
                    },
                )
            }
    }

    fun loadOlder() {
        val previous = state.value!!
        if (previous.loadingOlder || !previous.hasOlder) return
        val boundary = cursor ?: return
        val token = epoch
        pagingError = null
        state.value = previous.copy(loadingOlder = true, error = historyError ?: summaryError)
        repo.olderInbox(boundary) { result ->
            if (token != epoch) return@olderInbox
            result.fold(
                { page ->
                    history.addOlder(page)
                    cursor = page.lastOrNull() ?: cursor
                    state.value =
                        state.value!!.copy(loadingOlder = false, hasOlder = page.size == 100)
                    render()
                },
                {
                    pagingError = it.userMessage()
                    state.value = state.value!!.copy(loadingOlder = false)
                    render()
                },
            )
        }
    }

    private fun render() {
        val id = repo.uid ?: return
        val recent = history.messages.map { Conversation(it.peerUid(id), it) }
        val items =
            (summaries + recent)
                .filter { it.peerUid.isNotBlank() && it.peerUid != id && it.peerUid !in blocked }
                .groupBy { it.peerUid }
                .map { (_, values) ->
                    values.maxWith(
                        Comparator { a, b ->
                            LinkMeLogic.messageComparator.compare(a.message, b.message)
                        }
                    )
                }
                .sortedWith(
                    Comparator { a, b ->
                        LinkMeLogic.messageComparator.compare(b.message, a.message)
                    }
                )
                .map { it.copy(profile = profiles[it.peerUid]) }
        state.value =
            state.value!!.copy(
                loading = false,
                conversations = items,
                error = historyError ?: summaryError ?: pagingError,
            )
        val token = epoch
        items.forEach { item ->
            if (requestedProfiles.add(item.peerUid))
                repo.publicProfile(item.peerUid, refresh = true) { profile ->
                    if (repo.uid == id && token == epoch) {
                        profiles[item.peerUid] = profile
                        render()
                    }
                }
        }
    }

    fun stop() {
        ++epoch
        historyListener?.remove()
        chatsListener?.remove()
        historyListener = null
        chatsListener = null
    }

    fun retry() {
        stop()
        start()
    }

    override fun onCleared() {
        stop()
    }
}
