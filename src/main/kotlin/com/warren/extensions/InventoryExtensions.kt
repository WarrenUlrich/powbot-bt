package com.warren.extensions

import com.warren.loadouts.InventoryLoadout
import com.warren.loadouts.InventoryLoadoutBuilder
import org.powbot.api.rt4.Inventory

object InventoryExtensions {
    fun Inventory.loadout(block: InventoryLoadoutBuilder.() -> Unit): InventoryLoadout =
        InventoryLoadoutBuilder().apply(block).build()
}