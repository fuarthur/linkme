package com.ams.linkme.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.model.Conversation
import com.ams.linkme.ui.*
import java.text.SimpleDateFormat
import java.util.Locale

class InboxAdapter(private val onChat: (Conversation) -> Unit) :
    ListAdapter<Conversation, InboxAdapter.Holder>(
        object : DiffUtil.ItemCallback<Conversation>() {
            override fun areItemsTheSame(old: Conversation, new: Conversation) =
                old.peerUid == new.peerUid

            override fun areContentsTheSame(old: Conversation, new: Conversation) = old == new
        }
    ) {
    class Holder(val row: LinearLayout) : RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        Holder(
            parent.context.row().apply {
                setPadding(context.dp(24), context.dp(18), context.dp(24), context.dp(18))
                minimumHeight = context.dp(96)
                isClickable = true
                isFocusable = true
                val value = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
                setBackgroundResource(value.resourceId)
                layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }
        )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val c = holder.row.context
        holder.row.removeAllViews()
        val name = item.profile?.username?.ifBlank { "LinkMe member" } ?: "LinkMe member"
        holder.row.addView(c.avatar(name))
        val text = c.column()
        val heading = c.row()
        heading.addView(
            c.label(name, 18f, true).apply { maxLines = 2 },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        heading.addView(
            c.label(
                    item.message.timestamp
                        ?.let {
                            SimpleDateFormat("MMM d", Locale.getDefault())
                                .format(java.util.Date(it))
                        }
                        .orEmpty(),
                    12f,
                    secondary = true,
                )
                .apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                            .apply { marginStart = c.dp(8) }
                }
        )
        text.addView(heading)
        text.addView(
            c.label(item.message.content, 14f, secondary = true).apply {
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                space(top = 4)
            }
        )
        holder.row.addView(
            text,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = c.dp(16)
            },
        )
        holder.row.contentDescription = "Conversation with $name. ${item.message.content}"
        holder.row.setOnClickListener { onChat(item) }
    }
}
