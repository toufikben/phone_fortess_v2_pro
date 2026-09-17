package com.phonefortress.app.platform.worker

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * HiltWorkerFactory تُوفَّر تلقائياً عند استخدام @HiltWorker.
 * لا توجد حاجة إلى @Provides إضافية للعمال الحاليين.
 */
@Module
@InstallIn(SingletonComponent::class)
object HiltWorkerModule
