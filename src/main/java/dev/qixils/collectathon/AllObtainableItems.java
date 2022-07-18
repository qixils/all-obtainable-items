package dev.qixils.collectathon;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import fr.minuskube.inv.InventoryManager;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class AllObtainableItems extends JavaPlugin implements Listener {
	private static final UUID ZERO_UUID = new UUID(0, 0);
	private static final String OBJECTIVE_NAME = "collectathon";
	private static final Sound ACQUIRE_SOUND = Sound.sound(
			Key.key(Key.MINECRAFT_NAMESPACE, "entity.firework_rocket.launch"),
			Source.PLAYER,
			1f,
			1f
	);
	private static final Set<Material> EXCLUDE = Set.of(
			Material.POTION,
			Material.LINGERING_POTION,
			Material.SPLASH_POTION,
			Material.TIPPED_ARROW,
			Material.ENCHANTED_BOOK,
			Material.SUSPICIOUS_STEW,
			Material.AIR,
			Material.CAVE_AIR,
			Material.VOID_AIR,
			Material.BEDROCK,
			Material.COMMAND_BLOCK,
			Material.COMMAND_BLOCK_MINECART,
			Material.CHAIN_COMMAND_BLOCK,
			Material.REPEATING_COMMAND_BLOCK,
			Material.STRUCTURE_BLOCK,
			Material.STRUCTURE_VOID,
			Material.SPAWNER,
			Material.BARRIER,
			Material.LIGHT,
			Material.JIGSAW,
			Material.KNOWLEDGE_BOOK,
			Material.DEBUG_STICK,
			Material.PLAYER_HEAD,
			Material.GOAT_HORN,
			Material.BUNDLE
	);

	private static @MonotonicNonNull List<ItemStack> ALL_ITEMS = null;
	private static @MonotonicNonNull Set<String> ALL_KEYS = null;
	private final @NotNull Map<UUID, BossBar> bossBarMap = new HashMap<>(1);
	private @MonotonicNonNull InventoryManager inventoryManager;
	@SuppressWarnings("FieldCanBeLocal")
	private @MonotonicNonNull BukkitCommandManager<CommandSender> commandManager;
	private @Nullable FileSystemDataMap data;
	private @Nullable Objective objective;
	private @Nullable BukkitAudiences adventure;
	private @Nullable PlainTextComponentSerializer plainSerializer;
	private @Nullable LegacyComponentSerializer legacySerializer;
	private @Nullable BungeeComponentSerializer bungeeSerializer;
	private boolean coop = true;

	public static String getKey(ItemStack item) {
		Material type = item.getType();
		StringBuilder key = new StringBuilder(type.getKey().getKey());

		if (!item.hasItemMeta())
			return key.toString();

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof SuspiciousStewMeta stew) {
			if (stew.hasCustomEffects()) {
				key.append('|')
						.append(stew.getCustomEffects().stream()
								.map(effect -> effect.getType().getKey().getKey())
								.sorted()
								.collect(Collectors.joining(",")));
			}
		} else if (meta instanceof PotionMeta potion) {
			if (type != Material.TIPPED_ARROW) {
				key.delete(0, key.length());
				key.append("potion");
			}
			if (potion.hasCustomEffects()) {
				key.append('|')
						.append(potion.getCustomEffects().stream()
								.map(effect -> effect.getType().getKey().getKey())
								.sorted()
								.collect(Collectors.joining(",")));
			}
		} else if (meta instanceof EnchantmentStorageMeta enchantments) {
			if (enchantments.hasStoredEnchants()) {
				key.append('|')
						.append(enchantments.getStoredEnchants().entrySet().stream()
								.map(entry -> entry.getKey().getKey().getKey() + ':' + entry.getValue())
								.sorted()
								.collect(Collectors.joining(",")));
			}
		}
		// TODO: "explorer" map (buried treasure/mansion/monument)
		// TODO: goat horns

		return key.toString();
	}

	// non-static stuff

	@SuppressWarnings("deprecation")
	public @NotNull List<ItemStack> getAllItems() {
		if (ALL_ITEMS != null)
			return ALL_ITEMS;
		if (plainSerializer == null)
			return Collections.emptyList();

		// initial capacity is a very rough estimate
		int initialCapacity = Material.values().length
				+ (PotionEffectType.values().length * 4)
				+ Enchantment.values().length;
		List<ItemStack> items = new ArrayList<>(initialCapacity);
		for (Material material : Material.values()) {
			if (!material.isItem())
				continue;
			if (material.isLegacy())
				continue;
			if (EXCLUDE.contains(material))
				continue;
			if (material.name().contains("SPAWN_EGG"))
				continue;
			items.add(new ItemStack(material));
		}

		// TODO potions
		// TODO sussy stew
		for (Enchantment enchantment : Enchantment.values()) {
			ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
			meta.addStoredEnchant(enchantment, 1, false);
			item.setItemMeta(meta);
			items.add(item);
		}
		// TODO: "explorer" map (buried treasure/mansion/monument)
		// TODO: goat horns

		return ALL_ITEMS = items.stream().sorted(Comparator.comparing(item -> plainSerializer.serialize(getDisplayName(item)))).toList();
	}

	public @NotNull Set<String> getAllKeys() {
		if (ALL_KEYS != null)
			return ALL_KEYS;
		List<ItemStack> items = getAllItems();
		if (items.isEmpty())
			return Collections.emptySet();
		return ALL_KEYS = items.stream().map(AllObtainableItems::getKey).collect(Collectors.toUnmodifiableSet());
	}

	public @NotNull BukkitAudiences adventure() {
		if (adventure == null)
			throw new IllegalStateException("Tried to access adventure while plugin is disabled");
		return adventure;
	}

	public @NotNull PlainTextComponentSerializer plainSerializer() {
		if (plainSerializer == null)
			throw new IllegalStateException("Tried to access adventure while plugin is disabled");
		return plainSerializer;
	}

	public @NotNull LegacyComponentSerializer legacySerializer() {
		if (legacySerializer == null)
			throw new IllegalStateException("Tried to access adventure while plugin is disabled");
		return legacySerializer;
	}

	public @NotNull BungeeComponentSerializer bungeeSerializer() {
		if (bungeeSerializer == null)
			throw new IllegalStateException("Tried to access adventure while plugin is disabled");
		return bungeeSerializer;
	}

	public InventoryManager getInventoryManager() {
		if (inventoryManager == null)
			throw new IllegalStateException("Tried to access inventory manager while plugin is disabled");
		return inventoryManager;
	}

	public Component getDisplayName(ItemStack item) {
		if (PaperLib.isPaper()) {
			// TODO remove custom item stack name (but only if it's TextComponent ??)
			return item.displayName();
		}
		Material type = item.getType();
		NamespacedKey key = type.getKey();
		String subKey = key.getNamespace() + "." + key.getKey();
		String blockKey = "block." + subKey;
		TranslatableComponent.Builder builder = Component.translatable();
		// this basically checks to see if "block.minecraft.xyz" is a valid translatable string in
		//  en_us, otherwise it falls back to "item.minecraft.xyz"
		if (!type.isBlock() || blockKey.equals(plainSerializer().serialize(builder.key(blockKey).build()))) {
			builder.key("item." + subKey);
		}
		// append misc info
		// TODO
		return builder.build();
	}

	@Override
	public void onLoad() {
		saveDefaultConfig();
	}

	@Override
	public void onEnable() {
		coop = getConfig().getBoolean("coop", coop);

		inventoryManager = new InventoryManager(this);
		inventoryManager.init();

		PaperLib.suggestPaper(this, Level.WARNING);
		this.adventure = BukkitAudiences.create(this);
		this.plainSerializer = PlainTextComponentSerializer.builder()
				.flattener(this.adventure.flattener())
				.build();
		this.legacySerializer = LegacyComponentSerializer.legacySection().toBuilder()
				.flattener(this.adventure.flattener())
				.build();
		this.bungeeSerializer = BungeeComponentSerializer.get();

		// register event handler
		Bukkit.getPluginManager().registerEvents(this, this);

		this.data = new FileSystemDataMap(new File(getDataFolder(), "data"));

		if (!isCoop()) {
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			objective = scoreboard.getObjective(OBJECTIVE_NAME);
			if (objective == null) {
				Component name = Component.text("Collectathon Progress", NamedTextColor.YELLOW);
				if (PaperLib.isPaper())
					objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", name);
				else //noinspection deprecation
					objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", plainSerializer().serialize(name));
				objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			}
			objective.getScore(LegacyComponentSerializer.SECTION_CHAR + "eGoal").setScore(getAllKeys().size());
		}

		// command stuff
		try {
			if (PaperLib.isPaper()) {
				commandManager = new PaperCommandManager<>(
						this,
						AsynchronousCommandExecutionCoordinator.<CommandSender>newBuilder()
								.withExecutor(command -> Bukkit.getScheduler().runTaskAsynchronously(this, command))
								.withAsynchronousParsing()
								.build(),
						Function.identity(),
						Function.identity()
				);
			} else {
				commandManager = new BukkitCommandManager<>(
						this,
						AsynchronousCommandExecutionCoordinator.<CommandSender>newBuilder()
								.withExecutor(command -> Bukkit.getScheduler().runTaskAsynchronously(this, command))
								.withAsynchronousParsing()
								.build(),
						Function.identity(),
						Function.identity()
				);
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not instantiate command manager", e);
		}

		try {
			commandManager.registerBrigadier();
		} catch (final Exception e) {
			getLogger().log(Level.WARNING, "Failed to initialize Brigadier support", e);
		}

		commandManager.command(commandManager.commandBuilder("items",
						ArgumentDescription.of("Opens the obtainable items menu"), "aoi")
				.senderType(Player.class)
				.handler(context -> {
					ItemMenu menu = new ItemMenu(this, (Player) context.getSender(), null);
					Bukkit.getScheduler().runTask(this, menu::open);
				})
		);
	}

	@Override
	public void onDisable() {
		if (this.adventure != null) {
			this.adventure.close();
			this.adventure = null;
		}
		this.data = null;
	}

	public boolean isCoop() {
		return coop;
	}

	public UUID getUUID(Player sender) {
		if (isCoop())
			return ZERO_UUID;
		return sender.getUniqueId();
	}

	public boolean collect(@NotNull Player player, @Nullable ItemStack item) {
		if (data == null)
			return false;
		if (item == null)
			return false;

		Audience audience;
		UUID uuid;

		if (coop) {
			// usage of PaperLib here is working around an issue with adventure platform (#97)
			audience = PaperLib.isPaper() ? Bukkit.getServer() : adventure().all();
			uuid = ZERO_UUID;
		} else {
			// usage of PaperLib here is working around an issue with adventure platform (#97)
			audience = PaperLib.isPaper() ? player : adventure().player(player);
			uuid = player.getUniqueId();
		}

		String key = getKey(item);
		if (!getAllKeys().contains(key))
			return false;

		Set<String> items = data.getOrEmpty(uuid);
		if (items.contains(key))
			return false;

		Set<String> newItems = new HashSet<>(items.size() + 1);
		newItems.addAll(items);
		newItems.add(key);
		data.put(uuid, newItems);

		audience.sendMessage(
				Component.text().content("Acquired ").color(TextColor.color(0xfff8e7))
						.append(getDisplayName(item))
						.append(Component.text('!'))
		);

		// usage of PaperLib here is working around an issue with adventure platform (#97)
		if (PaperLib.isPaper())
			audience.playSound(ACQUIRE_SOUND, Sound.Emitter.self());
		else
			audience.playSound(ACQUIRE_SOUND);

		BossBar bossBar = bossBarMap.get(uuid);
		assert bossBar != null : "Boss bar was null for " + player.getName() + " but should've been instantiated on player join";
		int collected = newItems.size();
		int total = getAllItems().size();
		bossBar.name(Component.text("Collectathon Progress: " + collected + " out of " + total, NamedTextColor.YELLOW));
		bossBar.progress(((float) collected) / total);

		if (objective != null)
			objective.getScore(player).setScore(collected);

		return true;
	}

	public FileSystemDataMap getData() {
		if (data == null)
			throw new IllegalStateException("Tried to access data before plugin loaded");
		return data;
	}

	// event listeners

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		adventure().player(event.getPlayer()).showBossBar(bossBarMap.computeIfAbsent(getUUID(event.getPlayer()), uuid -> {
			int collected = getData().getOrEmpty(uuid).size();
			int total = getAllItems().size();
			return BossBar.bossBar(
					Component.text("Collectathon Progress: " + collected + " out of " + total, NamedTextColor.YELLOW),
					((float) collected) / total,
					Color.YELLOW,
					Overlay.PROGRESS
			);
		}));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPickupEvent(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player) {
			collect(player, event.getItem().getItemStack());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			player.getInventory().forEach(item -> collect(player, item));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCraftItemEvent(CraftItemEvent event) {
		// TODO: test to make sure this does not fire every time a recipe is completed in the crafting matrix
		if (event.getWhoClicked() instanceof Player player) {
			collect(player, event.getRecipe().getResult());
		}
	}
}
