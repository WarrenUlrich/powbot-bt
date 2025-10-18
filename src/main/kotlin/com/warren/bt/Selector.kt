package com.warren.bt

class Selector : Composite("Selector") {
  private var currentIndex = 0

  override fun tick(): Status {
    while (currentIndex < children.size) {
      val status = children[currentIndex].tick()
      when (status) {
        Status.SUCCESS -> {
          return Status.SUCCESS
        }
        Status.FAILURE -> currentIndex++
        Status.RUNNING -> {
          return Status.RUNNING
        }
      }
    }
    return Status.FAILURE
  }

  override fun onReset() {
    currentIndex = 0
  }
}
