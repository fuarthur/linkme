package com.ams.linkme.ui.screen

import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.adapter.PeopleAdapter
import com.ams.linkme.domain.Interests
import com.ams.linkme.model.User
import com.ams.linkme.ui.*
import com.ams.linkme.ui.viewmodel.MainViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class DiscoverScreen(
    private val activity: AppCompatActivity,
    private val model: MainViewModel,
    onChat: (User) -> Unit,
    onProfile: () -> Unit,
) {
    val view = activity.column()

    init {
        val configuration = activity.resources.configuration
        val compact = configuration.screenHeightDp / configuration.fontScale < 600
        val padding = if (compact) 16 else 24
        val header = activity.column(padding)
        val top = activity.row()
        top.addView(
            activity.label("Discover", 30f, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        top.addView(
            activity.button("Refresh", textOnly = true).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setOnClickListener { model.refresh() }
            }
        )
        header.addView(top)
        header.addView(
            activity
                .label("Good conversations start with common ground.", 16f, secondary = true)
                .apply {
                    space(top = 8, bottom = 20)
                    visibility = if (compact) View.GONE else View.VISIBLE
                }
        )
        header.addView(
            activity.label("Explore your interests", 14f, true).apply {
                visibility = if (compact) View.GONE else View.VISIBLE
            }
        )
        val filters =
            ChipGroup(activity).apply {
                isSingleSelection = false
                isSingleLine = compact
                chipSpacingHorizontal = activity.dp(8)
                chipSpacingVertical = activity.dp(4)
            }
        val chips =
            Interests.all.associateWith { interest ->
                Chip(activity).apply {
                    text = Interests.label(interest)
                    isCheckable = true
                    minimumHeight = activity.dp(48)
                    setEnsureMinTouchTargetSize(true)
                    tag = interest
                    filters.addView(this)
                }
            }
        if (compact) {
            header.addView(
                HorizontalScrollView(activity).apply {
                    contentDescription = "Interest filters"
                    isHorizontalScrollBarEnabled = false
                    space(top = 8)
                    addView(filters)
                }
            )
        } else {
            filters.space(top = 8)
            header.addView(filters)
        }
        view.addView(header)
        val count =
            activity.label("", 13f, secondary = true).apply {
                setPadding(activity.dp(padding), 0, activity.dp(padding), activity.dp(8))
            }
        view.addView(count)
        val progress = activity.spinner()
        view.addView(progress)
        val empty = activity.column(28)
        val emptyTitle = activity.label("", 24f, true)
        empty.addView(emptyTitle)
        val emptyBody = activity.label("", 16f, secondary = true).apply { space(top = 12) }
        empty.addView(emptyBody)
        val action =
            activity.button("Edit my interests", outlined = true).apply {
                space(top = 24)
                setOnClickListener { onProfile() }
            }
        empty.addView(action)
        val retry =
            activity.button("Try again", outlined = true).apply {
                setOnClickListener { model.refresh() }
                space(top = 8)
            }
        empty.addView(retry)
        view.addView(
            activity.scrollContent(empty),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        val list =
            RecyclerView(activity).apply {
                id = R.id.people_list
                layoutManager = LinearLayoutManager(activity)
                clipToPadding = false
                setPadding(0, 0, 0, activity.dp(20))
            }
        val adapter = PeopleAdapter(onChat)
        list.adapter = adapter
        view.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        var syncing = false
        chips.forEach { (interest, chip) ->
            chip.setOnCheckedChangeListener { _, _ ->
                if (!syncing) model.select(chips.filterValues { it.isChecked }.keys.toList())
            }
        }
        model.state.observe(activity) { state ->
            syncing = true
            chips.forEach { (interest, chip) -> chip.isChecked = interest in state.selected }
            syncing = false
            progress.visibility = if (state.loading) View.VISIBLE else View.GONE
            val showEmpty = !state.loading && state.people.isEmpty()
            (empty.parent as View).visibility = if (showEmpty) View.VISIBLE else View.GONE
            list.visibility =
                if (!state.loading && state.people.isNotEmpty()) View.VISIBLE else View.GONE
            count.text =
                if (state.people.isEmpty()) ""
                else if (state.people.size == 1) "1 person with something in common"
                else "${state.people.size} people with something in common"
            emptyTitle.text =
                when {
                    state.error != null -> "Could not load people"
                    state.selected.isEmpty() -> "Find your common ground"
                    else -> "No matches here yet"
                }
            emptyBody.text =
                state.error
                    ?: if (state.selected.isEmpty())
                        "Choose one or more interests above, or complete your profile to get started."
                    else
                        "Try a different interest or come back later. Your next conversation can start here."
            action.visibility = if (state.error == null) View.VISIBLE else View.GONE
            retry.visibility = if (state.error != null) View.VISIBLE else View.GONE
            adapter.selected = state.selected
            adapter.submitList(state.people)
        }
    }
}
