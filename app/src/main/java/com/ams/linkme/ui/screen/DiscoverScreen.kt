package com.ams.linkme.ui.screen

import android.view.View
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
    activity: AppCompatActivity,
    model: MainViewModel,
    onChat: (User) -> Unit,
    onEditInterests: () -> Unit,
) {
    val view = activity.column()

    init {
        val compact =
            activity.resources.configuration.let { it.screenHeightDp / it.fontScale < 600 }
        val padding = if (compact) 16 else 24
        val header = activity.column(padding)
        val top = activity.row()
        top.addView(activity.label("Discover", 30f, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(
            activity.button("Refresh", textOnly = true).apply {
                layoutParams = LinearLayout.LayoutParams(-2, -2)
                setOnClickListener { model.refresh() }
            }
        )
        header.addView(top)
        if (!compact)
            header.addView(
                activity
                    .label("Meet over something you actually love.", 16f, secondary = true)
                    .apply { space(top = 8, bottom = 12) }
            )
        val mix =
            ChipGroup(activity).apply {
                isSingleLine = true
                chipSpacingHorizontal = activity.dp(8)
                chipSpacingVertical = activity.dp(4)
            }
        header.addView(
            HorizontalScrollView(activity).apply {
                isHorizontalScrollBarEnabled = false
                addView(mix)
            }
        )
        header.addView(
            activity.button("Explore & filter interests", outlined = true).apply {
                id = R.id.discover_interests
                space(top = 8)
                setOnClickListener { onEditInterests() }
            }
        )
        view.addView(header)
        val count =
            activity.label("", 13f, secondary = true).apply {
                setPadding(activity.dp(padding), 0, activity.dp(padding), activity.dp(8))
            }
        view.addView(count)
        val progress = activity.spinner()
        view.addView(progress)
        val empty = activity.column(28)
        val title = activity.label("", 24f, true)
        empty.addView(title)
        val body = activity.label("", 16f, secondary = true).apply { space(top = 12) }
        empty.addView(body)
        val find =
            activity.button("Find my interests", outlined = true).apply {
                space(top = 24)
                setOnClickListener { onEditInterests() }
            }
        empty.addView(find)
        val retry =
            activity.button("Try again", outlined = true).apply {
                space(top = 8)
                setOnClickListener { model.refresh() }
            }
        empty.addView(retry)
        val emptyScroll = activity.scrollContent(empty)
        view.addView(emptyScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val adapter = PeopleAdapter(onChat)
        val list =
            RecyclerView(activity).apply {
                id = R.id.people_list
                layoutManager = LinearLayoutManager(activity)
                this.adapter = adapter
                clipToPadding = false
                setPadding(0, 0, 0, activity.dp(20))
            }
        view.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        var displayed: List<String>? = null
        model.state.observe(activity) { state ->
            if (displayed != state.selected) {
                displayed = state.selected
                mix.removeAllViews()
                state.selected.forEach { tag ->
                    mix.addView(
                        Chip(activity).apply {
                            text = Interests.label(tag)
                            minimumHeight = activity.dp(48)
                            setEnsureMinTouchTargetSize(true)
                            setOnClickListener { onEditInterests() }
                        }
                    )
                }
            }
            progress.visibility = if (state.loading) View.VISIBLE else View.GONE
            val isEmpty = !state.loading && state.people.isEmpty()
            emptyScroll.visibility = if (isEmpty) View.VISIBLE else View.GONE
            list.visibility =
                if (!state.loading && state.people.isNotEmpty()) View.VISIBLE else View.GONE
            count.text =
                if (state.people.isEmpty()) ""
                else "${state.people.size} people with shared or related interests"
            title.text =
                when {
                    state.error != null -> "Could not load people"
                    state.selected.isEmpty() -> "Find your common ground"
                    else -> "No connections here yet"
                }
            body.text =
                state.error
                    ?: if (state.selected.isEmpty())
                        "Explore a few ideas or create your own topics. You choose what becomes part of your mix."
                    else
                        "Try a broader topic or another combination. New people appear when they publish their profiles."
            retry.visibility = if (state.error != null) View.VISIBLE else View.GONE
            find.visibility = if (state.error == null) View.VISIBLE else View.GONE
            adapter.selected = state.selected
            adapter.submitList(state.people)
        }
    }
}
