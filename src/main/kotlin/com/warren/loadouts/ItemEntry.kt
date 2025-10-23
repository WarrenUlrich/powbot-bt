package com.warren.loadouts

import org.powbot.api.rt4.Item

data class ItemEntry(
    val pattern: Regex,
    val minQuantity: Int,
    val maxQuantity: Int,
    val stackable: Boolean,
    val optional: Boolean
) {
    fun matches(item: Item): Boolean = pattern.matches(item.name())

    fun contained(items: Iterable<Item>): Boolean {
        if (optional) return true

        var quantity = 0
        for (item in items) {
            if (!matches(item)) continue
            quantity += if (stackable) item.stackSize() else 1
            if (quantity > maxQuantity) return false // exceeding quantity after incrementing
        }
        return quantity >= minQuantity
    }

    fun get(items: Iterable<Item>): Item {
        for (item in items) {
            if (matches(item))
                return item
        }

        return Item.Nil
    }
}

class ItemEntryBuilder {
    private var names: List<String> = emptyList()
    private var minQuantity: Int = 1
    private var maxQuantity: Int = 1
    private var minConsumableQuantity: Int = -1
    private var maxConsumableQuantity: Int = -1
    private var stackable: Boolean = false
    private var optional: Boolean = false

    fun names(vararg names: String) {
        this.names = names.toList()
    }

    fun quantity(qty: Int) {
        this.minQuantity = qty
    }

    fun quantity(range: IntRange) {
        this.minQuantity = range.first
        this.maxQuantity = range.last
    }

    fun consumableQuantity(range: IntRange) {
        this.minConsumableQuantity = range.first
        this.maxConsumableQuantity = range.last
    }

    fun stackable(value: Boolean = true) {
        this.stackable = value
    }

    fun optional(value: Boolean = true) {
        this.optional = value
    }

    fun build(): ItemEntry {
        require(names.isNotEmpty()) { "At least one name must be provided." }

        val hasRange = minConsumableQuantity >= 0 &&
                maxConsumableQuantity >= minConsumableQuantity

        val numberAlternation = if (hasRange) {
            (minConsumableQuantity..maxConsumableQuantity).joinToString("|")
        } else null

        val body = names.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { baseName ->
                val quoted = Regex.escape(baseName)
                if (!hasRange) {
                    // Exact literal name
                    quoted
                } else {
                    // If min == 0, make the charge suffix optional; else required. No spaces.
                    if (minConsumableQuantity == 0) {
                        "$quoted(?:\\((${numberAlternation})\\))?"
                    } else {
                        "$quoted\\((${numberAlternation})\\)"
                    }
                }
            }
            .joinToString("|")

        val pattern = Regex("^(?:$body)$")

        return ItemEntry(
            pattern = pattern,
            minQuantity = minQuantity,
            maxQuantity = maxQuantity,
            stackable = stackable,
            optional = optional
        )
    }
}

fun itemEntry(block: ItemEntryBuilder.() -> Unit): ItemEntry =
    ItemEntryBuilder().apply(block).build()
