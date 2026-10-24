package com.minimods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Zapis i odczyt ustawień (config/minimods.json). */
public final class Config {
	/** Wygląd wszystkich modułów i menu. Kolory w formacie RGB (0xRRGGBB). */
	public static class Theme {
		public int accent = 0x5B8CFF;
		public int background = 0x0B0B10;
		public int text = 0xFFFFFF;
		/** Przezroczystość tła w procentach (0-100). */
		public int opacity = 60;
		public boolean shadow = true;
		public boolean rounded = true;

		public void resetColors() {
			accent = 0x5B8CFF;
			background = 0x0B0B10;
			text = 0xFFFFFF;
			opacity = 60;
		}
	}

	/** Ustawienia pojedynczego modułu: włączony oraz pozycja na ekranie (piksele GUI). */
	public static class Entry {
		public boolean enabled;
		public int x;
		public int y;

		public Entry() {
		}

		public Entry(boolean enabled, int x, int y) {
			this.enabled = enabled;
			this.x = x;
			this.y = y;
		}
	}

	private static class Root {
		Theme theme = new Theme();
		Map<String, Entry> modules = new LinkedHashMap<>();
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("minimods.json");

	private static Root root = new Root();

	private Config() {
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			Root loaded = GSON.fromJson(Files.readString(FILE), Root.class);
			if (loaded != null) {
				if (loaded.theme == null) {
					loaded.theme = new Theme();
				}
				if (loaded.modules == null) {
					loaded.modules = new LinkedHashMap<>();
				}
				root = loaded;
			}
		} catch (Exception e) {
			System.err.println("[minimods] Nie udało się wczytać configu, używam domyślnych: " + e);
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(root));
		} catch (IOException e) {
			System.err.println("[minimods] Nie udało się zapisać configu: " + e);
		}
	}

	public static Theme theme() {
		return root.theme;
	}

	public static Entry get(ModuleType type) {
		return root.modules.computeIfAbsent(type.name(), k -> defaultFor(type));
	}

	public static void resetPositions() {
		for (ModuleType type : ModuleType.values()) {
			Entry fresh = defaultFor(type);
			Entry current = get(type);
			current.x = fresh.x;
			current.y = fresh.y;
		}
	}

	private static Entry defaultFor(ModuleType type) {
		return switch (type) {
			case FPS -> new Entry(true, 6, 6);
			case CPS -> new Entry(true, 6, 28);
			case COORDS -> new Entry(true, 6, 50);
			case DIRECTION -> new Entry(false, 110, 6);
			case SPEED -> new Entry(false, 110, 28);
			case PING -> new Entry(false, 110, 50);
			case CLOCK -> new Entry(false, 110, 72);
			case ARMOR -> new Entry(false, 6, 96);
			case KEYSTROKES -> new Entry(false, 110, 100);
		};
	}
}
