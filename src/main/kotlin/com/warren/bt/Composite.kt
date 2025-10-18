package com.warren.bt

abstract class Composite(override val name: String = "Composite") : Node(name) {
    protected val children = mutableListOf<Node>()

    fun addChild(node: Node) {
        children += node
    }

    override fun reset() {
        children.forEach { it.reset() }
        onReset()
    }

    protected open fun onReset() {}
}
