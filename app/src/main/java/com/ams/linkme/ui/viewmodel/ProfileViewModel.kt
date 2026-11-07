package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.data.userMessage
import com.ams.linkme.domain.LinkMeLogic
import com.ams.linkme.model.User

data class ProfileState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val profile: User? = null,
    val error: String? = null,
    val saved: Int = 0,
)

class ProfileViewModel : ViewModel() {
    private val repo = LinkMeRepository.instance
    val state = MutableLiveData(ProfileState())
    var name = ""
    var bio = ""
    val interests = linkedSetOf<String>()
    var discoverable = true
    private var interestsEdited = false
    var draftLoaded = false
    val email: String
        get() = repo.auth.currentUser?.email.orEmpty()

    fun load() {
        if (draftLoaded) return
        state.value = ProfileState()
        repo.loadProfile { result ->
            result.fold(
                { user ->
                    if (!draftLoaded) {
                        name = user.username
                        bio = user.bio
                        if (!interestsEdited) interests.addAll(user.interests)
                        discoverable = if (user.username.isBlank()) true else user.discoverable
                        draftLoaded = true
                    }
                    state.value = ProfileState(loading = false, profile = user)
                },
                { state.value = ProfileState(loading = false, error = it.userMessage()) },
            )
        }
    }

    fun setInterests(values: List<String>) {
        interestsEdited = true
        interests.clear()
        interests.addAll(com.ams.linkme.domain.InterestCatalog.normalize(values))
        state.value = state.value!!.copy(error = null)
    }

    fun save() {
        if (!draftLoaded || state.value?.saving == true) return
        val validation = LinkMeLogic.validateProfile(name, bio, interests.toList())
        if (validation != null) {
            state.value = state.value!!.copy(error = validation)
            return
        }
        val user =
            User(repo.uid.orEmpty(), name.trim(), bio.trim(), interests.toList(), discoverable)
        val previous = state.value!!
        state.value = previous.copy(saving = true, error = null)
        repo.saveProfile(user) { result ->
            state.value =
                result.fold(
                    {
                        previous.copy(
                            saving = false,
                            profile = user,
                            error = null,
                            saved = previous.saved + 1,
                        )
                    },
                    { previous.copy(saving = false, error = it.userMessage()) },
                )
        }
    }
}
