package com.phonefortress.app.platform.worker

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** مراقب العمال لتسجيل الحالات والأخطاء لأغراض التشخيص. */
@Singleton
class WorkerObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _states = MutableStateFlow<Map<String, List<WorkInfo>>>(emptyMap())
    val states: StateFlow<Map<String, List<WorkInfo>>> = _states

    fun observeAll() {
        val manager = WorkManager.getInstance(context)
        listOf("capture_retry", "event_dispatch", "photo_cleanup").forEach { tag ->
            manager.getWorkInfosByTagLiveData(tag).observeForever { infos ->
                _states.value = _states.value + (tag to infos)
                infos.filter { it.state == WorkInfo.State.FAILED }.forEach {
                    Logger.w("Worker failed: $tag/${it.id}")
                }
            }
        }
        scope.launch { Logger.i("WorkerObserver started") }
    }
}
