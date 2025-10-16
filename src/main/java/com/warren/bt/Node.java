package com.warren.bt;

public interface Node {
  Status tick();

  default void reset() {}
}