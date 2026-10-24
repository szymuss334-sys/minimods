# Mini Mody (Fabric, Minecraft 1.21.11)

Menu mini modów w stylu Lunar Client. Otwierasz je klawiszem **Prawy Shift**
(zmienisz go w Sterowaniu, kategoria „Mini Mody”).

## Co potrafi
- **9 modułów:** FPS, CPS (lewy | prawy), współrzędne, kierunek (strona świata + kąt),
  prędkość, ping, pancerz (ikony i kolorowa wytrzymałość), klawisze (WASD + myszka), zegar.
- **Zakładka „Moduły”:** włączanie i wyłączanie przełącznikami.
- **Zakładka „Wygląd”:** własne kolory – akcent, tło i tekst (pole kolorów, pasek odcienia,
  gotowe kolory), przezroczystość tła, cień tekstu, zaokrąglone rogi. Kolor akcentu
  zmienia też wygląd samego menu.
- **Tryb pozycji** (przycisk „Pozycje”): przeciągasz moduły myszką w dowolne miejsce,
  z przyciąganiem do krawędzi i środka ekranu.
- Wszystko zapisuje się samo w `config/minimods.json`.

## Jak zbudować
Potrzebujesz Javy 21 i internetu (Gradle pobierze Minecrafta i Fabric).
Najprościej otwórz folder w IntelliJ IDEA i uruchom zadanie `build`
(albo w terminalu z zainstalowanym Gradle 9.x: `gradle build`).

Gotowy plik: `build/libs/minimods-1.1.0.jar`

## Jak uruchomić
1. Zainstaluj Fabric Loader 0.18.1 lub nowszy dla Minecrafta 1.21.11.
2. Wrzuć do folderu `mods`: Fabric API (0.141.2+1.21.11 lub nowszy) oraz `minimods-1.1.0.jar`.
3. Wejdź do świata i naciśnij Prawy Shift.

## Testowanie w grze bez budowania jara
    gradle runClient
