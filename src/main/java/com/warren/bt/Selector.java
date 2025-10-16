package com.warren.bt;

public class Selector extends Composite {
  @Override
  public Status tick() {
    if (!hasChildren()) {
      return Status.FAILURE;
    }

    while (currentChild < children.size()) {
      Node child = children.get(currentChild);
      Status status = child.tick();

      if (status == Status.SUCCESS) {
        reset();
        return Status.SUCCESS;
      }

      if (status == Status.RUNNING) {
        return Status.RUNNING;
      }

      currentChild++;
    }

    reset();
    return Status.FAILURE;
  }
}
