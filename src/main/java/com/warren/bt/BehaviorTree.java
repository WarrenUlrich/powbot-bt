package com.warren.bt;

import java.util.Stack;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class BehaviorTree {
  private final Node root;

  private BehaviorTree(Node root) {
    this.root = root;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Node getRoot() {
    return root;
  }

  public Status tick() {
    return root.tick();
  }

  public Status run() {
    Status status;
    do {
      status = tick();
    } while (status == Status.RUNNING);

    return status;
  }

  public void reset() {
    root.reset();
  }

  public static class Builder {
    private Stack<Node> nodeStack = new Stack<>();
    private Stack<Composite> compositeStack = new Stack<>();

    private Builder() {
    }

    public Builder sequence() {
      Sequence sequence = new Sequence();
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(sequence);
      }
      compositeStack.push(sequence);
      return this;
    }

    public Builder selector() {
      Selector selector = new Selector();
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(selector);
      }
      compositeStack.push(selector);
      return this;
    }

    public Builder randomSelector() {
      RandomSelector randomSelector = new RandomSelector();
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(randomSelector);
      }
      compositeStack.push(randomSelector);
      return this;
    }

    public Builder parallel(Parallel.Policy successPolicy, Parallel.Policy failurePolicy) {
      Parallel parallel = new Parallel(successPolicy, failurePolicy);
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(parallel);
      }
      compositeStack.push(parallel);
      return this;
    }

    public Builder parallel() {
      return parallel(Parallel.Policy.REQUIRE_ALL, Parallel.Policy.REQUIRE_ONE);
    }

    public Builder action(Supplier<Status> action) {
      return action("Action", action);
    }

    public Builder action(String name, Supplier<Status> action) {
      Node node = new Action(name, action);
      addNode(node);
      return this;
    }

    public Builder succeed() {
      return action("Succeed", () -> {
        return Status.SUCCESS;
      });
    }

    public Builder succeed(Runnable action) {
      return action("Succeed", () -> {
        action.run();
        return Status.SUCCESS;
      });
    }

    public Builder fail(Runnable action) {
      return action("Fail", () -> {
        action.run();
        return Status.FAILURE;
      });
    }

    public Builder condition(BooleanSupplier condition) {
      return condition("Condition", condition);
    }

    public Builder condition(String name, BooleanSupplier condition) {
      Node node = new Condition(name, condition);
      addNode(node);
      return this;
    }

    public Builder sleep(long duration) {
      Node node = new Sleep(duration);
      addNode(node);
      return this;
    }

    public Builder sleepUntil(long duration) {
      nodeStack.push(new SleepUntil.Decorator(duration));
      return this;
    }

    public Builder sleepUntil(BooleanSupplier predicate, long duration) {
      Node node = new SleepUntil(predicate, duration);
      addNode(node);
      return this;
    }

    public Builder invert() {
      nodeStack.push(new Inverter());
      return this;
    }

    public Builder repeat(int maxRepeats) {
      nodeStack.push(new Repeater(null, maxRepeats));
      return this;
    }

    public Builder repeatForever() {
      return repeat(-1);
    }

    public Builder retry(int maxAttempts) {
      nodeStack.push(new RetryUntilSuccess(null, maxAttempts));
      return this;
    }

    public Builder retryForever() {
      return retry(-1);
    }

    public Builder subtree(BehaviorTree tree) {
      return subtree("SubTree", tree);
    }

    public Builder subtree(String name, BehaviorTree tree) {
      Node node = new SubTree(name, () -> tree.root);
      addNode(node);
      return this;
    }

    public Builder subtree(Supplier<BehaviorTree> treeSupplier) {
      return subtree("SubTree", treeSupplier);
    }

    public Builder subtree(String name, Supplier<BehaviorTree> treeSupplier) {
      Node node = new DynamicSubTree(name, treeSupplier);
      addNode(node);
      return this;
    }

    public Builder cooldown(int ticks) {
      nodeStack.push(new Cooldown(null, ticks));
      return this;
    }

    public Builder cooldown(Supplier<Integer> ticksSupplier) {
      nodeStack.push(new Cooldown(null, ticksSupplier.get()));
      return this;
    }

    public Builder successRate(float chance) {
      nodeStack.push(new SuccessRate(null, chance));
      return this;
    }

    public Builder successRate(Supplier<Float> chanceSupplier) {
      nodeStack.push(new SuccessRate(null, chanceSupplier.get()));
      return this;
    }

    public Builder end() {
      if (!compositeStack.isEmpty()) {
        Node composite = compositeStack.pop();
        if (!compositeStack.isEmpty()) {
          // If there's still a parent composite, we already added this as a child
        } else {
          // This is the root node
          nodeStack.push(composite);
        }
      }
      return this;
    }

    public Builder apply(Function<Builder, Builder> builderFunction) {
      return builderFunction.apply(this);
    }

    public Builder node(Node node) {
      addNode(node);
      return this;
    }

    public BehaviorTree build() {
      if (!compositeStack.isEmpty()) {
        throw new IllegalStateException("Unclosed composite nodes. Call end() for each composite.");
      }
      if (nodeStack.size() != 1) {
        throw new IllegalStateException("Invalid tree structure. Expected exactly one root node.");
      }
      return new BehaviorTree(nodeStack.pop());
    }

    private void addNode(Node node) {
      while (!nodeStack.isEmpty() && nodeStack.peek() instanceof Decorator) {
        Decorator decorator = (Decorator) nodeStack.pop();
        decorator.setChild(node);
        node = decorator;
      }

      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(node);
      } else {
        nodeStack.push(node);
      }
    }
  }
}
