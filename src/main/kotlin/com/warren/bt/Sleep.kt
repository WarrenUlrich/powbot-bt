package com.warren.bt

class Sleep(private val durationMillis: Long) : Node("Sleep(${durationMillis}ms)") {
    private var startNanos: Long? = null
    private val durationNanos = durationMillis * 1_000_000

    override fun tick(): Status {
        val now = System.nanoTime()
        val startedAt = startNanos ?: now.also { startNanos = it }
        val elapsed = now - startedAt
        return if (elapsed < durationNanos) Status.RUNNING else Status.SUCCESS
    }

    override fun reset() {
        startNanos = null
    }
}
