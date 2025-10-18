package com.warren.bt

class Parallel(
        private val successPolicy: Policy = Policy.REQUIRE_ALL,
        private val failurePolicy: Policy = Policy.REQUIRE_ONE
) : Composite("Parallel") {

  enum class Policy {
    REQUIRE_ALL,
    REQUIRE_ONE
  }

  override fun tick(): Status {
    if (children.isEmpty()) return Status.SUCCESS

    var successCount = 0
    var failureCount = 0
    var runningFound = false

    for (c in children) {
      when (c.tick()) {
        Status.SUCCESS -> successCount++
        Status.FAILURE -> failureCount++
        Status.RUNNING -> runningFound = true
      }
    }

    val all = children.size

    val success =
            when (successPolicy) {
              Policy.REQUIRE_ALL -> successCount == all
              Policy.REQUIRE_ONE -> successCount > 0
            }
    val failure =
            when (failurePolicy) {
              Policy.REQUIRE_ALL -> failureCount == all
              Policy.REQUIRE_ONE -> failureCount > 0
            }

    return when {
      success -> Status.SUCCESS
      failure -> Status.FAILURE
      else -> Status.RUNNING
    }
  }
}
