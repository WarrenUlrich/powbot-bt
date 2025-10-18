package com.warren.bt

class Sequence : Composite("Sequence") {
    private var currentIndex = 0

    override fun tick(): Status {
        if (currentIndex >= children.size) return Status.SUCCESS
        return when (val status = children[currentIndex].tick()) {
            Status.SUCCESS -> {
                currentIndex++
                if (currentIndex >= children.size) Status.SUCCESS else Status.RUNNING
            }

            Status.RUNNING -> Status.RUNNING

            Status.FAILURE -> Status.FAILURE
        }
    }


    override fun onReset() {
        currentIndex = 0
    }
}
