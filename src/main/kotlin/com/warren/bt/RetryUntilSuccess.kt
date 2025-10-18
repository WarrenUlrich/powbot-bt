package com.warren.bt

class RetryUntilSuccess(private val maxAttempts: Int = -1) : Decorator("RetryUntilSuccess") {
    private var attempts = 0

    override fun tick(): Status {
        val c = child ?: return Status.FAILURE
        val s = c.tick()
        return when (s) {
            Status.SUCCESS -> Status.SUCCESS
            Status.RUNNING -> Status.RUNNING
            Status.FAILURE -> {
                attempts++
                if (maxAttempts >= 0 && attempts >= maxAttempts) {
                    Status.FAILURE
                } else {
                    c.reset()
                    Status.RUNNING
                }
            }
        }
    }

    override fun onReset() {
        attempts = 0
    }
}
