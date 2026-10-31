package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R
import com.ams.linkme.data.LinkMeRepository
import com.ams.linkme.domain.InterestCatalog
import com.ams.linkme.model.User
import com.ams.linkme.ui.*
import com.ams.linkme.ui.screen.DiscoverScreen
import com.ams.linkme.ui.screen.InboxScreen
import com.ams.linkme.ui.screen.ProfileScreen
import com.ams.linkme.ui.viewmodel.InboxViewModel
import com.ams.linkme.ui.viewmodel.MainViewModel
import com.ams.linkme.ui.viewmodel.ProfileViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val repo = LinkMeRepository.instance
    private lateinit var inbox: InboxViewModel
    private lateinit var discovery: MainViewModel
    private lateinit var profile: ProfileViewModel
    private lateinit var navigation: NavigationBarView
    private var selected = R.id.nav_discover
    private var visible = false
    private var completingSetup = false
    private val pages = mutableMapOf<Int, View>()
    private val editProfileInterests =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                result.data?.getStringArrayListExtra("selectedInterests")?.let {
                    profile.setInterests(it)
                }
        }
    private val editDiscoveryInterests =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                result.data?.getStringArrayListExtra("selectedInterests")?.let {
                    discovery.select(it)
                }
        }

    private fun editorIntent(selected: List<String>, purpose: String) =
        Intent(this, InterestsActivity::class.java)
            .putStringArrayListExtra("selectedInterests", ArrayList(selected))
            .putExtra("introduction", profile.bio)
            .putExtra("editorPurpose", purpose)

    private val authListener =
        FirebaseAuth.AuthStateListener { if (it.currentUser == null) openLogin() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (repo.uid == null) {
            openLogin()
            return
        }
        discovery = ViewModelProvider(this)[MainViewModel::class.java]
        inbox = ViewModelProvider(this)[InboxViewModel::class.java]
        profile = ViewModelProvider(this)[ProfileViewModel::class.java]
        selected = savedInstanceState?.getInt("tab", R.id.nav_discover) ?: R.id.nav_discover
        completingSetup = savedInstanceState?.getBoolean("completingSetup") ?: false
        val discoverScreen =
            DiscoverScreen(this, discovery, ::openChat) {
                editDiscoveryInterests.launch(
                    editorIntent(discovery.state.value!!.selected, "discover")
                )
            }
        val inboxScreen =
            InboxScreen(
                this,
                inbox,
                { conversation ->
                    openChat(
                        conversation.profile
                            ?: User(uid = conversation.peerUid, username = "LinkMe member")
                    )
                },
            ) {
                showTab(R.id.nav_discover)
            }
        val profileScreen =
            ProfileScreen(
                this,
                profile,
                onEditInterests = {
                    editProfileInterests.launch(editorIntent(profile.interests.toList(), "profile"))
                },
            ) {
                inbox.stop()
                repo.signOut()
                openLogin()
            }
        pages[R.id.nav_discover] = discoverScreen.view
        pages[R.id.nav_chats] = inboxScreen.view
        pages[R.id.nav_profile] = profileScreen.view
        val content = ContentFrame(this)
        pages.values.forEach {
            content.addView(
                it,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        val center =
            FrameLayout(this).apply {
                addView(
                    content,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER_HORIZONTAL,
                    ),
                )
            }
        val root = column()
        root.addView(previewBanner())
        if (resources.configuration.screenWidthDp >= 600) {
            val horizontal = row()
            navigation = NavigationRailView(this)
            horizontal.addView(
                navigation,
                LinearLayout.LayoutParams(dp(88), ViewGroup.LayoutParams.MATCH_PARENT),
            )
            horizontal.addView(
                center,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
            )
            root.addView(
                horizontal,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
            )
        } else {
            root.addView(
                center,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
            )
            navigation = BottomNavigationView(this)
            root.addView(navigation)
        }
        navigation.id = R.id.main_navigation
        navigation.menu.add(0, R.id.nav_discover, 0, "Discover").setIcon(R.drawable.ic_discover)
        navigation.menu.add(0, R.id.nav_chats, 1, "Chats").setIcon(R.drawable.ic_chat)
        navigation.menu.add(0, R.id.nav_profile, 2, "Profile").setIcon(R.drawable.ic_profile)
        navigation.setOnItemSelectedListener {
            selectPage(it.itemId)
            true
        }
        var savedSeen = profile.state.value?.saved ?: 0
        var onboardingChecked = savedInstanceState?.getBoolean("onboardingChecked") ?: false
        profile.state.observe(this) { state ->
            state.profile?.let { user ->
                discovery.useProfile(user)
                if (!onboardingChecked) {
                    onboardingChecked = true
                    completingSetup = user.username.isBlank() || user.interests.isEmpty()
                    if (completingSetup) showTab(R.id.nav_profile)
                }
            }
            if (state.saved > savedSeen) {
                savedSeen = state.saved
                Snackbar.make(root, "Profile saved", Snackbar.LENGTH_SHORT)
                    .apply { if (navigation is BottomNavigationView) setAnchorView(navigation) }
                    .show()
                if (completingSetup) {
                    completingSetup = false
                    showTab(R.id.nav_discover)
                }
            }
        }
        mount(root, ime = true)
        showTab(selected)
        profile.load()
    }

    private fun openChat(user: User) {
        if (user.uid.isBlank() || user.uid == repo.uid) return
        startActivity(
            Intent(this, ChatActivity::class.java)
                .putExtra("userId", user.uid)
                .putExtra("username", user.username)
                .putStringArrayListExtra(
                    "sharedInterests",
                    ArrayList(
                        InterestCatalog.match(discovery.state.value!!.selected, user.interests)
                            .let { it.exact.ifEmpty { it.relatedGroups } }
                    ),
                )
        )
    }

    private fun showTab(id: Int) {
        if (::navigation.isInitialized && navigation.selectedItemId != id)
            navigation.selectedItemId = id
        else selectPage(id)
    }

    private fun selectPage(id: Int) {
        selected = id
        pages.forEach { (key, view) ->
            view.visibility = if (key == id) View.VISIBLE else View.GONE
        }
        if (::inbox.isInitialized) {
            if (id == R.id.nav_chats && visible) inbox.start() else inbox.stop()
        }
    }

    override fun onStart() {
        super.onStart()
        visible = true
        repo.auth.addAuthStateListener(authListener)
        if (::inbox.isInitialized && selected == R.id.nav_chats) inbox.start()
        if (::discovery.isInitialized && discovery.state.value?.searched == true)
            discovery.refresh()
    }

    override fun onStop() {
        visible = false
        repo.auth.removeAuthStateListener(authListener)
        if (::inbox.isInitialized) inbox.stop()
        super.onStop()
    }

    override fun onSaveInstanceState(out: Bundle) {
        out.putInt("tab", selected)
        out.putBoolean("onboardingChecked", true)
        out.putBoolean("completingSetup", completingSetup)
        super.onSaveInstanceState(out)
    }

    private fun openLogin() {
        if (isFinishing) return
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
