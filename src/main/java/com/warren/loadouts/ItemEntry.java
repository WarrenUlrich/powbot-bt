package com.warren.loadouts;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.powbot.api.rt4.stream.item.ItemStream;

public abstract class ItemEntry {
  private Pattern pattern;
  private int minQuantity;
  private int maxQuantity;
  private boolean stackable;

  // Let Builder construct an instance.
  private ItemEntry(Pattern pattern, int minQuantity, int maxQuantity, boolean stackable) {
    this.pattern = pattern;
    this.minQuantity = minQuantity;
    this.maxQuantity = maxQuantity;
    this.stackable = stackable;
  }

  public boolean contained(ItemStream<?> itemStream) {
    var quantity = 0;
    for (var item : itemStream) {
      if (!pattern.matcher(item.name()).matches())
        continue;
      if ((quantity += stackable ? item.stackSize() : 1) > maxQuantity)
        return false; // exceeding quantity after incrementing
    }
    return quantity >= minQuantity;
  }

  public static class Builder {
    private String[] names;
    private int minQuantity = 1;
    private int maxQuantity = Integer.MAX_VALUE;
    private int minConsumableQuantity = -1;
    private int maxConsumableQuantity = -1;
    private boolean stackable = false;

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

    public ItemEntry build() {
      if (names == null || names.length == 0) {
        throw new IllegalStateException("At least one name must be provided.");
      }

      final boolean hasRange = minConsumableQuantity >= 0 &&
          maxConsumableQuantity >= minConsumableQuantity;

      // Build "(n1|n2|...)" once, depending on the range.
      final String numberAlternation;
      if (hasRange) {
        numberAlternation = IntStream
            .rangeClosed(minConsumableQuantity, maxConsumableQuantity)
            .mapToObj(Integer::toString)
            .collect(Collectors.joining("|"));
      } else {
        numberAlternation = null; // no consumable suffix handling
      }

      // For each name, create an anchored alternative.
      String alternation = Arrays.stream(names)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(baseName -> {
            boolean nameHasParens = baseName.contains("(") && baseName.contains(")");
            String quoted = Pattern.quote(baseName);

            if (!hasRange || nameHasParens) {
              // No range specified OR explicit parens present => exact match of the literal
              // name.
              return "^" + quoted + "$";
            }

            // Range specified. If min==0 -> suffix is optional; otherwise suffix is
            // required.
            if (minConsumableQuantity == 0) {
              // Allow uncharged (no suffix) OR any (0..max) in parentheses.
              return "^" + quoted + "(?:\\s*\\((" + numberAlternation + ")\\))?$";
            } else {
              // Require a charge in (min..max).
              return "^" + quoted + "\\s*\\((" + numberAlternation + ")\\)$";
            }
          })
          .collect(Collectors.joining("|"));

      Pattern p = Pattern.compile("(?:" + alternation + ")");
      return new ItemEntry(p, minQuantity, maxQuantity, stackable) {
      };
    }
  }
}
