package com.phonefortress.app.domain.state

import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import org.junit.jupiter.api.Test

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
}
