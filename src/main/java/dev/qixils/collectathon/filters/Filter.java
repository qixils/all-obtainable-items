package dev.qixils.collectathon.filters;

import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

@FunctionalInterface
public interface Filter extends Predicate<ItemStack> {
	/**
	 * Determines whether an item should be hidden from view in the AOI items menu.
	 *
	 * @param item item to test
	 * @return {@code true} if the item should be hidden
	 */
	boolean test(ItemStack item);

	/**
	 * Creates a new filter which returns {@code false} if the item should be hidden instead of
	 * {@code true}.
	 *
	 * @return new inverted filter
	 */
	default Filter invert() {
		return new InvertedFilter(this);
	}
}
