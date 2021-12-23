package dev.qixils.collectathon;

import io.papermc.lib.PaperLib;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class AllObtainableItems extends JavaPlugin implements Listener {
	private static @MonotonicNonNull List<String> ALL_KEYS = null;
	private static @MonotonicNonNull Logger logger;
	private @MonotonicNonNull BukkitAudiences adventure;
	private @MonotonicNonNull PlainTextComponentSerializer plainSerializer;

	public static String getKey(ItemStack item) {
		Material type = item.getType();
		StringBuilder key = new StringBuilder(type.getKey().getKey());

		if (!item.hasItemMeta())
			return key.toString();

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof PotionMeta potion) {
			if (potion.hasCustomEffects()) {
				key.append('|')
						.append(potion.getCustomEffects().stream()
								.sorted(Comparator.comparing(effect -> effect.getType().getKey().getKey()))
								.map(effect -> effect.getType().getKey().getKey() + "_x"
										+ effect.getAmplifier() + ":" + effect.getDuration())
								.collect(Collectors.joining(",")));
			}
		} else if (meta instanceof EnchantmentStorageMeta enchantments) {
			if (enchantments.hasStoredEnchants()) {
				key.append('|')
						.append(enchantments.getStoredEnchants().entrySet().stream()
								.sorted(Comparator.comparing(entry -> entry.getKey().getKey().getKey()))
								.map(entry -> entry.getKey().getKey().getKey() + ':' + entry.getValue())
								.collect(Collectors.joining(",")));
			}
		} else if (meta instanceof SuspiciousStewMeta stew) {
			if (stew.hasCustomEffects()) {
				key.append('|')
						.append(stew.getCustomEffects().stream()
								.sorted(Comparator.comparing(effect -> effect.getType().getKey().getKey()))
								.map(effect -> effect.getType().getKey().getKey())
								.collect(Collectors.joining(",")));
			}
		} else if (meta instanceof AxolotlBucketMeta bucket) {
			// TODO: loren doesn't like this one :(
			if (bucket.hasVariant()) {
				key.append('|').append(bucket.getVariant().name().toLowerCase(Locale.ENGLISH));
			}
		}

		return key.toString();
	}

	public static List<String> getAllKeys() {
		if (ALL_KEYS != null)
			return ALL_KEYS;

		// initial capacity is a very rough estimate
		int initialCapacity = Material.values().length
				+ (PotionEffectType.values().length * 4)
				+ Enchantment.values().length;
		List<String> keys = new ArrayList<>(initialCapacity);
		Set<Material> exclude = Set.of(
				Material.POTION,
				Material.LINGERING_POTION,
				Material.SPLASH_POTION,
				Material.TIPPED_ARROW,
				Material.ENCHANTED_BOOK,
				Material.AXOLOTL_BUCKET,
				Material.SUSPICIOUS_STEW
		);
		for (Material material : Material.values()) {
			if (material.isLegacy())
				continue;
			if (exclude.contains(material))
				continue;
			keys.add(material.getKey().getKey());
		}

		// TODO potions
		// TODO sussy stew
		// TODO enchanted books
		// TODO axolotl

		return ALL_KEYS = List.copyOf(keys);
	}

	// non-static stuff

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

	public Component getDisplayName(ItemStack item) {
		if (PaperLib.isPaper()) {
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
	public void onEnable() {
		logger = getLogger();

		PaperLib.suggestPaper(this, Level.WARNING);
		this.adventure = BukkitAudiences.create(this);
		this.plainSerializer = PlainTextComponentSerializer.builder()
				.flattener(this.adventure.flattener())
				.build();

		// instantiate keys
		Bukkit.getScheduler().runTaskAsynchronously(this, AllObtainableItems::getAllKeys);
		// register event handler
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		if (this.adventure != null) {
			this.adventure.close();
			this.adventure = null;
		}
	}

	public boolean isCoop() {
		return false; // TODO
	}

	public boolean collect(HumanEntity player, ItemStack item) {
		//String key = getKey(item);
		if (true) { // TODO: store to database if not present
			// TODO: display
			return true;
		}
		return false;
	}
}
