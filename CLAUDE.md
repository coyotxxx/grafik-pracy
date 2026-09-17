# Grafik Pracy — zasady pracy w tym repozytorium

## Gałęzie i warianty

- **`main`** — działająca aplikacja Macieja. Na tej gałęzi powstają wydania.
- **`nowy-wyglad`** — przebudowa interfejsu. Tu wolno pracować nad wyglądem.

Na gałęzi `nowy-wyglad` są **dwa warianty aplikacji**:

| Wariant | Identyfikator | Co zawiera |
|---|---|---|
| `klasyczny` | `pl.grafik.pracy` | dokładnie to, co w `src/main` — bez zmian |
| `nowy` | `pl.grafik.pracy.nowy` | `src/main` + `src/nowy` (nowy wygląd, tokeny, fonty) |

Budowanie: `./gradlew assembleNowyDebug` albo `assembleKlasycznyDebug`.

**Nie ruszaj `app/src/main/` przy pracy nad nowym wyglądem.** Nowe ekrany piszesz
w `app/src/nowy/`. Modele widoku (`Vm`, `PresenceVm`, `UpdateVm`), domena, baza
i wykrywanie pracy są wspólne i zostają bez zmian — nowy wygląd pokazuje ten sam stan
inaczej, nie liczy go inaczej.

---

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

---

## Odstępstwa od makiet — decyzja Macieja z 17.09.2026

Reguła „przy rozbieżności wygrywa makieta" ma **trzy wyjątki**. Makiety pokazują
rzeczy, które Maciej wcześniej świadomie odrzucił. Obowiązują wcześniejsze ustalenia:

1. **Planer urlopu (`Vacation`)** — **bez przycisku „Wpisz w grafik"**.
   Planer jest tylko do przeglądania. Maciej: *„Tak, ale bez przycisku »Wpisz urlop«"*.

2. **Czas pracy (`Worktime`)** — stawka nadgodzin ma **tylko 50 % i 100 %**.
   Bez opcji „odbiór" — odbiór godzin to inny mechanizm, nie był uzgodniony.

3. **Czas pracy (`Worktime`)** — **bez przełącznika „Norma z kalendarza"**.
   Norma jest zawsze ustawowa (art. 130 KP). Normę zakładową usunięto w v1.23,
   gdy Maciej zapytał „po co nam to" — nie wraca.

Jeśli makieta pokazuje coś jeszcze, co wygląda na cofnięcie wcześniejszej decyzji,
**zapytaj**, zamiast odtwarzać.
