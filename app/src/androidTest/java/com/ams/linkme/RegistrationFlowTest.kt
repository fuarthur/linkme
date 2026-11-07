package com.ams.linkme

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.ams.linkme.ui.activity.LoginActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Serial emulator-only tests: these briefly replace the demo project's rules. */
@RunWith(AndroidJUnit4::class)
class RegistrationFlowTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val auth
        get() = FirebaseAuth.getInstance()

    private val db
        get() = FirebaseFirestore.getInstance()

    private fun requireLocalEmulators() {
        check(BuildConfig.DEBUG && BuildConfig.USE_EMULATORS) {
            "Registration tests require the debug emulator build; production is forbidden."
        }
        check(FirebaseApp.getInstance().options.projectId == "demo-linkme") {
            "Registration tests may mutate only the demo-linkme project."
        }
    }

    @Test
    fun pendingRegistrationRecreationWaitsForProfileAcknowledgement() {
        withRegistrationRules(denyPrivateProfileCreate = false) {
            ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
                val (uid, email) = createAccountWithPendingProfile()
                scenario.recreate()
                assertStillWaitingOnLogin(scenario)

                Tasks.await(db.enableNetwork(), 10, TimeUnit.SECONDS)
                waitFor {
                    onView(allOf(withId(R.id.nav_profile), isDisplayed()))
                        .check(matches(isDisplayed()))
                    onView(allOf(withId(R.id.profile_name), isDisplayed()))
                        .check(matches(withText("")))
                }
                assertEquals(uid, auth.currentUser?.uid)
                val saved =
                    Tasks.await(
                        db.collection("Users").document(uid).get(Source.SERVER),
                        10,
                        TimeUnit.SECONDS,
                    )
                assertTrue(saved.exists())
                assertFalse(saved.metadata.hasPendingWrites())
                assertEquals(email, saved.getString("email"))
            }
        }
    }

    @Test
    fun rejectedProfileAfterRecreationShowsSetupFailureAndSignsOut() {
        withRegistrationRules(denyPrivateProfileCreate = true) {
            ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
                createAccountWithPendingProfile()
                scenario.recreate()
                assertStillWaitingOnLogin(scenario)

                Tasks.await(db.enableNetwork(), 10, TimeUnit.SECONDS)
                waitFor {
                    assertNull(auth.currentUser)
                    onView(allOf(withText(SETUP_FAILURE), isDisplayed()))
                        .check(matches(isDisplayed()))
                    onView(withId(R.id.auth_submit)).check(matches(isEnabled()))
                }
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
                assertNoMainActivity()
            }
        }
    }

    private fun createAccountWithPendingProfile(): Pair<String, String> {
        val email = "registration-${UUID.randomUUID()}@linkme.example.test"
        val password = "registration-preview-pass"
        onView(withText("New here? Create an account")).perform(scrollTo(), click())
        onView(withId(R.id.email)).perform(scrollTo(), replaceText(email))
        onView(withId(R.id.password)).perform(scrollTo(), replaceText(password))
        onView(withId(R.id.password_confirmation))
            .perform(scrollTo(), replaceText(password), closeSoftKeyboard())
        onView(withId(R.id.auth_submit)).perform(scrollTo(), click())
        waitFor { assertNotNull(auth.currentUser) }
        val uid = auth.currentUser!!.uid
        waitFor {
            val pending =
                Tasks.await(
                    db.collection("Users").document(uid).get(Source.CACHE),
                    3,
                    TimeUnit.SECONDS,
                )
            assertTrue(pending.exists())
            assertTrue(pending.metadata.hasPendingWrites())
        }
        onView(withId(R.id.auth_submit)).check(matches(not(isEnabled())))
        return uid to email
    }

    private fun assertStillWaitingOnLogin(scenario: ActivityScenario<LoginActivity>) {
        instrumentation.waitForIdleSync()
        assertEquals(
            "Pending profile setup must not navigate away",
            Lifecycle.State.RESUMED,
            scenario.state,
        )
        onView(withId(R.id.auth_submit)).perform(scrollTo()).check(matches(not(isEnabled())))
        onView(withId(R.id.auth_submit)).check(matches(withText("Please wait…")))
        assertNoMainActivity()
    }

    private fun assertNoMainActivity() {
        instrumentation.runOnMainSync {
            val mainActivities =
                Stage.values()
                    .filter { it != Stage.DESTROYED }
                    .flatMap {
                        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it)
                    }
                    .filterIsInstance<com.ams.linkme.ui.activity.MainActivity>()
            assertTrue(
                "Registration must not expose MainActivity before setup succeeds",
                mainActivities.isEmpty(),
            )
        }
    }

    private fun withRegistrationRules(denyPrivateProfileCreate: Boolean, body: () -> Unit) {
        requireLocalEmulators()
        val bundledRules =
            instrumentation.context.assets.open("firestore.rules").bufferedReader().use {
                it.readText()
            }
        val ownerWrites = "allow create, update, delete: if owns(uid);"
        check(bundledRules.split(ownerWrites).size == 2) {
            "Expected exactly one private Users owner-write rule in the bundled fixture."
        }
        val rules =
            if (denyPrivateProfileCreate) {
                bundledRules.replace(
                    ownerWrites,
                    "allow create: if false;\n      allow update, delete: if owns(uid);",
                )
            } else bundledRules
        try {
            closeActivities()
            auth.signOut()
            putEmulatorRules(rules)
            Tasks.await(db.disableNetwork(), 10, TimeUnit.SECONDS)
            body()
        } finally {
            try {
                closeActivities()
            } finally {
                try {
                    putEmulatorRules(bundledRules)
                } finally {
                    try {
                        Tasks.await(db.enableNetwork(), 10, TimeUnit.SECONDS)
                        // Drain any queued setup left by an early RED assertion before signing out.
                        runCatching { Tasks.await(db.waitForPendingWrites(), 10, TimeUnit.SECONDS) }
                    } finally {
                        auth.signOut()
                    }
                }
            }
        }
    }

    private fun putEmulatorRules(rules: String) {
        requireLocalEmulators()
        val connection =
            URL("http://10.0.2.2:8080/emulator/v1/projects/demo-linkme:securityRules")
                .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PUT"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val payload =
                JSONObject()
                    .put(
                        "rules",
                        JSONObject()
                            .put("files", JSONArray().put(JSONObject().put("content", rules))),
                    )
                    .toString()
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
            val status = connection.responseCode
            val response =
                (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            check(status in 200..299) { "Demo emulator rejected test rules: $status $response" }
        } finally {
            connection.disconnect()
        }
    }

    private fun closeActivities() {
        instrumentation.runOnMainSync {
            Stage.values()
                .filter { it != Stage.DESTROYED }
                .flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .distinct()
                .forEach(Activity::finishAndRemoveTask)
        }
        instrumentation.waitForIdleSync()
    }

    private fun waitFor(check: () -> Unit) {
        val deadline = System.currentTimeMillis() + 15000
        var failure: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                check()
                return
            } catch (error: Throwable) {
                failure = error
                Thread.sleep(100)
            }
        }
        throw AssertionError("Registration did not reach the expected state", failure)
    }

    companion object {
        private const val SETUP_FAILURE =
            "Your account was created, but setup could not finish. Sign in to finish your profile."
    }
}
