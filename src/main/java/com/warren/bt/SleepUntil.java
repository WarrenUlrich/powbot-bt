package com.warren.bt;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public class SleepUntil implements Node {
  private static final long UNSET = -1L;

  private final BooleanSupplier condition;
  private final long maxNanos;     // timeout as nanoseconds (monotonic)
  private long startNanos = UNSET; // first time we started waiting

  /** Time-based: pass a Duration */
  public SleepUntil(BooleanSupplier condition, Duration maxWait) {
    this.condition = Objects.requireNonNull(condition, "condition");
    this.maxNanos = Objects.requireNonNull(maxWait, "maxWait").toNanos();
  }

  /** Convenience: milliseconds */
  public SleepUntil(BooleanSupplier condition, long maxMillis) {
    this(condition, Duration.ofMillis(maxMillis));
  }

  @Override
  public Status tick() {
    // Start the timer on first tick
    if (startNanos == UNSET) {
      startNanos = System.nanoTime();
    }

    // Condition met? We're done.
    if (condition.getAsBoolean()) {
      reset();
      return Status.SUCCESS;
    }

    // Check timeout
    long elapsed = System.nanoTime() - startNanos;
    if (elapsed >= maxNanos) {
      reset();
      return Status.FAILURE;
    }

    // Still waiting
    return Status.SLEEPING;
  }

  @Override
  public void reset() {
    startNanos = UNSET;
  }

  /**
   * Decorator that sleeps until its child node returns SUCCESS.
   * The child node acts as the condition.
   */
  public static class Decorator extends com.warren.bt.Decorator {
    private static final long UNSET = -1L;

    private final long maxNanos;
    private long startNanos = UNSET;

    public Decorator(Duration maxWait) {
      super();
      this.maxNanos = Objects.requireNonNull(maxWait, "maxWait").toNanos();
    }

    public Decorator(long maxMillis) {
      this(Duration.ofMillis(maxMillis));
    }

    public Decorator(Duration maxWait, Node child) {
      super(child);
      this.maxNanos = Objects.requireNonNull(maxWait, "maxWait").toNanos();
    }

    public Decorator(long maxMillis, Node child) {
      this(Duration.ofMillis(maxMillis), child);
    }

    @Override
    public Status tick() {
      if (child == null) {
        throw new IllegalStateException("SleepUntil.Decorator requires a child node");
      }

      if (startNanos == UNSET) {
        startNanos = System.nanoTime();
      }

      // Evaluate the child (our condition)
      Status childStatus = child.tick();

      if (childStatus == Status.SUCCESS) {
        reset();
        return Status.SUCCESS;
      }

      // If the child failed on this tick, reset it so it can be re-evaluated next tick.
      if (childStatus == Status.FAILURE) {
        child.reset();
      }

      // Timeout?
      long elapsed = System.nanoTime() - startNanos;
      if (elapsed >= maxNanos) {
        reset();
        return Status.FAILURE;
      }

      return Status.SLEEPING;
    }

    @Override
    public void reset() {
      startNanos = UNSET;
      super.reset();
    }
  }
}
