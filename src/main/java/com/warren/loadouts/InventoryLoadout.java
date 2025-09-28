package com.warren.loadouts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.powbot.api.rt4.Inventory;

public class InventoryLoadout implements ItemLoadout {
  private List<ItemEntry> required;

  public InventoryLoadout(List<ItemEntry> entries) {
    this.required = entries;
  }
  
  public InventoryLoadout require(ItemEntry entry) {
    required.add(entry);
    return this;
  }
  
  public static InventoryLoadout of(ItemEntry... entries) {
    return new InventoryLoadout(entries == null ? List.of() : Arrays.asList(entries));
  }

  @Override
  public Iterator<ItemEntry> iterator() {
    return required.iterator();
  }
}
