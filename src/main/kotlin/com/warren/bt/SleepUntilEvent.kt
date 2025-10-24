package com.warren.bt

import com.google.common.eventbus.Subscribe
import org.powbot.api.Events

class SleepUntilEvent<T : Any>(
    private val eventClass: Class<T>,
    private val timeoutMillis: Long,
    private val predicate: (T) -> Boolean = { true }
) : Node("SleepUntilEvent(${eventClass.simpleName}, timeout=${timeoutMillis}ms)") {

    private var startNanos: Long? = null
    private val timeoutNanos = timeoutMillis * 1_000_000
    private var registered = false

    @Volatile private var matched = false

    override fun tick(): Status {
        val now = System.nanoTime()
        val startedAt = startNanos ?: now.also { startNanos = it }

        if (!registered) {
            Events.register(this)
            registered = true
        }

        if (matched) {
            cleanup()
            return Status.SUCCESS
        }

        if (now - startedAt >= timeoutNanos) {
            cleanup()
            return Status.FAILURE
        }

        return Status.RUNNING
    }

    override fun reset() {
        matched = false
        startNanos = null
        cleanup()
    }

    private fun cleanup() {
        if (registered) {
            try { Events.unregister(this) } finally { registered = false }
        }
    }

    @Subscribe
    fun onEvent(ev: Any) {
        if (matched) return
        if (!eventClass.isInstance(ev)) return

        @Suppress("UNCHECKED_CAST")
        val typed = eventClass.cast(ev) as T
        if (predicate(typed)) matched = true
    }
}
