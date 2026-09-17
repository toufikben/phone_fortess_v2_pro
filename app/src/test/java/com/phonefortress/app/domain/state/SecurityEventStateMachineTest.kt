package com.phonefortress.app.domain.state

import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SecurityEventStateMachineTest {
    private fun event(status: SecurityEventStatus) = SecurityEvent("test-id", 1L, 3, 3, status = status)
    @Test fun `allowed transitions`() {
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.PENDING, SecurityEventStatus.IN_PROGRESS)).isTrue()
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.IN_PROGRESS, SecurityEventStatus.CAPTURED)).isTrue()
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.CAPTURED, SecurityEventStatus.SEND_PENDING)).isTrue()
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.SEND_PENDING, SecurityEventStatus.SENT)).isTrue()
    }
    @Test fun `terminal transitions are rejected`() {
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.SENT, SecurityEventStatus.IN_PROGRESS)).isFalse()
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.CANCELLED, SecurityEventStatus.PENDING)).isFalse()
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.PENDING, SecurityEventStatus.SENT)).isFalse()
    }
    @Test fun `transition applies only when legal`() {
        assertThat(SecurityEventStateMachine.transition(event(SecurityEventStatus.PENDING), SecurityEventStatus.IN_PROGRESS).status).isEqualTo(SecurityEventStatus.IN_PROGRESS)
        assertThat(SecurityEventStateMachine.transition(event(SecurityEventStatus.SENT), SecurityEventStatus.IN_PROGRESS).status).isEqualTo(SecurityEventStatus.SENT)
    }
    @Test fun `terminal status detection`() {
        assertThat(SecurityEventStateMachine.isTerminal(SecurityEventStatus.SENT)).isTrue()
        assertThat(SecurityEventStateMachine.isTerminal(SecurityEventStatus.FAILED_FINAL)).isTrue()
        assertThat(SecurityEventStateMachine.isTerminal(SecurityEventStatus.CANCELLED)).isTrue()
        assertThat(SecurityEventStateMachine.isTerminal(SecurityEventStatus.PENDING)).isFalse()
    }

    @Test fun `retry transitions preserve operation type`() {
        val captureFailure = SecurityEventStateMachine.transitionRequired(
            event(SecurityEventStatus.IN_PROGRESS), SecurityEventStatus.FAILED_RETRYABLE,
            "camera-timeout", EventOperation.CAPTURE
        )
        val captureRetry = SecurityEventStateMachine.transitionRequired(
            captureFailure, SecurityEventStatus.IN_PROGRESS, "capture-retry", EventOperation.CAPTURE
        )
        assertThat(captureRetry.operation).isEqualTo(EventOperation.CAPTURE)

        val sendFailure = SecurityEventStateMachine.transitionRequired(
            event(SecurityEventStatus.SEND_PENDING), SecurityEventStatus.FAILED_RETRYABLE,
            "network", EventOperation.SEND
        )
        val sendRetry = SecurityEventStateMachine.transitionRequired(
            sendFailure, SecurityEventStatus.SEND_PENDING, "send-retry", EventOperation.SEND
        )
        assertThat(sendRetry.operation).isEqualTo(EventOperation.SEND)
    }

    @Test fun `illegal transition leaves event unchanged`() {
        val terminal = event(SecurityEventStatus.SENT)
        assertThat(SecurityEventStateMachine.transition(terminal, SecurityEventStatus.PENDING, "invalid"))
            .isEqualTo(terminal)
    }

    @Test fun `in progress cannot transition to itself`() {
        assertThat(SecurityEventStateMachine.canTransition(SecurityEventStatus.IN_PROGRESS, SecurityEventStatus.IN_PROGRESS)).isFalse()
        assertThrows<IllegalStateException> {
            SecurityEventStateMachine.transitionRequired(
                event(SecurityEventStatus.IN_PROGRESS), SecurityEventStatus.IN_PROGRESS, "duplicate-claim"
            )
        }
    }

    @Test fun `retry operation selects only its own path`() {
        val captureFailure = SecurityEventStateMachine.transitionRequired(
            event(SecurityEventStatus.IN_PROGRESS), SecurityEventStatus.FAILED_RETRYABLE,
            "capture-failure", EventOperation.CAPTURE
        )
        assertThrows<IllegalStateException> {
            SecurityEventStateMachine.transitionRequired(
                captureFailure, SecurityEventStatus.SEND_PENDING, "wrong-path", EventOperation.CAPTURE
            )
        }

        val sendFailure = SecurityEventStateMachine.transitionRequired(
            event(SecurityEventStatus.SEND_PENDING), SecurityEventStatus.FAILED_RETRYABLE,
            "send-failure", EventOperation.SEND
        )
        assertThrows<IllegalStateException> {
            SecurityEventStateMachine.transitionRequired(
                sendFailure, SecurityEventStatus.IN_PROGRESS, "wrong-path", EventOperation.SEND
            )
        }
    }
}
