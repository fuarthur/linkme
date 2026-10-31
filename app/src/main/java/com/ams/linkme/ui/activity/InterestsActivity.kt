package com.ams.linkme.ui.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R
import com.ams.linkme.domain.InterestCatalog
import com.ams.linkme.ui.*
import com.ams.linkme.ui.viewmodel.InterestEditorState
import com.ams.linkme.ui.viewmodel.InterestsViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Explicit, offline interest exploration; the activity returns labels and never writes a profile.
 */
class InterestsActivity : AppCompatActivity() {
    private lateinit var model: InterestsViewModel
    private var syncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[InterestsViewModel::class.java]
        val root = column()
        root.addView(previewBanner())
        val content = column()
        val toolbar =
            MaterialToolbar(this).apply {
                title = "Your interests"
                setNavigationIcon(R.drawable.ic_back)
                navigationContentDescription = "Cancel interest editing"
                setNavigationOnClickListener { leave() }
            }
        content.addView(toolbar)
        val searchArea = column(24).apply { setPadding(dp(24), dp(8), dp(24), dp(8)) }
        val (searchBox, search) =
            field("Search or create an interest", R.id.interest_search, max = 40)
        searchArea.addView(searchBox)
        val counter =
            label("", 13f, secondary = true).apply {
                id = R.id.interests_selected
                space(top = 4)
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
        searchArea.addView(counter)
        val compact = resources.configuration.let { it.screenHeightDp / it.fontScale < 600 }
        if (!compact) content.addView(searchArea)
        val body = column(24).apply { setPadding(dp(24), dp(8), dp(24), dp(24)) }
        val scrollBody = column()
        if (compact) scrollBody.addView(searchArea)
        scrollBody.addView(body)
        val scroll = scrollContent(scrollBody)
        content.addView(
            scroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        val footer = column(24).apply { setPadding(dp(24), dp(8), dp(24), dp(12)) }
        val apply =
            button("Use interests").apply {
                id = R.id.interest_done
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
        val skip =
            button("Skip this question", textOnly = true).apply {
                id = R.id.interest_skip
                setOnClickListener { model.choose(null) }
            }
        footer.addView(apply)
        footer.addView(skip)
        content.addView(footer)
        val frame = ContentFrame(this).apply { addView(content, FrameLayout.LayoutParams(-1, -1)) }
        val centered =
            FrameLayout(this).apply {
                addView(frame, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER_HORIZONTAL))
            }
        root.addView(centered, LinearLayout.LayoutParams(-1, 0, 1f))
        search.doAfterTextChanged { if (!syncing) model.query(it.toString()) }
        var lastStep = -1
        var lastQuery = ""
        model.state.observe(this) { state ->
            val resetScroll = state.step != lastStep || state.query != lastQuery
            lastStep = state.step
            lastQuery = state.query
            syncing = true
            if (search.text.toString() != state.query) {
                search.setText(state.query)
                search.setSelection(search.length())
            }
            syncing = false
            searchBox.error =
                state.error
                    ?: state.query.takeIf { it.isNotBlank() }?.let(InterestCatalog::validateLabel)
            counter.text =
                resources.getQuantityString(
                    R.plurals.interests_selected_count,
                    state.selected.size,
                    state.selected.size,
                    InterestCatalog.MAX_INTERESTS,
                )
            val exploring = state.step >= 0
            searchArea.visibility = if (exploring) View.GONE else View.VISIBLE
            apply.visibility = if (exploring) View.GONE else View.VISIBLE
            skip.visibility = if (exploring) View.VISIBLE else View.GONE
            apply.isEnabled = state.selected.isNotEmpty()
            apply.text =
                if (state.selected.size == 1) "Use 1 interest"
                else "Use ${state.selected.size} interests"
            toolbar.title = if (exploring) "Find your interests" else "Your interests"
            toolbar.subtitle =
                if (exploring) "${state.step+1} of ${InterestCatalog.questions.size}" else null
            body.removeAllViews()
            if (exploring) renderQuestion(body, state) else renderEditor(body, state)
            if (resetScroll) scroll.post { scroll.scrollTo(0, 0) }
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    leave()
                }
            },
        )
        mount(root, ime = true)
    }

    private fun leave() {
        if (model.state.value!!.step >= 0) model.cancelExploration() else finish()
    }

    private fun chips() =
        ChipGroup(this).apply {
            chipSpacingHorizontal = dp(8)
            chipSpacingVertical = dp(4)
        }

    private fun topicChip(label: String, selected: Boolean = false, onClick: () -> Unit) =
        Chip(this).apply {
            text = InterestCatalog.label(label)
            isCheckable = true
            isChecked = selected
            minimumHeight = dp(48)
            setEnsureMinTouchTargetSize(true)
            setOnClickListener { onClick() }
        }

    private fun heading(body: LinearLayout, text: String, top: Int = 20) {
        body.addView(label(text, 20f, true).apply { space(top = top, bottom = 8) })
    }

    private fun renderEditor(body: LinearLayout, state: InterestEditorState) {
        if (state.query.isNotBlank()) {
            heading(body, "Search results", 0)
            val results = InterestCatalog.search(state.query)
            val options = chips()
            results.forEach { topic ->
                options.addView(
                    topicChip(topic.label, state.selected.contains(topic.label)) {
                        model.toggle(topic.label)
                    }
                )
            }
            body.addView(options)
            if (results.isEmpty())
                body.addView(
                    label("Make it yours. Create a topic in your own words.", 14f, secondary = true)
                )
            val normalized = InterestCatalog.normalize(listOf(state.query)).firstOrNull()
            val known = InterestCatalog.topics.any { it.label == normalized }
            if (!known && InterestCatalog.validateLabel(state.query) == null) {
                body.addView(
                    button("Create “${state.query.trim()}”", outlined = true).apply {
                        id = R.id.interest_create
                        space(top = 8)
                        setOnClickListener { model.add(state.query) }
                    }
                )
            }
        }
        heading(body, "Your mix", if (state.query.isBlank()) 0 else 24)
        if (state.selected.isEmpty())
            body.addView(
                label(
                    "Search for a topic, browse a category, or try a few scene choices.",
                    14f,
                    secondary = true,
                )
            )
        val selected = chips()
        state.selected.forEach { value ->
            selected.addView(
                Chip(this).apply {
                    text = InterestCatalog.label(value)
                    maxWidth = minOf(resources.displayMetrics.widthPixels, dp(720)) - dp(48)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    minimumHeight = dp(48)
                    setEnsureMinTouchTargetSize(true)
                    chipBackgroundColor = ColorStateList.valueOf(color(R.color.primary_container))
                    setTextColor(color(R.color.on_primary_container))
                    setCloseIconResource(R.drawable.ic_close)
                    isCloseIconVisible = true
                    closeIconTint = ColorStateList.valueOf(color(R.color.on_primary_container))
                    closeIconSize = dp(20).toFloat()
                    closeIconStartPadding = dp(12).toFloat()
                    closeIconEndPadding = dp(12).toFloat()
                    closeIconContentDescription = "Remove ${InterestCatalog.label(value)}"
                    setOnCloseIconClickListener { model.remove(value) }
                }
            )
        }
        body.addView(selected)
        if (state.query.isNotBlank()) return
        body.addView(
            button("Help me find my interests", outlined = true).apply {
                id = R.id.interest_explore
                space(top = 20)
                setOnClickListener { model.explore() }
            }
        )
        val ideas = InterestCatalog.suggestions(state.selected, model.introduction, state.choices)
        if (ideas.isNotEmpty()) {
            heading(body, "Ideas to explore")
            ideas
                .groupBy { it.reason }
                .forEach { (reason, items) ->
                    body.addView(label(reason, 13f, secondary = true).apply { space(bottom = 4) })
                    val options = chips()
                    items.forEach { idea ->
                        options.addView(topicChip(idea.label) { model.add(idea.label) })
                    }
                    body.addView(options)
                }
        }
        heading(body, "Browse interests", 28)
        val categories = chips().apply { isSingleLine = true }
        categories.addView(topicChip("All", state.groupId == null) { model.group(null) })
        InterestCatalog.groups.forEach { group ->
            categories.addView(
                topicChip(group.label, state.groupId == group.id) { model.group(group.id) }
            )
        }
        body.addView(
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(categories)
            }
        )
        val topics = InterestCatalog.search("", state.groupId, state.visibleTopics)
        val browse = chips()
        topics.forEach { topic ->
            browse.addView(
                topicChip(topic.label, topic.label in state.selected) { model.toggle(topic.label) }
            )
        }
        body.addView(browse)
        val total =
            InterestCatalog.topics.count { state.groupId == null || it.groupId == state.groupId }
        if (topics.size < total)
            body.addView(
                button("Show more topics", textOnly = true).apply {
                    setOnClickListener { model.more() }
                }
            )
        body.addView(
            label(
                    if (model.purpose == "discover")
                        "Use this mix to explore people. Save your profile separately."
                    else "Use this mix in your profile. Publish it when you save your profile.",
                    13f,
                    secondary = true,
                )
                .apply { space(top = 20) }
        )
    }

    private fun renderQuestion(body: LinearLayout, state: InterestEditorState) {
        val question = InterestCatalog.questions.getOrNull(state.step) ?: return
        heading(body, question.title, 8)
        body.addView(
            label("This shapes suggestions. You choose what to keep.", 14f, secondary = true)
                .apply { space(bottom = 20) }
        )
        val ids =
            listOf(R.id.interest_choice_one, R.id.interest_choice_two, R.id.interest_choice_three)
        question.choices.forEachIndexed { index, choice ->
            body.addView(
                button("${choice.title}\n${choice.detail}", outlined = true).apply {
                    if (index < ids.size) id = ids[index]
                    minHeight = dp(88)
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    space(top = 8)
                    setOnClickListener { model.choose(choice.id) }
                }
            )
        }
    }
}
