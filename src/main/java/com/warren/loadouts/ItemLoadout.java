package com.warren.loadouts;

import java.util.ArrayList;
import java.util.List;

import org.powbot.api.rt4.Equipment;
import org.powbot.api.rt4.Item;

public interface ItemLoadout extends Iterable<ItemEntry> {
  public default List<ItemEntry> getMissing(Iterable<? extends Item> items) {
    var results = new ArrayList<ItemEntry>();
    for (var entry : this) {
      if (!entry.contained(items))
        results.add(entry);
    }

    return results;
  }

  public default List<Item> getInvalid(Iterable<? extends Item> items) {
    var results = new ArrayList<Item>();
    outer: for (var item : items) {
      for (var entry : this) {
        if (entry.matches(item)) {
          continue outer; // item is part of the loadout
        }
      }
      
      results.add(item);
    }

    return results;
  }

}