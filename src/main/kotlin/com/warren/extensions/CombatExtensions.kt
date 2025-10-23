package com.warren.extensions

import org.powbot.api.rt4.Combat
import org.powbot.api.rt4.Magic
import org.powbot.api.rt4.VarpbitConstants
import org.powbot.api.rt4.Varpbits

object CombatExtensions {
    fun Combat.autocasting(): Magic.Spell {
        var varp = Varpbits.varpbit(VarpbitConstants.VARP_AUTOCASTING_SPELL)
        // TODO: get ids for this, probably on some git somewhere
        throw NotImplementedError("Not implemented yet")
    }
}