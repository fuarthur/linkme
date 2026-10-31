package com.ams.linkme

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ams.linkme.domain.InterestCatalog
import com.ams.linkme.ui.activity.MainActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Public bundled library and explicit local drafts; these tests never write production data. */
@RunWith(AndroidJUnit4::class)
class WikiFlowTest {
    private val context
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun demoOnly() {
        Assume.assumeTrue(BuildConfig.DEBUG && BuildConfig.USE_EMULATORS)
    }

    private fun launch(selected: List<String>, query: String = "", parent: Boolean = false) =
        ActivityScenario.launchActivityForResult<Activity>(
            Intent()
                .setClassName(
                    context.packageName,
                    "com.ams.linkme.ui.activity.${if(parent) "InterestsActivity" else "WikiTopicsActivity"}",
                )
                .putStringArrayListExtra("selectedInterests", ArrayList(selected))
                .putExtra("initialQuery", query)
        )

    private fun waitFor(check: () -> Unit) {
        val end = System.currentTimeMillis() + 15000
        var last: Throwable? = null
        while (System.currentTimeMillis() < end) {
            try {
                check()
                return
            } catch (e: Throwable) {
                last = e
                Thread.sleep(100)
            }
        }
        throw AssertionError("Wiki topic flow did not reach its expected state", last)
    }

    private fun select(title: String) = waitFor {
        onView(withId(R.id.wiki_results))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(title)),
                    click(),
                )
            )
    }

    private fun query(text: String) {
        onView(withId(R.id.wiki_search)).perform(replaceText(text), closeSoftKeyboard())
    }

    private fun longTopic(): List<String> =
        context.assets.open("wiki-topics/topics.tsv").bufferedReader().use { r ->
            r.lineSequence()
                .drop(1)
                .map { it.split('\t') }
                .first { it[1].length > 40 && it[2] == "Physics & astronomy" }
        }

    @Test
    fun searchAndRecreationReturnAnExplicitMix() {
        launch(listOf("Music"), "Python").use { scenario ->
            select("Python (programming language)")
            onView(withId(R.id.wiki_apply)).check(matches(withText("Use 2 interests")))
            scenario.recreate()
            onView(withId(R.id.wiki_search)).check(matches(withText("Python")))
            onView(withId(R.id.wiki_apply)).check(matches(withText("Use 2 interests")))
            onView(withId(R.id.wiki_apply)).perform(click())
            waitFor { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
            assertEquals(
                listOf("Music", "Python (programming language)"),
                scenario.result.resultData.getStringArrayListExtra("selectedInterests"),
            )
        }
    }

    @Test
    fun cancelDoesNotReturnAChangedDraft() {
        launch(listOf("Music"), "Quantum").use { scenario ->
            select("Quantum mechanics")
            Espresso.pressBackUnconditionally()
            waitFor { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test
    fun fullMixCanRemoveAnExistingWikiChoiceAndRecover() {
        val original = listOf("Quantum mechanics") + (1..15).map { "My topic $it" }
        launch(original, "Quantum").use { scenario ->
            select("Quantum entanglement")
            onView(withId(R.id.wiki_feedback)).check(matches(withText(containsString("full"))))
            onView(withId(R.id.wiki_apply)).check(matches(withText("Use 16 interests")))
            select("Quantum mechanics")
            select("Quantum entanglement")
            onView(withId(R.id.wiki_apply)).perform(click())
            waitFor { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
            val result = scenario.result.resultData.getStringArrayListExtra("selectedInterests")!!
            assertEquals(16, result.size)
            assertTrue(result.contains("Quantum entanglement"))
            assertFalse(result.contains("Quantum mechanics"))
            assertTrue(result.containsAll(original.drop(1)))
        }
    }

    @Test
    fun longWikiTitleSurvivesParentDraftAndRotation() {
        val row = longTopic()
        launch(listOf("Music"), parent = true).use { scenario ->
            onView(withId(R.id.wiki_browse)).perform(scrollTo(), click())
            query(row[1])
            select(row[1])
            onView(withId(R.id.wiki_apply)).perform(click())
            onView(withId(R.id.interests_selected)).check(matches(withText("2 / 16 selected")))
            scenario.recreate()
            onView(withId(R.id.interests_selected)).check(matches(withText("2 / 16 selected")))
            onView(withId(R.id.interest_done)).perform(click())
            waitFor { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
            val result = scenario.result.resultData.getStringArrayListExtra("selectedInterests")!!
            assertEquals(row[4], result.last())
            assertTrue(result.last().length <= 40)
            assertEquals(row[1], InterestCatalog.label(result.last()))
            assertTrue(InterestCatalog.indexKeys(result).all { it.length <= 72 })
        }
    }

    @Test
    fun profileSavePublishesLongWikiReferenceUsingTheUnchangedPolicy() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val row = longTopic()
        Tasks.await(
            auth.signInWithEmailAndPassword("alex@linkme.example.test", " linkme-preview-pass "),
            15,
            TimeUnit.SECONDS,
        )
        val uid = auth.currentUser!!.uid
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                waitFor { onView(withId(R.id.nav_profile)).check(matches(isDisplayed())) }
                onView(withId(R.id.nav_profile)).perform(click())
                waitFor {
                    onView(allOf(withId(R.id.profile_name), isDisplayed()))
                        .check(matches(withText("Alex Morgan")))
                }
                onView(withId(R.id.profile_interests)).perform(scrollTo(), click())
                onView(withId(R.id.wiki_browse)).perform(scrollTo(), click())
                query(row[1])
                select(row[1])
                onView(withId(R.id.wiki_apply)).perform(click())
                onView(withId(R.id.interest_done)).perform(click())
                val before =
                    Tasks.await(
                        db.collection("PublicProfiles").document(uid).get(Source.SERVER),
                        10,
                        TimeUnit.SECONDS,
                    )
                assertFalse((before.get("interests") as List<*>).contains(row[4]))
                onView(withId(R.id.profile_save)).perform(scrollTo(), click())
                waitFor {
                    val saved =
                        Tasks.await(
                            db.collection("PublicProfiles").document(uid).get(Source.SERVER),
                            5,
                            TimeUnit.SECONDS,
                        )
                    assertTrue((saved.get("interests") as List<*>).contains(row[4]))
                    assertTrue(
                        (saved.get("interestKeys") as List<*>).containsAll(
                            InterestCatalog.indexKeys(listOf(row[4]))
                        )
                    )
                    assertFalse(saved.contains("phone"))
                    assertFalse(saved.contains("email"))
                }
                waitFor { onView(withText("Profile saved")).check(matches(isDisplayed())) }
                waitFor {
                    onView(withText("Profile saved"))
                        .check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
                }
                onView(withId(R.id.profile_interests)).perform(scrollTo())
                onView(allOf(withText(row[1]), withClassName(endsWith("Chip")), isDisplayed()))
                    .check(matches(isDisplayed()))
                if (android.os.Build.VERSION.SDK_INT >= 29) capture("wiki-profile")
            }
        } finally {
            auth.signOut()
        }
    }

    private fun capture(name: String) {
        val i = InstrumentationRegistry.getInstrumentation()
        i.waitForIdleSync()
        i.uiAutomation.waitForIdle(500, 5000)
        val bitmap = i.uiAutomation.takeScreenshot()!!
        val suffix = InstrumentationRegistry.getArguments().getString("captureSuffix").orEmpty()
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name-22$suffix.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LinkMeWiki")
            }
        val uri =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        context.contentResolver.openOutputStream(uri)!!.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test
    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    fun captureWikiPickerStates() {
        val i = InstrumentationRegistry.getInstrumentation()
        i.runOnMainSync { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        try {
            launch(listOf("Music"), "").use { scenario ->
                waitFor {
                    onView(withId(R.id.wiki_status))
                        .check(matches(withText(containsString("topics"))))
                }
                waitFor {
                    onView(withId(R.id.wiki_results)).check { view, _ ->
                        val list = view as RecyclerView
                        assertEquals(30919, list.adapter!!.itemCount)
                        assertTrue(list.childCount in 1..30)
                    }
                }
                capture("wiki-all")
                query("Quantum")
                waitFor {
                    onView(withId(R.id.wiki_status))
                        .check(
                            matches(
                                withText(
                                    allOf(containsString("topics"), not(containsString("30,919")))
                                )
                            )
                        )
                }
                capture("wiki-search")
                onView(withId(R.id.wiki_search)).perform(click())
                capture("wiki-keyboard")
                Espresso.closeSoftKeyboard()
                onView(withText("Sources")).perform(click())
                onView(withText(containsString("CC0"))).check(matches(isDisplayed()))
                capture("wiki-sources")
                Espresso.pressBack()
                onView(withId(R.id.wiki_fields)).perform(click())
                Espresso.onData(equalTo("Physics & astronomy")).perform(click())
                waitFor {
                    onView(withId(R.id.wiki_status))
                        .check(matches(withText(containsString("Physics & astronomy"))))
                }
                capture("wiki-field")
                select("Quantum mechanics")
                scenario.recreate()
                onView(withId(R.id.wiki_apply)).check(matches(withText("Use 2 interests")))
                capture("wiki-selected")
                query("zzzzlinkmenomatchingtopic")
                waitFor { onView(withId(R.id.wiki_empty)).check(matches(isDisplayed())) }
                capture("wiki-empty")
                query("Quantum")
                i.runOnMainSync {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                waitFor { onView(withId(R.id.wiki_apply)).check(matches(isDisplayed())) }
                capture("wiki-dark")
            }
            i.runOnMainSync {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            val row = longTopic()
            launch(listOf("Music"), row[1]).use {
                waitFor {
                    onView(withId(R.id.wiki_results))
                        .perform(
                            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                                hasDescendant(withText(row[1])),
                                longClick(),
                            )
                        )
                }
                capture("wiki-details")
                onView(withText("Close")).perform(click())
                onView(withId(R.id.wiki_apply)).check(matches(withText("Use 1 interest")))
                select(row[1])
                capture("wiki-long-selected")
            }
            launch(listOf("Music"), parent = true).use {
                onView(withId(R.id.wiki_browse)).perform(scrollTo())
                capture("wiki-entry")
            }
        } finally {
            i.runOnMainSync {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
