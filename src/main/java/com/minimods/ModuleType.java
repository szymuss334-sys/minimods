package com.minimods;

/** Lista wszystkich mini modów dostępnych w menu. */
public enum ModuleType {
	FPS("FPS", "Klatki na sekundę"),
	CPS("CPS", "Kliknięcia na sekundę"),
	COORDS("Współrzędne", "Pozycja X / Y / Z"),
	DIRECTION("Kierunek", "Strona świata"),
	SPEED("Prędkość", "Bloki na sekundę"),
	PING("Ping", "Opóźnienie serwera"),
	ARMOR("Pancerz", "Wytrzymałość pancerza"),
	KEYSTROKES("Klawisze", "WASD i przyciski myszy"),
	CLOCK("Zegar", "Aktualna godzina");

	public final String title;
	public final String description;

	ModuleType(String title, String description) {
		this.title = title;
		this.description = description;
	}
}
