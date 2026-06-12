package com.demir.orders.economy;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultEconomyHook {
    private final JavaPlugin plugin;
    private Object vaultEconomy;
    private Plugin essentials;

    public VaultEconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        hookVault();
        hookEssentials();
    }

    private void hookVault() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider != null) {
                vaultEconomy = provider.getProvider();
                plugin.getLogger().info("Vault economy hooked: " + vaultEconomy.getClass().getName());
            }
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().info("Vault not found. Economy features are disabled.");
        }
    }

    private void hookEssentials() {
        Plugin found = Bukkit.getPluginManager().getPlugin("Essentials");
        if (found == null || !found.isEnabled()) {
            found = Bukkit.getPluginManager().getPlugin("EssentialsX");
        }
        if (found != null && found.isEnabled()) {
            essentials = found;
            plugin.getLogger().info("Essentials economy fallback hooked: " + found.getName());
        }
    }

    public boolean isReady() {
        return vaultEconomy != null || essentials != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (vaultEconomy != null) {
            return invokeVaultBoolean("has", player, amount);
        }
        Object user = essentialsUser(player);
        if (user == null) {
            return false;
        }
        try {
            Method canAfford = user.getClass().getMethod("canAfford", BigDecimal.class);
            return (boolean) canAfford.invoke(user, BigDecimal.valueOf(amount));
        } catch (ReflectiveOperationException ignored) {
            BigDecimal money = essentialsMoney(user);
            return money != null && money.compareTo(BigDecimal.valueOf(amount)) >= 0;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (vaultEconomy != null) {
            return vaultTransaction("withdrawPlayer", player, amount);
        }
        Object user = essentialsUser(player);
        return user != null && invokeEssentialsMoney(user, "takeMoney", amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (vaultEconomy != null) {
            return vaultTransaction("depositPlayer", player, amount);
        }
        Object user = essentialsUser(player);
        return user != null && invokeEssentialsMoney(user, "giveMoney", amount);
    }

    private boolean vaultTransaction(String methodName, OfflinePlayer player, double amount) {
        if (vaultEconomy == null) {
            return false;
        }
        try {
            Method method = vaultEconomy.getClass().getMethod(methodName, OfflinePlayer.class, double.class);
            Object response = method.invoke(vaultEconomy, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            return (boolean) success.invoke(response);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault transaction failed: " + exception.getMessage());
            return false;
        }
    }

    private boolean invokeVaultBoolean(String methodName, OfflinePlayer player, double amount) {
        if (vaultEconomy == null) {
            return false;
        }
        try {
            Method method = vaultEconomy.getClass().getMethod(methodName, OfflinePlayer.class, double.class);
            return (boolean) method.invoke(vaultEconomy, player, amount);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault check failed: " + exception.getMessage());
            return false;
        }
    }

    private Object essentialsUser(OfflinePlayer player) {
        if (essentials == null) {
            return null;
        }
        try {
            Method getUser = essentials.getClass().getMethod("getUser", OfflinePlayer.class);
            return getUser.invoke(essentials, player);
        } catch (ReflectiveOperationException first) {
            try {
                Method getUser = essentials.getClass().getMethod("getUser", String.class);
                return getUser.invoke(essentials, player.getName());
            } catch (ReflectiveOperationException second) {
                plugin.getLogger().warning("Essentials user lookup failed: " + second.getMessage());
                return null;
            }
        }
    }

    private BigDecimal essentialsMoney(Object user) {
        try {
            Method getMoney = user.getClass().getMethod("getMoney");
            Object value = getMoney.invoke(user);
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(String.valueOf(value));
        } catch (ReflectiveOperationException | NumberFormatException exception) {
            plugin.getLogger().warning("Essentials money check failed: " + exception.getMessage());
            return null;
        }
    }

    private boolean invokeEssentialsMoney(Object user, String methodName, double amount) {
        try {
            Method method = user.getClass().getMethod(methodName, BigDecimal.class);
            method.invoke(user, BigDecimal.valueOf(amount));
            return true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Essentials transaction failed: " + exception.getMessage());
            return false;
        }
    }
}
