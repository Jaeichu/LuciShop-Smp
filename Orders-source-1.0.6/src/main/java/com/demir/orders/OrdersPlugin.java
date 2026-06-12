package com.demir.orders;

import com.demir.orders.command.OrdersCommand;
import com.demir.orders.data.OrderRepository;
import com.demir.orders.economy.VaultEconomyHook;
import com.demir.orders.gui.MenuListener;
import com.demir.orders.gui.MenuService;
import com.demir.orders.input.TextPromptService;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrdersPlugin extends JavaPlugin {
    private OrderRepository repository;
    private VaultEconomyHook economy;
    private MenuService menus;
    private TextPromptService prompts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        repository = new OrderRepository(this);
        repository.load();
        economy = new VaultEconomyHook(this);
        economy.hook();
        prompts = new TextPromptService(this);
        menus = new MenuService(this, repository, economy, prompts);

        getServer().getPluginManager().registerEvents(new MenuListener(menus), this);
        getServer().getPluginManager().registerEvents(prompts, this);
        getCommand("orders").setExecutor(new OrdersCommand(menus));
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> menus.expireOrders(), 20L * 60L, 20L * 60L * 10L);
    }

    @Override
    public void onDisable() {
        if (repository != null) {
            repository.save();
        }
    }
}
