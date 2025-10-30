package com.warren.extensions

import org.powbot.api.rt4.Prayer

object PrayersExtensions {
    fun Prayer.protectionPrayers(): List<Prayer.Effect> {
        return listOf(
            Prayer.Effect.PROTECT_FROM_MAGIC,
            Prayer.Effect.PROTECT_FROM_MISSILES,
            Prayer.Effect.PROTECT_FROM_MELEE
        )
    }

    fun Prayer.currentProtection(): Prayer.Effect? {
        for (prot in protectionPrayers())
            if (Prayer.prayerActive(prot))
                return prot

        return null
    }
}