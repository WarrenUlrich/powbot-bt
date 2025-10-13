package com.warren.bt;

import java.time.Duration;
import java.util.Stack;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.powbot.api.Actionable;
import org.powbot.api.Area;
import org.powbot.api.Filter;
import org.powbot.api.Interactable;
import org.powbot.api.Locatable;
import org.powbot.api.Nameable;
import org.powbot.api.Tile;
import org.powbot.api.Viewable;
import org.powbot.api.rt4.Actor;
import org.powbot.api.rt4.Bank;
import org.powbot.api.rt4.Camera;
import org.powbot.api.rt4.Chat;
import org.powbot.api.rt4.Combat;
import org.powbot.api.rt4.Game;
import org.powbot.api.rt4.GameObject;
import org.powbot.api.rt4.Inventory;
import org.powbot.api.rt4.Item;
import org.powbot.api.rt4.ItemType;
import org.powbot.api.rt4.Magic;
import org.powbot.api.rt4.Movement;
import org.powbot.api.rt4.Npc;
import org.powbot.api.rt4.Npcs;
import org.powbot.api.rt4.Objects;
import org.powbot.api.rt4.Player;
import org.powbot.api.rt4.Players;
import org.powbot.api.rt4.Prayer;
import org.powbot.api.rt4.stream.item.BankItemStream;
import org.powbot.api.rt4.stream.item.InventoryItemStream;
import org.powbot.api.script.AbstractScript;
import org.powbot.api.waiter.Waiter;
import org.powbot.dax.engine.local.Reachable.Direction;
import org.powbot.dax.teleports.utils.ItemFilters;
import org.powbot.mobile.script.ScriptManager;
import org.slf4j.LoggerFactory;

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
      Node node = new SubTree(name, () -> treeSupplier.get().root);
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

    public Builder logInfo(Supplier<String> message) {
      return succeed(() -> {
        var logger = LoggerFactory.getLogger(AbstractScript.class);
        if (logger == null)
          return;

        logger.info(message.get());
      });
    }

    public Builder inventoryFull() {
      return condition(() -> Inventory.isFull());
    }

    public Builder inventoryEmpty() {
      return condition(() -> Inventory.isEmpty());
    }

    public Builder inventoryContains(Function<InventoryItemStream, InventoryItemStream> func) {
      return condition(() -> {
        return func.apply(Inventory.stream()).isNotEmpty();
      });
    }

    public Builder useItem(Supplier<Item> use, Supplier<Item> on) {
      return interact(() -> use.get(), "Use")
          .sleepUntil(() -> {
            return !Inventory.selectedItem().equals(Item.getNil());
          }, 2000)
          .interact(() -> on.get(), "Use");
    }

    public Builder useItem(Function<InventoryItemStream, Item> use, Function<InventoryItemStream, Item> on) {
      return interact(() -> use.apply(Inventory.stream()), "Use")
          .sleepUntil(() -> {
            return Inventory.selectedItem().equals(use.apply(Inventory.stream()));
          }, 2000)
          .sleep(200)
          .interact(() -> on.apply(Inventory.stream()), "Use");
    }

    public Builder drop(Function<InventoryItemStream, InventoryItemStream> func) {
      return condition(() -> {
        var items = func.apply(Inventory.stream()).list();
        return Inventory.drop(items);
      });
    }

    public Builder dropAll(Predicate<Item> filter) {
      return condition(() -> Inventory.dropAll(filter));
    }

    public Builder moveTo(Supplier<Movement.Builder> movementSupplier) {
      return condition(() -> {
        var movement = movementSupplier.get();
        return movement.move().getSuccess();
      });
    }

    public Builder moveToBank() {
      return condition(() -> {
        return Movement.moveToBank().getSuccess();
      });
    }

    public Builder stepTo(Supplier<Locatable> locatableSupplier) {
      return condition(() -> {
        var locatable = locatableSupplier.get();
        if (locatable == null)
          return false;

        return Movement.step(locatable);
      });
    }

    public Builder isBankOpen() {
      return condition(Bank::opened);
    }

    public Builder openBank() {
      return condition(Bank::open);
    }

    public Builder bankContains(Function<BankItemStream, BankItemStream> func) {
      //@formatter:off
      return sequence()
              .isBankOpen()
              .condition(() -> {
                return func.apply(Bank.stream()).isNotEmpty();
              })
            .end();
      //@formatter:on
    }

    public Builder withdraw(Function<BankItemStream, Item> func, int amount) {
      //@formatter:off
      return sequence()
              .isBankOpen()
              .condition(() -> {
                return Bank.withdraw(func.apply(Bank.stream()), amount);
              })
            .end();
      //@formatter:on
    }

    public Builder depositAll(Function<InventoryItemStream, InventoryItemStream> func) {
      //@formatter:off
      return sequence()
              .isBankOpen()
              .condition(() -> {
                var items = func.apply(Inventory.stream()).list();
                if (items.isEmpty())
                  return true;
                
                var counts = items.stream().collect(
                  Collectors.groupingBy(
                    item -> item.getId(),
                    Collectors.summingInt(item -> item.getStack())
                  )
                );

                for (var e : counts.entrySet()) {
                  int id = e.getKey();
                  int amount = e.getValue();
                  if (amount <= 0) continue; 
                  
                  if (!Bank.deposit(id, amount))
                    return false;
                }
                return true;
              })
            .end();
      //@formatter:on
    }

    public Builder inViewport(Supplier<? extends Viewable> actorSupplier) {
      return condition(() -> {
        var actor = actorSupplier.get();
        if (actor == null)
          return false;

        return actor.inViewport();
      });
    }

    public Builder click(Supplier<? extends Interactable> interactabSupplier) {
      return condition(() -> {
        var interactable = interactabSupplier.get();
        if (!interactable.inViewport()) {
          if (!(interactable instanceof Locatable))
            return false;

          Camera.turnTo((Locatable) interactable);
        }

        if (interactable instanceof Nameable) {
          return interactable.click();
        }

        return interactable.click();
      });
    }

    public Builder interact(Supplier<? extends Interactable> interactabSupplier, String action) {
      return condition(() -> {
        var interactable = interactabSupplier.get();
        if (interactable instanceof Item item) {
          Inventory.open();
          if (item.actions().get(0).equals(action))
            return item.click(); 
          
        } else {
          if (!interactable.inViewport()) {
            if (!(interactable instanceof Locatable))
              return false;

            Camera.turnTo((Locatable) interactable);
          }
        }

        if (interactable instanceof Nameable) {
          return interactable.interact(action, ((Nameable) interactable).name());
        }

        return interactable.interact(action);
      });
    }

    public Builder setCameraAngle(int angle, int tolerance) {
      return condition(() -> {
        var currentAngle = Camera.yaw();
        var diff = Math.abs(angle - currentAngle);
        if (diff <= tolerance)
          return true;

        return Camera.angle(angle);
      });
    }

    public Builder turnCameraTo(Supplier<? extends Locatable> locatableSupplier) {
      return condition(() -> {
        var locatable = locatableSupplier.get();
        if (locatable == null)
          return false;

        Camera.turnTo(locatable);
        return true;
      });
    }

    // lazy way to turn camera top down and zoom out
    public Builder fixCamera() {
      return condition(() -> {
        if (Camera.pitch() < 90)
          Camera.pitch(true);

        if (Camera.getZoom() > 10)
          Camera.moveZoomSlider(0);

        return true;
      });
    }

    public Builder changeTab(Game.Tab tab) {
      return condition(() -> {
        return Game.tab(tab);
      });
    }

    public Builder chatting() {
      return condition(Chat::chatting);
    }

    public Builder canContinueChat() {
      return condition(Chat::canContinue);
    }

    public Builder clickContinue() {
      return condition(Chat::clickContinue);
    }

    public Builder continueChat(String... dialogs) {
      return condition(() -> Chat.continueChat(dialogs));
    }

    public Builder completeChat(String... dialogs) {
      return condition(() -> Chat.completeChat(dialogs));
    }

    public Builder autoRetaliate(boolean value) {
      return condition(() -> Combat.autoRetaliate(value));
    }

    public Builder wildernessLevel(int level) {
      return condition(() -> Combat.wildernessLevel() >= level);
    }

    public Builder combatStyle(Combat.Style style) {
      return condition(() -> style.equals(Combat.style()));
    }

    public Builder combatStyle(Supplier<Combat.Style> styleSupplier) {
      return condition(() -> {
        var style = styleSupplier.get();
        if (style == null)
          return false;

        return style.equals(Combat.style());
      });
    }

    public Builder setCombatStyle(Combat.Style style) {
      return condition(() -> Combat.style(style));
    }

    public Builder setCombatStyle(Supplier<Combat.Style> styleSupplier) {
      return condition(() -> {
        var style = styleSupplier.get();
        return Combat.style(style);
      });
    }

    public Builder healthPercent(double percentage) {
      return condition(() -> Combat.healthPercent() >= percentage);
    }

    public Builder health(int health) {
      return condition(() -> Combat.health() >= health);
    }

    public Builder health(IntSupplier healthSupplier) {
      return condition(() -> Combat.health() >= healthSupplier.getAsInt());
    }

    public Builder prayersActive() {
      return condition(() -> {
        return Prayer.prayersActive();
      });
    }

    public Builder prayerActive(Prayer.Effect effect) {
      return condition(() -> {
        return Prayer.prayerActive(effect);
      });
    }

    public Builder prayerActive(Supplier<Prayer.Effect> effect) {
      return condition(() -> {
        return Prayer.prayerActive(effect.get());
      });
    }

    public Builder prayerPoints(int points) {
      return condition(() -> {
        return Prayer.prayerPoints() >= points;
      });
    }

    public Builder prayerPoints(IntSupplier pointsSupplier) {
      return condition(() -> {
        return Prayer.prayerPoints() >= pointsSupplier.getAsInt();
      });
    }

    public Builder casting(Supplier<Magic.Spell> spellSupplier) {
      return condition(() -> {
        return spellSupplier.get().casting();
      });
    }

    public Builder castSpell(Supplier<Magic.Spell> spellSupplier) {
      return condition(() -> {
        return spellSupplier.get().cast();
      });
    }

    public Builder castSpell(Supplier<Magic.Spell> spellSupplier, String action) {
      return condition(() -> {
        return spellSupplier.get().cast(action);
      });
    }

    public Builder castSpell(Supplier<? extends Interactable> interactableSupplier,
        Supplier<Magic.Spell> spellSupplier) {
      //@formatter:off
      return builder()
        .sequence()
          .castSpell(spellSupplier)
          .casting(spellSupplier)
          .condition(() -> {
            var interactable = interactableSupplier.get();
            return interactable.click();
          })
        .end();
      //@formatter:on
    }

    public Builder inInstance() {
      return condition(() -> {
        return true;
      });
    }

    public Builder on(Supplier<Tile> tileSupplier) {
      return condition(() -> {
        var localPlayer = Players.local();
        return localPlayer.tile().equals(tileSupplier.get());
      });
    }

    public Builder within(Supplier<Area> areaSupplier) {
      return condition(() -> {
        var area = areaSupplier.get();
        if (area == null)
          return false;

        return area.contains(Players.local());
      });
    }

    public Builder distance(Supplier<? extends Locatable> locatableSupplier, int dist) {
      return condition(() -> {
        var locatable = locatableSupplier.get();
        if (locatable == null)
          return false;

        return Players.local().distanceTo(locatable) <= dist;
      });
    }

    public Builder animating() {
      return condition(() -> {
        return Players.local().animation() != -1;
      });
    }

    public Builder animating(int animId) {
      return condition(() -> {
        return Players.local().animation() == animId;
      });
    }

    public Builder interacting() {
      return condition(() -> {
        return !Players.local().interacting().equals(Actor.getNil());
      });
    }

    public Builder interacting(Supplier<? extends Actor<?>> actorSupplier) {
      return condition(() -> {
        return Players.local().interacting().equals(actorSupplier.get());
      });
    }

    public Builder alive(Supplier<? extends Actor<?>> actorSupplier) {
      return condition(() -> {
        return actorSupplier.get().alive();
      });
    }

    public Builder isNil(Supplier<?> supplier) {
      return condition(() -> {
        var value = supplier.get();
        if (value instanceof Actor) {
          return value.equals(Actor.getNil());
        } else if (value instanceof GameObject) {
          return value.equals(GameObject.getNil());
        }

        throw new IllegalArgumentException("Unsupported type in isNil: " + value.getClass());
      });
    }

    public Builder stopScript() {
      return succeed(() -> {
        ScriptManager.INSTANCE.stop();
      });
    }
  }
}
