package com.warren.bt;

import java.util.function.Supplier;

/** Wraps another node built lazily from a supplier.
 *  Assumes Node has only tick() and reset().
 */
public class SubTree implements Node {
  private final String name;
  private final Supplier<Node> treeSupplier;
  private Node tree;

  public SubTree(String name, Supplier<Node> treeSupplier) {
    this.name = name;
    this.treeSupplier = treeSupplier;
  }

  @Override
  public Status tick() {
    // Lazily create the subtree on first tick.
    if (tree == null) {
      tree = treeSupplier.get();
      if (tree == null) {
        return Status.FAILURE;
      }
    }

    return tree.tick();
  }

  @Override
  public void reset() {
    if (tree != null) {
      tree.reset();
      tree = null; // allow rebuilding after a reset
    }
  }

  @Override
  public String toString() {
    return "SubTree[" + name + "]";
  }
}
