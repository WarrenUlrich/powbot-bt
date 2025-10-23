// File: EquipmentLoadout.kt
package com.warren.loadouts

import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Equipment.Slot
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item

class EquipmentLoadout internal constructor(
    val slots: Map<Slot, ItemEntry>
) : ItemLoadout {

    override val entries: List<ItemEntry> = slots.values.toList()

    override fun toString(): String = "EquipmentLoadout(slots=$slots)"

    companion object {
        @JvmStatic
        fun builder(): EquipmentLoadoutBuilder = EquipmentLoadoutBuilder()
    }

    fun equip(): Boolean {
        if (equipped())
            return true

        var anyFailed = false
        val equipActions = setOf(
            "Equip",
            "Wear",
            "Wield"
        )

        Inventory.open()

        val items = Inventory.stream().list()
        for (entry in this) {
            val item = entry.get(items)
            if (item == Item.Nil)
                anyFailed = true

            if (!item.interact { mc -> equipActions.contains(mc.action) })
                anyFailed = true
        }

        return !anyFailed
    }

    fun equipped(): Boolean {
        return getMissing(Equipment.stream().list()).isEmpty()
    }
}

class EquipmentLoadoutBuilder {
    private val map = LinkedHashMap<Slot, ItemEntry>()

    fun slot(slot: Slot, block: ItemEntryBuilder.() -> Unit) {
        map[slot] = ItemEntryBuilder().apply(block).build()
    }

    fun item(slot: Slot, vararg names: String, block: ItemEntryBuilder.() -> Unit = {}) {
        map[slot] = ItemEntryBuilder().apply {
            names(*names)
            block()
        }.build()
    }

    fun set(slot: Slot, entry: ItemEntry) {
        map[slot] = entry
    }

    fun build(): EquipmentLoadout = EquipmentLoadout(map.toMap())
}
