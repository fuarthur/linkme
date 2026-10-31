package com.ams.linkme.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.domain.InterestCatalog
import com.ams.linkme.domain.Interests
import com.ams.linkme.model.User
import com.ams.linkme.ui.*
import com.google.android.material.card.MaterialCardView

class PeopleAdapter(private val onChat: (User) -> Unit) :
    ListAdapter<User, PeopleAdapter.Holder>(
        object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(old: User, new: User) = old.uid == new.uid

            override fun areContentsTheSame(old: User, new: User) = old == new
        }
    ) {
    var selected = emptyList<String>()

    class Holder(val card: MaterialCardView) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder =
        Holder(
            MaterialCardView(parent.context).apply {
                radius = context.dp(16).toFloat()
                cardElevation = 0f
                strokeWidth = context.dp(1)
                setStrokeColor(context.color(R.color.outline_variant))
                setCardBackgroundColor(context.color(R.color.surface))
                layoutParams =
                    RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        .apply {
                            setMargins(context.dp(20), context.dp(6), context.dp(20), context.dp(6))
                        }
            }
        )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val user = getItem(position)
        val c = holder.card.context
        holder.card.removeAllViews()
        val body = c.column(20)
        val top = c.row()
        top.addView(c.avatar(user.username))
        val identity = c.column()
        identity.addView(c.label(user.username, 20f, true))
        val match = InterestCatalog.match(selected, user.interests)
        val common = match.exact
        identity.addView(
            c.label(
                if (common.isEmpty()) "A related interest"
                else if (common.size == 1) "1 shared interest"
                else "${common.size} shared interests",
                13f,
                secondary = true,
            )
        )
        top.addView(
            identity,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = c.dp(14)
            },
        )
        body.addView(top)
        if (user.bio.isNotBlank())
            body.addView(
                c.label(user.bio, 15f).apply {
                    maxLines = 4
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    space(top = 16)
                }
            )
        body.addView(
            c.label(
                    if (common.isNotEmpty())
                        "You both enjoy ${common.take(3).joinToString(" · ",transform=Interests::label)}${if(common.size>3) " +${common.size-3} more" else ""}"
                    else "Connected through ${match.relatedGroups.take(3).joinToString(" · ")}",
                    14f,
                    true,
                )
                .apply {
                    setTextColor(c.color(R.color.primary))
                    space(top = 16)
                }
        )
        val other =
            user.interests.filter { InterestCatalog.match(listOf(it), common).exact.isEmpty() }
        if (other.isNotEmpty())
            body.addView(
                c.label(
                        "Also into ${other.take(3).joinToString(" · ",transform=Interests::label)}${if(other.size>3) " +${other.size-3} more" else ""}",
                        13f,
                        secondary = true,
                    )
                    .apply { space(top = 4) }
            )
        body.addView(
            c.button("Say hello", outlined = true).apply {
                contentDescription = "Chat with ${user.username}"
                space(top = 12)
                setOnClickListener { onChat(user) }
            }
        )
        holder.card.addView(body)
    }
}
