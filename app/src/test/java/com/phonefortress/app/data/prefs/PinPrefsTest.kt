package com.phonefortress.app.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.data.crypto.PinHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PinPrefsTest {
    private lateinit var prefs: PinPrefs
    @BeforeEach fun setup() { prefs = PinPrefs(ApplicationProvider.getApplicationContext<Context>(), PinHasher()) }
    @Test fun `initial state is disabled`() = runTest { assertThat(prefs.isPinEnabled.first()).isFalse() }
    @Test fun `set pin then verify succeeds`() = runTest { prefs.setPin("123456"); assertThat(prefs.isPinEnabled.first()).isTrue(); assertThat(prefs.verifyPin("123456")).isInstanceOf(PinPrefs.VerifyResult.Success::class.java) }
    @Test fun `wrong pin increments attempts`() = runTest { prefs.setPin("123456"); val r = prefs.verifyPin("000000"); assertThat(r).isInstanceOf(PinPrefs.VerifyResult.WrongPin::class.java); assertThat((r as PinPrefs.VerifyResult.WrongPin).remainingAttempts).isEqualTo(4) }
    @Test fun `lockout after five failures`() = runTest { prefs.setPin("123456"); repeat(5) { prefs.verifyPin("000000") }; assertThat(prefs.verifyPin("123456")).isInstanceOf(PinPrefs.VerifyResult.LockedOut::class.java) }
    @Test fun `disable pin clears state`() = runTest { prefs.setPin("123456"); prefs.disablePin(); assertThat(prefs.isPinEnabled.first()).isFalse() }
}
