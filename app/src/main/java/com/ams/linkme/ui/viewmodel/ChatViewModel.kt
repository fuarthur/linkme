package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.data.userMessage
import com.ams.linkme.domain.LinkMeLogic
import com.ams.linkme.domain.MessageHistory
import com.ams.linkme.model.Message
import com.google.firebase.firestore.ListenerRegistration

data class ChatState(
    val loading: Boolean = true,
    val messages: List<Message> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null,
    val offline: Boolean = false,
    val completed: Int = 0,
    val loadingOlder: Boolean = false,
    val hasOlder: Boolean = false,
    val hasGap: Boolean = false,
    val blocked: Boolean = false,
)

class ChatViewModel : ViewModel() {
    private val repo = LinkMeRepository.instance
    val state = MutableLiveData(ChatState())
    var peer = ""
    var peerName = ""
    var draft = ""
    val currentUid: String
        get() = repo.uid.orEmpty()

    private var registration: ListenerRegistration? = null
    private var epoch = 0
    private var recent = emptyList<Message>()
    private val history = MessageHistory()
    private var loadedOlder = false
    private var olderAvailable = false
    private var failedSubmission: Pair<String, String>? = null

    fun start() {
        if (registration != null || peer.isBlank()) return
        val token = ++epoch
        state.value = state.value!!.copy(loading = recent.isEmpty(), error = null)
        repo.blocked { result ->
            if (token == epoch)
                result.onSuccess { state.value = state.value!!.copy(blocked = peer in it) }
        }
        registration =
            repo.observeMessages(peer) { result ->
                if (token != epoch) return@observeMessages
                result.fold(
                    { update ->
                        recent = update.items
                        history.updateRecent(recent)
                        if (!loadedOlder) olderAvailable = recent.size == 100
                        merge(
                            state.value!!.copy(
                                loading = false,
                                error = null,
                                offline = update.fromCache,
                            )
                        )
                    },
                    { state.value = state.value!!.copy(loading = false, error = it.userMessage()) },
                )
            }
    }

    private fun merge(next: ChatState) {
        state.value =
            next.copy(
                messages = history.messages,
                hasOlder = olderAvailable || history.hasGap,
                hasGap = history.hasGap,
            )
    }

    fun loadOlder() {
        val previous = state.value!!
        if (previous.loadingOlder) return
        val gapCursor = history.gapCursor
        val first = gapCursor ?: previous.messages.firstOrNull() ?: return
        state.value = previous.copy(loadingOlder = true, error = null)
        repo.olderMessages(peer, first) { result ->
            result.fold(
                { messages ->
                    if (gapCursor != null) {
                        history.addGapPage(gapCursor, messages)
                    } else {
                        history.addOlder(messages)
                        loadedOlder = true
                        olderAvailable = messages.size == 100
                    }
                    merge(state.value!!.copy(loadingOlder = false))
                },
                { state.value = state.value!!.copy(loadingOlder = false, error = it.userMessage()) },
            )
        }
    }

    fun send() {
        if (state.value!!.sending || state.value!!.blocked) return
        val submitted = draft
        val validation = LinkMeLogic.validateMessage(submitted)
        if (validation != null) {
            state.value = state.value!!.copy(error = validation)
            return
        }
        val id = failedSubmission?.takeIf { it.second == submitted }?.first ?: repo.newMessageId()
        state.value = state.value!!.copy(sending = true, error = null)
        repo.send(peer, submitted, id) { result ->
            result.fold(
                {
                    draft = LinkMeLogic.remainingDraft(draft, submitted)
                    failedSubmission = null
                    state.value =
                        state.value!!.copy(sending = false, completed = state.value!!.completed + 1)
                },
                {
                    failedSubmission = id to submitted
                    state.value = state.value!!.copy(sending = false, error = it.userMessage())
                },
            )
        }
    }

    fun toggleBlock() {
        val next = !state.value!!.blocked
        repo.setBlocked(peer, next) { result ->
            state.value =
                result.fold(
                    { state.value!!.copy(blocked = next, error = null) },
                    { state.value!!.copy(error = it.userMessage()) },
                )
        }
    }

    fun stop() {
        ++epoch
        registration?.remove()
        registration = null
    }

    fun retry() {
        stop()
        start()
    }

    override fun onCleared() {
        stop()
    }
}
