package com.warren.bt;

public interface Node {
  Status tick();

  // default void onStart() {}
  
  // default void onEnd() {}

  default void reset() {}
}