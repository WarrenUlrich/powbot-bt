package com.warren.bt;

import java.util.Random;
import java.util.function.Supplier;

public class SuccessRate extends Decorator {
  private final Supplier<Float> successRateSupplier;
  private final Random random;

  public SuccessRate(Node child, float successRate) {
    this(child, () -> successRate);
  }

  public SuccessRate(Node child, Supplier<Float> successRateSupplier) {
    super(child);
    this.successRateSupplier = successRateSupplier;
    this.random = new Random();
  }

  @Override
  public Status tick() {
    if (child == null) {
      return Status.FAILURE;
    }

    float currentSuccessRate = Math.max(0f, Math.min(100f, successRateSupplier.get()));

    float normalizedRate = currentSuccessRate / 100f;

    if (random.nextFloat() > normalizedRate) {
      return Status.FAILURE;
    }

    return child.tick();
  }
}
