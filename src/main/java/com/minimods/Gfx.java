package com.minimods;

import net.minecraft.client.gui.GuiGraphics;

/** Pomocnicze funkcje rysowania i kolorów. */
final class Gfx {
	private Gfx() {
	}

	static int argb(int rgb, int alpha) {
		return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
	}

	static int opaque(int rgb) {
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}

	/** Procenty (0-100) na kanał alfa (0-255). */
	static int pct(int percent) {
		int p = Math.max(0, Math.min(100, percent));
		return Math.round(p * 2.55f);
	}

	static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	static boolean inside(int px, int py, int x, int y, int w, int h) {
		return px >= x && px < x + w && py >= y && py < y + h;
	}

	/** Czarny albo biały tekst - zależnie od jasności tła, żeby napis był czytelny. */
	static int contrast(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		double lum = 0.299 * r + 0.587 * g + 0.114 * b;
		return lum > 150 ? 0xFF101014 : 0xFFFFFFFF;
	}

	/** Prostokąt z zaokrąglonymi rogami. Części nie nakładają się, więc półprzezroczyste kolory wyglądają równo. */
	static void roundedFill(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int radius) {
		int h = y2 - y1;
		if (radius <= 0 || h < 5 || x2 - x1 < 5) {
			g.fill(x1, y1, x2, y2, color);
		} else if (radius == 1) {
			g.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
			g.fill(x1, y1 + 1, x2, y2 - 1, color);
			g.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
		} else {
			g.fill(x1 + 2, y1, x2 - 2, y1 + 1, color);
			g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, color);
			g.fill(x1, y1 + 2, x2, y2 - 2, color);
			g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, color);
			g.fill(x1 + 2, y2 - 1, x2 - 2, y2, color);
		}
	}

	static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y1 + 1, color);
		g.fill(x1, y2 - 1, x2, y2, color);
		g.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
		g.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
	}

	/** HSV (każda wartość 0-1) na RGB. */
	static int hsvToRgb(float h, float s, float v) {
		h = (h - (float) Math.floor(h)) * 6f;
		int i = (int) h;
		float f = h - i;
		float p = v * (1f - s);
		float q = v * (1f - s * f);
		float t = v * (1f - s * (1f - f));
		float r;
		float g;
		float b;
		switch (i) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}

	/** RGB na HSV (zwraca tablicę {h, s, v}, każda wartość 0-1). */
	static float[] rgbToHsv(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h;
		if (d == 0f) {
			h = 0f;
		} else if (max == r) {
			h = ((g - b) / d) % 6f;
		} else if (max == g) {
			h = (b - r) / d + 2f;
		} else {
			h = (r - g) / d + 4f;
		}
		h /= 6f;
		if (h < 0f) {
			h += 1f;
		}
		float s = max == 0f ? 0f : d / max;
		return new float[] {h, s, max};
	}
}
