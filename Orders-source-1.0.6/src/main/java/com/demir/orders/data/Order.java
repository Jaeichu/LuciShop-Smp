package com.demir.orders.data;

import java.util.UUID;
import org.bukkit.Material;

public final class Order {
    private final UUID id;
    private final UUID owner;
    private final Material material;
    private final int amount;
    private final double pricePerItem;
    private final long createdAt;
    private final long expiresAt;
    private int delivered;
    private int claimed;
    private boolean closed;
    private boolean refunded;

    public Order(UUID id, UUID owner, Material material, int amount, double pricePerItem, int delivered, int claimed, long createdAt, long expiresAt, boolean closed, boolean refunded) {
        this.id = id;
        this.owner = owner;
        this.material = material;
        this.amount = amount;
        this.pricePerItem = pricePerItem;
        this.delivered = delivered;
        this.claimed = claimed;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.closed = closed;
        this.refunded = refunded;
    }

    public UUID id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public Material material() {
        return material;
    }

    public int amount() {
        return amount;
    }

    public double pricePerItem() {
        return pricePerItem;
    }

    public int delivered() {
        return delivered;
    }

    public int claimed() {
        return claimed;
    }

    public int claimable() {
        return Math.max(0, delivered - claimed);
    }

    public int remaining() {
        return closed ? 0 : Math.max(0, amount - delivered);
    }

    public int undelivered() {
        return Math.max(0, amount - delivered);
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isRefunded() {
        return refunded;
    }

    public double refundAmount() {
        return undelivered() * pricePerItem;
    }

    public double totalPaid() {
        return delivered * pricePerItem;
    }

    public boolean isComplete() {
        return closed || undelivered() <= 0;
    }

    public void addDelivered(int count) {
        delivered = Math.min(amount, delivered + count);
    }

    public void addClaimed(int count) {
        claimed = Math.min(delivered, claimed + count);
    }

    public void close() {
        closed = true;
    }

    public void markRefunded() {
        refunded = true;
    }
}
