package com.warren.loadouts

import org.powbot.api.rt4.Item

class InventoryLoadout internal constructor(
    override val entries: List<ItemEntry>
) : ItemLoadout {
    override fun toString(): String = "InventoryLoadout(entries=$entries)"

    companion object {
        @JvmStatic
        fun builder(): InventoryLoadoutBuilder = InventoryLoadoutBuilder()
    }
}

class InventoryLoadoutBuilder {
    private val entries = mutableListOf<ItemEntry>()

    fun entry(block: ItemEntryBuilder.() -> Unit) {
        entries += ItemEntryBuilder().apply(block).build()
    }

    fun item(vararg names: String, block: ItemEntryBuilder.() -> Unit = {}) {
        entries += ItemEntryBuilder().apply {
            names(*names)
            block()
        }.build()
    }

    fun build(): InventoryLoadout = InventoryLoadout(entries.toList())
}
