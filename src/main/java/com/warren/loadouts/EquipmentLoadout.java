package com.warren.loadouts;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import org.powbot.api.rt4.Equipment;
import org.powbot.mobile.rscache.loader.ItemLoader;

public class EquipmentLoadout implements ItemLoadout {
  private EnumMap<Equipment.Slot, ItemEntry> required;

  public EquipmentLoadout() {
    
  }

  public EquipmentLoadout(Map<Equipment.Slot, ItemEntry> entries) {
    if (entries != null)
      required.putAll(entries);
  }

  public EquipmentLoadout require(Equipment.Slot slot, ItemEntry entry) {
    if (slot != null && entry != null)
      required.put(slot, entry);
    return this;
  }

  @Override
  public Iterator<ItemEntry> iterator() {
    return required.values().iterator();
  }

  public boolean equipped() {
    return getMissing(Equipment.get()).isEmpty();
  }
}
