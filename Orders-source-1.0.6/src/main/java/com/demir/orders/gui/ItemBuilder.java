package com.demir.orders.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();

    private ItemBuilder(Material material) {
        item = new ItemStack(material);
        meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.values());
    }

    static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    ItemBuilder name(String name) {
        meta.displayName(Component.text(name, NamedTextColor.WHITE));
        return this;
    }

    ItemBuilder name(Component name) {
        meta.displayName(name);
        return this;
    }

    ItemBuilder lore(String line) {
        lore.add(Component.text(line, NamedTextColor.GRAY));
        return this;
    }

    ItemBuilder lore(Component line) {
        lore.add(line);
        return this;
    }

    ItemBuilder blankLore() {
        lore.add(Component.empty());
        return this;
    }

    ItemBuilder greenLore(String line) {
        lore.add(Component.text(line, NamedTextColor.GREEN));
        return this;
    }

    ItemBuilder redLore(String line) {
        lore.add(Component.text(line, NamedTextColor.RED));
        return this;
    }

    ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(99, amount)));
        return this;
    }

    ItemStack build() {
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
