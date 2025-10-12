package com.warren.bt;

public class Sequence extends Composite {
  @Override
  public Status tick() {
    if (!hasChildren()) {
      return Status.SUCCESS;
    }

    while (currentChild < children.size()) {
      Node child = children.get(currentChild);
      Status status = child.tick();

      if (status == Status.FAILURE) {
        reset();
        return Status.FAILURE;
      }

      if (status == Status.RUNNING) {
        return Status.RUNNING;
      }

      // Continue to next child if SUCCESS
      currentChild++;
    }

    reset();
    return Status.SUCCESS;
  }
}
