package com.phonefortress.app.data.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PinHasherTest {
    private lateinit var hasher: PinHasher
    @BeforeEach fun setup() { hasher = PinHasher() }

    @Nested inner class Hashing {
        @Test fun `hash produces different salt each time`() {
            val h1 = hasher.hash("123456"); val h2 = hasher.hash("123456")
            assertThat(h1.salt).isNotEqualTo(h2.salt); assertThat(h1.hash).isNotEqualTo(h2.hash)
        }
        @Test fun `hash produces Base64 strings`() {
            val h = hasher.hash("123456")
            assertThat(h.hash).isNotEmpty(); assertThat(h.salt).isNotEmpty()
            assertThat(h.hash).doesNotContain(" "); assertThat(h.salt).doesNotContain(" ")
        }
    }
    @Nested inner class Verification {
        @Test fun `verify returns true for correct pin`() = assertThat(hasher.verify("654321", hasher.hash("654321"))).isTrue()
        @Test fun `verify returns false for wrong pin`() {
            val h = hasher.hash("654321")
            assertThat(hasher.verify("111111", h)).isFalse(); assertThat(hasher.verify("654320", h)).isFalse(); assertThat(hasher.verify("", h)).isFalse()
        }
        @Test fun `verify handles invalid base64 gracefully`() = assertThat(hasher.verify("123456", PinHasher.HashedPin("!@#", "!@#"))).isFalse()
    }
    @Nested inner class EdgeCases {
        @Test fun `hash supports 6 digit pin`() { val h = hasher.hash("123456"); assertThat(hasher.verify("123456", h)).isTrue() }
        @Test fun `hash supports 8 digit pin`() { val h = hasher.hash("12345678"); assertThat(hasher.verify("12345678", h)).isTrue() }
        @Test fun `hash supports unicode pin`() { val h = hasher.hash("مرحبا1"); assertThat(hasher.verify("مرحبا1", h)).isTrue() }
    }
}
