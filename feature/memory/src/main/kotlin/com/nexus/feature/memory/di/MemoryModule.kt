package com.nexus.feature.memory.di

import android.content.Context
import androidx.room.Room
import com.nexus.feature.memory.data.local.EpisodicMemoryDatabase
import com.nexus.feature.memory.data.repository.EpisodicMemoryRepositoryImpl
import com.nexus.feature.memory.domain.repository.EpisodicMemoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MemoryModule {

    @Binds
    abstract fun bindRepository(impl: EpisodicMemoryRepositoryImpl): EpisodicMemoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): EpisodicMemoryDatabase = Room.databaseBuilder(
            context,
            EpisodicMemoryDatabase::class.java,
            "episodic_memory.db",
        ).build()

        @Provides
        @Singleton
        fun provideDao(db: EpisodicMemoryDatabase) = db.eventDao()
    }
}
