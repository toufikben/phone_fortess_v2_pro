package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SecurityPrefsTest {
    private lateinit var prefs: SecurityPrefs

    @Before
    fun setup() {
        prefs = SecurityPrefs(ApplicationProvider.getApplicationContext<Context>())
        runBlocking { prefs.resetAttempts() }
    }

    @Test
    fun `sequential threshold consumes window and returns official trigger count`() = runBlocking {
        assertThat(prefs.incrementAttemptsAndCheckThreshold(3)).isEqualTo(SecurityPrefs.AttemptEvaluation(1, false))
        assertThat(prefs.incrementAttemptsAndCheckThreshold(3)).isEqualTo(SecurityPrefs.AttemptEvaluation(2, false))
        assertThat(prefs.incrementAttemptsAndCheckThreshold(3)).isEqualTo(SecurityPrefs.AttemptEvaluation(3, true))
        assertThat(prefs.consecutiveAttempts.firstValue()).isEqualTo(0)
        assertThat(prefs.incrementAttemptsAndCheckThreshold(3)).isEqualTo(SecurityPrefs.AttemptEvaluation(1, false))
    }

    @Test
    fun `concurrent increments are serialized by DataStore`() = runBlocking {
        val results = coroutineScope {
            (1..20).map { async { prefs.incrementAttemptsAndCheckThreshold(100).newAttemptCount } }.awaitAll()
        }
        assertThat(results.toSet()).containsExactlyElementsIn(1..20)
        assertThat(prefs.consecutiveAttempts.firstValue()).isEqualTo(20)
    }

    @Test
    fun `newer attempt survives a prior threshold trigger`() = runBlocking {
        assertThat(prefs.incrementAttemptsAndCheckThreshold(2)).isEqualTo(SecurityPrefs.AttemptEvaluation(1, false))
        assertThat(prefs.incrementAttemptsAndCheckThreshold(2)).isEqualTo(SecurityPrefs.AttemptEvaluation(2, true))
        assertThat(prefs.incrementAttemptsAndCheckThreshold(2)).isEqualTo(SecurityPrefs.AttemptEvaluation(1, false))
        assertThat(prefs.consecutiveAttempts.firstValue()).isEqualTo(1)
    }

    @Test
    fun `process recreation reads the consumed counter state`() = runBlocking {
        prefs.incrementAttemptsAndCheckThreshold(2)
        prefs.incrementAttemptsAndCheckThreshold(2)
        val recreated = SecurityPrefs(ApplicationProvider.getApplicationContext())
        assertThat(recreated.consecutiveAttempts.firstValue()).isEqualTo(0)
    }

    private suspend fun kotlinx.coroutines.flow.Flow<Int>.firstValue(): Int = first()
}
