package com.nexus.feature.im.di

import com.nexus.feature.im.data.adapter.DingtalkAdapter
import com.nexus.feature.im.data.adapter.SlackAdapter
import com.nexus.feature.im.data.adapter.WechatAdapter
import com.nexus.feature.im.data.adapter.WeworkAdapter
import com.nexus.feature.im.data.desensitizer.DefaultDesensitizationRules
import com.nexus.feature.im.data.desensitizer.RegexDesensitizer
import com.nexus.feature.im.domain.adapter.ImAdapter
import com.nexus.feature.im.domain.desensitizer.Desensitizer
import com.nexus.feature.im.domain.desensitizer.DesensitizationRule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImAccessibilityModule {

    @Binds
    @IntoSet
    abstract fun bindWechatAdapter(adapter: WechatAdapter): ImAdapter

    @Binds
    @IntoSet
    abstract fun bindWeworkAdapter(adapter: WeworkAdapter): ImAdapter

    @Binds
    @IntoSet
    abstract fun bindDingtalkAdapter(adapter: DingtalkAdapter): ImAdapter

    @Binds
    @IntoSet
    abstract fun bindSlackAdapter(adapter: SlackAdapter): ImAdapter

    @Binds
    abstract fun bindDesensitizer(impl: RegexDesensitizer): Desensitizer

    companion object {
        @Provides
        @Singleton
        fun provideDesensitizationRules(): List<DesensitizationRule> =
            DefaultDesensitizationRules.ALL
    }
}
