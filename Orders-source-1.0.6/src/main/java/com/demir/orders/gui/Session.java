package com.demir.orders.gui;

import java.util.UUID;
import org.bukkit.Material;

final class Session {
    ViewKind view = ViewKind.MAIN;
    int page = 0;
    int selectPage = 0;
    OrderSort orderSort = OrderSort.RECENTLY_LISTED;
    OrderSort selectSort = OrderSort.ALPHABETICAL_A_Z;
    OrderFilter filter = OrderFilter.ALL;
    String search = "";
    boolean itemBrowserMode = false;
    Material selectedMaterial;
    int amount = 64;
    double pricePerItem = 1.0D;
    UUID deliveryOrder;
    int pendingDeliveryAmount = 0;
    boolean movingToConfirm = false;
    boolean confirmingDelivery = false;
}
