package com.ams.linkme.adapter

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.model.Message
import com.ams.linkme.ui.*
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(private val currentUid: String) :
    ListAdapter<Message, MessageAdapter.Holder>(
        object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(old: Message, new: Message) = old.id == new.id

            override fun areContentsTheSame(old: Message, new: Message) = old == new
        }
    ) {
    class Holder(val frame: FrameLayout) : RecyclerView.ViewHolder(frame)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        Holder(
            FrameLayout(parent.context).apply {
                setPadding(context.dp(20), context.dp(5), context.dp(20), context.dp(5))
                layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }
        )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val message = getItem(position)
        val c = holder.frame.context
        val mine = message.senderId == currentUid
        holder.frame.removeAllViews()
        val bubble = c.column(14)
        bubble.background =
            GradientDrawable().apply {
                cornerRadius = c.dp(18).toFloat()
                setColor(c.color(if (mine) R.color.primary else R.color.surface_container))
            }
        val maxWidth = (minOf(c.resources.displayMetrics.widthPixels, c.dp(720)) * 0.72).toInt()
        bubble.addView(
            c.label(message.content, 16f).apply {
                this.maxWidth = maxWidth
                setTextIsSelectable(true)
                setTextColor(c.color(if (mine) R.color.on_primary else R.color.on_surface))
            }
        )
        val stamp =
            if (message.pending) "Waiting to send…"
            else
                message.timestamp
                    ?.let {
                        SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
                            .format(java.util.Date(it))
                    }
                    .orEmpty()
        bubble.addView(
            c.label(stamp, 11f).apply {
                setTextColor(c.color(if (mine) R.color.on_primary else R.color.on_surface_variant))
                space(top = 5)
            }
        )
        holder.frame.addView(
            bubble,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (mine) Gravity.END else Gravity.START,
            ),
        )
    }
}
