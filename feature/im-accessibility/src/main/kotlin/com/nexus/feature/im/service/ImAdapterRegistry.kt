package com.nexus.feature.im.service

import com.nexus.feature.im.domain.adapter.ImAdapter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds all registered [ImAdapter]s and routes node trees to the correct one
 * based on package name.
 */
@Singleton
class ImAdapterRegistry @Inject constructor(
    adapters: Set<@JvmSuppressWildcards ImAdapter>,
) {

    private val byPackage: Map<String, ImAdapter> = adapters
        .flatMap { adapter -> adapter.supportedPackages.map { it to adapter } }
        .toMap()

    /** Returns the adapter for [packageName], or `null` if none is registered. */
    fun find(packageName: String): ImAdapter? = byPackage[packageName]

    /** All packages currently supported. */
    val supportedPackages: Set<String> get() = byPackage.keys
}
