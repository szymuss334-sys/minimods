package com.minimods;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Menu mini modów (Prawy Shift). Dwie zakładki: Moduły (włączanie) i Wygląd (kolory),
 * plus tryb pozycji, w którym moduły przeciąga się myszką.
 *
 * Myszka jest odczytywana wprost w render(), dzięki czemu kod nie zależy od
 * sygnatur zdarzeń myszy, które zmieniały się między wersjami Minecrafta.
 */
public class MiniModsScreen extends Screen {
	private static final int PANEL_BG = 0xF2121217;
	private static final int SIDEBAR_BG = 0xFF0D0D11;
	private static final int BORDER = 0xFF2A2A33;
	private static final int TEXT = 0xFFE8E8EE;
	private static final int DIM = 0xFF8C8C99;
	private static final int OFF = 0xFF363641;
	private static final int HOVER = 0x22FFFFFF;
	private static final int IDLE = 0x16FFFFFF;

	private static final int[] PRESETS = {
			0x5B8CFF, 0xA06BFF, 0xFF5FA2, 0xFF5555, 0xFFA040,
			0xFFD84A, 0x4ADE80, 0x3DDCFF, 0xFFFFFF, 0x0B0B10
	};

	@FunctionalInterface
	private interface Action {
		void apply(int x, int y);
	}

	private record Region(int x, int y, int w, int h, Action action, boolean draggable) {
		boolean contains(int px, int py) {
			return Gfx.inside(px, py, x, y, w, h);
		}
	}

	private enum Tab {
		MODULES("Moduły"),
		LOOK("Wygląd");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private enum Target {
		ACCENT("Akcent"),
		BACKGROUND("Tło"),
		TEXT("Tekst");

		final String label;

		Target(String label) {
			this.label = label;
		}
	}

	private final List<Region> regions = new ArrayList<>();
	private Region active;
	private ModuleType dragging;
	private int dragOffX;
	private int dragOffY;
	private boolean wasDown;

	private Tab tab = Tab.MODULES;
	private Target target = Target.ACCENT;
	private float hue;
	private float sat;
	private float val;
	private boolean positionMode;

	private int mx;
	private int my;

	public MiniModsScreen() {
		super(Component.literal("Mini Mody"));
	}

	@Override
	protected void init() {
		wasDown = this.minecraft.mouseHandler.isLeftPressed();
		syncPicker();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		Config.save();
	}

	// ---------------------------------------------------------------- wejście

	private void handleInput() {
		boolean down = this.minecraft.mouseHandler.isLeftPressed();
		if (down && !wasDown) {
			press();
		} else if (down) {
			drag();
		} else if (wasDown) {
			release();
		}
		wasDown = down;
	}

	private void press() {
		for (int i = regions.size() - 1; i >= 0; i--) {
			Region region = regions.get(i);
			if (region.contains(mx, my)) {
				region.action().apply(mx, my);
				if (region.draggable()) {
					active = region;
				}
				return;
			}
		}
		ModuleType[] types = ModuleType.values();
		for (int i = types.length - 1; i >= 0; i--) {
			int[] b = MiniModsHud.BOUNDS.get(types[i]);
			if (b != null && Gfx.inside(mx, my, b[0], b[1], b[2], b[3])) {
				dragging = types[i];
				dragOffX = mx - b[0];
				dragOffY = my - b[1];
				return;
			}
		}
	}

	private void drag() {
		if (active != null) {
			active.action().apply(mx, my);
		} else if (dragging != null) {
			moveModule();
		}
	}

	private void release() {
		active = null;
		dragging = null;
		Config.save();
	}

	private void moveModule() {
		int[] b = MiniModsHud.BOUNDS.get(dragging);
		if (b == null) {
			return;
		}
		int nx = Math.max(0, Math.min(this.width - b[2], mx - dragOffX));
		int ny = Math.max(0, Math.min(this.height - b[3], my - dragOffY));
		// przyciąganie do krawędzi i środka ekranu
		if (Math.abs(nx) < 5) {
			nx = 0;
		}
		if (Math.abs(nx + b[2] - this.width) < 5) {
			nx = this.width - b[2];
		}
		if (Math.abs(nx + b[2] / 2 - this.width / 2) < 4) {
			nx = this.width / 2 - b[2] / 2;
		}
		if (Math.abs(ny) < 5) {
			ny = 0;
		}
		if (Math.abs(ny + b[3] - this.height) < 5) {
			ny = this.height - b[3];
		}
		if (Math.abs(ny + b[3] / 2 - this.height / 2) < 4) {
			ny = this.height / 2 - b[3] / 2;
		}
		Config.Entry entry = Config.get(dragging);
		entry.x = nx;
		entry.y = ny;
	}

	// ------------------------------------------------------------ kolory/picker

	private int targetColor() {
		Config.Theme theme = Config.theme();
		return switch (target) {
			case ACCENT -> theme.accent;
			case BACKGROUND -> theme.background;
			case TEXT -> theme.text;
		};
	}

	private void setTargetColor(int rgb) {
		Config.Theme theme = Config.theme();
		switch (target) {
			case ACCENT -> theme.accent = rgb;
			case BACKGROUND -> theme.background = rgb;
			case TEXT -> theme.text = rgb;
		}
	}

	private void syncPicker() {
		float[] hsv = Gfx.rgbToHsv(targetColor());
		hue = hsv[0];
		sat = hsv[1];
		val = hsv[2];
	}

	private void applyPicker() {
		setTargetColor(Gfx.hsvToRgb(hue, sat, val));
	}

	// ----------------------------------------------------------------- rysowanie

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.mx = mouseX;
		this.my = mouseY;
		handleInput();
		regions.clear();

		Config.Theme theme = Config.theme();
		if (positionMode) {
			drawPositionMode(g, theme);
		} else {
			g.fill(0, 0, this.width, this.height, 0x55000000);
			drawPanel(g, theme);
		}
	}

	private boolean hover(int x, int y, int w, int h) {
		return Gfx.inside(mx, my, x, y, w, h);
	}

	private void region(int x, int y, int w, int h, Action action, boolean draggable) {
		regions.add(new Region(x, y, w, h, action, draggable));
	}

	private void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean selected, Runnable onClick) {
		int accent = Config.theme().accent;
		int fill = selected ? Gfx.opaque(accent) : hover(x, y, w, h) ? HOVER : IDLE;
		Gfx.roundedFill(g, x, y, x + w, y + h, fill, 2);
		int color = selected ? Gfx.contrast(accent) : TEXT;
		g.drawCenteredString(this.font, label, x + w / 2, y + (h - 8) / 2, color);
		region(x, y, w, h, (px, py) -> onClick.run(), false);
	}

	private void drawSwitch(GuiGraphics g, int x, int y, boolean on) {
		Gfx.roundedFill(g, x, y, x + 24, y + 12, on ? Gfx.opaque(Config.theme().accent) : OFF, 2);
		int knobX = on ? x + 14 : x + 2;
		Gfx.roundedFill(g, knobX, y + 2, knobX + 8, y + 10, 0xFFFFFFFF, 1);
	}

	private void drawPanel(GuiGraphics g, Config.Theme theme) {
		int pw = Math.min(400, this.width - 16);
		int ph = Math.min(264, this.height - 16);
		int px = (this.width - pw) / 2;
		int py = (this.height - ph) / 2;
		int accent = Gfx.opaque(theme.accent);

		Gfx.roundedFill(g, px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER, 2);
		Gfx.roundedFill(g, px, py, px + pw, py + ph, PANEL_BG, 2);
		region(px, py, pw, ph, (x, y) -> { }, false);

		// nagłówek
		Gfx.roundedFill(g, px + 10, py + 9, px + 20, py + 19, accent, 1);
		g.drawString(this.font, Component.literal("Mini Mody").withStyle(ChatFormatting.BOLD), px + 26, py + 10, TEXT, false);
		g.fill(px + 1, py + 28, px + pw - 1, py + 29, Gfx.argb(theme.accent, 0xAA));
		int closeX = px + pw - 22;
		int closeY = py + 6;
		Gfx.roundedFill(g, closeX, closeY, closeX + 16, closeY + 16, hover(closeX, closeY, 16, 16) ? 0x44FF5555 : IDLE, 2);
		g.drawCenteredString(this.font, "X", closeX + 8, closeY + 4, TEXT);
		region(closeX, closeY, 16, 16, (x, y) -> this.onClose(), false);

		// pasek boczny
		g.fill(px + 2, py + 29, px + 94, py + ph - 2, SIDEBAR_BG);
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) {
			final Tab t = tabs[i];
			button(g, px + 8, py + 38 + i * 24, 78, 20, t.label, tab == t, () -> {
				tab = t;
				syncPicker();
			});
		}
		button(g, px + 8, py + ph - 56, 78, 20, "Pozycje", false, () -> positionMode = true);
		button(g, px + 8, py + ph - 32, 78, 20, "Reset pozycji", false, Config::resetPositions);

		int cx = px + 102;
		int cy = py + 37;
		int cw = pw - 110;
		int ch = ph - 45;
		if (tab == Tab.MODULES) {
			drawModules(g, cx, cy, cw, ch);
		} else {
			drawLook(g, theme, cx, cy, cw);
		}
	}

	private void drawModules(GuiGraphics g, int cx, int cy, int cw, int ch) {
		ModuleType[] types = ModuleType.values();
		int rowH = Math.min(22, ch / types.length);
		for (int i = 0; i < types.length; i++) {
			final ModuleType type = types[i];
			final Config.Entry entry = Config.get(type);
			int y = cy + i * rowH;
			int h = rowH - 2;
			if (hover(cx, y, cw, h)) {
				Gfx.roundedFill(g, cx, y, cx + cw, y + h, HOVER, 2);
			} else {
				Gfx.roundedFill(g, cx, y, cx + cw, y + h, 0x0DFFFFFF, 2);
			}
			int textY = y + (h - 8) / 2;
			g.drawString(this.font, type.title, cx + 8, textY, TEXT, false);
			g.drawString(this.font, type.description, cx + 92, textY, DIM, false);
			drawSwitch(g, cx + cw - 32, y + (h - 12) / 2, entry.enabled);
			region(cx, y, cw, h, (x, yy) -> {
				entry.enabled = !entry.enabled;
				Config.save();
			}, false);
		}
	}

	private void drawLook(GuiGraphics g, Config.Theme theme, int cx, int cy, int cw) {
		// wybór elementu, którego kolor zmieniamy
		Target[] targets = Target.values();
		int chipW = (cw - 8) / 3;
		for (int i = 0; i < targets.length; i++) {
			final Target t = targets[i];
			int x = cx + i * (chipW + 4);
			button(g, x, cy, chipW, 16, t.label, target == t, () -> {
				target = t;
				syncPicker();
			});
		}

		// pole nasycenie/jasność
		final int bx = cx;
		final int by = cy + 24;
		final int bw = 130;
		final int bh = 80;
		final int cell = 5;
		int cols = bw / cell;
		int rows = bh / cell;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				float s = (c + 0.5f) / cols;
				float v = 1f - (r + 0.5f) / rows;
				g.fill(bx + c * cell, by + r * cell, bx + (c + 1) * cell, by + (r + 1) * cell,
						Gfx.opaque(Gfx.hsvToRgb(hue, s, v)));
			}
		}
		Gfx.outline(g, bx - 1, by - 1, bx + bw + 1, by + bh + 1, BORDER);
		int markX = bx + Math.round(sat * (bw - 1));
		int markY = by + Math.round((1f - val) * (bh - 1));
		g.fill(markX - 3, markY - 3, markX + 4, markY + 4, 0xFF000000);
		g.fill(markX - 2, markY - 2, markX + 3, markY + 3, 0xFFFFFFFF);
		g.fill(markX - 1, markY - 1, markX + 2, markY + 2, Gfx.opaque(targetColor()));
		region(bx, by, bw, bh, (x, y) -> {
			sat = Gfx.clamp01((x - bx) / (float) (bw - 1));
			val = 1f - Gfx.clamp01((y - by) / (float) (bh - 1));
			applyPicker();
		}, true);

		// pasek odcienia
		final int hy = by + bh + 6;
		final int hh = 10;
		for (int c = 0; c < bw / 2; c++) {
			g.fill(bx + c * 2, hy, bx + c * 2 + 2, hy + hh, Gfx.opaque(Gfx.hsvToRgb(c / (float) (bw / 2), 1f, 1f)));
		}
		Gfx.outline(g, bx - 1, hy - 1, bx + bw + 1, hy + hh + 1, BORDER);
		int hueX = bx + Math.round(hue * (bw - 2));
		g.fill(hueX - 1, hy - 2, hueX + 3, hy + hh + 2, 0xFF000000);
		g.fill(hueX, hy - 1, hueX + 2, hy + hh + 1, 0xFFFFFFFF);
		region(bx, hy - 2, bw, hh + 4, (x, y) -> {
			hue = Gfx.clamp01((x - bx) / (float) (bw - 2));
			applyPicker();
		}, true);

		// prawa kolumna: aktualny kolor, gotowe kolory, podgląd
		int rx = cx + 144;
		int color = targetColor();
		Gfx.roundedFill(g, rx, by, rx + 28, by + 14, Gfx.opaque(color), 2);
		g.drawString(this.font, String.format("#%06X", color), rx + 34, by + 3, TEXT, false);
		g.drawString(this.font, "Gotowe kolory", rx, by + 20, DIM, false);
		for (int i = 0; i < PRESETS.length; i++) {
			final int preset = PRESETS[i];
			int sx = rx + (i % 5) * 20;
			int sy = by + 32 + (i / 5) * 20;
			Gfx.roundedFill(g, sx, sy, sx + 16, sy + 16, Gfx.opaque(preset), 1);
			if (hover(sx, sy, 16, 16)) {
				Gfx.outline(g, sx - 1, sy - 1, sx + 17, sy + 17, 0xFFFFFFFF);
			} else if (preset == color) {
				Gfx.outline(g, sx - 1, sy - 1, sx + 17, sy + 17, Gfx.opaque(theme.accent));
			}
			region(sx, sy, 16, 16, (x, y) -> {
				setTargetColor(preset);
				syncPicker();
				Config.save();
			}, false);
		}
		g.drawString(this.font, "Podgląd", rx, by + 74, DIM, false);
		MiniModsHud.drawSample(g, this.font, rx + 44, by + 70, "FPS", "144");
		button(g, rx, by + 90, 96, 14, "Domyślne kolory", false, () -> {
			theme.resetColors();
			syncPicker();
			Config.save();
		});

		// suwak przezroczystości
		final int sy = hy + hh + 12;
		g.drawString(this.font, "Przezroczystość tła", cx, sy + 2, DIM, false);
		final int tx = cx + 120;
		final int tw = cw - 120 - 38;
		float t = theme.opacity / 100f;
		Gfx.roundedFill(g, tx, sy + 4, tx + tw, sy + 8, OFF, 1);
		Gfx.roundedFill(g, tx, sy + 4, tx + Math.max(2, Math.round(tw * t)), sy + 8, Gfx.opaque(theme.accent), 1);
		int knobX = tx + Math.round(tw * t);
		Gfx.roundedFill(g, knobX - 3, sy + 1, knobX + 3, sy + 11, 0xFFFFFFFF, 1);
		g.drawString(this.font, theme.opacity + "%", tx + tw + 10, sy + 2, TEXT, false);
		region(tx - 4, sy, tw + 8, 12, (x, y) -> {
			theme.opacity = Math.round(Gfx.clamp01((x - tx) / (float) tw) * 100f);
		}, true);

		// przełączniki
		drawToggleRow(g, cx, sy + 22, cw, "Cień tekstu", theme.shadow, () -> theme.shadow = !theme.shadow);
		drawToggleRow(g, cx, sy + 44, cw, "Zaokrąglone rogi", theme.rounded, () -> theme.rounded = !theme.rounded);
	}

	private void drawToggleRow(GuiGraphics g, int x, int y, int w, String label, boolean on, Runnable toggle) {
		Gfx.roundedFill(g, x, y, x + w, y + 18, hover(x, y, w, 18) ? HOVER : 0x0DFFFFFF, 2);
		g.drawString(this.font, label, x + 8, y + 5, TEXT, false);
		drawSwitch(g, x + w - 32, y + 3, on);
		region(x, y, w, 18, (px, py) -> {
			toggle.run();
			Config.save();
		}, false);
	}

	private void drawPositionMode(GuiGraphics g, Config.Theme theme) {
		// linie pomocnicze (środek ekranu)
		g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x22FFFFFF);
		g.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x22FFFFFF);

		for (ModuleType type : ModuleType.values()) {
			int[] b = MiniModsHud.BOUNDS.get(type);
			if (b == null) {
				continue;
			}
			boolean isDragged = type == dragging;
			boolean isHovered = hover(b[0], b[1], b[2], b[3]);
			if (isDragged || isHovered) {
				Gfx.outline(g, b[0] - 2, b[1] - 2, b[0] + b[2] + 2, b[1] + b[3] + 2, Gfx.opaque(theme.accent));
			} else {
				Gfx.outline(g, b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1] + b[3] + 1, 0x55FFFFFF);
			}
		}

		int bw = Math.min(330, this.width - 16);
		int bh = 28;
		int bx = (this.width - bw) / 2;
		int by = this.height - bh - 12;
		Gfx.roundedFill(g, bx - 1, by - 1, bx + bw + 1, by + bh + 1, BORDER, 2);
		Gfx.roundedFill(g, bx, by, bx + bw, by + bh, PANEL_BG, 2);
		region(bx, by, bw, bh, (x, y) -> { }, false);
		g.drawString(this.font, "Przeciągaj moduły myszką", bx + 10, by + 10, TEXT, false);
		button(g, bx + bw - 140, by + 5, 62, 18, "Reset", false, Config::resetPositions);
		button(g, bx + bw - 72, by + 5, 62, 18, "Gotowe", true, () -> {
			positionMode = false;
			Config.save();
		});
	}
}
