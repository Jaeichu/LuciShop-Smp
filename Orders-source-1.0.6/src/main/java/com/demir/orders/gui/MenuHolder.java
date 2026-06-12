package com.demir.orders.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

final class MenuHolder implements InventoryHolder {
    private final ViewKind kind;

    MenuHolder(ViewKind kind) {
        this.kind = kind;
    }

    ViewKind kind() {
        return kind;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("MenuHolder does not store an inventory.");
    }
}
