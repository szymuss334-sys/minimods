package com.minimods;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Rysuje wszystkie włączone mini mody na ekranie. */
public final class MiniModsHud {
	/** Ostatnio narysowane pozycje modułów {x, y, szerokość, wysokość} - używa ich edytor do przeciągania. */
	public static final Map<ModuleType, int[]> BOUNDS = new EnumMap<>(ModuleType.class);

	private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final int LINE_H = 11;

	private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();
	private static boolean lastAttack;
	private static boolean lastUse;

	private static int frames;
	private static int fps;
	private static long fpsWindowStart = System.nanoTime();

	private static double speed;
	private static double lastX;
	private static double lastZ;
	private static boolean hasLast;

	private MiniModsHud() {
	}

	@FunctionalInterface
	private interface Painter {
		void paint(GuiGraphics graphics, int x, int y);
	}

	private record Widget(int width, int height, Painter painter) {
	}

	private record Line(String label, String value, int valueColor) {
		static Line of(String label, String value) {
			return new Line(label, value, -1);
		}
	}

	/** Wołane co tick gry (20 razy na sekundę) - liczy prędkość. */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			hasLast = false;
			speed = 0;
			return;
		}
		if (hasLast) {
			speed = Math.hypot(player.getX() - lastX, player.getZ() - lastZ) * 20.0;
		}
		lastX = player.getX();
		lastZ = player.getZ();
		hasLast = true;
	}

	/** Wołane co klatkę przez HUD API. */
	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		updateFps();

		Minecraft mc = Minecraft.getInstance();
		updateClicks(mc);
		BOUNDS.clear();
		if (mc.player == null || mc.options.hideGui) {
			return;
		}

		Font font = mc.font;
		Config.Theme theme = Config.theme();
		int screenW = mc.getWindow().getGuiScaledWidth();
		int screenH = mc.getWindow().getGuiScaledHeight();

		for (ModuleType type : ModuleType.values()) {
			Config.Entry entry = Config.get(type);
			if (!entry.enabled) {
				continue;
			}
			Widget widget = build(type, mc, font, theme);
			if (widget == null) {
				continue;
			}
			int x = Mth.clamp(entry.x, 0, Math.max(0, screenW - widget.width()));
			int y = Mth.clamp(entry.y, 0, Math.max(0, screenH - widget.height()));
			widget.painter().paint(graphics, x, y);
			BOUNDS.put(type, new int[] {x, y, widget.width(), widget.height()});
		}
	}

	/** Mały podgląd wyglądu modułu (używany w menu przy zmianie kolorów). */
	public static void drawSample(GuiGraphics graphics, Font font, int x, int y, String label, String value) {
		Widget widget = textPanel(font, Config.theme(), List.of(Line.of(label, value)));
		widget.painter().paint(graphics, x, y);
	}

	private static void updateFps() {
		frames++;
		long now = System.nanoTime();
		if (now - fpsWindowStart >= 1_000_000_000L) {
			fps = frames;
			frames = 0;
			fpsWindowStart = now;
		}
	}

	private static void updateClicks(Minecraft mc) {
		long now = System.currentTimeMillis();
		boolean attack = mc.options.keyAttack.isDown();
		boolean use = mc.options.keyUse.isDown();
		if (attack && !lastAttack) {
			LEFT_CLICKS.addLast(now);
		}
		if (use && !lastUse) {
			RIGHT_CLICKS.addLast(now);
		}
		lastAttack = attack;
		lastUse = use;
		while (!LEFT_CLICKS.isEmpty() && now - LEFT_CLICKS.peekFirst() > 1000) {
			LEFT_CLICKS.pollFirst();
		}
		while (!RIGHT_CLICKS.isEmpty() && now - RIGHT_CLICKS.peekFirst() > 1000) {
			RIGHT_CLICKS.pollFirst();
		}
	}

	private static Widget build(ModuleType type, Minecraft mc, Font font, Config.Theme theme) {
		LocalPlayer player = mc.player;
		return switch (type) {
			case FPS -> textPanel(font, theme, List.of(Line.of("FPS", String.valueOf(fps))));
			case CPS -> textPanel(font, theme, List.of(
					Line.of("CPS", LEFT_CLICKS.size() + " | " + RIGHT_CLICKS.size())));
			case COORDS -> textPanel(font, theme, List.of(
					Line.of("X", String.valueOf(player.getBlockX())),
					Line.of("Y", String.valueOf(player.getBlockY())),
					Line.of("Z", String.valueOf(player.getBlockZ()))));
			case DIRECTION -> {
				float yaw = Mth.wrapDegrees(player.getYRot());
				int index = Math.floorMod(Math.round((yaw + 180f) / 45f), 8);
				int bearing = Math.floorMod(Math.round(yaw + 180f), 360);
				yield textPanel(font, theme, List.of(Line.of(COMPASS[index], bearing + "°")));
			}
			case SPEED -> textPanel(font, theme, List.of(
					Line.of("Prędkość", String.format("%.1f bl/s", speed))));
			case PING -> {
				int latency = -1;
				if (mc.getConnection() != null) {
					PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
					if (info != null) {
						latency = info.getLatency();
					}
				}
				yield textPanel(font, theme, List.of(
						Line.of("Ping", latency >= 0 ? latency + " ms" : "-")));
			}
			case ARMOR -> armorPanel(font, theme, player);
			case KEYSTROKES -> keystrokes(font, theme, mc);
			case CLOCK -> textPanel(font, theme, List.of(Line.of("", LocalTime.now().format(TIME))));
		};
	}

	private static void drawBackground(GuiGraphics g, Config.Theme theme, int x, int y, int w, int h) {
		int bg = Gfx.argb(theme.background, Gfx.pct(theme.opacity));
		Gfx.roundedFill(g, x, y, x + w, y + h, bg, theme.rounded ? 2 : 0);
		int bar = Gfx.opaque(theme.accent);
		if (theme.rounded) {
			g.fill(x, y + 2, x + 2, y + h - 2, bar);
		} else {
			g.fill(x, y, x + 2, y + h, bar);
		}
	}

	private static void drawText(GuiGraphics g, Font font, Config.Theme theme, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, theme.shadow);
	}

	private static Widget textPanel(Font font, Config.Theme theme, List<Line> lines) {
		int maxLine = 0;
		for (Line line : lines) {
			int w = font.width(line.value());
			if (!line.label().isEmpty()) {
				w += font.width(line.label()) + 5;
			}
			maxLine = Math.max(maxLine, w);
		}
		final int width = maxLine + 14;
		final int height = lines.size() * LINE_H + 5;
		return new Widget(width, height, (g, x, y) -> {
			drawBackground(g, theme, x, y, width, height);
			for (int i = 0; i < lines.size(); i++) {
				Line line = lines.get(i);
				int ty = y + 3 + i * LINE_H;
				int tx = x + 8;
				if (!line.label().isEmpty()) {
					drawText(g, font, theme, line.label(), tx, ty, Gfx.opaque(theme.accent));
					tx += font.width(line.label()) + 5;
				}
				int valueColor = line.valueColor() >= 0 ? Gfx.opaque(line.valueColor()) : Gfx.opaque(theme.text);
				drawText(g, font, theme, line.value(), tx, ty, valueColor);
			}
		});
	}

	private static Widget armorPanel(Font font, Config.Theme theme, LocalPlayer player) {
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		List<ItemStack> stacks = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		for (EquipmentSlot slot : slots) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			stacks.add(stack);
			if (stack.getMaxDamage() > 0) {
				int left = stack.getMaxDamage() - stack.getDamageValue();
				texts.add(left + "/" + stack.getMaxDamage());
				float fraction = Gfx.clamp01(left / (float) stack.getMaxDamage());
				colors.add(Gfx.hsvToRgb(fraction * 0.33f, 0.7f, 1f));
			} else {
				texts.add("OK");
				colors.add(theme.text);
			}
		}
		if (stacks.isEmpty()) {
			return textPanel(font, theme, List.of(Line.of("Pancerz", "brak")));
		}
		int maxText = 0;
		for (String text : texts) {
			maxText = Math.max(maxText, font.width(text));
		}
		final int width = 4 + 16 + 4 + maxText + 8;
		final int height = stacks.size() * 18 + 4;
		return new Widget(width, height, (g, x, y) -> {
			drawBackground(g, theme, x, y, width, height);
			for (int i = 0; i < stacks.size(); i++) {
				int rowY = y + 2 + i * 18;
				g.renderItem(stacks.get(i), x + 5, rowY);
				drawText(g, font, theme, texts.get(i), x + 25, rowY + 5, Gfx.opaque(colors.get(i)));
			}
		});
	}

	private static Widget keystrokes(Font font, Config.Theme theme, Minecraft mc) {
		final int box = 20;
		final int gap = 2;
		final int width = box * 3 + gap * 2;
		final int mouseH = 16;
		final int height = box * 2 + gap * 2 + mouseH;
		final boolean w = mc.options.keyUp.isDown();
		final boolean a = mc.options.keyLeft.isDown();
		final boolean s = mc.options.keyDown.isDown();
		final boolean d = mc.options.keyRight.isDown();
		final boolean lmb = mc.options.keyAttack.isDown();
		final boolean rmb = mc.options.keyUse.isDown();
		final int left = LEFT_CLICKS.size();
		final int right = RIGHT_CLICKS.size();

		return new Widget(width, height, (g, x, y) -> {
			drawKey(g, font, theme, x + box + gap, y, box, box, "W", w);
			drawKey(g, font, theme, x, y + box + gap, box, box, "A", a);
			drawKey(g, font, theme, x + box + gap, y + box + gap, box, box, "S", s);
			drawKey(g, font, theme, x + (box + gap) * 2, y + box + gap, box, box, "D", d);
			int mouseY = y + (box + gap) * 2;
			int mouseW = (width - gap) / 2;
			drawKey(g, font, theme, x, mouseY, mouseW, mouseH, "LPM " + left, lmb);
			drawKey(g, font, theme, x + mouseW + gap, mouseY, width - mouseW - gap, mouseH, "PPM " + right, rmb);
		});
	}

	private static void drawKey(GuiGraphics g, Font font, Config.Theme theme, int x, int y, int w, int h,
			String label, boolean pressed) {
		int fill = pressed ? Gfx.argb(theme.accent, 0xE6) : Gfx.argb(theme.background, Gfx.pct(theme.opacity));
		Gfx.roundedFill(g, x, y, x + w, y + h, fill, theme.rounded ? 1 : 0);
		int color = pressed ? Gfx.contrast(theme.accent) : Gfx.opaque(theme.text);
		int textX = x + (w - font.width(label)) / 2;
		int textY = y + (h - 8) / 2;
		g.drawString(font, label, textX, textY, color, theme.shadow && !pressed);
	}
}
