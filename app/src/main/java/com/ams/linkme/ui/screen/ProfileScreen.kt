package com.ams.linkme.ui.screen

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.ams.linkme.R
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.data.userMessage
import com.ams.linkme.domain.Interests
import com.ams.linkme.ui.*
import com.ams.linkme.ui.viewmodel.ProfileViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar

class ProfileScreen(
    private val activity: AppCompatActivity,
    private val model: ProfileViewModel,
    onEditInterests: () -> Unit,
    onSignOut: () -> Unit,
) {
    private val body = activity.column(24)
    val view = activity.scrollContent(body)

    init {
        body.addView(activity.label("Your profile", 30f, true))
        body.addView(
            activity.label("Give someone a reason to say hello.", 16f, secondary = true).apply {
                space(top = 8, bottom = 20)
            }
        )
        val progress = activity.spinner()
        body.addView(progress)
        val error =
            activity.label("", 14f).apply {
                setTextColor(activity.color(R.color.error))
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                space(bottom = 12)
            }
        body.addView(error)
        val retry =
            activity.button("Try loading again", outlined = true).apply {
                setOnClickListener { model.load() }
            }
        body.addView(retry)
        val form = activity.column()
        body.addView(form)
        val (nameBox, name) = activity.field("Name", R.id.profile_name, max = 40)
        form.addView(nameBox)
        val (bioBox, bio) =
            activity.field("A little about you", R.id.profile_bio, max = 240, multiline = true)
        bioBox.space(top = 12)
        form.addView(bioBox)
        form.addView(
            activity.label(activity.getString(R.string.bio_hint), 13f, secondary = true).apply {
                space(top = 4)
            }
        )
        form.addView(activity.label("Your interest mix", 20f, true).apply { space(top = 28) })
        form.addView(
            activity
                .label(
                    "Specific topics, unexpected combinations, your own words.",
                    14f,
                    secondary = true,
                )
                .apply { space(top = 6, bottom = 8) }
        )
        val group =
            ChipGroup(activity).apply {
                chipSpacingHorizontal = activity.dp(8)
                chipSpacingVertical = activity.dp(4)
            }
        form.addView(group)
        form.addView(
            activity.button("Edit interests", outlined = true).apply {
                id = R.id.profile_interests
                space(top = 8)
                setOnClickListener { onEditInterests() }
            }
        )
        var displayedInterests: List<String>? = null
        val visible =
            MaterialSwitch(activity).apply {
                text = "Show my profile in Discover"
                minHeight = activity.dp(56)
                space(top = 20)
            }
        form.addView(visible)
        form.addView(
            activity.label(
                "Others can see your name, introduction and interests. Your email and existing contact details stay private.",
                14f,
                secondary = true,
            )
        )
        val save =
            activity.button("Save profile").apply {
                id = R.id.profile_save
                space(top = 24)
                setOnClickListener { model.save() }
            }
        form.addView(save)
        val privateLabel =
            activity.label("Signed in as ${model.email}", 13f, secondary = true).apply {
                space(top = 24)
            }
        body.addView(privateLabel)
        form.addView(
            activity.button("Blocked people", textOnly = true).apply {
                setOnClickListener { showBlocked() }
                space(top = 8)
            }
        )
        body.addView(
            activity.button("Sign out", outlined = true).apply {
                setOnClickListener { onSignOut() }
                space(top = 8, bottom = 24)
            }
        )
        var populated = false
        model.state.observe(activity) { state ->
            progress.visibility = if (state.loading) View.VISIBLE else View.GONE
            error.text = state.error.orEmpty()
            error.visibility = if (state.error != null) View.VISIBLE else View.GONE
            retry.visibility = if (!model.draftLoaded && !state.loading) View.VISIBLE else View.GONE
            form.visibility = if (model.draftLoaded) View.VISIBLE else View.GONE
            save.text = if (state.saving) "Saving…" else "Save profile"
            save.isEnabled = !state.saving && model.draftLoaded
            val tags = model.interests.toList()
            if (model.draftLoaded && tags != displayedInterests) {
                displayedInterests = tags
                group.removeAllViews()
                tags.forEach { tag ->
                    group.addView(
                        Chip(activity).apply {
                            text = Interests.label(tag)
                            maxWidth =
                                minOf(
                                    activity.resources.displayMetrics.widthPixels,
                                    activity.dp(720),
                                ) - activity.dp(48)
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            minimumHeight = activity.dp(48)
                            setEnsureMinTouchTargetSize(true)
                            setOnClickListener { onEditInterests() }
                        }
                    )
                }
            }
            if (model.draftLoaded && !populated) {
                name.setText(model.name)
                bio.setText(model.bio)
                visible.isChecked = model.discoverable
                populated = true
            }
        }
        name.doAfterTextChanged { model.name = it.toString() }
        bio.doAfterTextChanged { model.bio = it.toString() }
        visible.setOnCheckedChangeListener { _, checked -> model.discoverable = checked }
    }

    private fun showBlocked() {
        val repo = LinkMeRepository.instance
        repo.blocked { result ->
            if (activity.isFinishing) return@blocked
            result.fold(
                { ids ->
                    if (ids.isEmpty()) {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("No blocked people")
                            .setMessage("You can block someone from the menu in a conversation.")
                            .setPositiveButton("Got it", null)
                            .show()
                        return@fold
                    }
                    val people = ids.toList()
                    val names = Array(people.size) { "LinkMe member ${it+1}" }
                    var left = people.size
                    people.forEachIndexed { index, id ->
                        repo.publicProfile(id) { user ->
                            names[index] = user?.username?.ifBlank { names[index] } ?: names[index]
                            left--
                            if (left == 0 && !activity.isFinishing)
                                MaterialAlertDialogBuilder(activity)
                                    .setTitle("Blocked people")
                                    .setItems(names) { _, which ->
                                        MaterialAlertDialogBuilder(activity)
                                            .setTitle("Unblock ${names[which]}?")
                                            .setMessage(
                                                "You will be able to discover and message each other again."
                                            )
                                            .setNegativeButton("Cancel", null)
                                            .setPositiveButton("Unblock") { _, _ ->
                                                repo.setBlocked(people[which], false) { update ->
                                                    Snackbar.make(
                                                            view,
                                                            update.fold(
                                                                { "Person unblocked" },
                                                                { it.userMessage() },
                                                            ),
                                                            Snackbar.LENGTH_LONG,
                                                        )
                                                        .setAnchorView(
                                                            activity.findViewById<View>(
                                                                R.id.main_navigation
                                                            )
                                                        )
                                                        .show()
                                                }
                                            }
                                            .show()
                                    }
                                    .setNegativeButton("Close", null)
                                    .show()
                        }
                    }
                },
                {
                    Snackbar.make(view, it.userMessage(), Snackbar.LENGTH_LONG)
                        .setAnchorView(activity.findViewById<View>(R.id.main_navigation))
                        .show()
                },
            )
        }
    }
}
