package com.warren.bt;

import java.util.function.Supplier;

/** Re-evaluates which subtree to run on every tick.
 *  Assumes Node has only tick() and reset().
 */
public class DynamicSubTree implements Node {
  private final String name;
  private final Supplier<Node> nodeSupplier;
  private Node current;

  public DynamicSubTree(String name, Supplier<BehaviorTree> treeSupplier) {
    this.name = name;
    this.nodeSupplier = () -> {
      BehaviorTree t = treeSupplier.get();
      return (t == null) ? null : t.getRoot();
    };
  }

  @Override
  public Status tick() {
    // Choose the node to run *this* tick.
    Node next = nodeSupplier.get();

    // If no subtree is available now, end any previous one and fail.
    if (next == null) {
      if (current != null) {
        current.reset();
        current = null;
      }
      return Status.FAILURE;
    }

    // If the chosen subtree instance changed, reset the previous one.
    if (current != next) {
      if (current != null) {
        current.reset();
      }
      current = next;
    }

    // Delegate to the active child.
    return current.tick();
  }

  @Override
  public void reset() {
    if (current != null) {
      current.reset();
      current = null;
    }
  }

  @Override
  public String toString() {
    return "DynamicSubTree[" + name + "]";
  }
}
