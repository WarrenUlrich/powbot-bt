package com.warren.bt

class Cooldown(private val cooldownMs: Long) : Decorator("Cooldown") {
  private var nextAllowedTime = 0L
  private var runningChild = false

  override fun tick(): Status {
    val c = child ?: return Status.FAILURE
    val now = System.currentTimeMillis()

    if (!runningChild) {
      if (now < nextAllowedTime) {
        return Status.RUNNING
      }
      runningChild = true
    }

    val s = c.tick()
    if (s != Status.RUNNING) {
      // completed; start cooldown timer
      c.reset()
      runningChild = false
      nextAllowedTime = now + cooldownMs
    }
    return s
  }

  override fun onReset() {
    runningChild = false
    nextAllowedTime = 0L
  }
}
