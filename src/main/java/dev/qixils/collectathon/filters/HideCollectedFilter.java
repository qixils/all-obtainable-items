package dev.qixils.collectathon.filters;

import dev.qixils.collectathon.AllObtainableItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

// TODO: seemingly broken

public class HideCollectedFilter implements Filter {
	private final Set<String> keys;

	public HideCollectedFilter(AllObtainableItems plugin, Player player) {
		this.keys = plugin.getData().getOrEmpty(player.getUniqueId());
	}

	@Override
	public boolean test(ItemStack item) {
		return keys.contains(AllObtainableItems.getKey(item));
	}
}
