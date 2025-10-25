package com.warren.bt

import com.warren.extensions.ActorExtensions.hasLineOfSightTo
import com.warren.loadouts.EquipmentLoadout
import com.warren.loadouts.InventoryLoadout
import com.warren.loadouts.ItemEntry
import com.warren.loadouts.ItemLoadout
import org.powbot.api.Area
import org.powbot.api.Condition
import org.powbot.api.Interactable
import org.powbot.api.Locatable
import org.powbot.api.Nameable
import org.powbot.api.Nillable
import org.powbot.api.Notifications
import org.powbot.api.Tile
import org.powbot.api.Viewable
import org.powbot.api.rt4.Actor
import org.powbot.api.rt4.Bank
import org.powbot.api.rt4.Camera
import org.powbot.api.rt4.Chat
import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Equipment
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

        @PublishedApi
        internal fun attachPublished(node: Node) {
            attach(node) // protected in the superclass; OK to call from here
        }

        inline fun <reified T : Any> sleepUntilEvent(
            timeoutMs: Long,
            noinline predicate: (T) -> Boolean = { true }
        ) = attachPublished(SleepUntilEvent(T::class.java, timeoutMs, predicate))

        fun <T : Any> sleepUntilEvent(
            eventClass: Class<T>,
            timeoutMs: Long,
            predicate: (T) -> Boolean = { true }
        ) = attachPublished(SleepUntilEvent(eventClass, timeoutMs, predicate))


        fun info(messageSupplier: () -> String) = succeed {
            ScriptManager.script()!!.logger.info(messageSupplier())
        }

        fun debug(messageSupplier: () -> String) = succeed {
            ScriptManager.script()!!.logger.debug(messageSupplier())
        }

        fun warn(messageSupplier: () -> String) = succeed {
            ScriptManager.script()!!.logger.warn(messageSupplier())
        }

        fun error(messageSupplier: () -> String) = succeed {
            ScriptManager.script()!!.logger.error(messageSupplier())
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
            Movement.step(locatableSupplier())
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

        fun openBank() = selector {
            isBankOpen()
            condition { Bank.open() }
        }

        fun bankContains(func: (BankItemStream) -> BankItemStream) = condition {
            func(Bank.stream()).isNotEmpty()
        }

        fun withdraw(func: (BankItemStream) -> Item, amount: Int) {
            sequence {
                isBankOpen()
                condition { Bank.currentTab(0) }
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

        fun bankContainsLoadout(func: () -> InventoryLoadout) = sequence {
            isBankOpen()
            condition {
                val loadout = func()

                fun qty(entry: ItemEntry, items: Iterable<Item>): Int {
                    var q = 0
                    for (i in items) {
                        if (!entry.matches(i)) continue
                        q += if (entry.stackable) i.stackSize() else 1
                    }
                    return q
                }

                val invEquip = Inventory.stream().list() + org.powbot.api.rt4.Equipment.stream().list()
                val bankItems = Bank.stream().list()

                for (entry in loadout.entries) {
                    if (entry.optional) continue  // optional entries never block containment
                    val have = qty(entry, invEquip)
                    val needed = (entry.minQuantity - have).coerceAtLeast(0)
                    if (needed == 0) continue

                    val inBank = qty(entry, bankItems)
                    if (inBank < needed) return@condition false
                }
                true
            }
        }

        fun withdrawLoadout(func: () -> ItemLoadout) = condition {
            if (!Bank.opened()) false
            if (!Bank.currentTab(0)) false

            val loadout = func()

            fun qty(entry: com.warren.loadouts.ItemEntry, items: Iterable<Item>): Int {
                var q = 0
                for (i in items) {
                    if (!entry.matches(i)) continue
                    q += if (entry.stackable) i.stackSize() else 1
                }
                return q
            }

            var ok = true

            for (entry in loadout.entries) {
                if (entry.optional) continue

                val invEquip = Inventory.stream().list() + Equipment.stream().list()
                val have = qty(entry, invEquip)
                val need = (entry.maxQuantity - have).coerceAtLeast(0)
                if (need <= 0) continue

                val bankItem = entry.get(Bank.stream().list())
                if (bankItem == Item.Nil) {
                    ok = false
                    continue
                }

                val success = Bank.withdraw(bankItem.name(), need)
                if (!success) {
                    ok = false
                    continue
                }

                Condition.wait(
                    {
                        qty(entry, Inventory.stream().list() + Equipment.stream().list()) >= entry.minQuantity
                    },
                    100, 20
                )
            }

            ok && Condition.wait({ loadout.getMissing().isEmpty() }, 100, 20)
        }


        fun hasLoadout(loadoutSupplier: () -> ItemLoadout) = condition {
            loadoutSupplier().getMissing().isEmpty()
        }

        fun loadoutEquipped(loadoutSupplier: () -> EquipmentLoadout) = condition {
            loadoutSupplier().getMissing(Equipment.stream().list()).isEmpty()
        }

        fun equipLoadout(loadoutSupplier: () -> EquipmentLoadout) = condition {
            loadoutSupplier().equip()
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

        fun interact(action: String, interactableSupplier: () -> Interactable) = condition {
            val interactable = interactableSupplier()

            when (interactable) {
                is Item -> {
                    Inventory.open()
                    if (interactable.actions().firstOrNull() == action) {
                        interactable.click()
                    } else {
                        interactable.interact(action)
                    }
                }

                else -> {
                    if (!interactable.inViewport()) {
                        val loc = interactable as? Locatable ?: return@condition false
                        Camera.turnTo(loc)
                    }

                    if (interactable is Nameable) {
                        interactable.interact(action, interactable.name())
                    } else {
                        interactable.interact(action)
                    }
                }
            }
        }

        // backwards compatibility
        fun interact(interactableSupplier: () -> Interactable, action: String) =
            interact(action, interactableSupplier)

        fun attack(interactableSupplier: () -> Interactable) = interact(interactableSupplier, "Attack")

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

        fun prayerActive(func: () -> Prayer.Effect) = condition {
            Prayer.prayerActive(func())
        }

        fun prayerPoints(pointsSupplier: () -> Int) = condition {
            Prayer.prayerPoints() >= pointsSupplier()
        }

        fun togglePrayer(func: () -> Prayer.Effect) = condition {
            val effect = func()
            Prayer.prayer(effect, !Prayer.prayerActive(effect))
        }

        fun disablePrayers() = condition {
            val activePrayers = Prayer.activePrayers()
            if (activePrayers.isEmpty())
                true

            var anyFailed = false
            for (active in Prayer.activePrayers()) {
                if (!Prayer.prayer(active, false)) {
                    anyFailed = true
                }
            }
            anyFailed
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
            aSupplier().hasLineOfSightTo(bSupplier(), dist)
        }

        fun hasLineOfSight(actorSupplier: () -> Actor<*>, dist: Int) =
            hasLineOfSight(actorSupplier, Players::local, dist)


        fun stopScript() = succeed {
            ScriptManager.stop()
        }

        fun stopScript(messageSupplier: () -> String) = succeed {
            val message = messageSupplier()
            Notifications.showNotification(message)
            ScriptManager.script()!!.logger!!.info(messageSupplier())
            ScriptManager.stop()
        }

        fun notification(messageSupplier: () -> String) = succeed {
            Notifications.showNotification(messageSupplier())
        }
    }
}
