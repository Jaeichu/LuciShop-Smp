package com.demir.orders.command;

import com.demir.orders.gui.MenuService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class OrdersCommand implements CommandExecutor {
    private final MenuService menus;

    public OrdersCommand(MenuService menus) {
        this.menus = menus;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("orders.use")) {
            player.sendMessage("No permission.");
            return true;
        }
        menus.openMainFresh(player);
        return true;
    }
}
