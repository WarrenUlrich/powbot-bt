package com.warren.extensions

import org.powbot.api.Area
import org.powbot.api.Tile
import org.powbot.api.rt4.Game
import org.powbot.dax.engine.local.CollisionFlags
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

object TileExtensions {
    fun Tile.region(): Int {
        return ((x() shr 6) shl 8) or (y() shr 6)
    }

    fun Tile.isLocal(): Boolean {
        val base = Game.mapOffset()
        val dx = x() - base.x()
        val dy = y() - base.y()
        return dx in 0..103 && dy in 0..103
    }

    fun Tile.local(): Tile {
        val base = Game.mapOffset()
        return if (isLocal()) this else Tile(x() - base.x(), y() - base.y(), floor())
    }

    fun Tile.toWorld(): Tile {
        return if (!isLocal()) this
        else {
            val base = Game.mapOffset()
            Tile(base.x() + x(), base.y() + y(), floor())
        }
    }

    fun Tile.hasLineOfSightTo(to: Tile?, maxTiles: Int = 32): Boolean {
        if (to == null) return false
        if (floor() != to.floor()) return false
        if (this == to) return true

        val dx = to.x() - x()
        val dy = to.y() - y()
        val distance = sqrt((dx * dx + dy * dy).toDouble())
        if (distance > maxTiles) return false

        val plane = floor()
        var curX = x()
        var curY = y()
        val absDx = abs(dx)
        val absDy = abs(dy)
        val sx = dx.sign
        val sy = dy.sign

        val isBlockedFull: (Int) -> Boolean = { flag ->
            CollisionFlags.checkFlag(flag, CollisionFlags.SOLID) ||
                    CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED) ||
                    CollisionFlags.checkFlag(flag, CollisionFlags.CLOSED)
        }

        val isBlockedDirectional: (Int, Int, Int) -> Boolean = { flag, dxStep, dyStep ->
            if (isBlockedFull(flag)) true

            when {
                dxStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_EAST_WALL) -> true
                dxStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_WEST_WALL) -> true
                dyStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_NORTH_WALL) -> true
                dyStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_SOUTH_WALL) -> true
                dxStep > 0 && dyStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_NORTHEAST) -> true
                dxStep > 0 && dyStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_SOUTHEAST) -> true
                dxStep < 0 && dyStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_NORTHWEST) -> true
                dxStep < 0 && dyStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_SOUTHWEST) -> true
                else -> false
            }
        }

        val startFlag = collisionFlag()
        if (isBlockedDirectional(startFlag, sx, sy)) return false

        // Bresenham line traversal
        if (absDx > absDy) {
            var err = absDx / 2
            while (curX != to.x()) {
                curX += sx
                err -= absDy
                if (err < 0) {
                    curY += sy
                    err += absDx
                }

                val flag = Tile(curX, curY, plane).collisionFlag()
                if (isBlockedDirectional(flag, sx, sy)) return false
            }
        } else if (absDy > absDx) {
            var err = absDy / 2
            while (curY != to.y()) {
                curY += sy
                err -= absDx
                if (err < 0) {
                    curX += sx
                    err += absDy
                }

                val flag = Tile(curX, curY, plane).collisionFlag()
                if (isBlockedDirectional(flag, sx, sy)) return false
            }
        } else {
            // Perfect diagonal
            while (curX != to.x() || curY != to.y()) {
                curX += sx
                curY += sy

                val flag = Tile(curX, curY, plane).collisionFlag()
                if (isBlockedDirectional(flag, sx, sy)) return false
            }
        }

        return true
    }

    fun Tile.surroundingArea(radius: Int): Area {
        require(radius >= 0) { "radius must be >= 0" }

        val x1 = x - radius
        val y1 = y - radius
        val x2 = x + radius
        val y2 = y + radius

        val minX = minOf(x1, x2)
        val minY = minOf(y1, y2)
        val maxX = maxOf(x1, x2)
        val maxY = maxOf(y1, y2)

        // if Tile has a plane field, keep it the same (adjust to your Tile ctor)
        return Area(Tile(minX, minY, floor), Tile(maxX, maxY, floor))
    }

}