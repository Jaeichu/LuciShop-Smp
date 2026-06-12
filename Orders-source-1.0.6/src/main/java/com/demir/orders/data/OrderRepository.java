package com.demir.orders.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrderRepository {
    public static final long DEFAULT_EXPIRE_MILLIS = 30L * 24L * 60L * 60L * 1000L;

    private final JavaPlugin plugin;
    private final File file;
    private final List<Order> orders = new ArrayList<>();

    public OrderRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "orders.yml");
    }

    public void load() {
        orders.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("orders");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(section.getString(key + ".material", ""));
            if (material == null || material.isAir()) {
                continue;
            }
            orders.add(new Order(
                    UUID.fromString(key),
                    UUID.fromString(section.getString(key + ".owner")),
                    material,
                    section.getInt(key + ".amount"),
                    section.getDouble(key + ".price-per-item"),
                    section.getInt(key + ".delivered"),
                    section.getInt(key + ".claimed"),
                    section.getLong(key + ".created-at"),
                    section.getLong(key + ".expires-at", section.getLong(key + ".created-at") + DEFAULT_EXPIRE_MILLIS),
                    section.getBoolean(key + ".closed"),
                    section.getBoolean(key + ".refunded")
            ));
        }
        orders.sort(Comparator.comparingLong(Order::createdAt).reversed());
    }

    public void save() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        for (Order order : orders) {
            String path = "orders." + order.id();
            yaml.set(path + ".owner", order.owner().toString());
            yaml.set(path + ".material", order.material().name());
            yaml.set(path + ".amount", order.amount());
            yaml.set(path + ".price-per-item", order.pricePerItem());
            yaml.set(path + ".delivered", order.delivered());
            yaml.set(path + ".claimed", order.claimed());
            yaml.set(path + ".created-at", order.createdAt());
            yaml.set(path + ".expires-at", order.expiresAt());
            yaml.set(path + ".closed", order.isClosed());
            yaml.set(path + ".refunded", order.isRefunded());
        }
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save orders.yml: " + exception.getMessage());
        }
    }

    public List<Order> allOpen() {
        return orders.stream().filter(order -> !order.isComplete()).toList();
    }

    public List<Order> byOwner(UUID owner) {
        return orders.stream().filter(order -> order.owner().equals(owner)).toList();
    }

    public long openCountByOwner(UUID owner) {
        return orders.stream()
                .filter(order -> order.owner().equals(owner))
                .filter(order -> !order.isComplete())
                .count();
    }

    public List<Order> expiredOpen(long now) {
        return orders.stream()
                .filter(order -> !order.isComplete())
                .filter(order -> order.expiresAt() <= now)
                .toList();
    }

    public Optional<Order> find(UUID id) {
        return orders.stream().filter(order -> order.id().equals(id)).findFirst();
    }

    public Order create(UUID owner, Material material, int amount, double pricePerItem) {
        long now = System.currentTimeMillis();
        long expireMillis = plugin.getConfig().getLong("settings.order-expire-days", 30L) * 24L * 60L * 60L * 1000L;
        Order order = new Order(UUID.randomUUID(), owner, material, amount, pricePerItem, 0, 0, now, now + expireMillis, false, false);
        orders.add(order);
        save();
        return order;
    }

    public void delete(UUID id) {
        orders.removeIf(order -> order.id().equals(id));
        save();
    }
}
