package dev.qixils.collectathon;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// TODO: this does not work. saved files are blank

@SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored", "UnusedReturnValue"})
public class FileSystemDataMap {
	private final Gson GSON = new Gson();
	private final Map<UUID, Set<String>> cache = new HashMap<>(1);
	private final File directory;

	public FileSystemDataMap(@NotNull File directory) {
		assert directory.isDirectory() : "`directory` argument must be a directory";
		this.directory = directory;
		directory.mkdirs();
	}

	public FileSystemDataMap(@NotNull Path directory) {
		this(directory.toFile());
	}

	@Nullable
	public Set<String> get(UUID key) {
		if (key == null)
			throw new NullPointerException("key cannot be null");
		synchronized (cache) {
			if (cache.containsKey(key))
				return cache.get(key);
			File file = new File(directory, key + ".json");
			if (!file.exists())
				return null;

			String[] array;
			try {
				array = GSON.fromJson(new FileReader(file), String[].class);
			} catch (FileNotFoundException e) {
				// TODO log error
				return null;
			}

			Set<String> set = Set.copyOf(Arrays.asList(array));
			cache.put(key, set);
			return set;
		}
	}

	public Set<String> getOrEmpty(UUID key) {
		return Objects.requireNonNullElseGet(get(key), Collections::emptySet);
	}

	@Nullable
	public Set<String> put(UUID key, Set<String> value) {
		if (key == null)
			throw new NullPointerException("key cannot be null");

		synchronized (cache) {
			// per java specification this method should return the removed value, but that might not
			// yet be loaded, so we must ensure it is loaded by fetching it:
			Set<String> old = cache.get(key);

			File file = new File(directory, key + ".json");
			try {
				file.createNewFile();
				// TODO: test that this properly wipes the file (ig not that it matters much)
				GSON.toJson(
						value,
						new TypeToken<Set<String>>() {
						}.getType(),
						new FileWriter(file, false));
			} catch (IOException e) {
				// TODO log error
			}

			return old;
		}
	}
}
