package com.demir.orders.input;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class TextPromptService implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> waiting = new ConcurrentHashMap<>();

    public TextPromptService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void ask(Player player, String message, Consumer<String> callback) {
        waiting.put(player.getUniqueId(), callback);
        player.closeInventory();
        player.sendMessage(Component.text(message));
        player.sendMessage(Component.text("Iptal etmek icin cancel yaz."));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Consumer<String> callback = waiting.remove(event.getPlayer().getUniqueId());
        if (callback == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (text.equalsIgnoreCase("cancel")) {
            event.getPlayer().sendMessage(Component.text("Iptal edildi."));
            return;
        }
        event.getPlayer().getScheduler().run(plugin, task -> callback.accept(text), null);
    }
}
