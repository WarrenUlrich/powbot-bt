package com.warren.bt

import org.powbot.api.rt4.Players

class SleepUntilIdle(
    private val idleDurationMillis: Long,
    private val timeoutMillis: Long,
    private val bypassFunc: (() -> Boolean)? = null
) : Node("SleepUntilIdle(${idleDurationMillis}ms, timeout=${timeoutMillis}ms)") {

    private var idleStartNanos: Long? = null
    private var totalStartNanos: Long? = null

    private val idleDurationNanos = idleDurationMillis * 1_000_000
    private val timeoutNanos = timeoutMillis * 1_000_000

    override fun tick(): Status {
        // Allow bypassing the idle wait condition entirely.
        if (bypassFunc?.invoke() == true) return Status.SUCCESS

        val player = Players.local()
        val now = System.nanoTime()

        val totalStart = totalStartNanos ?: now.also { totalStartNanos = it }
        val totalElapsed = now - totalStart

        // Timeout check
        if (totalElapsed > timeoutNanos) return Status.FAILURE

        val isIdle = player.animation() == -1 && !player.inMotion()

        if (isIdle) {
            // Start or continue counting idle duration
            if (idleStartNanos == null) idleStartNanos = now
            val idleElapsed = now - idleStartNanos!!
            if (idleElapsed >= idleDurationNanos) {
                return Status.SUCCESS
            }
        } else {
            // Reset idle timer if player moved / started animating again
            idleStartNanos = null
        }

        return Status.RUNNING
    }

    override fun reset() {
        idleStartNanos = null
        totalStartNanos = null
    }
}
