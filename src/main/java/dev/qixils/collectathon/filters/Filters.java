package dev.qixils.collectathon.filters;

import dev.qixils.collectathon.AllObtainableItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Filters {
	HIDE_COLLECTED("Hide Collected") {
		@Override
		public Filter createFilter(AllObtainableItems plugin, Player player) {
			return new HideCollectedFilter(plugin, player);
		}
	},
	HIDE_UNCOLLECTED("Show Collected") {
		@Override
		public Filter createFilter(AllObtainableItems plugin, Player player) {
			return new HideUncollectedFilter(plugin, player);
		}
	};

	private static final Map<Integer, Filters> ORDINAL_MAP;

	static {
		Filters[] values = values();
		Map<Integer, Filters> ordinalMap = new HashMap<>(values.length);
		for (Filters filter : values) {
			ordinalMap.put(filter.ordinal(), filter);
		}
		ORDINAL_MAP = Collections.unmodifiableMap(ordinalMap);
	}

	private final String displayName;

	Filters(String displayName) {
		this.displayName = displayName;
	}

	@Nullable
	public static Filters fromOrdinal(int ordinal) {
		return ORDINAL_MAP.get(ordinal);
	}

	public String displayName() {
		return displayName;
	}

	public abstract Filter createFilter(AllObtainableItems plugin, Player player);
}
