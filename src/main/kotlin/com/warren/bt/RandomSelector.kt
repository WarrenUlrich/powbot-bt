package com.warren.bt

class RandomSelector : Composite("RandomSelector") {
  private var order: MutableList<Int> = mutableListOf()
  private var idx = 0

  override fun tick(): Status {
    if (order.isEmpty()) {
      order = children.indices.shuffled().toMutableList()
      idx = 0
    }
    while (idx < order.size) {
      val status = children[order[idx]].tick()
      when (status) {
        Status.SUCCESS -> {
          return Status.SUCCESS
        }
        Status.FAILURE -> idx++
        Status.RUNNING -> {
          return Status.RUNNING
        }
      }
    }
    return Status.FAILURE
  }

  override fun onReset() {
    order.clear()
    idx = 0
  }
}
