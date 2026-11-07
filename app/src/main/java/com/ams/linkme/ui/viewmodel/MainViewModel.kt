package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.data.userMessage
import com.ams.linkme.domain.LinkMeLogic
import com.ams.linkme.model.User

data class DiscoveryState(
    val loading: Boolean = false,
    val people: List<User> = emptyList(),
    val selected: List<String> = emptyList(),
    val error: String? = null,
    val searched: Boolean = false,
)

class MainViewModel : ViewModel() {
    private val repo = LinkMeRepository.instance
    val state = MutableLiveData(DiscoveryState())
    private var generation = 0
    private var manualFilters = false

    fun useProfile(user: User) {
        if (!manualFilters && user.interests.isNotEmpty()) applySelection(user.interests)
    }

    fun select(interests: List<String>) {
        manualFilters = true
        applySelection(interests)
    }

    private fun applySelection(interests: List<String>) {
        val values = com.ams.linkme.domain.InterestCatalog.normalize(interests)
        if (state.value!!.selected == values && (state.value!!.searched || state.value!!.loading))
            return
        state.value = state.value!!.copy(selected = values)
        refresh()
    }

    fun refresh() {
        val selected = state.value!!.selected
        val request = ++generation
        if (selected.isEmpty()) {
            state.value = DiscoveryState(selected = selected)
            return
        }
        state.value = DiscoveryState(loading = true, selected = selected)
        repo.blocked { blocked ->
            if (request != generation) return@blocked
            blocked.fold(
                { excluded ->
                    repo.discover(selected) { result ->
                        if (request != generation) return@discover
                        state.value =
                            result.fold(
                                {
                                    DiscoveryState(
                                        people =
                                            LinkMeLogic.rankProfiles(
                                                    it,
                                                    repo.uid.orEmpty(),
                                                    selected,
                                                    excluded,
                                                )
                                                .take(60),
                                        selected = selected,
                                        searched = true,
                                    )
                                },
                                {
                                    DiscoveryState(
                                        selected = selected,
                                        error = it.userMessage(),
                                        searched = true,
                                    )
                                },
                            )
                    }
                },
                {
                    state.value =
                        DiscoveryState(
                            selected = selected,
                            error = it.userMessage(),
                            searched = true,
                        )
                },
            )
        }
    }
}
