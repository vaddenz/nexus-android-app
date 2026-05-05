package com.nexus.core.common.log

import android.util.Log

/**
 * Lightweight log facade. Backed by [Log] in production; tests can inject [InMemoryLogger].
 */
interface Logger {
    fun v(tag: String, message: String, throwable: Throwable? = null)
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

class AndroidLogger : Logger {
    override fun v(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.v(tag, message, throwable) else Log.v(tag, message)
    }

    override fun d(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
    }

    override fun i(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}

/** In-memory recording logger for tests. */
class InMemoryLogger : Logger {
    data class Entry(val level: Char, val tag: String, val message: String, val throwable: Throwable?)

    private val _entries = mutableListOf<Entry>()
    val entries: List<Entry> get() = _entries.toList()

    override fun v(tag: String, message: String, throwable: Throwable?) { _entries += Entry('V', tag, message, throwable) }
    override fun d(tag: String, message: String, throwable: Throwable?) { _entries += Entry('D', tag, message, throwable) }
    override fun i(tag: String, message: String, throwable: Throwable?) { _entries += Entry('I', tag, message, throwable) }
    override fun w(tag: String, message: String, throwable: Throwable?) { _entries += Entry('W', tag, message, throwable) }
    override fun e(tag: String, message: String, throwable: Throwable?) { _entries += Entry('E', tag, message, throwable) }
}
