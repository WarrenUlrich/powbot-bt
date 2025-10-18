package com.warren.bt

class Inverter : Decorator("Inverter") {
  override fun tick(): Status {
    val s = child?.tick() ?: return Status.FAILURE
    return when (s) {
      Status.SUCCESS -> Status.FAILURE
      Status.FAILURE -> Status.SUCCESS
      Status.RUNNING -> Status.RUNNING
    }
  }
}
