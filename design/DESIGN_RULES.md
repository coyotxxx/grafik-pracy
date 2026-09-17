# REGUŁY UI — wklej do CLAUDE.md projektu

Ten projekt ma gotowy, zatwierdzony projekt interfejsu. Twoim zadaniem jest odtworzyć go **1:1**, nie zaprojektować na nowo.

## Źródła prawdy (kolejność ważności)

1. `design/mockups/*.html` — makiety z dokładnymi wartościami w `style="..."`.
2. `design/DESIGN_SPEC.md` — tokeny, komponenty, opis ekranów, animacje.
3. `design/theme/GrafikTokens.kt` — te same tokeny w Compose.

Przy rozbieżności wygrywa makieta. Przy braku informacji — **pytaj, nie wymyślaj**.

## Twarde zakazy

- ❌ Nie zmieniaj kolorów, promieni, odstępów, rozmiarów czcionek ani czasów animacji.
- ❌ Nie dodawaj elementów, których nie ma w makiecie (przycisków, ikon, nagłówków, FAB, Snackbar, dividerów).
- ❌ Nie usuwaj elementów, które są w makiecie — łącznie z notkami prawnymi i zdaniami o szacunkach.
- ❌ Nie przepisuj polskich tekstów. Kopiuj znak w znak, z myślnikami „–" i wielokropkami.
- ❌ Nie przestawiaj kolejności sekcji na ekranie.
- ❌ Nie używaj domyślnych kolorów Material 3. `Switch`, `Slider`, `Button`, `Card`, `NavigationBar` zawsze z jawnie podanymi kolorami z tokenów.
- ❌ Zero emoji w interfejsie. Ikony tylko wektorowe, obrys 1.6–1.9, zaokrąglone końce.
- ❌ Nie używaj `Icons.Filled.*` — przepisz ścieżki SVG z makiet.

## Twarde nakazy

- ✅ 1 px w makiecie = 1 dp. Ekrany projektowane na 390 dp.
- ✅ Wszystkie liczby z `fontFeatureSettings = "tnum"`.
- ✅ Jedna krzywa animacji: `CubicBezierEasing(0.22f, 1f, 0.36f, 1f)`.
- ✅ Cele dotyku ≥ 44 dp, zakładki nawigacji 48 dp.
- ✅ Każda ikona bez tekstu ma `contentDescription` (weź z `aria-label` w makiecie).
- ✅ Przy włączonym „ogranicz animacje" w systemie — wszystkie animacje wyłączone, stan końcowy od razu.
- ✅ Dolna nawigacja ma **dokładnie 4 pozycje**: Teraz · Grafik · Bilans · Ustawienia.

## Tryb pracy

- Jeden ekran = jedno zadanie = jeden commit. Nie rób dwóch naraz.
- Po każdym ekranie: zrzut z emulatora + porównanie z makietą + lista rozbieżności.
- Jeśli uważasz, że coś w projekcie jest błędem — **zaimplementuj jak w makiecie** i zgłoś to osobno w podsumowaniu.
- Przed zamknięciem zadania przejdź checklistę z sekcji 7 `DESIGN_SPEC.md`.
