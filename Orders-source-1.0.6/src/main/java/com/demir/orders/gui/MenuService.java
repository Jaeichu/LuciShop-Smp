package com.demir.orders.gui;

import com.demir.orders.data.Order;
import com.demir.orders.data.OrderRepository;
import com.demir.orders.economy.VaultEconomyHook;
import com.demir.orders.input.TextPromptService;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MenuService {
    private static final int[] CONTENT = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final Plugin plugin;
    private final OrderRepository repository;
    private final VaultEconomyHook economy;
    private final TextPromptService prompts;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Map<Integer, Order>> visibleOrders = new HashMap<>();
    private final Map<UUID, Map<Integer, Material>> visibleMaterials = new HashMap<>();

    public MenuService(Plugin plugin, OrderRepository repository, VaultEconomyHook economy, TextPromptService prompts) {
        this.plugin = plugin;
        this.repository = repository;
        this.economy = economy;
        this.prompts = prompts;
    }

    public Session session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), id -> new Session());
    }

    public void openMainFresh(Player player) {
        Session session = session(player);
        resetBrowsing(session);
        openMain(player, 0);
    }

    public void openMain(Player player, int page) {
        expireOrders();
        Session session = session(player);
        session.view = ViewKind.MAIN;
        session.itemBrowserMode = false;
        session.page = Math.max(0, page);
        List<Order> orders = filteredOrders(session);
        int totalPages = pages(orders.size());
        session.page = Math.min(session.page, totalPages - 1);

        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.MAIN), 54, Component.text("ORDERS (" + (session.page + 1) + "/" + totalPages + ")"));
        visibleOrders.put(player.getUniqueId(), new HashMap<>());
        int start = session.page * CONTENT.length;
        for (int i = 0; i < CONTENT.length && start + i < orders.size(); i++) {
            Order order = orders.get(start + i);
            inv.setItem(CONTENT[i], orderIcon(order));
            visibleOrders.get(player.getUniqueId()).put(CONTENT[i], order);
        }
        controls(inv, session);
        player.openInventory(inv);
    }

    public void openSort(Player player, boolean selectMode) {
        Session session = session(player);
        session.view = ViewKind.SORT;
        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.SORT), 27, Component.text("ORDERS -> Sort"));
        OrderSort[] sorts = selectMode
                ? new OrderSort[] {OrderSort.ALPHABETICAL_A_Z, OrderSort.ALPHABETICAL_Z_A}
                : new OrderSort[] {OrderSort.MOST_PAID, OrderSort.MOST_DELIVERED, OrderSort.RECENTLY_LISTED, OrderSort.MOST_MONEY_PER_ITEM};
        for (int i = 0; i < sorts.length; i++) {
            inv.setItem(10 + i, ItemBuilder.of(Material.CAULDRON).name(sorts[i].label()).greenLore("Click to select").build());
        }
        player.openInventory(inv);
    }

    public void openFilter(Player player) {
        session(player).view = ViewKind.FILTER;
        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.FILTER), 27, Component.text("ORDERS -> Filter"));
        OrderFilter[] filters = OrderFilter.values();
        for (int i = 0; i < filters.length; i++) {
            inv.setItem(9 + i, ItemBuilder.of(Material.HOPPER).name(filters[i].label()).greenLore("Click to select").build());
        }
        player.openInventory(inv);
    }

    public void openYourItems(Player player) {
        expireOrders();
        Session session = session(player);
        session.view = ViewKind.YOUR_ITEMS;
        session.itemBrowserMode = false;
        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.YOUR_ITEMS), 54, Component.text("ORDERS -> Your Items"));
        List<Order> orders = repository.byOwner(player.getUniqueId()).stream()
                .filter(order -> !order.isComplete() || order.claimable() > 0)
                .sorted(Comparator.comparingLong(Order::createdAt).reversed())
                .toList();
        visibleOrders.put(player.getUniqueId(), new HashMap<>());
        for (int i = 0; i < Math.min(CONTENT.length, orders.size()); i++) {
            Order order = orders.get(i);
            inv.setItem(CONTENT[i], orderIcon(order));
            visibleOrders.get(player.getUniqueId()).put(CONTENT[i], order);
        }
        inv.setItem(53, ItemBuilder.of(Material.PAPER).name("New Order").greenLore("Click to create").build());
        inv.setItem(45, ItemBuilder.of(Material.ARROW).name("Back").build());
        player.openInventory(inv);
    }

    public void openNewOrder(Player player) {
        Session session = session(player);
        session.view = ViewKind.NEW_ORDER;
        session.itemBrowserMode = false;
        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.NEW_ORDER), 27, Component.text("ORDERS -> New Order"));
        inv.setItem(10, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).name("Cancel").redLore("Close this order").build());
        inv.setItem(12, ItemBuilder.of(session.selectedMaterial == null ? Material.STONE : session.selectedMaterial).name("Item").lore("Click to choose").build());
        inv.setItem(13, ItemBuilder.of(Material.CHEST).name("Amount").lore(String.valueOf(session.amount)).greenLore("Click to edit").build());
        inv.setItem(14, ItemBuilder.of(Material.EMERALD).name("Money per item").lore(MONEY.format(session.pricePerItem)).greenLore("Click to edit").build());
        inv.setItem(16, ItemBuilder.of(Material.LIME_STAINED_GLASS_PANE).name("Open Order").greenLore("Create this order").build());
        player.openInventory(inv);
    }

    public void openSelectItem(Player player, int page) {
        Session session = session(player);
        session.view = ViewKind.SELECT_ITEM;
        session.itemBrowserMode = true;
        session.selectPage = Math.max(0, page);
        List<Material> materials = selectableMaterials(session);
        int totalPages = pages(materials.size());
        session.selectPage = Math.min(session.selectPage, totalPages - 1);
        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.SELECT_ITEM), 54, Component.text("ORDERS -> Select Item (" + (session.selectPage + 1) + "/" + totalPages + ")"));
        visibleMaterials.put(player.getUniqueId(), new HashMap<>());
        int start = session.selectPage * CONTENT.length;
        for (int i = 0; i < CONTENT.length && start + i < materials.size(); i++) {
            Material material = materials.get(start + i);
            inv.setItem(CONTENT[i], ItemBuilder.of(material).name(clean(material)).build());
            visibleMaterials.get(player.getUniqueId()).put(CONTENT[i], material);
        }
        controls(inv, session);
        player.openInventory(inv);
    }

    public void openDeliver(Player player, Order order) {
        expireOrders();
        if (order.isComplete()) {
            player.sendMessage(Component.text("Bu order artik kapali."));
            openMain(player, 0);
            return;
        }
        Session session = session(player);
        session.view = ViewKind.DELIVER;
        session.itemBrowserMode = false;
        session.deliveryOrder = order.id();
        session.pendingDeliveryAmount = 0;
        session.movingToConfirm = false;
        session.confirmingDelivery = false;
        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.DELIVER), 54, Component.text("ORDERS -> Deliver Items"));
        inv.setItem(53, ItemBuilder.of(Material.PAPER).name("Continue").greenLore("Confirm delivery").build());
        player.openInventory(inv);
    }

    public void openConfirmDelivery(Player player, Inventory deliveryInventory) {
        Session session = session(player);
        Order order = currentDelivery(session);
        if (order == null) {
            openMain(player, 0);
            return;
        }
        int amount = countDeliveryInventory(deliveryInventory, order.material());
        if (amount <= 0) {
            player.sendMessage(Component.text("Teslim etmek istedigin itemleri ust kisma koy."));
            return;
        }
        if (amount > order.remaining()) {
            player.sendMessage(Component.text("Bu order icin kalan miktardan fazla item koydun."));
            return;
        }
        clearDeliveryInventory(deliveryInventory);
        session.pendingDeliveryAmount = amount;
        session.movingToConfirm = true;
        session.view = ViewKind.CONFIRM_DELIVERY;
        session.itemBrowserMode = false;
        Inventory inv = Bukkit.createInventory(new MenuHolder(ViewKind.CONFIRM_DELIVERY), 27, Component.text("ORDERS -> Confirm Delivery"));
        inv.setItem(11, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).name("Cancel").build());
        inv.setItem(13, ItemBuilder.of(order.material()).name(clean(order.material())).lore("Deliver: " + amount).lore("Earn: " + MONEY.format(amount * order.pricePerItem())).build());
        inv.setItem(15, ItemBuilder.of(Material.LIME_STAINED_GLASS_PANE).name("Confirm").greenLore("Get paid").build());
        player.openInventory(inv);
        session.movingToConfirm = false;
    }

    public void askSearch(Player player, boolean selectMode) {
        prompts.ask(player, "Aranacak item adini yaz:", text -> {
            Session session = session(player);
            session.search = text.toLowerCase(Locale.ROOT);
        if (selectMode) {
            session.itemBrowserMode = true;
            openSelectItem(player, 0);
        } else {
            session.itemBrowserMode = false;
            openMain(player, 0);
        }
        });
    }

    public void askAmount(Player player) {
        prompts.ask(player, "Amount yaz:", text -> {
            try {
                session(player).amount = Math.max(1, Math.min(999999, Integer.parseInt(text)));
            } catch (NumberFormatException ignored) {
                player.sendMessage(Component.text("Sayi girmelisin."));
            }
            openNewOrder(player);
        });
    }

    public void askPrice(Player player) {
        prompts.ask(player, "Item basina para yaz:", text -> {
            try {
                session(player).pricePerItem = Math.max(0.01D, Double.parseDouble(text.replace(',', '.')));
            } catch (NumberFormatException ignored) {
                player.sendMessage(Component.text("Sayi girmelisin."));
            }
            openNewOrder(player);
        });
    }

    public void createOrder(Player player) {
        expireOrders();
        Session session = session(player);
        if (session.selectedMaterial == null) {
            player.sendMessage(Component.text("Once item sec."));
            openNewOrder(player);
            return;
        }
        int maxOpenOrders = plugin.getConfig().getInt("settings.max-open-orders-per-player", 10);
        long openOrders = repository.openCountByOwner(player.getUniqueId());
        if (openOrders >= maxOpenOrders) {
            player.sendMessage(Component.text("En fazla " + maxOpenOrders + " acik order acabilirsin."));
            openYourItems(player);
            return;
        }
        double total = session.amount * session.pricePerItem;
        if (!economy.isReady()) {
            player.sendMessage(Component.text("Economy plugin bulunamadi."));
            openNewOrder(player);
            return;
        }
        if (!economy.has(player, total) || !economy.withdraw(player, total)) {
            player.sendMessage(Component.text("Yeterli paran yok."));
            openNewOrder(player);
            return;
        }
        repository.create(player.getUniqueId(), session.selectedMaterial, session.amount, session.pricePerItem);
        resetDraft(session);
        resetBrowsing(session);
        player.sendMessage(Component.text("Order acildi."));
        openMain(player, 0);
    }

    public void cancelNewOrder(Player player) {
        Session session = session(player);
        resetDraft(session);
        openMainFresh(player);
    }

    public void confirmDelivery(Player player) {
        expireOrders();
        Session session = session(player);
        Order order = currentDelivery(session);
        if (order == null || order.isComplete()) {
            returnPendingDelivery(player);
            player.sendMessage(Component.text("Bu order artik kapali."));
            openMain(player, 0);
            return;
        }
        int deliverable = Math.min(order.remaining(), session.pendingDeliveryAmount);
        if (deliverable <= 0) {
            player.sendMessage(Component.text("Teslim edilecek item yok."));
            openMain(player, 0);
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(order.owner());
        double pay = deliverable * order.pricePerItem();
        session.confirmingDelivery = true;
        order.addDelivered(deliverable);
        session.pendingDeliveryAmount = 0;
        repository.save();
        economy.deposit(player, pay);
        player.sendMessage(Component.text("Teslim edildi: " + deliverable + " item, kazanc: " + MONEY.format(pay)));
        if (owner.isOnline() && owner.getPlayer() != null) {
            owner.getPlayer().sendMessage(Component.text("Order teslim edildi: " + clean(order.material()) + " x" + deliverable));
        }
        openMain(player, 0);
        session.confirmingDelivery = false;
    }

    public void returnPendingDelivery(Player player) {
        Session session = session(player);
        Order order = currentDelivery(session);
        if (order == null || session.pendingDeliveryAmount <= 0) {
            session.pendingDeliveryAmount = 0;
            return;
        }
        giveOrDrop(player, new ItemStack(order.material(), session.pendingDeliveryAmount));
        session.pendingDeliveryAmount = 0;
    }

    public void returnDeliveryInventory(Player player, Inventory inventory) {
        Session session = session(player);
        if (session.movingToConfirm) {
            return;
        }
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            giveOrDrop(player, item);
            inventory.setItem(i, null);
        }
    }

    public void claimYourItems(Player player, Order order) {
        expireOrders();
        if (!order.owner().equals(player.getUniqueId()) || order.claimable() <= 0) {
            return;
        }
        int amount = order.claimable();
        ItemStack stack = new ItemStack(order.material(), Math.min(99, amount));
        int left = amount;
        while (left > 0) {
            stack.setAmount(Math.min(stack.getMaxStackSize(), left));
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack.clone());
            int added = stack.getAmount() - leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (added <= 0) {
                break;
            }
            order.addClaimed(added);
            left -= added;
        }
        repository.save();
        openYourItems(player);
    }

    Order visibleOrder(Player player, int slot) {
        return visibleOrders.getOrDefault(player.getUniqueId(), Map.of()).get(slot);
    }

    Order orderById(UUID id) {
        return repository.find(id).orElse(null);
    }

    public void expireOrders() {
        boolean changed = false;
        for (Order order : repository.expiredOpen(System.currentTimeMillis())) {
            order.close();
            if (!order.isRefunded() && order.refundAmount() > 0.0D) {
                economy.deposit(Bukkit.getOfflinePlayer(order.owner()), order.refundAmount());
                order.markRefunded();
            }
            changed = true;
        }
        if (changed) {
            repository.save();
        }
    }

    Material visibleMaterial(Player player, int slot) {
        return visibleMaterials.getOrDefault(player.getUniqueId(), Map.of()).get(slot);
    }

    private List<Order> filteredOrders(Session session) {
        Comparator<Order> comparator = switch (session.orderSort) {
            case MOST_PAID -> Comparator.comparingDouble(Order::totalPaid).reversed();
            case MOST_DELIVERED -> Comparator.comparingInt(Order::delivered).reversed();
            case MOST_MONEY_PER_ITEM -> Comparator.comparingDouble(Order::pricePerItem).reversed();
            case ALPHABETICAL_A_Z -> Comparator.comparing(order -> order.material().name());
            case ALPHABETICAL_Z_A -> Comparator.<Order, String>comparing(order -> order.material().name()).reversed();
            case RECENTLY_LISTED -> Comparator.comparingLong(Order::createdAt).reversed();
        };
        return repository.allOpen().stream()
                .filter(order -> session.filter.matches(order.material()))
                .filter(order -> session.search.isBlank() || order.material().name().toLowerCase(Locale.ROOT).contains(session.search))
                .sorted(comparator)
                .toList();
    }

    private List<Material> selectableMaterials(Session session) {
        Comparator<Material> comparator = session.selectSort == OrderSort.ALPHABETICAL_Z_A
                ? Comparator.comparing(Material::name).reversed()
                : Comparator.comparing(Material::name);
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isAir() || !material.isItem()) {
                continue;
            }
            if (!session.filter.matches(material)) {
                continue;
            }
            if (!session.search.isBlank() && !material.name().toLowerCase(Locale.ROOT).contains(session.search)) {
                continue;
            }
            materials.add(material);
        }
        materials.sort(comparator);
        return materials;
    }

    private void controls(Inventory inv, Session session) {
        inv.setItem(45, ItemBuilder.of(Material.ARROW).name("Back").build());
        inv.setItem(47, ItemBuilder.of(Material.CAULDRON).name("Sort").lore(session.view == ViewKind.SELECT_ITEM ? session.selectSort.label() : session.orderSort.label()).build());
        inv.setItem(48, ItemBuilder.of(Material.HOPPER).name("Filter").lore(session.filter.label()).build());
        inv.setItem(49, ItemBuilder.of(Material.PAPER).name("Orders").greenLore("Click to refresh").build());
        inv.setItem(50, ItemBuilder.of(Material.OAK_SIGN).name("Search").lore(session.search.isBlank() ? "No search" : session.search).build());
        inv.setItem(51, ItemBuilder.of(Material.CHEST).name("Your Items").build());
        inv.setItem(53, ItemBuilder.of(Material.ARROW).name("Next").build());
    }

    private ItemStack orderIcon(Order order) {
        String owner = Bukkit.getOfflinePlayer(order.owner()).getName();
        if (owner == null || owner.isBlank()) {
            owner = "Unknown";
        }
        Component title = Component.text(owner + "'s", NamedTextColor.GREEN)
                .append(Component.text(" Order", NamedTextColor.WHITE));
        Component price = Component.text(formatMoney(order.pricePerItem()), NamedTextColor.GREEN)
                .append(Component.text(" each", NamedTextColor.WHITE));
        Component delivered = Component.text(order.delivered(), NamedTextColor.GOLD)
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text(order.amount(), NamedTextColor.GREEN))
                .append(Component.text(" Delivered", NamedTextColor.WHITE));
        Component paid = Component.text(formatMoney(order.totalPaid()), NamedTextColor.GOLD)
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text(formatMoney(order.amount() * order.pricePerItem()), NamedTextColor.GREEN))
                .append(Component.text(" Paid", NamedTextColor.WHITE));
        Component click = Component.text("Click to deliver ", NamedTextColor.WHITE)
                .append(Component.text(owner + "'s ", NamedTextColor.GREEN))
                .append(Component.text(clean(order.material()), NamedTextColor.WHITE));
        return ItemBuilder.of(order.material())
                .name(title)
                .lore(Component.text(clean(order.material()), NamedTextColor.WHITE))
                .lore(price)
                .blankLore()
                .lore(delivered)
                .lore(paid)
                .blankLore()
                .lore(click)
                .lore(Component.text(timeLeft(order.expiresAt()) + " Until Order expires", NamedTextColor.DARK_GRAY))
                .build();
    }

    private Order currentDelivery(Session session) {
        if (session.deliveryOrder == null) {
            return null;
        }
        return repository.find(session.deliveryOrder).orElse(null);
    }

    private int countDeliveryInventory(Inventory inventory, Material material) {
        int total = 0;
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void clearDeliveryInventory(Inventory inventory) {
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, null);
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private int pages(int size) {
        return Math.max(1, (int) Math.ceil(size / (double) CONTENT.length));
    }

    private String formatMoney(double value) {
        if (value >= 1_000_000_000D) {
            return "$" + MONEY.format(value / 1_000_000_000D) + "b";
        }
        if (value >= 1_000_000D) {
            return "$" + MONEY.format(value / 1_000_000D) + "m";
        }
        if (value >= 1_000D) {
            return "$" + MONEY.format(value / 1_000D) + "k";
        }
        return "$" + MONEY.format(value);
    }

    private String timeLeft(long expiresAt) {
        long millis = Math.max(0L, expiresAt - System.currentTimeMillis());
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private void resetBrowsing(Session session) {
        session.page = 0;
        session.selectPage = 0;
        session.orderSort = OrderSort.RECENTLY_LISTED;
        session.selectSort = OrderSort.ALPHABETICAL_A_Z;
        session.filter = OrderFilter.ALL;
        session.search = "";
        session.itemBrowserMode = false;
    }

    private void resetDraft(Session session) {
        session.selectedMaterial = null;
        session.amount = 64;
        session.pricePerItem = 1.0D;
    }

    private String clean(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
