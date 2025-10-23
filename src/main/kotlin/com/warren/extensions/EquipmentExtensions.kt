package com.warren.extensions

import com.warren.loadouts.EquipmentLoadout
import com.warren.loadouts.EquipmentLoadoutBuilder
import org.powbot.api.rt4.Equipment

object EquipmentExtensions {
    fun Equipment.loadout(block: EquipmentLoadoutBuilder.() -> Unit): EquipmentLoadout =
        EquipmentLoadoutBuilder().apply(block).build()
}