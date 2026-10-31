package com.ams.linkme.adapter

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.domain.InterestCatalog
import com.ams.linkme.domain.WikiTopic
import com.ams.linkme.ui.*
import com.google.android.material.checkbox.MaterialCheckBox

/** RecyclerView binds only visible rows, even when all31k public topics match. */
class WikiTopicsAdapter(
    private val onToggle: (WikiTopic) -> Unit,
    private val onDetails: (WikiTopic) -> Unit,
) : RecyclerView.Adapter<WikiTopicsAdapter.Holder>() {
    private var topics = emptyList<WikiTopic>()
    private var selected = emptyList<String>()

    fun submit(topics: List<WikiTopic>, selected: List<String>) {
        if (this.topics === topics && this.selected == selected) return
        this.topics = topics
        this.selected = selected
        notifyDataSetChanged()
    }

    override fun getItemCount() = topics.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        val compact = context.resources.configuration.let { it.screenHeightDp / it.fontScale < 600 }
        val root =
            context.row().apply {
                layoutParams = RecyclerView.LayoutParams(-1, -2)
                minimumHeight = context.dp(96)
                setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(12))
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                val attrs =
                    context.obtainStyledAttributes(
                        intArrayOf(android.R.attr.selectableItemBackground)
                    )
                background = attrs.getDrawable(0)
                attrs.recycle()
            }
        val check =
            MaterialCheckBox(context).apply {
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        root.addView(check, LinearLayout.LayoutParams(context.dp(48), context.dp(48)))
        val text = context.column().apply { setPadding(context.dp(8), 0, context.dp(8), 0) }
        val title =
            context.label("", 16f, true).apply {
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        val description =
            context.label("", 14f, secondary = true).apply {
                maxLines = if (compact) 1 else 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                space(top = 4)
            }
        val domain =
            context.label("", 12f, secondary = true).apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                space(top = 4)
            }
        listOf(title, description, domain).forEach {
            it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            text.addView(it)
        }
        root.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        val info =
            context.button(context.getString(R.string.wiki_info), textOnly = true).apply {
                layoutParams = LinearLayout.LayoutParams(-2, -2)
                minimumWidth = context.dp(48)
            }
        root.addView(info)
        return Holder(root, check, title, description, domain, info)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val topic = topics[position]
        holder.title.text = topic.title
        holder.description.text = topic.description
        holder.domain.text = topic.domain
        holder.check.isChecked =
            InterestCatalog.match(listOf(topic.value), selected).exact.isNotEmpty()
        holder.itemView.contentDescription = "${topic.title}. ${topic.description}. ${topic.domain}"
        holder.itemView.setOnClickListener { onToggle(topic) }
        holder.itemView.setOnLongClickListener {
            onDetails(topic)
            true
        }
        holder.info.contentDescription =
            holder.itemView.context.getString(R.string.wiki_about_topic, topic.title)
        holder.info.setOnClickListener { onDetails(topic) }
        ViewCompat.setAccessibilityDelegate(
            holder.itemView,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.CheckBox"
                    info.isCheckable = true
                    info.isChecked = holder.check.isChecked
                }
            },
        )
    }

    class Holder(
        root: View,
        val check: MaterialCheckBox,
        val title: android.widget.TextView,
        val description: android.widget.TextView,
        val domain: android.widget.TextView,
        val info: View,
    ) : RecyclerView.ViewHolder(root)
}
