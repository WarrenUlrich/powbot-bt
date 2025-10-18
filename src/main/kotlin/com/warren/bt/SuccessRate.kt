package com.warren.bt

import kotlin.random.Random

/**
 * Probabilistic gate: each tick, with probability [chance], tick the child; otherwise FAIL
 * immediately.
 */
class SuccessRate(private val chance: Float) : Decorator("SuccessRate") {
  override fun tick(): Status {
    if (chance <= 0f) return Status.FAILURE
    if (chance >= 1f) return child?.tick() ?: Status.FAILURE
    if (Random.nextFloat() > chance) return Status.FAILURE
    return child?.tick() ?: Status.FAILURE
  }
}
