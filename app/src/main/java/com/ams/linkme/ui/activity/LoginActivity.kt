package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R
import com.ams.linkme.ui.*
import com.ams.linkme.ui.viewmodel.AuthState
import com.ams.linkme.ui.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var model: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[LoginViewModel::class.java]
        // Auth can complete while registration is still waiting for its profile write.
        if (model.hasSession && model.state.value?.busy != true) {
            openMain()
            return
        }
        val root = column()
        root.addView(previewBanner())
        val centered = FrameLayout(this)
        val form = column(28)
        val brand = row()
        brand.addView(avatar("LinkMe", 56))
        brand.addView(label("LinkMe", 24f, true).apply { space(start = 14) })
        form.addView(brand)
        val title = label("", 34f, true).apply { space(top = 36) }
        form.addView(title)
        val subtitle =
            label("Meet through the things you love.", 17f, secondary = true).apply {
                space(top = 12, bottom = 28)
            }
        form.addView(subtitle)
        val (emailBox, email) = field("Email", R.id.email, email = true)
        form.addView(emailBox)
        email.setText(model.email)
        email.doAfterTextChanged { model.email = it.toString() }
        val (passwordBox, password) = field("Password", R.id.password, password = true)
        passwordBox.space(top = 12)
        form.addView(passwordBox)
        val (confirmationBox, confirmation) =
            field("Confirm password", R.id.password_confirmation, password = true)
        confirmationBox.space(top = 12)
        form.addView(confirmationBox)
        val feedback =
            label("", 14f).apply {
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                space(top = 16)
            }
        form.addView(feedback)
        val progress = spinner()
        form.addView(progress)
        val submit =
            button("").apply {
                id = R.id.auth_submit
                space(top = 16)
                setOnClickListener {
                    model.submit(
                        email.text.toString(),
                        password.text.toString(),
                        confirmation.text.toString(),
                    )
                }
            }
        form.addView(submit)
        val forgot =
            button("Forgot password?", textOnly = true).apply {
                setOnClickListener { model.reset(email.text.toString()) }
            }
        form.addView(forgot)
        val switch =
            button("", outlined = true).apply {
                space(top = 20)
                setOnClickListener {
                    model.registering = !model.registering
                    model.state.value = AuthState()
                }
            }
        form.addView(switch)
        val privacy =
            label(
                    "Your interests are how you connect. Your email stays private.",
                    13f,
                    secondary = true,
                )
                .apply {
                    space(top = 28)
                    gravity = Gravity.CENTER
                }
        form.addView(privacy)
        fun render(state: AuthState) {
            title.text = if (model.registering) "Find your people." else "Good to see you."
            subtitle.text =
                if (model.registering) "A shared interest is a good place to start."
                else "Sign in and pick up the conversation."
            confirmationBox.visibility = if (model.registering) View.VISIBLE else View.GONE
            forgot.visibility = if (model.registering) View.GONE else View.VISIBLE
            submit.text =
                if (state.busy) "Please wait…"
                else if (model.registering) "Create account" else "Sign in"
            switch.text =
                if (model.registering) "Already a member? Sign in"
                else "New here? Create an account"
            submit.isEnabled = !state.busy
            switch.isEnabled = !state.busy
            forgot.isEnabled = !state.busy
            email.isEnabled = !state.busy
            password.isEnabled = !state.busy
            confirmation.isEnabled = !state.busy
            progress.visibility = if (state.busy) View.VISIBLE else View.GONE
            feedback.text = state.error ?: state.notice.orEmpty()
            feedback.setTextColor(
                color(if (state.error != null) R.color.error else R.color.on_surface_variant)
            )
            feedback.visibility = if (feedback.text.isEmpty()) View.GONE else View.VISIBLE
            if (state.signedIn) {
                password.text?.clear()
                confirmation.text?.clear()
                openMain()
            }
        }
        model.state.observe(this, ::render)
        password.imeOptions = EditorInfo.IME_ACTION_DONE
        password.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE && !model.registering) {
                submit.performClick()
                true
            } else false
        }
        confirmation.imeOptions = EditorInfo.IME_ACTION_DONE
        confirmation.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                submit.performClick()
                true
            } else false
        }
        val width = ContentFrame(this)
        width.addView(scrollContent(form))
        centered.addView(
            width,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_HORIZONTAL,
            ),
        )
        root.addView(
            centered,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        mount(root, ime = true)
    }

    private fun openMain() {
        if (isFinishing) return
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
