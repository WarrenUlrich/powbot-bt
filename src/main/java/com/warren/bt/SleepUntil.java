package com.warren.bt;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;
import java.util.function.BooleanSupplier;

public class SleepUntil implements Node {
  private static final long UNSET = -1L;
  private static final Random RAND = new Random();

  private final BooleanSupplier condition;
  private final long baseMaxNanos;
  private final double spread;
  private long currentMaxNanos;
  private long startNanos = UNSET;

  public SleepUntil(BooleanSupplier condition, Duration maxWait) {
    this(condition, maxWait, 0.33);
  }

  public SleepUntil(BooleanSupplier condition, Duration maxWait, double spread) {
    this.condition = Objects.requireNonNull(condition, "condition");
    this.baseMaxNanos = Objects.requireNonNull(maxWait, "maxWait").toNanos();
    this.spread = spread;
    this.currentMaxNanos = randomizeDuration();
  }

  public SleepUntil(BooleanSupplier condition, long maxMillis) {
    this(condition, Duration.ofMillis(maxMillis));
  }

  public SleepUntil(BooleanSupplier condition, long maxMillis, double spread) {
    this(condition, Duration.ofMillis(maxMillis), spread);
  }

  private long randomizeDuration() {
    double gaussian = RAND.nextGaussian();
    double factor = 1.0 + gaussian * spread;
    factor = Math.max(0.1, factor);
    return (long) (baseMaxNanos * factor);
  }

  @Override
  public Status tick() {
    if (startNanos == UNSET) {
      startNanos = System.nanoTime();
    }

    if (condition.getAsBoolean()) {
      reset();
      return Status.SUCCESS;
    }

    long elapsed = System.nanoTime() - startNanos;
    if (elapsed >= currentMaxNanos) {
      reset();
      return Status.FAILURE;
    }

    return Status.RUNNING;
  }

  @Override
  public void reset() {
    startNanos = UNSET;
    currentMaxNanos = randomizeDuration();
  }

  public static class Decorator extends com.warren.bt.Decorator {
    private static final long UNSET = -1L;
    private static final Random RAND = new Random();

    private final long baseMaxNanos;
    private final double spread;
    private long currentMaxNanos;
    private long startNanos = UNSET;

    public Decorator(Duration maxWait) {
      this(maxWait, 0.33);
    }

    public Decorator(Duration maxWait, double spread) {
      super();
      this.baseMaxNanos = Objects.requireNonNull(maxWait, "maxWait").toNanos();
      this.spread = spread;
      this.currentMaxNanos = randomizeDuration();
    }

    public Decorator(long maxMillis) {
      this(Duration.ofMillis(maxMillis));
    }

    public Decorator(long maxMillis, double spread) {
      this(Duration.ofMillis(maxMillis), spread);
    }

    public Decorator(Duration maxWait, Node child) {
      this(maxWait, 0.33, child);
    }

    public Decorator(Duration maxWait, double spread, Node child) {
      super(child);
      this.baseMaxNanos = Objects.requireNonNull(maxWait, "maxWait").toNanos();
      this.spread = spread;
      this.currentMaxNanos = randomizeDuration();
    }

    public Decorator(long maxMillis, Node child) {
      this(Duration.ofMillis(maxMillis), 0.33, child);
    }

    public Decorator(long maxMillis, double spread, Node child) {
      this(Duration.ofMillis(maxMillis), spread, child);
    }

    private long randomizeDuration() {
      double gaussian = RAND.nextGaussian();
      double factor = 1.0 + gaussian * spread;
      factor = Math.max(0.1, factor);
      return (long) (baseMaxNanos * factor);
    }

    @Override
    public Status tick() {
      if (child == null) {
        throw new IllegalStateException("SleepUntil.Decorator requires a child node");
      }

      if (startNanos == UNSET) {
        startNanos = System.nanoTime();
      }

      Status childStatus = child.tick();

      if (childStatus == Status.SUCCESS) {
        reset();
        return Status.SUCCESS;
      }

      if (childStatus == Status.FAILURE) {
        child.reset();
      }

      long elapsed = System.nanoTime() - startNanos;
      if (elapsed >= currentMaxNanos) {
        reset();
        return Status.FAILURE;
      }

      return Status.RUNNING;
    }

    @Override
    public void reset() {
      startNanos = UNSET;
      currentMaxNanos = randomizeDuration();
      super.reset();
    }
  }
}
