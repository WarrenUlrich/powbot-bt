package com.warren.bt

import org.powbot.api.script.AbstractScript

abstract class BehaviorScript : AbstractScript() {
    private lateinit var behaviorTree: BehaviorTree

    override fun onStart() {
        behaviorTree = behaviorTree()
    }

    override fun poll() {
        val s = behaviorTree.tick()
        if (s != Status.RUNNING) {
            behaviorTree.reset()
        }
    }


    abstract fun behaviorTree(): BehaviorTree
}
