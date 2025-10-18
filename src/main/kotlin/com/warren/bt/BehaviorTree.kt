package com.warren.bt

import org.powbot.mobile.script.ScriptManager

open class BehaviorTree protected constructor(val root: Node) {
    fun tick(): Status = root.tick()

    fun run(): Status {
        var s: Status
        do {
            s = tick()
        } while (s == Status.RUNNING)
        return s
    }

    fun reset() = root.reset()

    companion object {
        fun build(block: Builder.() -> Unit): BehaviorTree = Builder().apply(block).build()
    }

    open class Builder @JvmOverloads constructor() {
        protected val nodeStack = ArrayDeque<Decorator>()
        protected val compositeStack = ArrayDeque<Composite>()
        protected var root: Node? = null

        protected fun attach(node: Node) {
            var n = node
            while (nodeStack.isNotEmpty()) {
                val d = nodeStack.removeLast()
                d.assignChild(n)
                n = d
            }
            if (compositeStack.isNotEmpty()) {
                compositeStack.last().addChild(n)
            } else {
                if (root != null) error("Root already set; only one root node is allowed")
                root = n
            }
        }

        protected inline fun <T : Composite> composite(node: T, block: Builder.() -> Unit): T {
            attach(node)
            compositeStack.addLast(node)
            this.block()
            compositeStack.removeLast()
            return node
        }

        fun sequence(block: Builder.() -> Unit) = composite(Sequence(), block)

        fun selector(block: Builder.() -> Unit) = composite(Selector(), block)

        fun randomSelector(block: Builder.() -> Unit) = composite(RandomSelector(), block)

        fun parallel(
            successPolicy: Parallel.Policy = Parallel.Policy.REQUIRE_ALL,
            failurePolicy: Parallel.Policy = Parallel.Policy.REQUIRE_ONE,
            block: Builder.() -> Unit
        ) = composite(Parallel(successPolicy, failurePolicy), block)

        fun action(name: String = "Action", action: () -> Status) = attach(Action(name, action))

        fun succeed(name: String = "Succeed", after: (() -> Unit)? = null) = attach(
            Action(name) {
                after?.invoke()
                Status.SUCCESS
            }
        )

        fun fail(name: String = "Fail", after: (() -> Unit)? = null) = attach(
            Action(name) {
                after?.invoke()
                Status.FAILURE
            }
        )

        fun condition(name: String = "Condition", predicate: () -> Boolean) =
            attach(Condition(name, predicate))

        fun sleep(ms: Long) = attach(Sleep(ms))

        fun sleepUntil(ms: Long, predicate: () -> Boolean) =
            attach(SleepUntil(predicate, ms))

        fun invert(block: Builder.() -> Unit) {
            nodeStack.addLast(Inverter())
            this.block()
        }

        fun repeat(maxRepeats: Int = -1, block: Builder.() -> Unit) {
            nodeStack.addLast(Repeater(maxRepeats))
            this.block()
        }

        fun retry(maxAttempts: Int = -1, block: Builder.() -> Unit) {
            nodeStack.addLast(RetryUntilSuccess(maxAttempts))
            this.block()
        }

        fun cooldown(ms: Long, block: Builder.() -> Unit) {
            nodeStack.addLast(Cooldown(ms))
            this.block()
        }

        fun successRate(chance: Float, block: Builder.() -> Unit) {
            nodeStack.addLast(SuccessRate(chance))
            this.block()
        }

        fun subtree(name: String = "SubTree", tree: BehaviorTree) =
            attach(SubTree(name) { tree.root })

        fun subtree(name: String = "SubTree", supplier: () -> BehaviorTree) =
            attach(SubTree(name) { supplier().root })

        open fun build(): BehaviorTree {
            if (compositeStack.isNotEmpty()) error("Unclosed composite blocks. Make sure all blocks have finished.")
            val r = root ?: error("No root node defined. Provide at least one node.")
            return BehaviorTree(r)
        }
    }
}
