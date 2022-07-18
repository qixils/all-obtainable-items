package dev.qixils.collectathon;

import dev.qixils.collectathon.filters.Filter;
import dev.qixils.collectathon.filters.Filters;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator.Type;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextDecoration.State;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ItemMenu implements InventoryProvider {
	private static final Component FILTER_NAME = Component.text("Filter", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, State.FALSE);
	private static final int ITEMS_PER_PAGE = 9 * 5;

	private final SmartInventory inventory;
	private final AllObtainableItems plugin;
	private final Filters filterEnum;
	private final Filter filter;
	private final Player originalPlayer;
	private final AtomicInteger page = new AtomicInteger(0);
	private @Nullable List<ClickableItem> items = null;
	private List<Component> filterLore = null;
	private List<BaseComponent[]> bungeeFilterLore = null;
	private List<String> legacyFilterLore = null;
	private BaseComponent[] bungeeFilterName = null;
	private String legacyFilterName = null;

	public ItemMenu(@NotNull AllObtainableItems plugin, @NotNull Player player, @Nullable Filters filter) {
		this.filterEnum = Objects.requireNonNullElse(filter, Filters.SHOW_ALL);
		this.originalPlayer = player;
		this.plugin = plugin;
		this.filter = this.filterEnum.createFilter(plugin, player);
		this.inventory = SmartInventory.builder()
				.provider(this)
				.size(6, 9)
				.title("All Obtainable Items")
				.closeable(true)
				.manager(plugin.getInventoryManager())
				.build();

		getItems(); // create item array while async
	}

	public SmartInventory getInventory() {
		return inventory;
	}

	private Filters nextFilter() {
		int target = filterEnum.ordinal() + 1;
		if (target == Filters.values().length)
			target = 0;
		return Filters.fromOrdinal(target);
	}

	private Filters previousFilter() {
		int target = filterEnum.ordinal() - 1;
		if (target == -1)
			target = Filters.values().length - 1;
		return Filters.fromOrdinal(target);
	}

	public void open() {
		inventory.open(originalPlayer);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void init(Player player, InventoryContents contents) {
		final int curPage = page.get();
		final List<ClickableItem> items = getItems();
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			int index = curPage * ITEMS_PER_PAGE + i;
			ClickableItem item = index < items.size() ? items.get(index) : ClickableItem.empty(new ItemStack(Material.AIR));
			contents.set(i / 9, i % 9, item);
		}

		// previous page
		if (curPage > 0)
			contents.set(5, 0, pageClickableItem(curPage - 1, player, contents));
		else
			contents.set(5, 0, ClickableItem.empty(new ItemStack(Material.AIR)));
		// next page
		if (curPage < (items.size() / ITEMS_PER_PAGE))
			contents.set(5, 8, pageClickableItem(curPage + 1, player, contents));
		else
			contents.set(5, 8, ClickableItem.empty(new ItemStack(Material.AIR)));
		// filter
		ItemStack item = new ItemStack(Material.HOPPER);
		ItemMeta meta = item.getItemMeta();
		if (PaperLib.isPaper()) {
			meta.displayName(FILTER_NAME);
			meta.lore(getFilterItemLore());
		} else if (PaperLib.isSpigot()) {
			meta.setDisplayNameComponent(getBungeeFilterName());
			meta.setLoreComponents(getBungeeFilterItemLore());
		} else {
			meta.setDisplayName(getLegacyFilterName());
			meta.setLore(getLegacyFilterLore());
		}
		item.setItemMeta(meta);
		contents.set(5, 4, ClickableItem.of(item, event -> Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			ItemMenu newMenu = new ItemMenu(plugin, player, event.isLeftClick() ? nextFilter() : previousFilter());
			Bukkit.getScheduler().runTask(plugin, () -> newMenu.getInventory().open(player));
			playClickSound(player);
		})));
	}

	@Override
	public void update(Player player, InventoryContents contents) {
	}

	public @NotNull List<ClickableItem> getItems() {
		if (items != null)
			return items;
		return plugin.getAllItems().stream()
				.filter(filter.invert())
				.map(ClickableItem::empty)
				.collect(Collectors.toList());
	}

	public List<Component> getFilterItemLore() {
		if (filterLore != null)
			return filterLore;
		List<Component> lore = new ArrayList<>(Filters.values().length);
		for (Filters filter : Filters.values()) {
			TextColor color = this.filterEnum == filter
					? NamedTextColor.AQUA
					: NamedTextColor.DARK_AQUA;
			lore.add(Component.text(filter.displayName(), color).decoration(TextDecoration.ITALIC, State.FALSE));
		}
		return filterLore = Collections.unmodifiableList(lore);
	}

	public List<BaseComponent[]> getBungeeFilterItemLore() {
		if (bungeeFilterLore != null)
			return bungeeFilterLore;
		BungeeComponentSerializer serializer = plugin.bungeeSerializer();
		return bungeeFilterLore = getFilterItemLore().stream().map(serializer::serialize).toList();
	}

	public List<String> getLegacyFilterLore() {
		if (legacyFilterLore != null)
			return legacyFilterLore;
		LegacyComponentSerializer serializer = plugin.legacySerializer();
		return legacyFilterLore = getFilterItemLore().stream().map(serializer::serialize).toList();
	}

	public BaseComponent[] getBungeeFilterName() {
		if (bungeeFilterName != null)
			return bungeeFilterName;
		return bungeeFilterName = plugin.bungeeSerializer().serialize(FILTER_NAME);
	}

	public String getLegacyFilterName() {
		if (legacyFilterName != null)
			return legacyFilterName;
		return legacyFilterName = plugin.legacySerializer().serialize(FILTER_NAME);
	}

	@SuppressWarnings("deprecation")
	public ItemStack pageItemStack(int page) {
		ItemStack item = new ItemStack(Material.ARROW);
		ItemMeta meta = item.getItemMeta();
		Component component = Component.text("Page " + (page + 1), NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false);
		if (PaperLib.isPaper())
			meta.displayName(component);
		else if (PaperLib.isSpigot())
			meta.setDisplayNameComponent(plugin.bungeeSerializer().serialize(component));
		else
			meta.setDisplayName(plugin.legacySerializer().serialize(component));
		item.setItemMeta(meta);
		return item;
	}
	
	public ClickableItem pageClickableItem(int page, Player player, InventoryContents contents) {
		return ClickableItem.of(pageItemStack(page), $ -> {
			this.page.set(page);
			init(player, contents);
			playClickSound(player);
		});
	}

	private void playClickSound(Player player) {
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.2f, 1);
	}
}
