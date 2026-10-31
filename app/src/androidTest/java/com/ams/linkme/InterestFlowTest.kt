package com.ams.linkme

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.ams.linkme.ui.activity.MainActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Explicit local drafts and demo-only profile publication; production is never used. */
@RunWith(AndroidJUnit4::class)
class InterestFlowTest {
    private val context
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun previewOnly() {
        Assume.assumeTrue(BuildConfig.DEBUG && BuildConfig.USE_EMULATORS)
        closeActivities()
        FirebaseAuth.getInstance().signOut()
    }

    @After
    fun cleanup() {
        if (BuildConfig.USE_EMULATORS) {
            closeActivities()
            FirebaseAuth.getInstance().signOut()
        }
    }

    private fun closeActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            Stage.values()
                .filter { it != Stage.DESTROYED }
                .flatMap { monitor.getActivitiesInStage(it) }
                .distinct()
                .forEach(Activity::finishAndRemoveTask)
        }
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.waitForIdle(500, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()!!
        val suffix = InstrumentationRegistry.getArguments().getString("captureSuffix").orEmpty()
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name-21$suffix.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LinkMeInterests")
            }
        val uri =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        context.contentResolver.openOutputStream(uri)!!.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun launch(selected: List<String>, bio: String = "") =
        ActivityScenario.launchActivityForResult<Activity>(
            Intent()
                .setClassName(context.packageName, "com.ams.linkme.ui.activity.InterestsActivity")
                .putStringArrayListExtra("selectedInterests", ArrayList(selected))
                .putExtra("introduction", bio)
        )

    private fun waitFor(check: () -> Unit) {
        val end = System.currentTimeMillis() + 15000
        var error: Throwable? = null
        while (System.currentTimeMillis() < end) {
            try {
                check()
                return
            } catch (t: Throwable) {
                error = t
                Thread.sleep(100)
            }
        }
        throw AssertionError("Interest flow did not reach its expected state", error)
    }

    private fun visible(text: String) = waitFor {
        onView(allOf(withText(text), isDisplayed())).check(matches(isDisplayed()))
    }

    @Test
    fun searchCustomAliasAndRotationReturnAnExplicitDraft() {
        launch(listOf("Music")).use { scenario ->
            onView(withId(R.id.interest_search)).perform(replaceText("摄影"), closeSoftKeyboard())
            visible("Photography")
            onView(allOf(withText("Photography"), isDisplayed())).perform(click())
            onView(withId(R.id.interest_search))
                .perform(replaceText("Urban sketch walks"), closeSoftKeyboard())
            onView(withId(R.id.interest_create)).perform(scrollTo(), click())
            onView(withId(R.id.interests_selected)).check(matches(withText("3 / 16 selected")))
            scenario.recreate()
            onView(withId(R.id.interests_selected)).check(matches(withText("3 / 16 selected")))
            onView(withId(R.id.interest_done)).perform(click())
            waitFor { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
            assertEquals(
                listOf("Music", "Photography", "Urban sketch walks"),
                scenario.result.resultData.getStringArrayListExtra("selectedInterests"),
            )
        }
    }

    @Test
    fun introductionAndSceneSuggestionsStayUnselectedAndBackCancels() {
        launch(emptyList(), "I enjoy photography and indie games.").use { scenario ->
            onView(withId(R.id.interests_selected)).check(matches(withText("0 / 16 selected")))
            onView(withId(R.id.interest_explore)).perform(scrollTo(), click())
            repeat(3) { onView(withId(R.id.interest_choice_one)).perform(scrollTo(), click()) }
            onView(withId(R.id.interests_selected)).check(matches(withText("0 / 16 selected")))
            Espresso.pressBackUnconditionally()
            waitFor { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test
    fun profileSavePublishesCustomInterestsButDiscoveryEditingOnlyFilters() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        Tasks.await(
            auth.signInWithEmailAndPassword("alex@linkme.example.test", " linkme-preview-pass "),
            15,
            TimeUnit.SECONDS,
        )
        val uid = auth.currentUser!!.uid
        val custom = "Urban sketch ${System.currentTimeMillis()}"
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { onView(withId(R.id.nav_profile)).check(matches(isDisplayed())) }
            onView(withId(R.id.nav_profile)).perform(click())
            waitFor {
                onView(allOf(withId(R.id.profile_name), isDisplayed()))
                    .check(matches(withText("Alex Morgan")))
            }
            onView(withId(R.id.profile_interests)).perform(scrollTo(), click())
            onView(withId(R.id.interest_search)).perform(replaceText(custom), closeSoftKeyboard())
            onView(withId(R.id.interest_create)).perform(scrollTo(), click())
            onView(withId(R.id.interest_done)).perform(click())
            val before =
                Tasks.await(
                    db.collection("PublicProfiles").document(uid).get(Source.SERVER),
                    10,
                    TimeUnit.SECONDS,
                )
            assertFalse((before.get("interests") as List<*>).contains(custom))
            onView(withId(R.id.profile_save)).perform(scrollTo(), click())
            var published = emptyList<Any?>()
            waitFor {
                val saved =
                    Tasks.await(
                        db.collection("PublicProfiles").document(uid).get(Source.SERVER),
                        5,
                        TimeUnit.SECONDS,
                    )
                published = saved.get("interests") as List<*>
                assertTrue(published.contains(custom))
                assertTrue(
                    (saved.get("interestKeys") as List<*>).any { (it as String).startsWith("c:") }
                )
                assertFalse(saved.contains("phone"))
                assertFalse(saved.contains("email"))
            }
            waitFor { onView(withText("Profile saved")).check(matches(isCompletelyDisplayed())) }
            capture("profile-saved")
            waitFor {
                onView(withText("Profile saved"))
                    .check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
            }
            onView(withId(R.id.profile_interests)).perform(scrollTo())
            capture("profile")
            onView(withId(R.id.nav_discover)).perform(click())
            onView(withId(R.id.discover_interests)).perform(click())
            onView(withId(R.id.interest_search))
                .perform(replaceText("Board games"), closeSoftKeyboard())
            onView(
                    allOf(
                        withText("Board games"),
                        withClassName(org.hamcrest.Matchers.endsWith("Chip")),
                    )
                )
                .perform(scrollTo(), click())
            onView(withId(R.id.interest_done)).perform(click())
            waitFor {
                onView(
                        allOf(
                            withText("Board games"),
                            withClassName(org.hamcrest.Matchers.endsWith("Chip")),
                        )
                    )
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
            }
            val after =
                Tasks.await(
                    db.collection("PublicProfiles").document(uid).get(Source.SERVER),
                    10,
                    TimeUnit.SECONDS,
                )
            assertEquals(published, after.get("interests"))
            visible("Maya Chen")
            capture("discover")
        }
    }

    @Test
    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    fun captureNativeEditorAndExploration() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        launch(
                listOf("Indie games", "Cooking", "Live music"),
                "I take photos and enjoy indie games.",
            )
            .use {
                onView(isAssignableFrom(android.widget.ScrollView::class.java)).check { view, _ ->
                    assertTrue(
                        "Interest body must retain usable scrolling height",
                        view.height >= context.resources.displayMetrics.density * 128,
                    )
                }
                capture("editor")
                onView(allOf(withText("Indie games"), org.hamcrest.Matchers.not(isChecked())))
                    .perform(scrollTo())
                    .check(matches(isCompletelyDisplayed()))
                capture("mix-scrolled")
                if (context.resources.configuration.let { it.screenHeightDp / it.fontScale < 600 })
                    onView(withId(R.id.interest_search)).perform(scrollTo())
                onView(withId(R.id.interest_search))
                    .perform(click(), replaceText("Urban sketch walks"))
                capture("custom-keyboard")
                Espresso.closeSoftKeyboard()
                capture("custom")
                onView(withId(R.id.interest_create)).perform(scrollTo())
                capture("custom-action")
                onView(withId(R.id.interest_create)).perform(click())
                onView(withId(R.id.interest_explore)).perform(scrollTo(), click())
                capture("explore")
                repeat(3) { onView(withId(R.id.interest_choice_one)).perform(scrollTo(), click()) }
                capture("suggestions")
                instrumentation.runOnMainSync {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                waitFor { onView(withId(R.id.interest_done)).check(matches(isDisplayed())) }
                capture("editor-dark")
            }
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
