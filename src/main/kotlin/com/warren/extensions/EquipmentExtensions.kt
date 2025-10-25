package com.warren.extensions

import com.warren.loadouts.EquipmentLoadout
import com.warren.loadouts.EquipmentLoadoutBuilder
import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Item

object EquipmentExtensions {
    fun Equipment.loadout(block: EquipmentLoadoutBuilder.() -> Unit): EquipmentLoadout =
        EquipmentLoadoutBuilder().apply(block).build()

    fun Equipment.equippedLoadout(): EquipmentLoadout {
        return loadout {
            for (s in Equipment.Slot.entries) {
                val equippedItem = Equipment.itemAt(s)
                if (equippedItem == Item.Nil)
                    continue

                slot(s) {
                    names(equippedItem.name())
                    if (s == Equipment.Slot.QUIVER)
                        quantity(1..equippedItem.stackSize())
                }
            }
        }
    }
}