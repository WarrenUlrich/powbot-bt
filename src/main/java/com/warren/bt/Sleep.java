package com.warren.bt;

public class Sleep implements Node {
  private static final long UNSET = -1L;

  private final long durationNanos;  // store duration in nanoseconds
  private long startNanos = UNSET;

  public Sleep(long durationMillis) {
    this.durationNanos = durationMillis * 1_000_000L;
  }

  @Override
  public Status tick() {
    // First tick: initialize timer
    if (startNanos == UNSET) {
      startNanos = System.nanoTime();
    }

    long elapsed = System.nanoTime() - startNanos;
    if (elapsed >= durationNanos) {
      reset();
      return Status.SUCCESS;
    }

    return Status.SLEEPING;
  }

  @Override
  public void reset() {
    startNanos = UNSET;
  }
}
