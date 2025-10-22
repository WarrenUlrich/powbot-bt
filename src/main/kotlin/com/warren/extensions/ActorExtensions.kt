package com.warren.extensions

import com.warren.extensions.TileExtensions.hasLineOfSightTo
import org.powbot.api.rt4.Actor

object ActorExtensions {
    fun Actor<*>.hasLineOfSightTo(target: Actor<*>, maxTiles: Int = 64): Boolean {
        return trueTile().hasLineOfSightTo(target.trueTile(), maxTiles)
    }
}