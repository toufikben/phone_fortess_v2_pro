package com.phonefortress.app.di

import com.phonefortress.app.alerts.AlertDispatcher
import com.phonefortress.app.alerts.channels.GenericSmtpChannel
import com.phonefortress.app.alerts.channels.LocalNotificationChannel
import com.phonefortress.app.alerts.channels.NtfyChannel
import com.phonefortress.app.alerts.channels.SmsChannel
import com.phonefortress.app.alerts.channels.TelegramChannel
import com.phonefortress.app.alerts.channels.WebhookChannel
import com.phonefortress.app.domain.model.AlertChannel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlertsModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideTelegramChannel(impl: TelegramChannel): AlertChannel = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideNtfyChannel(impl: NtfyChannel): AlertChannel = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideSmtpChannel(impl: GenericSmtpChannel): AlertChannel = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideWebhookChannel(impl: WebhookChannel): AlertChannel = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideSmsChannel(impl: SmsChannel): AlertChannel = impl

    @Provides
    @IntoSet
    @Singleton
    fun provideLocalChannel(impl: LocalNotificationChannel): AlertChannel = impl
}
