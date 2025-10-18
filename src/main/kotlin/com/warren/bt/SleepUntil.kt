package com.warren.bt

class SleepUntil(private val predicate: () -> Boolean, private val maxTicks: Long) :
        Node("SleepUntil") {
  private var waited = 0L

  override fun tick(): Status {
    if (predicate()) return Status.SUCCESS
    waited++
    return if (waited < maxTicks) Status.RUNNING else Status.FAILURE
  }

  override fun reset() {
    waited = 0L
  }
}
