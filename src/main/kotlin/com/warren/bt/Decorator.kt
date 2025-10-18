package com.warren.bt

abstract class Decorator(override val name: String = "Decorator", var child: Node? = null) :
        Node(name) {
  fun assignChild(node: Node) {
    child = node
  }

  override fun reset() {
    child?.reset()
    onReset()
  }

  protected open fun onReset() {}
}
