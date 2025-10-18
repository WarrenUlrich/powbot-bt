package com.warren.bt

/**
 * Simple cooldown: wait for [ticks] RUNNING frames before letting the child execute once. After the
 * child completes (success/failure), resets and starts cooldown again.
 */
class Cooldown(private val ticks: Int) : Decorator("Cooldown") {
  private var remaining = ticks
  private var runningChild = false

  override fun tick(): Status {
    val c = child ?: return Status.FAILURE

    if (!runningChild) {
      if (remaining > 0) {
        remaining--
        return Status.RUNNING
      }
      runningChild = true
    }

    val s = c.tick()
    if (s != Status.RUNNING) {
      // completed; start cooldown again
      c.reset()
      runningChild = false
      remaining = ticks
    }
    return s
  }

  override fun onReset() {
    remaining = ticks
    runningChild = false
  }
}
