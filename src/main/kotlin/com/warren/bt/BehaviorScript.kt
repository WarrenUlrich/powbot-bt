package com.warren.bt

import org.powbot.api.script.AbstractScript

abstract class BehaviorScript : AbstractScript() {
    private lateinit var behaviorTree: BehaviorTree

    override fun onStart() {
        try {
            behaviorTree = behaviorTree()
        } catch (e: Error) {
            logger.info(e.stackTraceToString())
//            controller.stop()
        }
    }

    override fun poll() {
        try {
            val s = behaviorTree.tick()
            if (s != Status.RUNNING) {
                behaviorTree.reset()
            }
        } catch (e: Error) {
            logger.info(e.message)
        }
    }


    abstract fun behaviorTree(): BehaviorTree
}
