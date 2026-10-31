package com.ams.linkme.ui.screen

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.adapter.InboxAdapter
import com.ams.linkme.model.Conversation
import com.ams.linkme.ui.*
import com.ams.linkme.ui.viewmodel.InboxViewModel

class InboxScreen(
    activity: AppCompatActivity,
    model: InboxViewModel,
    onChat: (Conversation) -> Unit,
    onDiscover: () -> Unit,
) {
    val view = activity.column()

    init {
        val header = activity.column(24)
        header.addView(activity.label("Chats", 30f, true))
        header.addView(
            activity.label("Keep a good connection going.", 16f, secondary = true).apply {
                space(top = 8)
            }
        )
        view.addView(header)
        val status =
            activity.label("", 13f, secondary = true).apply {
                setPadding(activity.dp(24), 0, activity.dp(24), activity.dp(8))
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
        view.addView(status)
        val progress = activity.spinner()
        view.addView(progress)
        val empty = activity.column(28)
        val title = activity.label("No conversations yet", 24f, true)
        empty.addView(title)
        val body =
            activity
                .label(
                    "Say hello to someone who shares an interest. Your conversations will be right here.",
                    16f,
                    secondary = true,
                )
                .apply { space(top = 12) }
        empty.addView(body)
        val find =
            activity.button("Find your people").apply {
                space(top = 24)
                setOnClickListener { onDiscover() }
            }
        empty.addView(find)
        val retry =
            activity.button("Try again", outlined = true).apply {
                setOnClickListener { model.retry() }
                space(top = 8)
            }
        empty.addView(retry)
        val emptyScroll = activity.scrollContent(empty)
        view.addView(
            emptyScroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        val adapter = InboxAdapter(onChat)
        val list =
            RecyclerView(activity).apply {
                id = R.id.inbox_list
                layoutManager = LinearLayoutManager(activity)
                this.adapter = adapter
                clipToPadding = false
                setPadding(0, 0, 0, activity.dp(16))
            }
        view.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val loadOlder =
            activity.button("Look for older chats", outlined = true).apply {
                id = R.id.older_chats
                setOnClickListener { model.loadOlder() }
                space(start = 24, end = 24, top = 8, bottom = 8)
            }
        view.addView(loadOlder)
        model.state.observe(activity) { state ->
            loadOlder.visibility = if (state.hasOlder) View.VISIBLE else View.GONE
            loadOlder.isEnabled = !state.loadingOlder
            loadOlder.text =
                if (state.loadingOlder) "Loading earlier chats…" else "Look for older chats"
            progress.visibility = if (state.loading) View.VISIBLE else View.GONE
            emptyScroll.visibility =
                if (!state.loading && state.conversations.isEmpty()) View.VISIBLE else View.GONE
            list.visibility =
                if (!state.loading && state.conversations.isNotEmpty()) View.VISIBLE else View.GONE
            status.text = state.error ?: if (state.offline) "Syncing your conversations…" else ""
            status.visibility = if (status.text.isBlank()) View.GONE else View.VISIBLE
            title.text = if (state.error != null) "Could not load chats" else "No conversations yet"
            body.text =
                state.error
                    ?: "Say hello to someone who shares an interest. Your conversations will be right here."
            retry.visibility = if (state.error != null) View.VISIBLE else View.GONE
            find.visibility = if (state.error == null) View.VISIBLE else View.GONE
            adapter.submitList(state.conversations)
        }
    }
}
