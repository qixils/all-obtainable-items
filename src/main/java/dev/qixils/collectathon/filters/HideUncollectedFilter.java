package dev.qixils.collectathon.filters;

import dev.qixils.collectathon.AllObtainableItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HideUncollectedFilter extends HideCollectedFilter {
	public HideUncollectedFilter(AllObtainableItems plugin, Player player) {
		super(plugin, player);
	}

	@Override
	public boolean test(ItemStack item) {
		return !super.test(item);
	}
}
