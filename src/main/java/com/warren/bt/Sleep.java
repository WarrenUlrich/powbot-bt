package com.warren.bt;

import java.util.Random;

public class Sleep implements Node {
  private static final long UNSET = -1L;
  private static final Random RAND = new Random();

  private final long baseDurationNanos;
  private final double spread;
  private long currentDurationNanos;
  private long startNanos = UNSET;

  public Sleep(long durationMillis) {
    this(durationMillis, 0.33);
  }

  public Sleep(long durationMillis, double spread) {
    this.baseDurationNanos = durationMillis * 1_000_000L;
    this.spread = spread;
    this.currentDurationNanos = randomizeDuration();
  }

  private long randomizeDuration() {
    double gaussian = RAND.nextGaussian();
    double factor = 1.0 + gaussian * spread;
    factor = Math.max(0.1, factor);
    return (long) (baseDurationNanos * factor);
  }

  @Override
  public Status tick() {
    if (startNanos == UNSET) {
      startNanos = System.nanoTime();
    }

    long elapsed = System.nanoTime() - startNanos;
    if (elapsed >= currentDurationNanos) {
      reset();
      return Status.SUCCESS;
    }

    return Status.SLEEPING;
  }

  @Override
  public void reset() {
    startNanos = UNSET;
    currentDurationNanos = randomizeDuration();
  }
}
