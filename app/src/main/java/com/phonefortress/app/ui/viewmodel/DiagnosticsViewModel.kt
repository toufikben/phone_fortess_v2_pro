package com.phonefortress.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WorkerInfo(
    val id: String,
    val tag: String,
    val state: String,
    val runAttemptCount: Int
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _workers = MutableStateFlow<List<WorkerInfo>>(emptyList())
    val workers: StateFlow<List<WorkerInfo>> = _workers.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _workers.value = withContext(Dispatchers.IO) {
                runCatching {
                    val manager = WorkManager.getInstance(context)
                    listOf("capture_retry", "event_dispatch", "photo_cleanup")
                        .flatMap { tag ->
                            manager.getWorkInfosByTag(tag).get().map { info ->
                                WorkerInfo(info.id.toString().take(8), tag, info.state.name, info.runAttemptCount)
                            }
                        }
                }.getOrDefault(emptyList())
            }
        }
    }
}
