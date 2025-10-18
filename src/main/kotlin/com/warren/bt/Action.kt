package com.warren.bt

class Action(override val name: String = "Action", private val action: () -> Status) : Node(name) {
  override fun tick(): Status = action()
}
