# Pakiet projektowy „Grafik pracy" — instrukcja przekazania do Claude Code

## 1. Co wrzucić do repo

```
<repo>/
└── design/
    ├── DESIGN_SPEC.md          ← pełna specyfikacja (tokeny, komponenty, 18 ekranów)
    ├── DESIGN_RULES.md         ← krótkie reguły, treść do CLAUDE.md
    ├── theme/GrafikTokens.kt   ← gotowe tokeny w Compose (do app/src/.../ui/theme/)
    └── mockups/                ← 18 makiet HTML + _layout.json (źródło prawdy)
```

Potem **dopisz do `CLAUDE.md` w korzeniu repo** całą treść `DESIGN_RULES.md`. To najważniejszy krok — reguły muszą być w kontekście **każdej** sesji, nie tylko tej, w której o nich powiesz.

## 2. Dlaczego makiety HTML, a nie zrzuty ekranu

Claude Code odtwarza interfejs znacznie dokładniej z kodu niż z obrazka. W makietach każda wartość jest zapisana wprost (`padding: 14px`, `background: rgba(218,197,89,0.13)`, `animation-delay: .16s`) — nie ma miejsca na interpretację. Zrzuty ekranu prowadzą do zgadywania kolorów i odstępów.

Pliki `.html` otwierają się w przeglądarce — możesz je oglądać, a Claude Code może je czytać.

## 3. Kolejność implementacji

Trzymaj tę kolejność — każdy kolejny ekran korzysta z komponentów poprzedniego:

1. `theme/GrafikTokens.kt` + fonty + podstawowe komponenty (karta, przełącznik, stepper, chip, przycisk, pasek nawigacji)
2. `Main` — Grafik (kafelek dnia to najczęściej używany komponent w aplikacji)
3. `Day` — arkusz dnia
4. `Edit` — tryb edycji
5. `Home` — Teraz (tarcza doby)
6. `Summary` — Bilans
7. `Settings` + podstrony: `Cycle`, `Worktime`, `Leave`, `Detect`, `Notify`, `Look`, `Data`
8. `Pay`, `Rest`, `Vacation`
9. `Month-Light` — weryfikacja motywu jasnego
10. `Widget` — widżety (Glance)

## 4. Gotowe prompty

**Start sesji:**

> Przeczytaj `design/DESIGN_RULES.md` i `design/DESIGN_SPEC.md` (sekcje 1–4). Potem przeczytaj `design/mockups/Main.html`. Nie pisz jeszcze kodu — wypisz listę komponentów, które trzeba zbudować, i wartości tokenów, których użyjesz.

**Jeden ekran:**

> Zaimplementuj ekran Grafik według `design/mockups/Main.html` i sekcji 5.2 `DESIGN_SPEC.md`. Wszystkie wartości przepisz dokładnie z makiety — kolory, promienie, odstępy, rozmiary czcionek, czasy i opóźnienia animacji. Nie dodawaj ani nie usuwaj żadnego elementu. Teksty polskie skopiuj znak w znak. Na koniec wypisz tabelę: element → wartość z makiety → wartość w Twoim kodzie.

**Weryfikacja:**

> Zrób zrzut ekranu z emulatora, otwórz `design/mockups/Main.html` i porównaj oba. Wypisz wszystkie rozbieżności: kolor, odstęp, rozmiar, brakujące i nadmiarowe elementy. Popraw i pokaż zrzut ponownie.

**Gdy zacznie improwizować:**

> Zmieniłeś wygląd w stosunku do makiety. Wróć do `design/mockups/<plik>.html` i przywróć oryginalne wartości. Reguła z CLAUDE.md: przy rozbieżności wygrywa makieta.

## 5. Co działa w praktyce

- **Jeden ekran = jedna sesja.** Długie sesje to moment, w którym model zaczyna „ulepszać".
- **Wymagaj tabeli porównawczej** (element → makieta → kod) po każdym ekranie. To wymusza faktyczne sprawdzenie zamiast deklaracji „zrobione zgodnie z projektem".
- **Zrzut ekranu w commicie.** Bez tego weryfikacja jest iluzoryczna.
- Przy poprawkach mów **„zmień tylko X"** i dodaj **„nie dotykaj niczego innego"** — inaczej przy okazji zostanie przebudowana połowa ekranu.
- Jeśli model twierdzi, że coś w projekcie jest błędne — niech zaimplementuje jak w makiecie i zgłosi uwagę osobno. Decyzja należy do Ciebie.

## 6. Czego świadomie brakuje

Zaprojektowane i opisane jest 18 ekranów. **Nie ma** makiet dla: zamiany zmian z kolegą, kreatora pierwszego uruchomienia, wysyłki grafiku (PDF / obrazek). Jeśli Claude Code je „domyśli", powstaną ekrany niepasujące do reszty — lepiej najpierw zamówić makiety.
