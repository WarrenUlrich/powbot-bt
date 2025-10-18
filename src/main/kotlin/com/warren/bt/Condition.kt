package com.warren.bt

class Condition(override val name: String = "Condition", private val predicate: () -> Boolean) :
        Node(name) {
  override fun tick(): Status = if (predicate()) Status.SUCCESS else Status.FAILURE
}
