package com.nexus.android

import android.app.Application
import com.nexus.android.harness.NexusHarness
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class NexusApp : Application() {

    @Inject lateinit var harness: NexusHarness

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        harness.bootstrap(appScope)
    }

    override fun onTerminate() {
        super.onTerminate()
        harness.shutdown()
    }
}
