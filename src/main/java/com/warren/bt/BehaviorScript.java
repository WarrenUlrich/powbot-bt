package com.warren.bt;

import org.powbot.api.script.AbstractScript;

public abstract class BehaviorScript extends AbstractScript {
  private BehaviorTree behaviorTree;
  
  @Override
  public void onStart() {
    behaviorTree = behaviorTree();
  }

  @Override
  public final void poll() {
    behaviorTree.run();
  }
  
  public abstract BehaviorTree behaviorTree();
}
