package com.phonefortress.app

import com.phonefortress.app.data.crypto.PinGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinGateTest {
    @Test fun `valid pin verifies and wrong pin fails`() {
        val gate = PinGate()
        val stored = gate.hash("123456")
        assertTrue(gate.verify("123456", stored))
        assertFalse(gate.verify("123457", stored))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `short pin is rejected`() { PinGate().hash("12345") }
}
