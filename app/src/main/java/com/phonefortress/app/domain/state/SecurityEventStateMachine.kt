package com.phonefortress.app.domain.state

import com.phonefortress.app.domain.model.EventOperation
import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.util.Logger

/** Single source of truth for legal security-event transitions. */
object SecurityEventStateMachine {
    private val allowedTransitions = mapOf(
        SecurityEventStatus.PENDING to setOf(SecurityEventStatus.IN_PROGRESS, SecurityEventStatus.DEFERRED, SecurityEventStatus.CANCELLED),
        SecurityEventStatus.DEFERRED to setOf(SecurityEventStatus.IN_PROGRESS, SecurityEventStatus.CANCELLED),
        SecurityEventStatus.IN_PROGRESS to setOf(SecurityEventStatus.CAPTURED, SecurityEventStatus.FAILED_RETRYABLE, SecurityEventStatus.CANCELLED),
        SecurityEventStatus.CAPTURED to setOf(SecurityEventStatus.SEND_PENDING, SecurityEventStatus.FAILED_RETRYABLE, SecurityEventStatus.CANCELLED),
        SecurityEventStatus.SEND_PENDING to setOf(SecurityEventStatus.SENT, SecurityEventStatus.FAILED_RETRYABLE, SecurityEventStatus.FAILED_FINAL),
        SecurityEventStatus.FAILED_RETRYABLE to setOf(SecurityEventStatus.IN_PROGRESS, SecurityEventStatus.SEND_PENDING, SecurityEventStatus.FAILED_FINAL, SecurityEventStatus.CANCELLED),
        SecurityEventStatus.SENT to emptySet(),
        SecurityEventStatus.FAILED_FINAL to emptySet(),
        SecurityEventStatus.CANCELLED to emptySet()
    )

    fun canTransition(from: SecurityEventStatus, to: SecurityEventStatus): Boolean =
        allowedTransitions[from]?.contains(to) == true

    fun transition(
        event: SecurityEvent,
        target: SecurityEventStatus,
        reason: String = "unspecified",
        operation: EventOperation = event.operation
    ): SecurityEvent {
        if (!canTransition(event.status, target) || !operationMatches(event.status, target, operation)) {
            Logger.w("Illegal security-event transition")
            return event
        }
        Logger.d("Security-event transition accepted")
        return event.copy(status = target, operation = operation, lastTransitionReason = reason)
    }

    fun transitionRequired(
        event: SecurityEvent,
        target: SecurityEventStatus,
        reason: String,
        operation: EventOperation = event.operation
    ): SecurityEvent {
        check(canTransition(event.status, target)) {
            "Illegal transition ${event.status} -> $target for ${event.id} ($reason)"
        }
        check(operationMatches(event.status, target, operation)) {
            "Operation $operation cannot transition ${event.status} -> $target for ${event.id}"
        }
        return transition(event, target, reason, operation)
    }

    private fun operationMatches(
        from: SecurityEventStatus,
        to: SecurityEventStatus,
        operation: EventOperation
    ): Boolean = when {
        to == SecurityEventStatus.IN_PROGRESS -> operation == EventOperation.CAPTURE
        from == SecurityEventStatus.IN_PROGRESS && to == SecurityEventStatus.CAPTURED -> operation == EventOperation.CAPTURE
        from == SecurityEventStatus.CAPTURED && to == SecurityEventStatus.SEND_PENDING -> operation == EventOperation.SEND
        from == SecurityEventStatus.SEND_PENDING && to == SecurityEventStatus.SENT -> operation == EventOperation.SEND
        from == SecurityEventStatus.FAILED_RETRYABLE && to == SecurityEventStatus.SEND_PENDING -> operation == EventOperation.SEND
        from == SecurityEventStatus.FAILED_RETRYABLE && to == SecurityEventStatus.FAILED_FINAL -> true
        else -> true
    }

    fun isTerminal(status: SecurityEventStatus): Boolean = status in setOf(
        SecurityEventStatus.SENT, SecurityEventStatus.FAILED_FINAL, SecurityEventStatus.CANCELLED
    )

    fun activeStates(): Set<SecurityEventStatus> = SecurityEventStatus.entries.filterNot(::isTerminal).toSet()
}
