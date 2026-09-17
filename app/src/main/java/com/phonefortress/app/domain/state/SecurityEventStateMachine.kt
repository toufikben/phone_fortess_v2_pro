package com.phonefortress.app.domain.state

import com.phonefortress.app.domain.model.SecurityEvent
import com.phonefortress.app.domain.model.SecurityEventStatus
import com.phonefortress.app.util.Logger

/**
 * آلة حالة الحدث الأمني — تتحكم في كل الانتقالات المسموح بها.
 * تمنع الانتقالات غير الشرعية وتُسجّل كل تغيير.
 */
object SecurityEventStateMachine {

    /**
     * الانتقالات المسموح بها من كل حالة.
     */
    private val allowedTransitions: Map<SecurityEventStatus, Set<SecurityEventStatus>> = mapOf(
        SecurityEventStatus.PENDING to setOf(
            SecurityEventStatus.IN_PROGRESS,
            SecurityEventStatus.DEFERRED,
            SecurityEventStatus.CANCELLED
        ),
        SecurityEventStatus.DEFERRED to setOf(
            SecurityEventStatus.IN_PROGRESS,
            SecurityEventStatus.CANCELLED
        ),
        SecurityEventStatus.IN_PROGRESS to setOf(
            SecurityEventStatus.CAPTURED,
            SecurityEventStatus.FAILED_RETRYABLE,
            SecurityEventStatus.CANCELLED
        ),
        SecurityEventStatus.CAPTURED to setOf(
            SecurityEventStatus.SEND_PENDING,
            SecurityEventStatus.CANCELLED
        ),
        SecurityEventStatus.SEND_PENDING to setOf(
            SecurityEventStatus.SENT,
            SecurityEventStatus.FAILED_RETRYABLE,
            SecurityEventStatus.FAILED_FINAL
        ),
        SecurityEventStatus.FAILED_RETRYABLE to setOf(
            SecurityEventStatus.SEND_PENDING,
            SecurityEventStatus.FAILED_FINAL,
            SecurityEventStatus.CANCELLED
        ),
        SecurityEventStatus.SENT to emptySet(),
        SecurityEventStatus.FAILED_FINAL to emptySet(),
        SecurityEventStatus.CANCELLED to emptySet()
    )

    /**
     * هل الانتقال مسموح؟
     */
    fun canTransition(from: SecurityEventStatus, to: SecurityEventStatus): Boolean {
        return allowedTransitions[from]?.contains(to) == true
    }

    /**
     * يُجري الانتقال إذا كان مسموحاً، وإلا يعيد الحدث بدون تغيير.
     */
    fun transition(event: SecurityEvent, target: SecurityEventStatus): SecurityEvent {
        if (!canTransition(event.status, target)) {
            Logger.w("Illegal transition: ${event.status} → $target (event ${event.id})")
            return event
        }
        Logger.d("Event ${event.id}: ${event.status} → $target")
        return event.copy(status = target)
    }

    /**
     * هل الحالة نهائية؟
     */
    fun isTerminal(status: SecurityEventStatus): Boolean = when (status) {
        SecurityEventStatus.SENT,
        SecurityEventStatus.FAILED_FINAL,
        SecurityEventStatus.CANCELLED -> true
        else -> false
    }

    /**
     * قائمة كل الحالات النشطة (غير نهائية).
     */
    fun activeStates(): Set<SecurityEventStatus> = SecurityEventStatus.entries
        .filterNot { isTerminal(it) }
        .toSet()
}
