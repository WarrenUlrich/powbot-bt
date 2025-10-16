package com.warren.util;

import org.powbot.api.Tile;
import org.powbot.api.rt4.Actor;
import org.powbot.dax.engine.local.CollisionFlags;

import java.util.function.BiPredicate;
import java.util.function.IntPredicate;

public final class Projection {
  private Projection() {
    // prevent instantiation
  }

  public static boolean hasLineOfSight(Actor<?> source, Actor<?> target, int maxTiles) {
    if (source == null || target == null)
      return false;

    Tile src = source.trueTile();
    Tile dst = target.trueTile();

    return hasLineOfSight(src, dst, maxTiles);
  }

  public static boolean hasLineOfSight(Actor<?> source, Actor<?> target) {
    return hasLineOfSight(source, target, 64);
  }

  public static boolean hasLineOfSight(Tile from, Tile to, int maxTiles) {
    if (from == null || to == null)
      return false;

    if (from.floor() != to.floor())
      return false;

    if (from.equals(to))
      return true;

    // distance cap
    int dx = to.x() - from.x();
    int dy = to.y() - from.y();
    double distance = Math.sqrt(dx * dx + dy * dy);
    if (distance > maxTiles)
      return false;

    int plane = from.floor();
    int x0 = from.x();
    int y0 = from.y();
    int x1 = to.x();
    int y1 = to.y();

    int absDx = Math.abs(dx);
    int absDy = Math.abs(dy);
    int sx = Integer.compare(x1, x0);
    int sy = Integer.compare(y1, y0);

    // --- inline helpers ---

    IntPredicate isBlockedFull = (flag) -> CollisionFlags.checkFlag(flag, CollisionFlags.SOLID)
        || CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED)
        || CollisionFlags.checkFlag(flag, CollisionFlags.CLOSED);

    BiPredicate<Integer, int[]> isBlockedDirectional = (flag, dir) -> {
      int dxStep = dir[0];
      int dyStep = dir[1];

      if (isBlockedFull.test(flag))
        return true;

      if (dxStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_EAST_WALL))
        return true;
      if (dxStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_WEST_WALL))
        return true;
      if (dyStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_NORTH_WALL))
        return true;
      if (dyStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_SOUTH_WALL))
        return true;

      if (dxStep > 0 && dyStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_NORTHEAST))
        return true;
      if (dxStep > 0 && dyStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_SOUTHEAST))
        return true;
      if (dxStep < 0 && dyStep > 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_NORTHWEST))
        return true;
      if (dxStep < 0 && dyStep < 0 && CollisionFlags.checkFlag(flag, CollisionFlags.BLOCKED_SOUTHWEST))
        return true;

      return false;
    };

    // Bresenham traversal

    int curX = x0;
    int curY = y0;

    int startFlag = from.collisionFlag();
    if (isBlockedDirectional.test(startFlag, new int[] { sx, sy })) {
      return false;
    }
    
    if (absDx > absDy) {
      int err = absDx / 2;
      while (curX != x1) {
        curX += sx;
        err -= absDy;
        if (err < 0) {
          curY += sy;
          err += absDx;
        }

        Tile t = new Tile(curX, curY, plane);
        int flag = t.collisionFlag();
        if (isBlockedDirectional.test(flag, new int[] { sx, sy }))
          return false;
      }
    } else if (absDy > absDx) {
      int err = absDy / 2;
      while (curY != y1) {
        curY += sy;
        err -= absDx;
        if (err < 0) {
          curX += sx;
          err += absDy;
        }

        Tile t = new Tile(curX, curY, plane);
        int flag = t.collisionFlag();
        if (isBlockedDirectional.test(flag, new int[] { sx, sy }))
          return false;
      }
    } else {
      // Perfect diagonal (equal delta)
      while (curX != x1 || curY != y1) {
        curX += sx;
        curY += sy;

        Tile t = new Tile(curX, curY, plane);
        int flag = t.collisionFlag();
        if (isBlockedDirectional.test(flag, new int[] { sx, sy }))
          return false;
      }
    }

    return true;
  }

  public static boolean withinAngularFOV(Actor<?> source, Actor<?> target, double fovDegrees) {
    if (source == null || target == null)
      return false;

    Tile src = source.trueTile();
    Tile dst = target.trueTile();

    double facing = (source.orientation() * 360.0 / 2048.0);
    double toTarget = Math.toDegrees(Math.atan2(dst.y() - src.y(), dst.x() - src.x()));
    double diff = Math.abs(((facing - toTarget + 540) % 360) - 180);

    return diff <= (fovDegrees / 2.0);
  }

  public static boolean canSee(Actor<?> source, Actor<?> target, double fovDegrees, int maxTiles) {
    return withinAngularFOV(source, target, fovDegrees)
        && hasLineOfSight(source, target, maxTiles);
  }

  public static boolean canSee(Actor<?> source, Actor<?> target) {
    return canSee(source, target, 90.0, 15);
  }

}
