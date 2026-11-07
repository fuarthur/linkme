package com.ams.linkme

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.ams.linkme.domain.LinkMeLogic
import com.ams.linkme.ui.activity.ChatActivity
import com.ams.linkme.ui.activity.LoginActivity
import com.ams.linkme.ui.activity.MainActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.io.File
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LinkMeFlowTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val auth
        get() = FirebaseAuth.getInstance()

    private val db
        get() = FirebaseFirestore.getInstance()

    private val password = " linkme-preview-pass "

    @Before
    fun requireLocalPreview() {
        Assume.assumeTrue(
            "Device flow tests never write to production",
            BuildConfig.DEBUG && BuildConfig.USE_EMULATORS,
        )
        assertEquals("demo-linkme", FirebaseApp.getInstance().options.projectId)
        closeActivities()
        auth.signOut()
    }

    @After
    fun close() {
        if (BuildConfig.USE_EMULATORS) {
            closeActivities()
            auth.signOut()
        }
    }

    private fun closeActivities() {
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            Stage.values()
                .filter { it != Stage.DESTROYED }
                .flatMap { monitor.getActivitiesInStage(it) }
                .distinct()
                .forEach(Activity::finishAndRemoveTask)
        }
    }

    private fun signIn() {
        Tasks.await(
            auth.signInWithEmailAndPassword("alex@linkme.example.test", password),
            15,
            TimeUnit.SECONDS,
        )
    }

    private fun waitFor(check: () -> Unit) {
        val deadline = System.currentTimeMillis() + 15000
        var failure: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                check()
                return
            } catch (t: Throwable) {
                failure = t
                Thread.sleep(100)
            }
        }
        throw AssertionError("UI did not reach the expected state", failure)
    }

    private fun visible(text: String) = waitFor {
        onView(allOf(withText(text), isDisplayed())).check(matches(isDisplayed()))
    }

    private fun renderedFrames() {
        // Property values reach the compositor on a later frame.
        val rendered = java.util.concurrent.CountDownLatch(1)
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .first()
                .window
                .decorView
                .let { view ->
                    view.postOnAnimation { view.postOnAnimation { rendered.countDown() } }
                }
        }
        assertTrue(rendered.await(3, TimeUnit.SECONDS))
    }

    private fun screenshot(name: String) {
        repeat(3) {
            renderedFrames()
            waitFor {
                instrumentation.runOnMainSync {
                    fun settled(view: android.view.View) {
                        if (!view.isShown) return
                        if (view is RecyclerView) {
                            assertFalse(
                                "Capture waits for list animation",
                                view.itemAnimator?.isRunning == true,
                            )
                            assertFalse(view.hasPendingAdapterUpdates())
                            assertFalse(view.isComputingLayout)
                            for (index in 0 until view.childCount) assertEquals(
                                "Capture waits for visible rows",
                                1f,
                                view.getChildAt(index).alpha,
                                0.001f,
                            )
                        }
                        if (view is android.view.ViewGroup)
                            for (index in 0 until view.childCount) settled(view.getChildAt(index))
                    }
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .forEach { settled(it.window.decorView) }
                }
            }
        }
        instrumentation.waitForIdleSync()
        val bitmap =
            instrumentation.uiAutomation.takeScreenshot() ?: error("No emulator screenshot")
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val suffix = InstrumentationRegistry.getArguments().getString("captureSuffix").orEmpty()
            val values =
                ContentValues().apply {
                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        if (suffix.isEmpty()) "$name.png" else "$name-$suffix.png",
                    )
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LinkMePreview")
                }
            val uri =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("No screenshot output")
            context.contentResolver.openOutputStream(uri)!!.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } else {
            val folder = File(context.getExternalFilesDir(null), "review").apply { mkdirs() }
            File(folder, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        bitmap.recycle()
    }

    @Test
    fun a_backendProfileReadUsesTheDemoProject() {
        signIn()
        val profile =
            Tasks.await(
                db.collection("Users").document(auth.currentUser!!.uid).get(),
                10,
                TimeUnit.SECONDS,
            )
        assertEquals("Alex Morgan", profile.getString("username"))
    }

    @Test
    fun a_loginValidatesAndPreservesPasswordWhitespace() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.auth_submit)).perform(click())
        visible("Enter a valid email address.")
        screenshot("login")
        onView(withId(R.id.email)).perform(replaceText("alex@linkme.example.test"))
        onView(withId(R.id.password)).perform(replaceText(password), closeSoftKeyboard())
        onView(withId(R.id.auth_submit)).perform(click())
        waitFor { onView(withId(R.id.nav_discover)).check(matches(isDisplayed())) }
        assertNotNull(auth.currentUser)
    }

    @Test
    fun b_profileRoundTripPreservesPrivateContactFields() {
        signIn()
        ActivityScenario.launch(MainActivity::class.java)
        waitFor { onView(withId(R.id.nav_profile)).check(matches(isDisplayed())) }
        onView(withId(R.id.nav_profile)).perform(click())
        waitFor { onView(withId(R.id.profile_name)).check(matches(withText("Alex Morgan"))) }
        screenshot("profile")
        val introduction = "Co-op games, a new playlist, and good conversation."
        onView(withId(R.id.profile_bio))
            .perform(scrollTo(), replaceText(introduction), closeSoftKeyboard())
        onView(withId(R.id.profile_save)).perform(scrollTo(), click())
        waitFor {
            assertEquals(
                introduction,
                Tasks.await(
                        db.collection("Users").document(auth.currentUser!!.uid).get(),
                        5,
                        TimeUnit.SECONDS,
                    )
                    .getString("bio"),
            )
        }
        val privateDoc =
            Tasks.await(
                db.collection("Users").document(auth.currentUser!!.uid).get(),
                5,
                TimeUnit.SECONDS,
            )
        assertEquals("202-555-0100", privateDoc.getString("phone"))
        val publicDoc =
            Tasks.await(
                db.collection("PublicProfiles").document(auth.currentUser!!.uid).get(),
                5,
                TimeUnit.SECONDS,
            )
        assertFalse(publicDoc.contains("email"))
        assertFalse(publicDoc.contains("phone"))
        onView(withId(R.id.nav_profile)).perform(click())
        onView(withId(R.id.profile_bio)).perform(scrollTo()).check(matches(withText(introduction)))
        onView(withId(R.id.nav_discover)).perform(click())
        waitFor { onView(withId(R.id.people_list)).check(matches(isDisplayed())) }
        try {
            visible("Leo Rivera")
        } finally {
            screenshot("phone")
        }
        onView(withId(R.id.nav_chats)).perform(click())
        visible("Maya Chen")
        screenshot("inbox")
    }

    @Test
    fun c_realtimeChatPreservesNewDraftAndSupportsBlocking() {
        signIn()
        ActivityScenario.launch(MainActivity::class.java)
        waitFor { onView(withId(R.id.people_list)).check(matches(isDisplayed())) }
        onView(withId(R.id.people_list))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Maya Chen"))
                )
            )
        onView(withContentDescription("Chat with Maya Chen")).perform(click())
        visible("Maya Chen")
        val submitted = "Android flow test ${System.currentTimeMillis()}"
        Tasks.await(db.disableNetwork(), 5, TimeUnit.SECONDS)
        onView(withId(R.id.message_draft)).perform(replaceText(submitted), closeSoftKeyboard())
        onView(withId(R.id.send_message)).perform(click())
        onView(withId(R.id.send_message)).check(matches(not(isEnabled())))
        onView(withId(R.id.message_draft))
            .perform(replaceText("A newer draft stays here"), closeSoftKeyboard())
        Tasks.await(db.enableNetwork(), 5, TimeUnit.SECONDS)
        waitFor { onView(withId(R.id.send_message)).check(matches(withText("Send"))) }
        onView(withId(R.id.message_draft)).check(matches(withText("A newer draft stays here")))
        val matches =
            Tasks.await(
                db.collection("Messages")
                    .whereEqualTo("senderId", auth.currentUser!!.uid)
                    .whereEqualTo("content", submitted)
                    .limit(100)
                    .get(),
                5,
                TimeUnit.SECONDS,
            )
        assertEquals(1, matches.size())
        val peerApp =
            FirebaseApp.initializeApp(
                context,
                FirebaseApp.getInstance().options,
                "qa-peer-${System.currentTimeMillis()}",
            )
        val peerAuth = FirebaseAuth.getInstance(peerApp)
        peerAuth.useEmulator("10.0.2.2", 9099)
        val peerDb = FirebaseFirestore.getInstance(peerApp)
        peerDb.firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        peerDb.useEmulator("10.0.2.2", 8080)
        try {
            Tasks.await(
                peerAuth.signInWithEmailAndPassword("maya@linkme.example.test", password),
                10,
                TimeUnit.SECONDS,
            )
            val sender = peerAuth.currentUser!!.uid
            val receiver = auth.currentUser!!.uid
            val members = listOf(sender, receiver).sorted()
            val id = LinkMeLogic.conversationId(sender, receiver)
            val reply = "A live reply from the other person. ${System.currentTimeMillis()}"
            val batch = peerDb.batch()
            val replyDocument = peerDb.collection("Messages").document()
            batch.set(
                peerDb.collection("Chats").document(id),
                mapOf(
                    "members" to members,
                    "lastMessage" to reply,
                    "lastMessageId" to replyDocument.id,
                    "lastSenderId" to sender,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            batch.set(
                replyDocument,
                mapOf(
                    "members" to members,
                    "senderId" to sender,
                    "receiveId" to receiver,
                    "conversationId" to id,
                    "content" to reply,
                    "timestamp" to FieldValue.serverTimestamp(),
                ),
            )
            Tasks.await(batch.commit(), 10, TimeUnit.SECONDS)
            visible(reply)
            screenshot("chat")
        } finally {
            Tasks.await(peerDb.terminate(), 5, TimeUnit.SECONDS)
            peerApp.delete()
        }
        onView(withContentDescription("Conversation options")).perform(click())
        onView(withText("Block")).perform(click())
        waitFor { onView(withId(R.id.message_draft)).check(matches(not(isEnabled()))) }
        screenshot("blocked")
        onView(withContentDescription("Conversation options")).perform(click())
        onView(withText("Unblock")).perform(click())
        waitFor { onView(withId(R.id.message_draft)).check(matches(isEnabled())) }
    }

    @Test
    fun e_captureNativeLightAndDarkScreens() {
        signIn()
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        ActivityScenario.launch(MainActivity::class.java)
        waitFor { onView(withId(R.id.people_list)).check(matches(isDisplayed())) }
        try {
            visible("Leo Rivera")
        } finally {
            screenshot("phone")
        }
        onView(withId(R.id.nav_chats)).perform(click())
        visible("Maya Chen")
        screenshot("inbox")
        onView(allOf(withText("Maya Chen"), isDisplayed())).perform(click())
        visible("I am in. What have you been playing lately?")
        screenshot("chat")
        androidx.test.espresso.Espresso.pressBack()
        onView(withId(R.id.nav_discover)).perform(click())
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        waitFor { onView(withId(R.id.people_list)).check(matches(isDisplayed())) }
        visible("Leo Rivera")
        screenshot("phone-dark")
        onView(withId(R.id.nav_profile)).perform(click())
        waitFor { onView(withId(R.id.profile_name)).check(matches(withText("Alex Morgan"))) }
        screenshot("profile-dark")
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Test
    fun f_emptyChatKeepsComposerReachable() {
        signIn()
        val candidates =
            Tasks.await(
                db.collection("PublicProfiles").whereEqualTo("discoverable", true).limit(60).get(),
                10,
                TimeUnit.SECONDS,
            )
        val peer = candidates.documents.first { it.getString("username") == "Leo Rivera" }
        ActivityScenario.launch<ChatActivity>(
                Intent(context, ChatActivity::class.java)
                    .putExtra("userId", peer.id)
                    .putExtra("username", "Leo Rivera")
            )
            .use {
                visible("Start with common ground")
                try {
                    waitFor {
                        onView(withId(R.id.message_draft)).check(matches(isCompletelyDisplayed()))
                    }
                    onView(withText("Break the ice")).perform(click())
                    onView(withId(R.id.message_draft))
                        .check(matches(withText("Hey Leo Rivera, what have you been into lately?")))
                    onView(withId(R.id.message_draft)).perform(click())
                    var fullscreenKeyboard = false
                    waitFor {
                        var keyboardExpanded = false
                        instrumentation.runOnMainSync {
                            val window =
                                ActivityLifecycleMonitorRegistry.getInstance()
                                    .getActivitiesInStage(Stage.RESUMED)
                                    .first()
                                    .window
                            val insets =
                                androidx.core.view.ViewCompat.getRootWindowInsets(window.decorView)
                            fullscreenKeyboard =
                                window.context
                                    .getSystemService(
                                        android.view.inputmethod.InputMethodManager::class.java
                                    )
                                    .isFullscreenMode
                            keyboardExpanded =
                                fullscreenKeyboard ||
                                    insets?.isVisible(
                                        androidx.core.view.WindowInsetsCompat.Type.ime()
                                    ) == true &&
                                        insets
                                            .getInsets(
                                                androidx.core.view.WindowInsetsCompat.Type.ime()
                                            )
                                            .bottom > 80 * context.resources.displayMetrics.density
                        }
                        assertTrue(
                            "Keyboard must be expanded for this regression",
                            keyboardExpanded,
                        )
                    }
                    screenshot("keyboard")
                    // A landscape IME may legitimately replace the app with its extract editor.
                    if (fullscreenKeyboard) androidx.test.espresso.Espresso.closeSoftKeyboard()
                    waitFor {
                        onView(withId(R.id.message_draft)).check(matches(isCompletelyDisplayed()))
                        onView(withId(R.id.send_message)).check(matches(isCompletelyDisplayed()))
                    }
                    androidx.test.espresso.Espresso.closeSoftKeyboard()
                    onView(withId(R.id.message_draft))
                        .check(matches(withText("Hey Leo Rivera, what have you been into lately?")))
                } finally {
                    screenshot("empty-chat")
                }
            }
    }

    @Test
    fun d_missingChatRecipientFinishesWithoutCrash() {
        signIn()
        val scenario =
            ActivityScenario.launch<ChatActivity>(Intent(context, ChatActivity::class.java))
        waitFor { assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state) }
        scenario.close()
    }
}
