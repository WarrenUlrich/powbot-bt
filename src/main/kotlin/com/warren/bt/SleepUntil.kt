package com.warren.bt

class SleepUntil(
    private val predicate: () -> Boolean,
    private val timeoutMillis: Long
) : Node("SleepUntil(${timeoutMillis}ms)") {

    private var startNanos: Long? = null
    private val timeoutNanos = timeoutMillis * 1_000_000

    override fun tick(): Status {
        // If condition is already satisfied, we’re done.
        if (predicate()) return Status.SUCCESS

        val now = System.nanoTime()
        val startedAt = startNanos ?: now.also { startNanos = it }
        val elapsed = now - startedAt

        // Keep running until either predicate() succeeds or we hit the timeout.
        return if (elapsed < timeoutNanos) Status.RUNNING else Status.FAILURE
    }

    override fun reset() {
        startNanos = null
    }
}
