package com.warren.loadouts

import org.powbot.api.rt4.Equipment
import org.powbot.api.rt4.Inventory
import org.powbot.api.rt4.Item

interface ItemLoadout : Iterable<ItemEntry> {
    val entries: List<ItemEntry>

    override fun iterator(): Iterator<ItemEntry> = entries.iterator()

    fun getMissing(): List<ItemEntry> {
        return getMissing(Inventory.stream().list() + Equipment.stream().list())
    }

    fun getMissing(items: Iterable<Item>): List<ItemEntry> {
        val missing = ArrayList<ItemEntry>()
        for (entry in this) {
            if (!entry.contained(items)) missing += entry
        }
        return missing
    }

    fun getInvalid(items: Iterable<Item>): List<Item> {
        val invalid = ArrayList<Item>()
        outer@ for (item in items) {
            for (entry in this) {
                if (entry.matches(item)) continue@outer
            }
            invalid += item
        }
        return invalid
    }
}