package com.warren.bt;

import java.util.function.Supplier;

public class Action implements Node {
  private final Supplier<Status> action;
  private final String name;

  public Action(String name, Supplier<Status> action) {
    this.name = name;
    this.action = action;
  }

  public Action(String name, Status status) {
    this(name, () -> status);
  }

  @Override
  public Status tick() {
    try {
      return action.get();
    } catch (Exception e) {
      return Status.FAILURE;
    }
  }

  @Override
  public String toString() {
    return "Action[" + name + "]";
  }
}