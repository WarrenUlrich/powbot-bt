package com.warren.loadouts;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.powbot.api.rt4.Item;
import org.powbot.api.rt4.stream.item.ItemStream;

public class ItemEntry {
  private Pattern pattern;
  private int minQuantity;
  private int maxQuantity;
  private boolean stackable;
  private boolean optional;

  private ItemEntry(Pattern pattern, int minQuantity, int maxQuantity, boolean stackable, boolean optional) {
    this.pattern = pattern;
    this.minQuantity = minQuantity;
    this.maxQuantity = maxQuantity;
    this.stackable = stackable;
    this.optional = optional;
  }

  public boolean matches(Item item) {
    return pattern.matcher(item.name()).matches();
  }
  
  public boolean contained(Iterable<? extends Item> items) {
    if (optional)
      return true;
    
    var quantity = 0;
    for (var item : items) {
      if (!matches(item))
        continue;
      if ((quantity += stackable ? item.stackSize() : 1) > maxQuantity)
        return false; // exceeding quantity after incrementing
    }
    return quantity >= minQuantity;
  }

  public String getPattern() {
    return pattern.toString();
  }

  public static class Builder {
    private String[] names;
    private int minQuantity = 1;
    private int maxQuantity = Integer.MAX_VALUE;
    private int minConsumableQuantity = -1;
    private int maxConsumableQuantity = -1;
    private boolean stackable = false;
    private boolean optional = false;

    public Builder names(String... names) {
      this.names = names;
      return this;
    }

    public Builder quantity(int quantity) {
      this.minQuantity = quantity;
      return this;
    }

    public Builder quantity(int minQuantity, int maxQuantity) {
      this.minQuantity = minQuantity;
      this.maxQuantity = maxQuantity;
      return this;
    }

    public Builder consumableQuantity(int min, int max) {
      this.minConsumableQuantity = min;
      this.maxConsumableQuantity = max;
      return this;
    }

    public Builder stackable(boolean value) {
      this.stackable = value;
      return this;
    }

    public Builder optional(boolean value) {
      this.optional = value;
      return this;
    }

    public ItemEntry build() {
      if (names == null || names.length == 0) {
        throw new IllegalStateException("At least one name must be provided.");
      }

      final boolean hasRange = minConsumableQuantity >= 0 &&
          maxConsumableQuantity >= minConsumableQuantity;

      final String numberAlternation = hasRange
          ? IntStream.rangeClosed(minConsumableQuantity, maxConsumableQuantity)
              .mapToObj(Integer::toString)
              .collect(Collectors.joining("|"))
          : null;

      final String body = Arrays.stream(names)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(baseName -> {
            String quoted = Pattern.quote(baseName);

            if (!hasRange) {
              // Exact literal name
              return quoted;
            }

            // If min == 0, make the charge suffix optional; else required.
            if (minConsumableQuantity == 0) {
              // optional suffix, no spaces
              return quoted + "(?:\\((" + numberAlternation + ")\\))?";
            } else {
              // required suffix, no spaces
              return quoted + "\\((" + numberAlternation + ")\\)";
            }

          })
          .collect(Collectors.joining("|"));

      // Anchor once, around the whole alternation.
      Pattern p = Pattern.compile("^(?:" + body + ")$");
      return new ItemEntry(p, minQuantity, maxQuantity, stackable, optional);
    }

  }

  public static Builder builder() {
    return new Builder();
  }
}
