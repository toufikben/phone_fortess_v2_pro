package com.phonefortress.app.di

import android.content.Context
import androidx.room.Room
import com.phonefortress.app.data.local.AppDatabase
import com.phonefortress.app.data.local.RoomMigrations
import com.phonefortress.app.data.local.dao.AlertLogDao
import com.phonefortress.app.data.local.dao.EventDao
import com.phonefortress.app.data.local.dao.SafeZoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "phone_fortress.db")
            .addMigrations(RoomMigrations.MIGRATION_4_5, RoomMigrations.MIGRATION_5_6, RoomMigrations.MIGRATION_6_7)
            .build()

    @Provides
    @Singleton
    fun provideAlertLogDao(db: AppDatabase): AlertLogDao = db.alertLogDao()

    @Provides
    @Singleton
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    @Singleton
    fun provideSafeZoneDao(db: AppDatabase): SafeZoneDao = db.safeZoneDao()
}
