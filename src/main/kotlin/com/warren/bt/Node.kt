package com.warren.bt

abstract class Node(open val name: String = "Node") {
  abstract fun tick(): Status
  
  open fun reset() {}
}
