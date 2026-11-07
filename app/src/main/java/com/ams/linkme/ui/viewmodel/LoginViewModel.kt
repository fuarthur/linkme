package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.data.userMessage

data class AuthState(
    val busy: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
    val notice: String? = null,
)

class LoginViewModel : ViewModel() {
    private val repo = LinkMeRepository.instance
    val state = MutableLiveData(AuthState())
    val hasSession: Boolean
        get() = repo.uid != null

    var registering = false
    var email = ""

    fun submit(email: String, password: String, confirmation: String) {
        if (state.value?.busy == true) return
        this.email = email
        val error =
            when {
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                    "Enter a valid email address."
                password.isEmpty() -> "Enter your password."
                registering && password.length < 6 -> "Use at least 6 characters for your password."
                registering && password != confirmation -> "Your passwords do not match."
                else -> null
            }
        if (error != null) {
            state.value = AuthState(error = error)
            return
        }
        state.value = AuthState(busy = true)
        val callback: (Result<Unit>) -> Unit = { result ->
            state.value =
                result.fold({ AuthState(signedIn = true) }, { AuthState(error = it.userMessage()) })
        }
        if (registering) repo.register(email, password, callback)
        else repo.signIn(email, password, callback)
    }

    fun reset(email: String) {
        if (state.value?.busy == true) return
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            state.value = AuthState(error = "Enter your email above, then choose Forgot password.")
            return
        }
        state.value = AuthState(busy = true)
        repo.resetPassword(email) { result ->
            state.value =
                result.fold(
                    {
                        AuthState(
                            notice =
                                "If an account uses this email, a reset link is on its way. Check your inbox."
                        )
                    },
                    { AuthState(error = it.userMessage()) },
                )
        }
    }
}
