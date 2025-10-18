package com.warren.bt

class Repeater(private val maxRepeats: Int = -1) : Decorator("Repeater") {
    private var count = 0

    override fun tick(): Status {
        val c = child ?: return Status.FAILURE
        val s = c.tick()
        return when (s) {
            Status.RUNNING -> Status.RUNNING
            Status.SUCCESS, Status.FAILURE -> {
                count++
                c.reset()
                if (maxRepeats >= 0 && count >= maxRepeats) Status.SUCCESS else Status.RUNNING
            }
        }
    }

    override fun onReset() {
        count = 0
    }
}
