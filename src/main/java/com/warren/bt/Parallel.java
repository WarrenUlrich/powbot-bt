package com.warren.bt;

public class Parallel extends Composite {

  public enum Policy {
    REQUIRE_ONE, // Succeed when one child succeeds
    REQUIRE_ALL  // Succeed when all children succeed
  }

  private final Policy successPolicy;
  private final Policy failurePolicy;

  public Parallel(Policy successPolicy, Policy failurePolicy) {
    this.successPolicy = successPolicy;
    this.failurePolicy = failurePolicy;
  }

  public Parallel() {
    this(Policy.REQUIRE_ALL, Policy.REQUIRE_ONE);
  }

  @Override
  public Status tick() {
    if (!hasChildren()) {
      return Status.SUCCESS;
    }

    int successCount = 0;
    int failureCount = 0;
    int runningCount = 0;

    for (Node child : children) {
      Status status = child.tick();

      switch (status) {
        case SUCCESS:
          successCount++;
          break;
        case FAILURE:
          failureCount++;
          break;
        case RUNNING:
          runningCount++;
          break;
      }
    }

    // Failure conditions
    if (failurePolicy == Policy.REQUIRE_ONE && failureCount > 0) {
      reset();
      return Status.FAILURE;
    }

    if (failurePolicy == Policy.REQUIRE_ALL && failureCount == children.size()) {
      reset();
      return Status.FAILURE;
    }

    // Success conditions
    if (successPolicy == Policy.REQUIRE_ONE && successCount > 0) {
      reset();
      return Status.SUCCESS;
    }

    if (successPolicy == Policy.REQUIRE_ALL && successCount == children.size()) {
      reset();
      return Status.SUCCESS;
    }

    // If none succeeded or failed fully, still active
    return Status.RUNNING;
  }
}
