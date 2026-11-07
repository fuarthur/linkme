package com.ams.linkme

import android.app.Activity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.ams.linkme.ui.activity.MainActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.allOf
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** A pre-v2 conversation must remain reachable even after another chat has 100+ messages. */
@RunWith(AndroidJUnit4::class)
class LegacyInboxTest {
    private val auth
        get() = FirebaseAuth.getInstance()

    private val db
        get() = FirebaseFirestore.getInstance()

    @Before
    fun localOnly() {
        Assume.assumeTrue("Never seed production", BuildConfig.DEBUG && BuildConfig.USE_EMULATORS)
        assertEquals("demo-linkme", FirebaseApp.getInstance().options.projectId)
        auth.signOut()
    }

    @After
    fun close() {
        if (!BuildConfig.USE_EMULATORS) return
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            Stage.values()
                .filter { it != Stage.DESTROYED }
                .flatMap { monitor.getActivitiesInStage(it) }
                .distinct()
                .forEach(Activity::finishAndRemoveTask)
        }
        auth.signOut()
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
        throw AssertionError("Expected legacy chat not visible", failure)
    }

    @Test
    fun olderLegacyConversationCanBeRecoveredAndOpened() {
        val run = System.currentTimeMillis().toString()
        Tasks.await(
            auth.createUserWithEmailAndPassword("legacy-$run@linkme.example.test", "preview-pass"),
            15,
            TimeUnit.SECONDS,
        )
        val uid = auth.currentUser!!.uid
        Tasks.await(
            db.collection("Users")
                .document(uid)
                .set(mapOf("username" to "Legacy Preview", "interests" to listOf("Music"))),
            10,
            TimeUnit.SECONDS,
        )
        val bobContent = "An older connection with Bob $run"
        val carolContent = "A newer connection with Carol $run"
        val writes = JSONArray()
        for (number in 0..150) {
            val fields = JSONObject()
            fields.put("senderId", JSONObject().put("stringValue", uid))
            fields.put(
                "receiveId",
                JSONObject()
                    .put(
                        "stringValue",
                        if (number == 0) "legacy-bob-preview" else "legacy-carol-preview",
                    ),
            )
            fields.put(
                "content",
                JSONObject().put("stringValue", if (number == 0) bobContent else carolContent),
            )
            fields.put(
                "timestamp",
                JSONObject()
                    .put(
                        "timestampValue",
                        String.format(
                            Locale.US,
                            "2020-01-01T00:%02d:%02dZ",
                            number / 60,
                            number % 60,
                        ),
                    ),
            )
            val document =
                JSONObject()
                    .put(
                        "name",
                        "projects/demo-linkme/databases/(default)/documents/Messages/legacy-$run-$number",
                    )
                    .put("fields", fields)
            writes.put(JSONObject().put("update", document))
        }
        val connection =
            URL("http://10.0.2.2:8080/v1/projects/demo-linkme/databases/(default)/documents:commit")
                .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.outputStream.use {
                it.write(JSONObject().put("writes", writes).toString().toByteArray())
            }
            assertEquals(200, connection.responseCode)
        } finally {
            connection.disconnect()
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor { onView(withId(R.id.nav_chats)).check(matches(isDisplayed())) }
            onView(withId(R.id.nav_chats)).perform(click())
            waitFor {
                onView(allOf(withText(carolContent), isDisplayed())).check(matches(isDisplayed()))
            }
            onView(withId(R.id.older_chats)).check(matches(isDisplayed())).perform(click())
            waitFor {
                onView(withId(R.id.inbox_list))
                    .perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText(bobContent))
                        )
                    )
            }
            onView(allOf(withText(bobContent), isDisplayed())).perform(click())
            waitFor {
                onView(allOf(withText(bobContent), isDisplayed())).check(matches(isDisplayed()))
            }
            onView(withId(R.id.message_draft)).check(matches(isDisplayed()))
        }
    }
}
