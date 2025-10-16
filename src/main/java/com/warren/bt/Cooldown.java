package com.warren.bt;

import java.util.function.Supplier;

public class Cooldown extends Decorator {
  private final Supplier<Integer> cooldownDurationSupplier;
  private long lastExecutionTime = -1;

  public Cooldown(Node child, int cooldownDuration) {
    this(child, () -> cooldownDuration);
  }

  public Cooldown(Node child, Supplier<Integer> cooldownDurationSupplier) {
    super(child);
    this.cooldownDurationSupplier = cooldownDurationSupplier;
  }

  @Override
  public Status tick() {
    if (child == null) {
      return Status.FAILURE;
    }

    long currentTime = System.currentTimeMillis();
    int cooldownDuration = Math.max(0, cooldownDurationSupplier.get());

    if (lastExecutionTime >= 0) {
      long elapsed = currentTime - lastExecutionTime;
      if (elapsed < cooldownDuration) {
        return Status.FAILURE;
      }
    }

    Status status = child.tick();

    if (lastExecutionTime < 0 || status != Status.RUNNING) {
      lastExecutionTime = currentTime;
    }

    return status;
  }

  @Override
  public void reset() {
    super.reset();
    lastExecutionTime = -1;
  }
}
