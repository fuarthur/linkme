package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.adapter.MessageAdapter
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.domain.Interests
import com.ams.linkme.ui.*
import com.ams.linkme.ui.viewmodel.ChatViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChatActivity : AppCompatActivity() {
    private lateinit var model: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = LinkMeRepository.instance
        if (repo.uid == null) {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
            return
        }
        val peer = intent.getStringExtra("userId")
        if (peer.isNullOrBlank() || peer == repo.uid) {
            finish()
            return
        }
        model = ViewModelProvider(this)[ChatViewModel::class.java]
        model.peer = peer
        model.peerName =
            intent.getStringExtra("username")?.ifBlank { "LinkMe member" } ?: "LinkMe member"
        if (model.draft.isEmpty()) model.draft = savedInstanceState?.getString("draft").orEmpty()
        val root = column()
        root.addView(previewBanner())
        val toolbar =
            MaterialToolbar(this).apply {
                title = model.peerName
                subtitle = "A little common ground goes a long way."
                setNavigationIcon(R.drawable.ic_back)
                navigationContentDescription = "Back to LinkMe"
                setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
                menu
                    .add("Conversation options")
                    .setIcon(R.drawable.ic_more)
                    .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
                setOnMenuItemClickListener {
                    val blocked = model.state.value!!.blocked
                    MaterialAlertDialogBuilder(this@ChatActivity)
                        .setTitle(
                            if (blocked) "Unblock ${model.peerName}?"
                            else "Block ${model.peerName}?"
                        )
                        .setMessage(
                            if (blocked) "You will be able to message each other again."
                            else
                                "You will not be able to send each other new messages. Your chat history will be kept."
                        )
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton(if (blocked) "Unblock" else "Block") { _, _ ->
                            model.toggleBlock()
                        }
                        .show()
                    true
                }
            }
        root.addView(toolbar)
        val stateText =
            label("", 13f, secondary = true).apply {
                setPadding(dp(24), dp(8), dp(24), dp(8))
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
        root.addView(stateText)
        val retry =
            button("Try again", textOnly = true).apply { setOnClickListener { model.retry() } }
        root.addView(retry)
        val older =
            button("Load earlier messages", textOnly = true).apply {
                setOnClickListener { model.loadOlder() }
            }
        root.addView(older)
        val progress = spinner()
        root.addView(progress)
        val welcome = column(24)
        welcome.addView(label("Start with common ground", 24f, true))
        welcome.addView(
            label("A simple hello can turn into a good conversation.", 16f, secondary = true)
                .apply { space(top = 10) }
        )
        val shared = intent.getStringArrayListExtra("sharedInterests").orEmpty()
        val prompt =
            button(
                    if (shared.isNotEmpty()) "Ask about ${Interests.label(shared.first())}"
                    else "Break the ice",
                    outlined = true,
                )
                .apply { space(top = 16) }
        welcome.addView(prompt)
        root.addView(welcome)
        val list =
            RecyclerView(this).apply {
                id = R.id.chat_list
                layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
                clipToPadding = false
                setPadding(0, dp(12), 0, dp(12))
            }
        val messages = MessageAdapter(model.currentUid)
        list.adapter = messages
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val composer =
            row().apply {
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setBackgroundColor(color(R.color.surface))
            }
        val (box, editor) = field("Message", R.id.message_draft, multiline = true)
        editor.minLines = 1
        editor.maxLines = 5
        editor.filters = arrayOf(android.text.InputFilter.LengthFilter(2000))
        editor.setText(model.draft)
        composer.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val send =
            button("Send").apply {
                id = R.id.send_message
                contentDescription = "Send message"
                layoutParams =
                    LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        .apply { marginStart = dp(8) }
                setOnClickListener { model.send() }
            }
        composer.addView(send)
        root.addView(composer)
        editor.doAfterTextChanged {
            model.draft = it.toString()
            send.isEnabled =
                model.draft.isNotBlank() &&
                    !model.state.value!!.sending &&
                    !model.state.value!!.blocked
        }
        prompt.setOnClickListener {
            editor.setText(
                if (shared.isNotEmpty())
                    "Hey ${model.peerName}, what do you enjoy most about ${Interests.label(shared.first()).lowercase()}?"
                else "Hey ${model.peerName}, what have you been into lately?"
            )
            editor.setSelection(editor.length())
            editor.requestFocus()
        }
        var completed = model.state.value!!.completed
        model.state.observe(this) { state ->
            progress.visibility = if (state.loading) View.VISIBLE else View.GONE
            welcome.visibility =
                if (!state.loading && state.messages.isEmpty() && state.error == null) View.VISIBLE
                else View.GONE
            stateText.text =
                when {
                    state.blocked ->
                        "You blocked this person. Unblock them from the menu to send a message."
                    state.error != null -> state.error
                    state.offline -> "Syncing messages…"
                    else -> ""
                }
            stateText.visibility = if (stateText.text.isBlank()) View.GONE else View.VISIBLE
            stateText.setTextColor(
                color(if (state.error != null) R.color.error else R.color.on_surface_variant)
            )
            retry.visibility = if (state.error != null) View.VISIBLE else View.GONE
            older.visibility = if (state.hasOlder) View.VISIBLE else View.GONE
            older.isEnabled = !state.loadingOlder
            older.text =
                if (state.loadingOlder) "Loading…"
                else if (state.hasGap) "Load missed messages" else "Load earlier messages"
            send.text = if (state.sending) "Sending…" else "Send"
            send.isEnabled = !state.sending && !state.blocked && model.draft.isNotBlank()
            editor.isEnabled = !state.blocked
            if (state.completed > completed) {
                completed = state.completed
                if (editor.text.toString() != model.draft) {
                    editor.setText(model.draft)
                    editor.setSelection(editor.length())
                }
            }
            val wasAtBottom = !list.canScrollVertically(1)
            messages.submitList(state.messages) {
                if (wasAtBottom && state.messages.isNotEmpty() && !state.loadingOlder)
                    list.scrollToPosition(state.messages.lastIndex)
            }
        }
        mount(root, ime = true)
    }

    override fun onStart() {
        super.onStart()
        if (::model.isInitialized) model.start()
    }

    override fun onStop() {
        if (::model.isInitialized) model.stop()
        super.onStop()
    }

    override fun onSaveInstanceState(out: Bundle) {
        if (::model.isInitialized) out.putString("draft", model.draft)
        super.onSaveInstanceState(out)
    }
}
