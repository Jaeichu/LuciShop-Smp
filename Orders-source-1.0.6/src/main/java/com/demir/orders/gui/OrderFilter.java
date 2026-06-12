package com.demir.orders.gui;

import java.util.Locale;
import org.bukkit.Material;

public enum OrderFilter {
    ALL("All"),
    BLOCKS("Blocks"),
    TOOLS("Tools"),
    FOOD("Food"),
    COMBAT("Combat"),
    POTIONS("Potions"),
    BOOKS("Books"),
    INGREDIENTS("Ingredients"),
    UTILITIES("Utilities");

    private final String label;

    OrderFilter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean matches(Material material) {
        String name = material.name().toLowerCase(Locale.ROOT);
        return switch (this) {
            case ALL -> true;
            case BLOCKS -> material.isBlock();
            case TOOLS -> name.endsWith("_pickaxe") || name.endsWith("_axe") || name.endsWith("_shovel") || name.endsWith("_hoe")
                    || name.equals("shears") || name.equals("fishing_rod") || name.equals("brush") || name.equals("flint_and_steel");
            case FOOD -> material.isEdible();
            case COMBAT -> name.endsWith("_sword") || name.endsWith("_helmet") || name.endsWith("_chestplate")
                    || name.endsWith("_leggings") || name.endsWith("_boots") || name.equals("bow") || name.equals("crossbow")
                    || name.equals("arrow") || name.equals("shield") || name.equals("trident") || name.equals("mace");
            case POTIONS -> name.contains("potion") || name.equals("dragon_breath");
            case BOOKS -> name.contains("book") || name.equals("paper") || name.equals("map");
            case INGREDIENTS -> name.contains("ingot") || name.contains("nugget") || name.contains("dust")
                    || name.contains("shard") || name.contains("crystal") || name.contains("gem") || name.contains("rod")
                    || name.equals("diamond") || name.equals("emerald") || name.equals("coal") || name.equals("charcoal")
                    || name.equals("lapis_lazuli") || name.equals("quartz") || name.equals("redstone");
            case UTILITIES -> !material.isBlock() && !FOOD.matches(material) && !COMBAT.matches(material)
                    && !POTIONS.matches(material) && !BOOKS.matches(material) && !INGREDIENTS.matches(material);
        };
    }
}
