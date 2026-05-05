package com.nexus.android.di

import com.nexus.core.common.coroutines.DefaultDispatcherProvider
import com.nexus.core.common.coroutines.DispatcherProvider
import com.nexus.core.common.time.SystemTimeProvider
import com.nexus.core.common.time.TimeProvider
import com.nexus.core.eventbus.EventBus
import com.nexus.core.eventbus.FlowEventBus
import com.nexus.harness.pipeline.PipelineRegistry
import com.nexus.harness.sensor.SensorRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HarnessModule {

    @Provides @Singleton
    fun provideEventBus(): EventBus = FlowEventBus(extraBufferCapacity = 64)

    @Provides @Singleton
    fun provideDispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider

    @Provides @Singleton
    fun provideSensorRegistry(): SensorRegistry = SensorRegistry()

    @Provides @Singleton
    fun providePipelineRegistry(): PipelineRegistry = PipelineRegistry()
}
