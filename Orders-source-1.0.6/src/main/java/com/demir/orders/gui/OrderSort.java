package com.demir.orders.gui;

public enum OrderSort {
    MOST_PAID("Most paid"),
    MOST_DELIVERED("Most delivered"),
    RECENTLY_LISTED("Recently listed"),
    MOST_MONEY_PER_ITEM("Most money per item"),
    ALPHABETICAL_A_Z("Alphabetical A-Z"),
    ALPHABETICAL_Z_A("Alphabetical Z-A");

    private final String label;

    OrderSort(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
