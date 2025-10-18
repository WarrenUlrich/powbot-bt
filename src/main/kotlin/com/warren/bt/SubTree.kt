package com.warren.bt

class SubTree(override val name: String = "SubTree", private val supplier: () -> Node) :
        Node(name) {
  private val root: Node by lazy { supplier() }
  override fun tick(): Status = root.tick()
  override fun reset() {
    root.reset()
  }
}
