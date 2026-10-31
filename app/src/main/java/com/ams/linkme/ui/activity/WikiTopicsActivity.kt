package com.ams.linkme.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.adapter.WikiTopicsAdapter
import com.ams.linkme.domain.WikiTopic
import com.ams.linkme.domain.WikiTopicReferences
import com.ams.linkme.ui.*
import com.ams.linkme.ui.viewmodel.WikiTopicsViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration

/** An offline multi-select library returning a local draft, never a backend profile write. */
class WikiTopicsActivity : AppCompatActivity() {
    private lateinit var model: WikiTopicsViewModel
    private var syncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[WikiTopicsViewModel::class.java]
        val root = column()
        root.addView(previewBanner())
        val content = column()
        val toolbar =
            MaterialToolbar(this).apply {
                setTitle(R.string.wiki_title)
                setNavigationIcon(R.drawable.ic_back)
                setNavigationContentDescription(R.string.wiki_cancel)
                setNavigationOnClickListener { finish() }
                menu
                    .add(0, R.id.wiki_sources, 0, R.string.wiki_sources)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                setOnMenuItemClickListener {
                    if (it.itemId == R.id.wiki_sources) {
                        sources()
                        true
                    } else false
                }
            }
        content.addView(toolbar)
        val compact = resources.configuration.let { it.screenHeightDp / it.fontScale < 600 }
        val header = column().apply { setPadding(dp(16), dp(4), dp(16), dp(8)) }
        val searchRow = row().apply { gravity = Gravity.CENTER_VERTICAL }
        val (searchBox, search) = field(getString(R.string.wiki_search), R.id.wiki_search)
        search.filters = arrayOf(InputFilter.LengthFilter(120))
        searchRow.addView(searchBox, LinearLayout.LayoutParams(0, -2, 1f))
        val fields =
            button(getString(R.string.wiki_fields), textOnly = true).apply {
                id = R.id.wiki_fields
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) }
                setOnClickListener {
                    val state = model.state.value!!
                    val choices = listOf(getString(R.string.wiki_all_fields)) + state.domains
                    val selected = state.domains.indexOf(state.domain) + 1
                    MaterialAlertDialogBuilder(this@WikiTopicsActivity)
                        .setTitle(R.string.wiki_choose_field)
                        .setSingleChoiceItems(choices.toTypedArray(), selected) { dialog, index ->
                            model.domain(state.domains.getOrNull(index - 1))
                            dialog.dismiss()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }
        searchRow.addView(fields)
        header.addView(searchRow)
        val status =
            label("", 13f, secondary = true).apply {
                id = R.id.wiki_status
                space(top = 4)
                if (compact) {
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
            }
        header.addView(status)
        val feedback =
            label("", 14f).apply {
                id = R.id.wiki_feedback
                setTextColor(color(R.color.error))
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
        header.addView(feedback)
        content.addView(header)
        val progress = spinner()
        content.addView(progress)
        val list =
            RecyclerView(this).apply {
                id = R.id.wiki_results
                layoutManager = LinearLayoutManager(this@WikiTopicsActivity)
                clipToPadding = false
                setPadding(0, 0, 0, dp(12))
                addItemDecoration(
                    MaterialDividerItemDecoration(
                            this@WikiTopicsActivity,
                            LinearLayoutManager.VERTICAL,
                        )
                        .apply { isLastItemDecorated = false }
                )
            }
        val adapter = WikiTopicsAdapter(model::toggle, ::details)
        list.adapter = adapter
        val empty = column(24)
        val emptyTitle = label("", 20f, true).apply { id = R.id.wiki_empty }
        empty.addView(emptyTitle)
        val emptyBody = label("", 14f, secondary = true).apply { space(top = 12) }
        empty.addView(emptyBody)
        val retry =
            button(getString(R.string.wiki_retry), outlined = true).apply {
                id = R.id.wiki_retry
                space(top = 16)
                setOnClickListener { model.retry() }
            }
        empty.addView(retry)
        val emptyScroll = scrollContent(empty)
        content.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(emptyScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = column().apply { setPadding(dp(16), dp(4), dp(16), dp(12)) }
        val apply =
            button("").apply {
                id = R.id.wiki_apply
                setOnClickListener {
                    val selected = model.state.value!!.selected
                    if (selected.isNotEmpty()) {
                        setResult(
                            RESULT_OK,
                            Intent()
                                .putStringArrayListExtra("selectedInterests", ArrayList(selected)),
                        )
                        finish()
                    }
                }
            }
        footer.addView(apply)
        content.addView(footer)
        val frame = ContentFrame(this).apply { addView(content, FrameLayout.LayoutParams(-1, -1)) }
        val centered =
            FrameLayout(this).apply {
                addView(frame, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER_HORIZONTAL))
            }
        root.addView(centered, LinearLayout.LayoutParams(-1, 0, 1f))
        search.doAfterTextChanged { if (!syncing) model.query(it.toString()) }
        var lastQuery: String? = null
        var lastDomain: String? = null
        model.state.observe(this) { state ->
            syncing = true
            if (search.text.toString() != state.query) {
                search.setText(state.query)
                search.setSelection(search.length())
            }
            syncing = false
            status.text =
                when {
                    state.loading -> getString(R.string.wiki_loading)
                    state.loadFailed -> getString(R.string.wiki_load_failed)
                    else ->
                        resources.getQuantityString(
                            R.plurals.wiki_status,
                            state.topics.size,
                            state.domain ?: getString(R.string.wiki_all_fields),
                            state.topics.size,
                            state.selected.size,
                        )
                }
            feedback.text = state.feedback
            feedback.visibility = if (state.feedback == null) View.GONE else View.VISIBLE
            fields.isEnabled = state.domains.isNotEmpty()
            fields.contentDescription =
                getString(
                    R.string.wiki_field_description,
                    state.domain ?: getString(R.string.wiki_all_fields),
                )
            progress.visibility = if (state.loading) View.VISIBLE else View.GONE
            val isEmpty = !state.loading && (state.loadFailed || state.topics.isEmpty())
            emptyScroll.visibility = if (isEmpty) View.VISIBLE else View.GONE
            list.visibility = if (!isEmpty) View.VISIBLE else View.GONE
            emptyTitle.setText(
                if (state.loadFailed) R.string.wiki_load_failed else R.string.wiki_nothing_matched
            )
            emptyBody.setText(
                if (state.loadFailed) R.string.wiki_retry_help else R.string.wiki_empty_help
            )
            retry.visibility = if (state.loadFailed) View.VISIBLE else View.GONE
            apply.text =
                resources.getQuantityString(
                    R.plurals.wiki_use_interests,
                    state.selected.size,
                    state.selected.size,
                )
            apply.isEnabled = state.selected.isNotEmpty()
            adapter.submit(state.topics, state.selected)
            if (state.query != lastQuery || state.domain != lastDomain) list.scrollToPosition(0)
            lastQuery = state.query
            lastDomain = state.domain
        }
        mount(root, ime = true)
    }

    private fun details(topic: WikiTopic) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.wiki_topic_details)
            .setMessage(
                getString(R.string.wiki_details, topic.title, topic.description, topic.domain)
            )
            .setPositiveButton(R.string.wiki_close, null)
            .setNeutralButton(R.string.wiki_read_source) { _, _ ->
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.wikidata.org/wiki/${topic.wikidataId}"),
                    )
                )
            }
            .show()
    }

    private fun sources() {
        val dialog =
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.wiki_sources)
                .setMessage(getString(R.string.wiki_sources_body, WikiTopicReferences.TOPIC_COUNT))
                .setPositiveButton(R.string.wiki_close, null)
                .show()
        dialog.findViewById<TextView>(android.R.id.message)?.let {
            Linkify.addLinks(it, Linkify.WEB_URLS)
            it.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}
