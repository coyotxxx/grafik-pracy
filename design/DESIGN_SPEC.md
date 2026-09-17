# GRAFIK PRACY — SPECYFIKACJA UI 1:1

**Wersja:** 1.0 · 17.09.2026
**Źródło prawdy:** pliki `mockups/*.html` w tym katalogu. Każda wartość w tym dokumencie pochodzi z nich.
**Zasada nadrzędna:** przy rozbieżności między tym plikiem a mockupem — **wygrywa mockup**. Przy rozbieżności między mockupem a Twoją intuicją — **wygrywa mockup**.

---

## 0. JAK CZYTAĆ MOCKUPY

Pliki w `mockups/` to HTML z **inline style** — każda liczba jest dokładna i przeznaczona do przepisania 1:1.

- Ignoruj: `<x-dc>`, `<helmet>`, `support.js`, `{{...}}`, `<sc-for>`, `<sc-if>`, blok `<script data-dc-script>`.
  To wrapper narzędzia do makiet, nie część projektu.
- Czytaj: wszystkie `style="..."` (kolory, wymiary, promienie, odstępy), `@keyframes` i klasy w `<style>`, strukturę DOM (kolejność elementów = kolejność w UI), teksty PL.
- `<script data-dc-script>` czytaj **tylko** po to, by odtworzyć dane przykładowe i logikę stanu (co się zmienia po kliknięciu).
- `_layout.json` = mapa kanwy: nazwy ekranów, ich wysokości i pozycje w trzech rzędach.

Konwersja jednostek: **1 px w mockupie = 1 dp w Androidzie.** Ekrany projektowane na szerokość 390 dp.

---

## 1. ZASADY DLA IMPLEMENTUJĄCEGO (obowiązkowe)

1. **Nie zmieniaj żadnego koloru, promienia, odstępu ani czasu animacji.** Jeśli uważasz, że wartość jest zła — zaimplementuj jak w mockupie i napisz o tym osobno.
2. **Nie dodawaj elementów, których nie ma w mockupie.** Żadnych dodatkowych przycisków, ikon, nagłówków, „pomocnych" podpowiedzi, FAB-ów, Snackbarów.
3. **Nie usuwaj elementów, które są w mockupie** — nawet jeśli wydają się redundantne.
4. **Nie przepisuj tekstów.** Polskie teksty są częścią projektu, łącznie z małą literą, myślnikami „–" i wielokropkami. Kopiuj znak w znak.
5. **Nie używaj domyślnych komponentów Material 3 bez pełnego przestylowania.** `Switch`, `Slider`, `Button`, `Card`, `TopAppBar` w domyślnych kolorach M3 natychmiast złamią projekt. Buduj z `Box`/`Row`/`Column` + tokeny z sekcji 2 albo przekazuj pełne `colors = ...`.
6. **Zero emoji w UI.** Ikony tylko jako wektory obrysowe (sekcja 4.1).
7. **Kolejność elementów w kolumnie jest częścią projektu.** Nie przestawiaj sekcji „dla czytelności".
8. **Jeden ekran = jeden commit.** Po każdym ekranie zrób zrzut i porównaj z mockupem side-by-side, zanim przejdziesz dalej.
9. Gdy czegoś nie ma w specyfikacji (np. stan pusty, błąd) — **zapytaj**, nie wymyślaj.

---

## 2. TOKENY

### 2.1 Kolory — motyw ciemny (domyślny)

| Token | Hex / rgba | Użycie |
|---|---|---|
| `bg` | `#0A0D0F` | tło ekranu |
| `bgElevated` | `#111417` | tło arkusza (bottom sheet) |
| `surface` | `rgba(255,255,255,0.035)` | karta na tle |
| `surfaceStrong` | `rgba(255,255,255,0.045)` | karta wyróżniona (pasek wybranego dnia) |
| `surfaceInput` | `#13171A` | pola, steppery, nieaktywne kafelki narzędzi |
| `line` | `#1F2428` | obrys karty |
| `lineStrong` | `#23282D` | obrys karty wyróżnionej / przycisku ikonowego |
| `lineInput` | `#262B30` | obrys pola / steppera |
| `lineSoft` | `#2A3036` | obrys przycisku drugorzędnego |
| `navBg` | `rgba(13,16,18,0.92)` | pasek nawigacji |
| `navLine` | `#1B1F23` | górna krawędź paska nawigacji |
| `navActiveBg` | `#17211F` | tło aktywnej zakładki |
| `ink` | `#F1F3F4` | tekst główny |
| `inkStrong` | `#E7EAEC` | liczby dni w kalendarzu |
| `ink2` | `#C5CCD1` | ikony w przyciskach |
| `ink3` | `#B7BEC3` | tekst drugorzędny w kartach |
| `inkMuted` | `#97A0A7` | podpisy, opisy |
| `inkFaint` | `#6E777E` | etykiety sekcji, notki prawne |
| `inkDisabled` | `#4E565C` | dni spoza miesiąca |
| `accent` | `#52D0B3` | akcent aplikacji |
| `accentOn` | `#06231D` | tekst na przycisku akcentowym |
| `accentTintBg` | `rgba(82,208,179,0.14)` | tło chipa akcentowego |
| `accentTintLine` | `rgba(82,208,179,0.35)` | obrys chipa akcentowego |
| `accentGlow` | `rgba(82,208,179,0.22)` | cień przycisku akcentowego |
| `ok` | `#3FA98F` | kropka „wykryto", wykres godzin |
| `warnInk` | `#FF937E` | ostrzeżenia (kolizja odpoczynku, strefa ryzyka) |
| `warnBg` | `rgba(255,147,126,0.08)` | tło ostrzeżenia |
| `warnLine` | `rgba(255,147,126,0.26)` | obrys ostrzeżenia |
| `warnInk2` | `#E6B0A2` | tekst wewnątrz ostrzeżenia |

### 2.2 Kolory — motyw jasny

| Token | Hex |
|---|---|
| `bg` | `#F7F6F2` |
| `surface` | `#FFFFFF` |
| `line` | `#E8E6DF` |
| `lineInput` | `#E4E2DB` |
| `trackBg` | `#ECEAE3` |
| `ink` | `#15181A` |
| `ink2` | `#3E454C` |
| `inkMuted` | `#5A6169` |
| `inkFaint` | `#8A9099` |
| `inkDisabled` | `#A9AEB3` |
| `accent` | `#00876D` |
| `accentTintBg` | `#E7F2EE` |
| `accentTintLine` | `#CADFD8` |
| cień karty | `0 6px 20px rgba(21,24,26,0.05)` |

### 2.3 Kolory typów dnia

Paleta jest **przeliczona i zwalidowana** pod kątem daltonizmu (deuteranopia/protanopia/tritanopia, ΔE ≥ 8 dla par sąsiednich) oraz kontrastu tekstu. **Nie podmieniaj tych wartości.**

| Typ | Tekst (ciemny motyw) | Tekst (jasny motyw) | Wypełnienie wykresów | Tło kafelka (ciemny) | Obrys kafelka (ciemny) |
|---|---|---|---|---|---|
| I — ranna 6–14 | `#DAC559` | `#7C6800` | `#AE9400` | `rgba(218,197,89,0.13)` | `rgba(218,197,89,0.30)` |
| II — popołudniowa 14–22 | `#FF937E` | `#AB331F` | `#C1351E` | `rgba(255,147,126,0.12)` | `rgba(255,147,126,0.28)` |
| III — nocna 22–6 | `#7ABDFF` | `#0465AF` | `#3587D3` | `rgba(122,189,255,0.12)` | `rgba(122,189,255,0.28)` |
| U — urlop | `#F490D9` | `#9C3084` | `#C04CA5` | `rgba(244,144,217,0.12)` | `rgba(244,144,217,0.28)` |
| w5 — wolne | `#8A939B` | `#6B7178` | — | `rgba(255,255,255,0.03)` | `#1F2428` |
| poza miesiącem | `#4E565C` | `#A9AEB3` | — | `transparent` | `#171B1E` |

Pozostałe typy dnia (paleta w ekranie „Wygląd"): `wś #F490D9`, `DWN #8FB4D9`, `bezw. #C9C06A`, `odb. #9AA6B2`.

Zestawy kolorów (ekran Wygląd → „Zestaw kolorów"), kolejność I / II / III / wolne:
- **Stonowana** `#B39338` `#C06A54` `#4E85B8` `#4C8C7A`
- **Wyrazista** `#E4B93C` `#F06A4A` `#4E9BE8` `#3FB08F`
- **Pory dnia** (domyślna) `#DAC559` `#FF937E` `#7ABDFF` `#5FB8A0`
- **Jasne kafelki** `#F3D37A` `#F8A78F` `#A6D2FA` `#93D8C4`

Paleta pojedynczego dnia (10 kolorów): `#DAC559` bursztyn · `#FF937E` koral · `#C79BF0` indygo · `#5FB8A0` szałwia · `#F490D9` malina · `#B79BF5` fiolet · `#7ABDFF` lazur · `#63D6B6` morski · `#C9C06A` oliwka · `#9AA6B2` grafit.

### 2.4 Typografia

Dwa kroje, oba do pobrania z Google Fonts i osadzenia w `res/font`:

- **Bricolage Grotesque** (600, 700) — nagłówki ekranów i wszystkie duże liczby.
- **Plus Jakarta Sans** (400, 500, 600, 700) — całe UI.

| Rola | Krój | Rozmiar | Waga | Interlinia / letter-spacing |
|---|---|---|---|---|
| H1 ekranu | Bricolage | 26–27 sp | 700 | line-height 1.0 · `-0.03em` |
| H1 kalendarza | Bricolage | 27 sp | 700 | line-height 1.0 · `-0.03em` |
| Hero liczba (Bilans) | Bricolage | 46 sp | 700 | line-height 0.9 · `-0.03em` |
| Hero liczba (Teraz, Wypłata) | Bricolage | 44–46 sp | 700 | line-height 0.9–1.0 · `-0.03em` |
| Licznik w karcie | Bricolage | 30–32 sp | 700 | line-height 1.0 · `-0.02em` |
| Tytuł karty | Jakarta | 14–15 sp | 600–700 | — |
| Treść | Jakarta | 13 sp | 400–500 | line-height 1.5 dla akapitów |
| Podpis | Jakarta | 11 sp | 400–500 | line-height 1.5 |
| Etykieta sekcji | Jakarta | 11 sp | 600 | `0.08em`, WERSALIKI, `inkFaint` |
| Mikro (nawigacja, kafelki) | Jakarta | 9–10 sp | 600 | — |

**Wszystkie liczby** (godziny, kwoty, daty, dni) renderuj z `fontFeatureSettings = "tnum"` (tabular figures). Bez tego liczniki i steppery będą „skakać".

### 2.5 Odstępy, promienie, cienie

- Margines boczny ekranu: **16 dp**. Górny padding treści: **44 dp** (pod pasek statusu).
- Odstęp między kartami: **14 dp**. Wewnątrz karty: **10–14 dp**. Padding karty: **14–16 dp**.
- Siatka kalendarza: gap **4 dp**. Kafelki narzędzi: **6 dp**. Kafelki statystyk: **8 dp**.
- Promienie: kafelek dnia **14**, karta **18–22**, przycisk **14–16**, kafelek ikony **11–15**, pole **12**, pigułka/chip **999**, arkusz dolny **26 (góra)**.
- Cień przycisku akcentowego: `0 10px 30px rgba(82,208,179,0.22)`.
- Cień arkusza dolnego: `0 -24px 60px rgba(0,0,0,0.55)`.
- Cień karty w motywie jasnym: `0 6px 20px rgba(21,24,26,0.05)`.
- **Minimalny cel dotyku: 44 dp.** Zakładki nawigacji: 48 dp.

### 2.6 Animacje

Jedna krzywa dla wszystkiego: `cubic-bezier(0.22, 1, 0.36, 1)` →
`val Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)`

| Nazwa | Co robi | Czas | Opóźnienie |
|---|---|---|---|
| `rise` | opacity 0→1, translateY 14–16 → 0 | 600–700 ms | kaskada 40–60 ms między kartami |
| `cellIn` | opacity 0→1, translateY 10→0, scale 0.94→1 | 500 ms | `0.16 s + index × 0.016 s` (42 kafelki ≈ 0,8 s) |
| `todayPulse` | pulsujący pierścień `inset 0 0 0 1.5px` + poświata | 3200 ms, w pętli | start po 1 s |
| `grow` | pasek postępu: szerokość 0 → docelowa | 1300 ms | 300 ms |
| `segIn` | segment wykresu: `scaleX 0→1`, origin left | 900–1100 ms | 250–700 ms kaskadowo |
| `barIn` | słupek: `scaleY 0→1`, origin bottom | 900 ms | kaskada 80 ms |
| count-up | liczba 0 → wartość, `1-(1-p)³` | 1100–1200 ms | 0 |
| `draw` (tarcza doby) | `stroke-dasharray 0 → 201.1` przy `r=96` | 1400 ms | 250 ms |
| `sweep` (wskazówka) | `rotate(-90°) → rotate(+104.8°)` | 1500 ms | 350 ms |
| `halo` | tło: translateY 0→-18/-22, scale 1→1.08/1.1, alternate infinite | 16–26 s | — |
| `tap` | wciśnięcie: `scale(0.93–0.97)` | 140–160 ms | — |
| `sheetUp` | arkusz: translateY 56→0 + opacity | 550 ms | — |

**Obowiązkowo:** przy włączonym „ogranicz animacje" w systemie wyłącz wszystkie powyższe (`prefers-reduced-motion` w mockupach = `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` lub `AccessibilityManager.isReduceMotionEnabled` na nowszych API). Stan końcowy ma być widoczny od razu.

Tarcza doby (ekran Teraz) — geometria dokładna: viewBox 240×240, środek 120/120, `r = 96`, `stroke-width 14`, obwód `2πr = 603,19`. Łuk zmiany = `8/24` obwodu = **201,06**, przerwa **402,13**, obrót łuku `rotate(240°)` dla startu o 22:00. Kąt wskazówki = `-90° + (godzina × 15°)`. Poświata: `drop-shadow(0 0 12px rgba(53,135,211,0.55))`.

---

## 3. ARCHITEKTURA INFORMACJI

Dolna nawigacja ma **dokładnie 4 pozycje**, w tej kolejności:

| Zakładka | Ekran | Ikona |
|---|---|---|
| Teraz | `Home` | zegar (koło + wskazówki) |
| Grafik | `Main` | kalendarz |
| Bilans | `Summary` | słupki |
| Ustawienia | `Settings` | dwa suwaki |

Ekrany **poza** nawigacją (wchodzi się z innych ekranów, mają przycisk powrotu):

```
Grafik    → Edit (tryb edycji)
          → Day (arkusz dnia)
          → Rest (odpoczynek — z paska ostrzeżenia)
Bilans    → Pay (wypłata)
          → Vacation (planer urlopu)
Ustawienia→ Cycle, Worktime, Pay, Rest, Leave, Detect, Notify, Look, Data
```

Poza aplikacją: `Widget` — widżety ekranu głównego i powiadomienie.

---

## 4. KOMPONENTY WSPÓLNE

### 4.1 Ikony

Wszystkie ikony rysowane jako wektory obrysowe: `stroke-width 1.6–1.9`, `stroke-linecap: round`, `stroke-linejoin: round`, `fill: none`, viewBox 24×24, rozmiar renderowania 15–22 dp. Kolor dziedziczony z tekstu (`currentColor`).

**Nie używaj `Icons.Filled.*` z Material** — mają inną grubość i wypełnienie. Przepisz ścieżki `<path d="...">` z mockupów do `ImageVector` (`materialIcon { path { ... } }` albo pliki `res/drawable/ic_*.xml`).

### 4.2 Kafelek dnia (kalendarz)

- Wysokość **62 dp** (tryb edycji: 60 dp), szerokość z siatki 7 kolumn, gap 4 dp.
- Promień **14 dp**, padding `7 dp / 6 dp`.
- Układ: kolumna, `space-between`.
  - Góra: rząd — numer dnia (13 sp / 600 / tnum) po lewej, godziny (9 sp / 500 / `#7E878E`) po prawej.
  - Dół: oznaczenie zmiany (13 sp / 700 / `0.06em`) w kolorze typu dnia.
- Zaznaczony: `inset 0 0 0 1.5px accent`.
- Dzisiejszy: animacja `todayPulse`.
- Wciśnięcie: `scale(0.93)`.

### 4.3 Karta

`background surface` + `1 dp line` + promień 18–22 dp + padding 14–16 dp. Karta kontekstowa (urlop, wypłata, ostrzeżenie) dostaje tło i obrys w kolorze tematu z przezroczystością **0.06–0.08 / 0.22–0.28**.

### 4.4 Przełącznik (switch)

Tor 46 × 28 dp, promień 999, tło `#262B30`, obrys `#333A40`. Uchwyt 20 dp, offset 4 dp, kolor `#8A939B`. Włączony: tor `rgba(82,208,179,0.22)`, obrys `rgba(82,208,179,0.5)`, uchwyt `accent`, przesunięcie **18 dp**, animacja 160 ms.
Cały wiersz (tekst + przełącznik) jest klikalny.

### 4.5 Stepper

Kontener: promień 14, tło `surfaceInput`, obrys `lineInput`, padding boczny 6 dp. Przyciski 34 dp (w arkuszu dnia 36–40 dp), ikona 14–16 dp. Wartość pośrodku: min. szerokość 26–58 dp, 14 sp / 700 / tnum.

### 4.6 Segment / chip wyboru

Nieaktywny: tło `surfaceInput` (`#13171A`), obrys `#262B30`, tekst `inkMuted`.
Aktywny: tło `rgba(akcent, 0.14–0.16)`, obrys `rgba(akcent, 0.42–0.45)`, tekst w kolorze akcentu.
Wysokość 38–46 dp, promień 11–14 dp, waga 600–700.

### 4.7 Przycisk główny

Wysokość 48–52 dp, promień 15–16 dp, tło `accent`, tekst `accentOn` 14–15 sp / 700, cień `accentGlow`. Wariant drugorzędny: tło przezroczyste, obrys `lineSoft`, tekst `ink2`.

### 4.8 Pasek nawigacji

Siatka 4 kolumny, gap 4 dp, padding `8 dp / 10 dp / 18 dp`, tło `navBg`, górna krawędź 1 dp `navLine`. Pozycja: kolumna — ikona 20 dp + etykieta 10 sp / 600, min. wysokość 48 dp, promień 13 dp. Aktywna: tło `navActiveBg`, kolor `accent`.

### 4.9 Suwak (slider)

`accent-color: #52D0B3`, wysokość toru 28 dp z etykietą nad nim: nazwa po lewej (13 sp), wartość po prawej (13 sp / 700 / accent / tnum). Pod suwakiem podpis 11 sp / `inkFaint`.

---

## 5. EKRANY

Poniżej: wysokość artboardu (= wysokość zawartości przy 390 dp szerokości), struktura od góry i zachowanie. Dokładne wartości — w pliku mockupu.

### 5.1 `Home` — Teraz (900)
1. Pasek: „BRYGADA A" (11/600/`0.08em`/accent) + data (13/600); po prawej przycisk ustawień 42 dp → `Settings`.
2. **Tarcza doby 240 dp** (sekcja 2.6): łuk zmiany, 8 znaczników co 3 h, wskazówka „teraz" w kolorze accent. W środku: etykieta zmiany z kropką, „9:01" (46 sp), „do startu zmiany", pigułka „22:00 – 06:00 · 8 h".
3. Trzy kafelki: godziny `104/176` → `Summary`, nadgodziny → `Summary`, dni urlopu (różowy) → `Vacation`.
4. „NAJBLIŻSZE DNI" — 5 kafelków 84 dp (dzień tygodnia / numer / oznaczenie), klikalne, zaznaczony dostaje pierścień accent; pod spodem wiersz z kropką i opisem wybranego dnia.
5. Karta wydarzenia (koralowa) → `Day`.
6. Przycisk „Otwórz grafik" + przycisk ikonowy 52 dp → `Day`.
7. Nawigacja (Teraz aktywne).

### 5.2 `Main` — Grafik (900)
1. Nagłówek: „Wrzesień **2026**" (rok w `#6E777E`), podtytuł brygady, dwa przyciski 40 dp.
2. Karta godzin: licznik count-up 104, `/ 176 h`, „do dziś", pasek 6 dp z animacją `grow` do 59 %, stopka „cały miesiąc 176 h · bilans 0 h" + „urlop 16 h".
3. **Pasek ostrzeżenia odpoczynku** (koralowy, 15 dp promień) → `Rest`. Pokazywany tylko gdy istnieje kolizja.
4. Nagłówki dni tygodnia (SO/ND w `#6E777E`).
5. Siatka 6 × 7 kafelków z kaskadą `cellIn`.
6. Pasek wybranego dnia: kafelek typu 42 dp z poświatą, nazwa dnia, opis zmiany, przycisk strzałki → `Day`.
7. Rząd: „Edytuj grafik" → `Edit` + przycisk filtra.
8. Nawigacja (Grafik aktywne).

### 5.3 `Edit` — tryb edycji (900)
1. Pasek: „Tryb edycji" + podtytuł; „Cofnij" (ikona) i **„Gotowe"** (przycisk accent) → `Main`.
2. Baner aktywnego narzędzia: kafelek z oznaczeniem, nazwa, podpowiedź, zakres godzin. Tło i obrys w kolorze wybranego narzędzia. Tło ekranu ma poświatę w tym samym kolorze.
3. Siatka kalendarza — **dotknięcie dnia maluje** aktualnym narzędziem. Dzień zmieniony ręcznie: pierścień `rgba(82,208,179,0.55)`. Nadgodziny: `+8h` w prawym górnym rogu kafelka, kolor `#DAC559`.
4. Panel dolny (promień 22 dp u góry, cień `0 -20px 50px rgba(0,0,0,0.45)`):
   - **6 kafelków narzędzi**: I, II, III, w5, U, „···" (więcej). Pod „···" kryją się: wś, DWN, bezw., odb., wyczyść.
   - **Wiersz nadgodzin jako modyfikator** (nie kolejny typ dnia): przełącznik + stepper godzin + przełącznik stawki 100 %/50 %. Włączony → bursztynowe tło i tekst „dopisywane do malowanych dni".
   - Stopka: notka + „Przywróć cykl".

### 5.4 `Day` — dzień (960, arkusz dolny)
Nad arkuszem widoczny przyciemniony (opacity 0.35) fragment kalendarza. Arkusz: uchwyt 40 × 4 dp, nagłówek z datą + przycisk zamknięcia.
1. Karta typu dnia: kafelek 48 dp, nazwa, godziny, pigułka z sumą godzin; pod spodem **5 segmentów** (I, II, III, Urlop, Wolne) — zmiana przełącza całą kartę.
2. Nadgodziny: stepper + stawka.
3. OBECNOŚĆ: wiersz „Wykryto 22:04 – 06:02" z zieloną kropką + przełącznik „Byłem w pracy".
4. WYDARZENIA: lista (godzina 14 sp/700 w kolorze III, nazwa, wiersz z ikoną dzwonka i opisem przypomnienia, kosz), następnie **„Dodaj wydarzenie"** (przerywany obrys) rozwijające formularz: godzina + nazwa + checkbox „Przypomnij dzień wcześniej o 18:00" + Anuluj/Dodaj.
5. Przycisk „Gotowe".

### 5.5 `Month-Light` — motyw jasny (844)
Kopia `Main` w tokenach z 2.2. Służy do weryfikacji motywu jasnego — ten sam układ, te same odstępy.

### 5.6 `Summary` — Bilans (1240)
1. Nagłówek „Bilans" + strzałki + pigułka miesiąca z ikoną rozwijania.
2. Hero: „PRZEPRACOWANE DO DZIŚ", licznik count-up (46 sp), chip „bilans 0 h", „norma 176 h", pasek 8 dp, stopka.
3. **Karta wypłaty** (bursztynowa) → `Pay`, z kwotą po prawej.
4. Okres rozliczeniowy: nadgodziny `0 / 104 h`, pasek, przycisk „Wszystkie okresy w 2026".
5. Rząd kafelków: nadgodziny 100 %, 50 %, prac. nd. (siatka 5 kolumn: 2 + 2 + 1).
6. Karta urlopu (różowa): 14 dni, wykorzystane, dwa boksy (zaległy / bieżący), wiersz z datami, przycisk „Kiedy najlepiej wziąć urlop" → `Vacation`.
7. Rozkład zmian: 3 paski poziome z animacją `segIn`, po prawej „X dni / Y h"; pod kreską dni pracy / wolne / urlop.
8. Nawigacja (Bilans aktywne).

### 5.7 `Pay` — Wypłata (1200)
1. Powrót „Bilans", nagłówek + chip „brutto".
2. Hero: „≈ **5 667** zł" (44 sp) + pasek składników (4 segmenty) + **notka o netto** — nie wolno jej usuwać.
3. „Z czego się składa" — 5 wierszy: kropka koloru, nazwa, wyliczenie (`160 h × 28,50 zł`), kwota. Pod kreską „Razem brutto".
4. „TWOJE STAWKI": stepper stawki (krok 0,50), **wybór podstawy dodatku nocnego** (moja stawka / minimalna krajowa — oba pokazują wyliczoną kwotę), stepper premii (krok 5 %), przełącznik „Pokazuj wypłatę w Bilansie".
5. Karta „Porównaj z paskiem".

**Logika przeliczeń (odtworzyć 1:1):**
```
podstawa      = stawka × 160
urlop         = stawka × 16
nadgodziny100 = stawka × 8 × 2          // wynagrodzenie + 100 % dodatku
dodatekNocny  = (podstawaNocna == MIN ? 30,50 : stawka) × 0,20 × 32
premia        = podstawa × procent/100
razem         = suma powyższych
```
Format kwot: spacja jako separator tysięcy, przecinek dziesiętny, zawsze 2 miejsca („5 667,20").

### 5.8 `Vacation` — Planer urlopu (1060)
Powrót „Bilans", nagłówek + wyjaśnienie, karta kontekstu cyklu z chipem „14 dni", 3 filtry (Najlepszy zysk / Najdłuższe / Najbliższe), lista propozycji. Karta propozycji: liczba dni wolnego (28 sp, kolor), zakres dat, boks „X dni urlopu", wiersz z datami urlopu, wiersz o świętach (zielony, gdy święto w środku), pasek efektywności + „Wpisz w grafik". Pierwsza pozycja wyróżniona zielonym tłem.

### 5.9 `Rest` — Odpoczynek (1060)
Powrót „Grafik", nagłówek, **karta statusu** z pulsującą poświatą („1 kolizja we wrześniu").
Karta kolizji: data, chip „8 h przerwy", **oś doby 18:00 → 18:00** (segmenty 16,7 % / 33,3 % / 33,3 % / 16,7 %; środkowy z przerywanym obrysem), podpisy godzin, wyjaśnienie „Brakuje 3 h", przyciski „Otwórz ten dzień" / „Wiem, ignoruj".
Karta odpoczynku tygodniowego: 5 tygodni, pasek (max 72 h), wartość; kolor zielony ≥ 35 h, koralowy poniżej.
Trzy przełączniki + notka prawna (art. 132 i 133 KP) — **notki nie usuwać ani nie skracać**.

### 5.10 `Settings` — hub (1160)
Nagłówek, wyróżniona karta „Mój cykl" (gradient + 3 linie opisu) → `Cycle`.
Sekcja **ROZLICZANIE**: Czas pracy i nadgodziny → `Worktime` · Stawki i wypłata → `Pay` · Odpoczynek (z licznikiem kolizji) → `Rest` · Urlop → `Leave`.
Sekcja **AUTOMATYKA**: Wykrywanie pracy (zielona kropka stanu) → `Detect` · Powiadomienia → `Notify`.
Sekcja **APLIKACJA**: Wygląd i kolory (3 próbki kolorów) → `Look` · Dane i kopia → `Data` · O aplikacji (chip „aktualna").
Każdy wiersz: kafelek ikony 40 dp + tytuł 14 sp/600 + **aktualna wartość** 11 sp/`inkMuted` + strzałka. Wartości w podpisach są częścią projektu — wypełnij je realnymi danymi z ustawień.

### 5.11 `Cycle` — Mój cykl (1200)
Karta główna (zielona): przełącznik „Wypełniaj grafik z cyklu", notka o pierwszeństwie dni ręcznych, wybór miesiąca startowego (steppery), 5 chipów zakresu, zdanie wynikowe.
SYSTEM PRACY: 4 karty wyboru (tytuł, wzór cyklu, długość, znacznik ✓).
KIERUNEK ROTACJI: 2 kafelki 58 dp. MOJA BRYGADA: A/B/C/D 52 dp.
PRZESUNIĘCIE: „−1 tydzień | tydzień 3 z 3 | +1 tydzień" + notka.
PODGLĄD 21 DNI: pasek 21 komórek 38 dp generowany **z aktualnych ustawień** (kierunek rotacji zmienia podgląd).
Przycisk „Przywróć ten miesiąc do cyklu".

### 5.12 `Worktime` — Czas pracy i nadgodziny (1120)
Okres rozliczeniowy (5 chipów w siatce 3 kolumn) + zielony wiersz „Bieżący okres: …" zmieniany dynamicznie.
Norma z kalendarza (przełącznik) + domyślna stawka nadgodzin (50/100/odbiór).
Limit nadgodzin: nagłówki kolumn USTAWOWO / ZAKŁAD, 4 wiersze kwartałów (nazwa, liczba tygodni, limit ustawowy, **pole na limit zakładowy** z placeholderem „—"), „W roku razem 408 h".
Karta informacyjna „Puste pole = limit ustawowy".

### 5.13 `Leave` — Urlop (1120)
Hero: „ZOSTAŁO NA 2026" + liczba (42 sp, różowa) + pasek wykorzystania — **liczone na żywo** z wymiaru, zaległego i stanu.
Wymiar roczny: 20 / 26 + stepper. Zaległy: stepper + **ostrzeżenie warunkowe** (bursztynowe gdy > 0, szare gdy 0).
Stan z zakładu: stepper „bieżący", przycisk „Zapisz stan na dziś" (różowy), wiersz z datą zapisu + „Wyczyść".
„JAK TO LICZYMY" — 3 punkty. Link do planera urlopu.

### 5.14 `Detect` — Wykrywanie pracy (1180)
Karta główna: **radar 180 dp** — 3 pierścienie animowane `ping` (3,4 s, offsety 0 / 1,15 / 2,3 s), okrąg przerywany, dwa okręgi wewnętrzne, pinezka. Pod nim przełączniki „Automatyczne wykrywanie" i „Zapisuj automatycznie".
Miejsce pracy: współrzędne, chip „zapisane", przycisk „Jestem teraz w pracy — zapisz miejsce", suwak promienia (100–1000, krok 50) — wartość aktualizuje też opis w karcie wyżej.
Wi-Fi: karta sieci + „Dodaj sieć, na której jesteś teraz".
Czułość: dwa suwaki (min. pobyt 10–120 krok 10; sklejanie 5–90 krok 5).
Ostatnie wykrycia: 3 wiersze + „Pokaż całą historię".

### 5.15 `Notify` — Powiadomienia (1080)
**Podgląd powiadomienia** na górze (animacja `shadePop`, wjazd od góry) — pokazuje realny wygląd.
Karta „Wydarzenia": przełącznik główny, „Dzień wcześniej" + pole godziny 18:00, „W dniu wydarzenia" + 4 chipy wyprzedzenia (10 min / 30 min / 1 h / 2 h).
Karta **„Nie budź mnie na nocce"** (niebieska) — przesuwanie powiadomień z godzin zmiany nocnej.
Karta z 4 przełącznikami: przypomnienie o zmianie, zmiana brygady w cyklu, limit nadgodzin, zaległy urlop.
Notka: powiadomienia lokalne.

### 5.16 `Look` — Wygląd i kolory (1160)
MOTYW: 3 karty z miniaturami (ciemny / jasny / systemowy — miniatura systemowego ma ukośny gradient 115°).
ZESTAW KOLORÓW: 4 wiersze z 4 próbkami 22 dp.
**PODGLĄD TYGODNIA** — 7 kafelków odzwierciedlających aktualnie wybrane kolory, aktualizowany natychmiast.
KOLOR POJEDYNCZEGO DNIA: siatka 3 × 3 z 9 typami; wybrany podświetlony.
Panel koloru: duża próbka z poświatą + nazwa typu + **10 kolorów 44 dp** (wybrany z podwójnym pierścieniem) + „Przywróć domyślne kolory".

### 5.17 `Data` — Dane i kopia (1040)
„CO JEST ZAPISANE": 3 liczniki + wyjaśnienie, że grafik liczy się z cyklu.
Kopia zapasowa (zielona): „Zapisz kopię" + przycisk udostępniania + „Wczytaj z pliku" + ostrzeżenie o nadpisaniu.
Automatyczna kopia + lista 3 plików z przyciskiem przywracania.
**OPERACJE NIEODWRACALNE** (czerwona karta, na samym dole): „Przywróć cały grafik do cyklu" i „Wyczyść wszystkie dane" + zdanie o potwierdzeniu i kopii.
Wersja aplikacji + „Sprawdź".

### 5.18 `Widget` — widżety i powiadomienie (900)
Makieta ekranu głównego telefonu (bez paska statusu — rysuje go system).
1. **Powiadomienie** z dwoma akcjami: „Będę w pracy", „Odłóż o 30 min".
2. **Widżet mały 2×2** (168 dp): „DZIŚ", oznaczenie 46 sp, godziny, pod kreską „jutro".
3. **Widżet średni** obok: „DO STARTU", 9:01, pasek + „104 / 176 h".
4. **Widżet tygodniowy** (pełna szerokość): 7 komórek 62 dp, dziś z pierścieniem, stopka z wydarzeniem i sumą godzin.
Implementacja: Glance AppWidget. Kolory i promienie jak wyżej; na widżetach tła są nieprzezroczyste (`rgba(...,0.96)`), bo system nie gwarantuje rozmycia.

---

## 6. DOSTĘPNOŚĆ (wymagane)

- Każdy element interaktywny to prawdziwy komponent klikalny z `contentDescription` — ikony bez tekstu **muszą** mieć opis (w mockupach są w `aria-label`, przepisz je).
- Kontrast tekstu ≥ 4,5:1 (≥ 3:1 dla ≥ 24 sp). Paleta z 2.1–2.3 ten warunek spełnia — nie rozjaśniaj szarości w dół.
- Kolor nigdy nie jest jedynym nośnikiem informacji: każdy typ dnia ma też **literę**, każde ostrzeżenie ma ikonę i tekst.
- Respektuj skalowanie czcionki systemowej (używaj `sp`), układy mają nie pękać przy 130 %.
- Respektuj „ogranicz animacje" (sekcja 2.6).

---

## 7. CHECKLISTA ODBIORU EKRANU

Zanim uznasz ekran za zrobiony:

- [ ] Wszystkie kolory zgadzają się co do hexa z mockupem (sprawdź pipetą na zrzucie).
- [ ] Rozmiary czcionek i wagi zgodne z sekcją 2.4.
- [ ] Promienie i odstępy zgodne (±0 dp, nie „mniej więcej").
- [ ] Wszystkie teksty PL przepisane znak w znak.
- [ ] Kolejność elementów identyczna jak w mockupie.
- [ ] Animacja wejścia obecna, z właściwym czasem i kaskadą.
- [ ] Liczby z `tnum`, nie skaczą przy zmianie.
- [ ] Cele dotyku ≥ 44 dp.
- [ ] Wszystkie ikony z opisem dla czytnika ekranu.
- [ ] Ekran wygląda poprawnie przy skalowaniu czcionki 130 % i na 360 dp szerokości.
- [ ] Zrzut ekranu porównany z mockupem side-by-side i **załączony do commita**.

---

## 8. CO JEST POZA TĄ SPECYFIKACJĄ

Zaprojektowane, ale jeszcze nieopisane jako ekrany: zamiany zmian z kolegą, kreator pierwszego uruchomienia, wysyłka grafiku (PDF/obrazek). Nie implementuj ich „z głowy" — poproś o makiety.
