package com.warren.bt

import com.warren.util.Projection
import org.powbot.api.Area
import org.powbot.api.Interactable
import org.powbot.api.Locatable
import org.powbot.api.Nameable
import org.powbot.api.Nillable
import org.powbot.api.Tile
import org.powbot.api.Viewable
import org.powbot.api.rt4.Actor
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Game
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item
import org.powbot.api.rt4.Magic
import org.powbot.api.rt4.Movement
import org.powbot.api.rt4.Players
import org.powbot.api.rt4.Prayer
import org.powbot.api.rt4.Quests
import org.powbot.api.rt4.Skills
import org.powbot.api.rt4.stream.item.BankItemStream
import org.powbot.api.rt4.stream.item.InventoryItemStream
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.mobile.script.ScriptManager

class PowBehaviorTree private constructor(root: Node) : BehaviorTree(root) {
    companion object {
        fun build(block: PowBuilder.() -> Unit): PowBehaviorTree {
            val built: BehaviorTree = PowBuilder().apply(block).build()
            return built as PowBehaviorTree
        }
    }

    class PowBuilder : BehaviorTree.Builder() {
        override fun build(): BehaviorTree {
            if (compositeStack.isNotEmpty()) error("Unclosed composite blocks. Make sure all blocks have finished.")
            val r = root ?: error("No root node defined. Provide at least one node.")
            return PowBehaviorTree(r)
        }

        fun info(messageSupplier: () -> String) = succeed {
            ScriptManager.script()!!.logger.info(messageSupplier())
        }

        fun inventoryFull() = condition { Inventory.isFull() }

        fun inventoryEmpty() = condition { Inventory.isEmpty() }

        fun inventoryNotEmpty() = condition { Inventory.isNotEmpty() }

        fun inventoryContains(func: (InventoryItemStream) -> InventoryItemStream) = condition {
            func(Inventory.stream()).isNotEmpty()
        }

        fun itemSelected() = condition { Inventory.selectedItemIndex() != -1 }

        fun itemSelected(itemSupplier: () -> Item) = condition { Inventory.selectedItem() == itemSupplier() }

        fun drop(func: (InventoryItemStream) -> InventoryItemStream) = condition {
            val items = func(Inventory.stream()).list()
            Inventory.drop(items)
        }

        fun <T> useItemOn(
            use: (InventoryItemStream) -> Item,
            on: () -> T
        ) where T : Interactable, T : Nameable = condition {
            use(Inventory.stream()).useOn(on())
        }

        fun moveTo(movementSupplier: () -> Movement.Builder) = condition {
            movementSupplier().move().success
        }

        fun moveToBank() = condition { Movement.moveToBank().success }

        fun stepTo(locatableSupplier: () -> Locatable) = condition {
            val loc = locatableSupplier()
            Movement.step(loc)
        }

        fun on(tileSupplier: () -> Tile) = condition {
            Players.local().tile() == tileSupplier()
        }

        fun within(areaSupplier: () -> Area) = condition {
            areaSupplier().contains(Players.local())
        }

        fun distance(locatableSupplier: () -> Locatable, dist: Int) = condition {
            Players.local().distanceTo(locatableSupplier()) <= dist
        }

        fun isBankOpen() = condition {
            Bank.opened()
        }

        fun atBank() = condition {
            // TODO: Need something better than this
            Bank.nearest().distanceTo(Players.local()) < 10
        }

        fun openBank() = condition {
            Bank.open()
        }

        fun bankContains(func: (BankItemStream) -> BankItemStream) = condition {
            func(Bank.stream()).isNotEmpty()
        }

        fun withdraw(func: (BankItemStream) -> Item, amount: Int) {
            sequence {
                isBankOpen()
                condition { Bank.withdraw(func(Bank.stream()), amount) }
            }
        }

        fun depositAll(func: (InventoryItemStream) -> InventoryItemStream) {
            sequence {
                isBankOpen()
                condition {
                    val items = func(Inventory.stream()).list()
                    if (items.isEmpty()) true

                    val counts = items.groupingBy {
                        it.id()
                    }.fold(0) { acc, it -> acc + it.stack }
                    for ((id, amt) in counts) {
                        if (amt <= 0) continue
                        if (!Bank.deposit(id, amt)) false
                    }

                    true
                }
            }
        }

        fun inViewport(actorSupplier: () -> Viewable) = condition {
            actorSupplier().inViewport()
        }

        fun click(interactableSupplier: () -> Interactable) = condition {
            val interactable = interactableSupplier()
            if (!interactable.inViewport()) {
                if (interactable !is Locatable) false

                Camera.turnTo(interactable as Locatable)
            }

            interactable.click()
        }

        fun interact(interactableSupplier: () -> Interactable, action: String) = condition {
            val interactable = interactableSupplier()
            when (interactable) {
                is Item -> {
                    Inventory.open()

                    if (interactable.actions().firstOrNull() == action)
                        interactable.click()
                    else
                        interactable.interact(action)
                }

                else -> {
                    if (!interactable.inViewport()) {
                        if (interactable !is Locatable) false

                        Camera.turnTo(interactable as Locatable)
                    }

                    if (interactable is Nameable)
                        interactable.interact(action, interactable.name())
                    else
                        interactable.interact(action)
                }
            }
        }

        fun fixCamera() = succeed {
            if (Camera.pitch() < 90) Camera.pitch(true)
            if (Camera.zoom > 10) Camera.moveZoomSlider(0.0)
        }

        fun changeTab(tab: Game.Tab) = condition {
            Game.tab(tab)
        }

        fun chatting() = condition { Chat.chatting() }

        fun canContinueChat() = condition { Chat.canContinue() }

        fun clickContinue() = condition { Chat.clickContinue() }

        fun continueChat(vararg dialogs: String) = condition {
            Chat.continueChat(*dialogs)
        }

        fun completeChat(vararg dialogs: String) = condition {
            Chat.completeChat(*dialogs)
        }

        fun autoRetaliate() = condition {
            Combat.autoRetaliate()
        }

        fun autoRetaliate(value: Boolean) = condition {
            Combat.autoRetaliate(value)
        }

        fun wildernessLevel(level: Int) = condition {
            Combat.wildernessLevel() >= level
        }

        fun combatStyle(style: Combat.Style) = condition {
            Combat.style() == style
        }

        fun setCombatStyle(style: Combat.Style) = condition {
            Combat.style(style)
        }

        fun health(healthSupplier: () -> Int) = condition {
            Combat.health() >= healthSupplier()
        }

        fun prayersActive() = condition {
            Prayer.prayersActive()
        }

        fun prayerActive(effect: Prayer.Effect) = condition {
            Prayer.prayerActive(effect)
        }

        fun prayerPoints(pointsSupplier: () -> Int) = condition {
            Prayer.prayerPoints() >= pointsSupplier()
        }

        fun skillLevel(skill: Skill, levelSupplier: () -> Int) {
            Skills.level(skill) >= levelSupplier()
        }

        fun casting(spellSupplier: () -> Magic.Spell) = condition {
            spellSupplier().casting()
        }

        fun castSpell(spellSupplier: () -> Magic.Spell) = condition {
            spellSupplier().cast()
        }

        fun castSpell(spellSupplier: () -> Magic.Spell, action: String) = condition {
            spellSupplier().cast(action)
        }

        fun castSpellOn(spellSupplier: () -> Magic.Spell, interactableSupplier: () -> Interactable) {
            sequence {
                castSpell { spellSupplier() }
                click { interactableSupplier() }
            }
        }

        fun interacting() = condition {
            Players.local().interactingIndex() != -1
        }

        fun interacting(actorSupplier: () -> Actor<*>) = condition {
            Players.local().interacting() == actorSupplier()
        }

        fun alive(actorSupplier: () -> Actor<*>) = condition {
            actorSupplier().alive()
        }

        fun questComplete(quest: () -> Quests.Quest) = condition {
            quest().completed()
        }

        fun <T> isNil(supplier: () -> T?) where T : Nillable<*> =
            condition {
                val v = supplier()
                v == v?.nil()
            }


        fun hasLineOfSight(aSupplier: () -> Actor<*>, bSupplier: () -> Actor<*>, dist: Int) = condition {
            Projection.hasLineOfSight(aSupplier(), bSupplier(), dist)
        }

        fun hasLineOfSight(actorSupplier: () -> Actor<*>, dist: Int) =
            hasLineOfSight(actorSupplier, Players::local, dist)

        fun stopScript() = succeed {
            ScriptManager.stop()
        }
    }
}
