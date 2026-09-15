# Do sprawdzenia na prawdziwym urządzeniu (po zakończeniu Etapu 2)

Rzeczy zaimplementowane i skompilowane, ale niezweryfikowane w 100% na
emulatorze (adb/`draganddrop` nie potrafi wiarygodnie zasymulować długiego
przytrzymania + przeciągnięcia — inny mechanizm niż surowe zdarzenia dotyku,
które czyta Compose).

- [ ] **Drag&drop reorder klipów** (`StudioScreen.kt`) — przeciągnięcie za
      uchwyt "☰" powinno zamieniać kolejność klipów. Logika (`moveItem`) była
      już wcześniej sprawdzona przez stare przyciski góra/dół, ale sam gest
      (`detectDragGesturesAfterLongPress`) nie był testowany na żywym dotyku.
