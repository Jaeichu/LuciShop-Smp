package com.demir.orders.gui;

import com.demir.orders.data.Order;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class MenuListener implements Listener {
    private final MenuService menus;

    public MenuListener(MenuService menus) {
        this.menus = menus;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder menu)) {
            return;
        }
        if (menu.kind() == ViewKind.DELIVER) {
            deliver(player, event);
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        Session session = menus.session(player);
        switch (menu.kind()) {
            case MAIN -> main(player, session, slot);
            case SORT -> sort(player, session, slot);
            case FILTER -> filter(player, session, slot);
            case YOUR_ITEMS -> yourItems(player, slot);
            case NEW_ORDER -> newOrder(player, slot);
            case SELECT_ITEM -> selectItem(player, session, slot);
            case DELIVER -> {
            }
            case CONFIRM_DELIVERY -> {
                if (slot == 11) {
                    menus.returnPendingDelivery(player);
                    menus.openMain(player, 0);
                }
                if (slot == 15) menus.confirmDelivery(player);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder menu)) {
            return;
        }
        Session session = menus.session(player);
        if (menu.kind() == ViewKind.DELIVER) {
            menus.returnDeliveryInventory(player, event.getInventory());
        } else if (menu.kind() == ViewKind.CONFIRM_DELIVERY && !session.confirmingDelivery) {
            menus.returnPendingDelivery(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder menu) || menu.kind() != ViewKind.DELIVER) {
            return;
        }
        Order order = currentOrder(player);
        if (order == null || event.getOldCursor().getType() != order.material()) {
            event.setCancelled(true);
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize() && slot >= 45) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void main(Player player, Session session, int slot) {
        if (slot == 45) {
            menus.openMain(player, session.page - 1);
        } else if (slot == 47) {
            menus.openSort(player, false);
        } else if (slot == 48) {
            menus.openFilter(player);
        } else if (slot == 49) {
            menus.openMain(player, session.page);
        } else if (slot == 50) {
            menus.askSearch(player, false);
        } else if (slot == 51) {
            menus.openYourItems(player);
        } else if (slot == 53) {
            menus.openMain(player, session.page + 1);
        } else {
            Order order = menus.visibleOrder(player, slot);
            if (order != null) {
                menus.openDeliver(player, order);
            }
        }
    }

    private void sort(Player player, Session session, int slot) {
        boolean select = session.itemBrowserMode;
        if (slot < 10 || slot > 13) {
            return;
        }
        if (select) {
            session.selectSort = slot == 10 ? OrderSort.ALPHABETICAL_A_Z : OrderSort.ALPHABETICAL_Z_A;
            menus.openSelectItem(player, session.selectPage);
            return;
        }
        OrderSort[] sorts = {OrderSort.MOST_PAID, OrderSort.MOST_DELIVERED, OrderSort.RECENTLY_LISTED, OrderSort.MOST_MONEY_PER_ITEM};
        session.orderSort = sorts[Math.min(sorts.length - 1, slot - 10)];
        menus.openMain(player, 0);
    }

    private void filter(Player player, Session session, int slot) {
        if (slot < 9 || slot >= 9 + OrderFilter.values().length) {
            return;
        }
        session.filter = OrderFilter.values()[slot - 9];
        if (session.itemBrowserMode) {
            menus.openSelectItem(player, 0);
        } else {
            menus.openMain(player, 0);
        }
    }

    private void yourItems(Player player, int slot) {
        if (slot == 45) {
            menus.openMainFresh(player);
        } else if (slot == 53) {
            menus.openNewOrder(player);
        } else {
            Order order = menus.visibleOrder(player, slot);
            if (order != null) {
                menus.claimYourItems(player, order);
            }
        }
    }

    private void newOrder(Player player, int slot) {
        if (slot == 10) {
            menus.cancelNewOrder(player);
        } else if (slot == 12) {
            menus.openSelectItem(player, 0);
        } else if (slot == 13) {
            menus.askAmount(player);
        } else if (slot == 14) {
            menus.askPrice(player);
        } else if (slot == 16) {
            menus.createOrder(player);
        }
    }

    private void selectItem(Player player, Session session, int slot) {
        if (slot == 45) {
            menus.openSelectItem(player, session.selectPage - 1);
        } else if (slot == 47) {
            menus.openSort(player, true);
        } else if (slot == 48) {
            menus.openFilter(player);
        } else if (slot == 49) {
            menus.openSelectItem(player, session.selectPage);
        } else if (slot == 50) {
            menus.askSearch(player, true);
        } else if (slot == 51) {
            menus.openYourItems(player);
        } else if (slot == 53) {
            menus.openSelectItem(player, session.selectPage + 1);
        } else {
            Material material = menus.visibleMaterial(player, slot);
            if (material != null) {
                session.selectedMaterial = material;
                menus.openNewOrder(player);
            }
        }
    }

    private void deliver(Player player, InventoryClickEvent event) {
        Order order = currentOrder(player);
        if (order == null) {
            event.setCancelled(true);
            menus.openMain(player, 0);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();
        if (rawSlot == 53) {
            event.setCancelled(true);
            menus.openConfirmDelivery(player, event.getView().getTopInventory());
            return;
        }
        if (rawSlot >= 45 && rawSlot < topSize) {
            event.setCancelled(true);
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot < topSize) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            if (event.getHotbarButton() >= 0) {
                ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
                if (!isEmpty(hotbar) && hotbar.getType() != order.material()) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (!isEmpty(cursor) && cursor.getType() != order.material()) {
                event.setCancelled(true);
                return;
            }
            if (!isEmpty(current) && current.getType() != order.material()) {
                event.setCancelled(true);
                return;
            }
        }
        event.setCancelled(false);
    }

    private Order currentOrder(Player player) {
        Session session = menus.session(player);
        if (session.deliveryOrder == null) {
            return null;
        }
        return menus.orderById(session.deliveryOrder);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
