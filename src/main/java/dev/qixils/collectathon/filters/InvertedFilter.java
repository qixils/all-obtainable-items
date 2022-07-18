package dev.qixils.collectathon.filters;

import org.bukkit.inventory.ItemStack;

public final class InvertedFilter implements Filter {
	private final Filter wrapped;

	public InvertedFilter(Filter wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public boolean test(ItemStack item) {
		return !wrapped.test(item);
	}

	@Override
	public Filter invert() {
		return wrapped;
	}
}
