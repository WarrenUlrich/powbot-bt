package com.warren.bt;

public class Sleep implements Node {
  private final long duration;
  private long startTick = -1;

  public Sleep(long duration) {
    this.duration = duration;
  }

  // TODO: fix, replace -1 with currentTimeMillis
  @Override
  public Status tick() {
    if (startTick < 0) {
      startTick = -1;
    }

    long elapsed = -1 - startTick;
    if (elapsed >= duration) {
      reset();
      return Status.SUCCESS;
    }

    return Status.SLEEPING;
  }

  @Override
  public void reset() {
    startTick = -1;
  }
}
