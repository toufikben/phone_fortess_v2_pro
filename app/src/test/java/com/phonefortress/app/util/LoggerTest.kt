package com.phonefortress.app.util

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class LoggerTest {
    private lateinit var tree: CapturingTree

    @Before
    fun setUp() {
        Timber.uprootAll()
        tree = CapturingTree()
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun formattedSensitiveArgumentsAreRedacted() {
        Logger.i("request metadata: %s", "token: super-secret-token")

        assertTrue(tree.message.contains("[REDACTED]"))
        assertFalse(tree.message.contains("super-secret-token"))
    }

    private class CapturingTree : Timber.Tree() {
        var message: String = ""

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            this.message = message
        }
    }
}
