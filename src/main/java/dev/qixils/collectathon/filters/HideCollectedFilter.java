package dev.qixils.collectathon.filters;

import dev.qixils.collectathon.AllObtainableItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Set;

public class HideCollectedFilter implements Filter {
	private final Set<String> keys;

	public HideCollectedFilter(AllObtainableItems plugin, Player player) {
		this.keys = Objects.requireNonNull(plugin.getData()).getOrEmpty(player.getUniqueId());
	}

	@Override
	public boolean test(ItemStack item) {
		return keys.contains(AllObtainableItems.getKey(item));
	}
}
