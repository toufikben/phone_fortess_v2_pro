package com.phonefortress.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.phonefortress.app.data.prefs.SecurityPrefs
import com.phonefortress.app.domain.model.ThreatLevel
import com.phonefortress.app.platform.ai.FaceDetector
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EvaluateThreatUseCaseTest {
    private lateinit var detector: FaceDetector
    private lateinit var prefs: SecurityPrefs
    private lateinit var useCase: EvaluateThreatUseCase
    @BeforeEach fun setup() { detector = mockk(relaxed = true); prefs = mockk(relaxed = true); useCase = EvaluateThreatUseCase(detector, prefs); every { prefs.threshold } returns flowOf(3) }
    @Test fun `no photo produces no face factor`() = runTest { val r = useCase(null, 3); assertThat(r.reasons.any { it.contains("كاميرا محجوبة") }).isTrue(); assertThat(r.score).isAtLeast(20) }
    @Test fun `multiple faces produces high threat`() = runTest { coEvery { detector.detect(any()) } returns FaceDetector.FaceResult(3, .9f, emptyList()); coEvery { detector.averageBrightness(any()) } returns .5f; val r = useCase("/fake.jpg", 3); assertThat(r.facesDetected).isEqualTo(3); assertThat(r.reasons.any { it.contains("أكثر من شخص") }).isTrue() }
    @Test fun `outside safe zone adds factor`() = runTest { coEvery { detector.detect(any()) } returns FaceDetector.FaceResult(1, .9f, emptyList()); coEvery { detector.averageBrightness(any()) } returns .5f; assertThat(useCase("/fake.jpg", 3, false).reasons.any { it.contains("خارج المنطقة") }).isTrue() }
    @Test fun `high attempts adds factor`() = runTest { coEvery { detector.detect(any()) } returns null; coEvery { detector.averageBrightness(any()) } returns -1f; assertThat(useCase(null, 10, true).reasons.any { it.contains("مرتفع") }).isTrue() }
    @Test fun `low light adds factor`() = runTest { coEvery { detector.detect(any()) } returns FaceDetector.FaceResult(1, .9f, emptyList()); coEvery { detector.averageBrightness(any()) } returns .1f; assertThat(useCase("/fake.jpg", 3).reasons.any { it.contains("إضاءة منخفضة") }).isTrue() }
    @Test fun `score maps to high or critical`() = runTest { coEvery { detector.detect(any()) } returns FaceDetector.FaceResult(3, .9f, emptyList()); coEvery { detector.averageBrightness(any()) } returns .1f; assertThat(useCase("/fake.jpg", 10, false).level).isAnyOf(ThreatLevel.HIGH, ThreatLevel.CRITICAL) }
}
