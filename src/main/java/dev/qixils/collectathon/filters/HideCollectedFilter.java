package dev.qixils.collectathon.filters;

import dev.qixils.collectathon.AllObtainableItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class HideCollectedFilter extends AbstractFilter {
	private final Set<String> keys;

	public HideCollectedFilter(AllObtainableItems plugin, Player player) {
		this.keys = plugin.getData().getOrEmpty(plugin.getUUID(player));
	}

	@Override
	public boolean test(ItemStack item) {
		return keys.contains(AllObtainableItems.getKey(item));
	}
}
