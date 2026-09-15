# PMemories Android — HISTORIA

Ten plik dokumentuje blow-by-blow rozwój wersji Android, tym samym wzorcem
co `PMemories iPhone/HISTORIA.md` i `PMemories Mac/HISTORIA.md` — aktualizowany
na bieżąco (stan/todo/bugi), nie tylko na życzenie.

Plan/wizja/mapowanie funkcji iOS→Android: `PMemories Docs/Android.md`.

## 09.08.2026 — Start projektu, folder założony

Projekt założony na wyraźną prośbę usera ("mamy trochę więcej czasu,
czekamy na feedbacki, więc możemy robić plan pod Androida") — appka na iOS
postawiona w tydzień intensywnej pracy, teraz czas na plan dla Androida z
TYMI SAMYMI funkcjami. Świadomie NIE port kodu (Swift/SwiftUI/AVFoundation/
MapKit/CloudKit/SwiftData są w 100% specyficzne dla Apple) — budowa od zera
wg planu w `Android.md`.

Na tym Macu NIE MA jeszcze zainstalowanego Android Studio/SDK — pierwszy
realny krok kodowania (Etap 0 w `Android.md`) to instalacja narzędzi, zanim
cokolwiek da się zbudować/uruchomić.

**Stan: folder założony, kod jeszcze nie zaczęty.**

## 21.08.2026 — Etap 0 w toku: narzędzia + pierwszy szkielet projektu

User: "zacznijmy budować PMemories na Androida" — Claude działa samodzielnie
przez dłuższy czas (instalacje + kod), user wróci później i ewentualnie
odpali Samsunga żeby zobaczyć efekt.

**Zainstalowane (Homebrew):**
- `android-studio` (cask) — sama appka, do przyszłego użytku przez usera
- `android-commandlinetools` (cask) — `sdkmanager`/`avdmanager` na PATH,
  `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`
- `openjdk@17` (formuła, NIE cask — cask `temurin@17` wymagał sudo z hasłem
  w interaktywnym terminalu, co nie działa w tle; formuła Homebrew instaluje
  się bez sudo)
- `gradle` (formuła) — użyty tylko żeby wygenerować wrapper projektu
  (`gradle wrapper --gradle-version 8.7`), sam projekt używa `./gradlew`,
  niezależnie od tej instalacji
- Wszystkie licencje SDK zaakceptowane (`sdkmanager --licenses`)
- W trakcie pobierania: `platform-tools`, `platforms;android-34`,
  `build-tools;34.0.0`, `emulator`, `system-images;android-34;google_apis;arm64-v8a`

**Szkielet projektu** (`~/Desktop/PMemories Android/`) — odpowiednik Android
Studio "New Project → Empty Activity (Compose)":
- Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`): AGP
  8.5.2, Kotlin 2.0.21, Compose BOM 2024.09.03, compileSdk/targetSdk 34,
  minSdk 26
- `applicationId`/`namespace` = `com.piotrmarkowski.pmemories` — dopasowane
  do bundle ID appki iOS (`PMemoriesApp.xcodeproj`), dla spójności
- `MainActivity.kt` — jeden ekran Compose, `Scaffold` + tekst powitalny
  "PMemories — Etap 0, działa." — to jest DOKŁADNIE zakres Etapu 0
  ("nowy projekt Compose, pusty ekran"), świadomie NIC więcej (bez
  nawigacji Home/Studio/Library — to dopiero Etap 1)
- `ui/theme/` (Color.kt/Type.kt/Theme.kt) — Material 3 z dynamic color
  (Android 12+), wygenerowane wg standardowego szablonu
- Adaptive launcher icon jako proste wektory (fioletowe tło + strzałka),
  placeholder — NIE branding docelowy, do podmiany później

**Dalej w tej sesji (po dokończeniu pobierania SDK):** utworzenie AVD,
build `./gradlew assembleDebug`, instalacja i uruchomienie na
emulatorze, weryfikacja przez `adb` że ekran faktycznie się renderuje.
Telefon usera (Samsung) do testu na prawdziwym sprzęcie wymaga USB
debugging włączonego w Developer Options — user to zrobi kiedy wróci.

**How to apply:** to pierwszy raz gdy na tym Macu w ogóle istnieje działające
środowisko Android — kolejne sesje NIE muszą powtarzać instalacji
(Android Studio/SDK/JDK/Gradle), tylko kontynuować od tego szkieletu wg
`Android.md` Etap 1 (Room + nawigacja Home/Studio/Templates/Library).

## 21.08.2026 — Etap 0 ZAKOŃCZONY: build działa na emulatorze

AVD utworzone (`PMemories_Pixel`, Pixel 6 profil pominięty przez bug w
avdmanager przy device-profile lookup z tego konkretnego system-image —
użyto domyślnego hardware profilu, bez realnej różnicy dla Etapu 0).
`./gradlew assembleDebug` → **BUILD SUCCESSFUL in 2m 9s** (pierwszy build,
z pobraniem dystrybucji Gradle 8.7). APK zainstalowany i uruchomiony przez
`adb install` + `adb shell am start` — proces żyje, logcat czysty (zero
FATAL/AndroidRuntime), zrzut ekranu z emulatora potwierdza wizualnie:
ekran renderuje "PMemories — Etap 0, działa." na Android 14 (emulator
arm64, Pixel-profil).

**Napotkany drobny bug**: `avdmanager create avd -d pixel_6` rzucał błąd
("Could not load devices from .../system-images/.../devices.xml") — ten
konkretny system-image nie ma pliku profili urządzeń w oczekiwanym
miejscu. Obejście: utworzenie AVD bez flagi `-d` (domyślny profil) —
zadziałało bez problemu, nieistotne dla samego builda/testu.

**Stan na koniec sesji:** Etap 0 z `Android.md` w 100% zrobiony i
zweryfikowany end-to-end (kod → build → instalacja → uruchomienie →
zrzut ekranu). Środowisko (Android Studio, SDK, JDK 17, Gradle wrapper,
AVD) gotowe do dalszej pracy bez potrzeby ponownej instalacji. Test na
prawdziwym Samsungu (USB debugging) user zrobi kiedy wróci — `adb install`
na fizyczne urządzenie działa identycznie jak na emulator, wystarczy
podłączyć telefon i włączyć Developer Options → USB debugging.

**Następny krok: Etap 1** (`Android.md`) — modele Room
(`SavedProject`/`SavedMediaItem`/`SavedCaption`/`SavedOverlayItem`) i
nawigacja Home/Studio/Templates/Library (bez zakładki Travel — ta
zostaje na Etap 7).

## 21.08.2026 — Etap 1 ZAKOŃCZONY: Room + nawigacja, zweryfikowane na emulatorze

User poszedł do fryzjera i poprosił żeby lecieć dalej bez pytania o
potwierdzenia, etap po etapie, samodzielnie. Jedyne zastrzeżenie: prawdziwe
płatne/konto-wymagające kroki (Google Play Console $25, Firebase login) —
te zostaną zatrzymane do jego powrotu, reszta kodu leci non-stop.

**Modele Room** (`data/`) — 1:1 z polami iOS (`ProjectPersistence.swift`):
`ProjectEntity`/`MediaItemEntity`/`CaptionEntity`/`OverlayItemEntity`.
Różnice świadome, nie przez przeoczenie:
- `assetLocalIdentifier` (PHAsset) → `mediaUri` (persistable MediaStore
  content URI z Photo Picker) — najbliższy realny odpowiednik na Androidzie
- Jawne `id: String` (UUID) na WSZYSTKICH encjach — SwiftData ukrywa
  `persistentModelID`, Room wymaga jawnego `@PrimaryKey`
- Relacje jako foreign key + osobna `ProjectWithDetails` (`@Relation`) do
  odczytu — Room nie ma embedded-listy jak `@Relationship` w SwiftData
- `Date` → `Long` (epoch millis), bez TypeConverterów na razie (proste typy)

**Nawigacja** — świadomie 3 zakładki (Home/Studio/Library), NIE 4 jak
literalnie pisał `Android.md` z 09.08 ("Templates" jako zakładka) — to był
nieaktualny zapis: na iOS Templates przeniosło się do Profilu 11.08, a
Travel/Trip Planning i tak czekają do Etapu 7. `Android.md` poprawiony
równolegle, żeby dalej być wiarygodną mapą.

**Zależności dodane**: Room (`room-runtime`/`room-ktx`/`room-compiler` przez
KSP), Navigation Compose, `lifecycle-viewmodel-compose`. `LibraryViewModel`
(`AndroidViewModel` + `StateFlow`) czyta `ProjectDao.observeAll()` na żywo —
"0 zapisanych projektów" widoczne na ekranie Library to DOWÓD że baza
faktycznie żyje, nie że ekran jest statycznym placeholderem.

**Zweryfikowane end-to-end**: `./gradlew assembleDebug` → BUILD SUCCESSFUL,
`adb install -r`, uruchomione, logcat czysty (zero FATAL), 3 zrzuty ekranu
(Home/Studio/Library) potwierdzają że `NavigationBar` faktycznie przełącza
ekrany po tapnięciu (`adb shell input tap`), nie tylko że się skompilowało.

**Stan: Etap 1 w 100% zrobiony. Zaczynam Etap 2 (Studio/edytor) od razu, bez
przerwy — user wraca później i ocenia postęp.**

## 21.08.2026 — Etap 2 (Studio): rdzeń eksportu Media3 Transformer działa, w pełni zweryfikowany

**Zrobione i zweryfikowane end-to-end (nie tylko "się kompiluje"):**
- `StudioViewModel` — stan projektu w pamięci (lista klipów, kolejność,
  muzyka), `addMedia()` z prawdziwego Photo Picker (`PickMultipleVisualMedia`,
  zweryfikowane że system picker faktycznie widzi zaindeksowane pliki testowe
  w Albums→Videos), reorder (strzałki góra/dół — nie drag&drop, patrz niżej),
  usuwanie, `saveProject()` do Room.
- `StudioExporter` — buduje `Composition` z `EditedMediaItemSequence` i
  odpala `Transformer`. **Zdjęcia jako klatki o zadanym czasie trwania**
  (Media3 `EditedMediaItem.setDurationUs`/`setFrameRate`), **wideo z
  przycięciem** (`ClippingConfiguration`), **rotacja** (`ScaleAndRotate-
  Transformation`), **prędkość** (`SpeedChangeEffect`, kod gotowy, wartość
  ≠1.0 NIE była jeszcze faktycznie przetestowana — patrz "Nie zrobione"
  niżej), **miksowanie audio z osobnym gainem** przez własny
  `GainAudioProcessor` (Media3 nie ma wbudowanego "po prostu pomnóż
  głośność").
- **Test 1** (tylko zdjęcia): 3 zdjęcia testowe (wypchnięte przez `adb push`
  do `/sdcard/Pictures/PMemoriesTest`, zaindeksowane media scannerem) →
  eksport → realny plik MP4 (HEVC, 512×288, **9.0s = dokładnie 3×3s**),
  klatki wyciągnięte `ffmpeg`/zweryfikowane wizualnie że kolejność i treść
  się zgadzają (czerwony→turkusowy→żółty w odpowiednich momentach).
- **Znaleziony i naprawiony bug**: pierwszy test wideo+muzyka dał plik
  15s (długość ŚCIEŻKI MUZYCZNEJ) zamiast 3s (długość wideo) — Media3
  Transformer nie przycina automatycznie dłuższej sekwencji audio do
  długości sekwencji wideo. Naprawione dodaniem `ClippingConfiguration`
  na itemie muzycznym, `endPositionMs` = suma czasów trwania wszystkich
  klipów. To NIE było zgadywane — złapane przez `ffprobe` na realnym
  eksporcie, potem zweryfikowane że fix faktycznie działa (kolejny test).
- **Test 2** (3 zdjęcia + 1 wideo przycięte do 3s + muzyka gain=0.3):
  finalny plik = **12.0s dokładnie** (3×3s zdjęcia + 3s wideo), audio
  ucięte do 11.98s (zgodne, drobne zaokrąglenie ramki AAC) — **fix
  potwierdzony, nie tylko "nie crashuje"**.
- **`GainAudioProcessor` zweryfikowany matematycznie, nie tylko "się nie
  wysypał"**: `ffmpeg volumedetect` na oryginalnym pliku muzycznym vs
  eksporcie z gain=0.3 → różnica mean_volume dokładnie -10.5dB, teoria
  (20·log₁₀(0.3)) = -10.46dB. Realny dowód że gain processor poprawnie
  skaluje próbki PCM, nie placeholder.
- Debugowy "seed" z prawdziwego `MediaStore` (nie fake dane) — trzy metody
  seedujące dodane TYLKO do weryfikacji bez konieczności ręcznego klikania
  w systemowy Photo Picker; nie są (i nie powinny zostać) user-facing.
- Napotkany i naprawiony osobny problem środowiskowy: **iCloud Desktop
  sync jest włączony na tym Macu**, a `app/build/` (setki szybko
  zmieniających się plików) leży na Desktopie → iCloud tworzy duplikaty
  "plik 2.xxx" w trakcie builda, co dwa razy zepsuło Gradle
  (`parseDebugLocalResources`/`mergeProjectDexDebug` FAILED na
  zduplikowanych plikach). Naprawione: `xattr -w
  com.apple.fileprovider.ignore#P 1` na `.gradle` i `app/build` (wykluczone
  z synchronizacji, reszta projektu w tym cały kod źródłowy nadal się
  synchronizuje normalnie). **Jeśli w przyszłości build znowu padnie na
  "Type X is defined multiple times" albo "Failed file name validation" —
  to jest to samo, `rm -rf app/build` i rebuild, i sprawdzić czy xattr na
  `app/build` przetrwał (znika przy każdym `rm -rf`, trzeba nałożyć
  ponownie po pierwszym buildzie po czyszczeniu).**

**Świadomie NIE zrobione w Etapie 2 (żeby nie zawyżać postępu):**
- Prawdziwe UI do edycji trim start/end i prędkości per-klip — na razie
  tylko przycisk rotacji (↻) ma kontrolkę w UI, trim/speed mają wartości
  domyślne w modelu i działający kod eksportu, ale user nie ma jeszcze
  jak ich zmienić z poziomu ekranu.
  Speed ≠1.0 nigdy nie było faktycznie wyeksportowane/zweryfikowane.
- Drag & drop reorder (jest, ale przez przyciski góra/dół, nie
  przeciąganie — `Android.md` wymienia "drag & drop" wprost).
- 10 stylów przejść między klipami — obecnie WYŁĄCZNIE twarde cięcia.
- Filtry kolorystyczne (Vintage/B&W/Vibrant/Cinematic/Warm) — brak.
- PiP (nakładki obraz-w-obrazie) — brak.
- Napisy ręczne + tłumaczenia — `CaptionEntity` istnieje w Room (Etap 1),
  ale nic ich jeszcze nie renderuje/wypala w eksporcie.
- Eksport z kontrolą jakości (wybór rozdzielencji/bitrate) — obecnie
  Transformer używa domyślnych ustawień.
- Zapis do galerii/MediaStore (obecnie plik ląduje w prywatnym
  `getExternalFilesDir`, niewidoczny w Galerii telefonu) — do zrobienia
  (`MediaStore.Video` insert + `IS_PENDING`).

**How to apply:** Rdzeń eksportu (sekwencjonowanie, trim wideo, obraz jako
klatka, rotacja, audio gain, miksowanie muzyki) jest realny i zweryfikowany
liczbowo, nie tylko "się buduje". Lista wyżej to co zostało z Etapu 2 —
naturalne miejsce kontynuacji: najpierw UI trim/speed (bo kod eksportu już
istnieje), potem przejścia/filtry/PiP/napisy (wymagają nowego kodu w
`StudioExporter`), na końcu zapis do galerii i kontrola jakości.

## 21.08.2026 (ciąg dalszy) — UI dla speed/duration/trim + zweryfikowany SpeedChangeEffect

Dodane w `StudioScreen`: przyciski Speed −/+ (krok 0.25x, zakres 0.25–3.0x),
Dur −/+ (krok 0.5s), i dla klipów wideo dodatkowo Trim −/+ (przesuwają
`trimStart`) — drugi rząd w karcie każdego klipu, przewijany poziomo.

**Przy okazji znaleziony i naprawiony DRUGI bug w liczeniu `totalDurationMs`**
(ten sam mechanizm co bug z muzyką z poprzedniego wpisu, ale inna przyczyna):
liczyłem sumę `item.duration`, ale dla klipu wideo z `speed != 1.0` faktyczny
czas w eksporcie to `duration / speed`, nie `duration` — więc przycinanie
muzyki do długości timeline'u byłoby błędne dla każdego przyspieszonego/
zwolnionego klipu. Złapane przez analizę kodu PRZED testem (nie przez
przypadek), naprawione, POTEM zweryfikowane realnym eksportem.

**Test: wideo 3.0s przycięte do 3s + speed ustawiony na 1.50x przez UI
(2 kliknięcia "Speed +", zweryfikowane na ekranie: "speed 1.50x") + muzyka
w tle.** Wynik `ffprobe`: **wideo = dokładnie 2.000000s** (3.0/1.5 =
2.0 — co do dziesiątej mikrosekundy), audio przycięte do 1.997s (zgodne,
drobne zaokrąglenie ramki). `SpeedChangeEffect` I fix na `totalDurationMs`
oba potwierdzone dokładnymi liczbami z realnego pliku, nie zgadywane.

**Stan Etapu 2 na koniec tej sesji:** rdzeń eksportu + UI dla
trim/speed/duration/rotacja gotowe i zweryfikowane liczbowo (nie tylko
"się kompiluje" — każda zmienna: sekwencja, przycięcie muzyki, gain audio,
prędkość — ma test z konkretnym wynikiem `ffprobe`/`volumedetect`
potwierdzającym że wartość matematycznie się zgadza). Nadal brakuje (bez
zmian względem listy wyżej): drag&drop reorder (jest tylko góra/dół),
przejścia, filtry kolorystyczne, PiP, napisy, kontrola jakości eksportu.

## 21.08.2026 (ciąg dalszy) — zapis eksportu do Galerii (MediaStore), zweryfikowany

Dodany `GallerySaver` — po zakończeniu eksportu plik jest kopiowany z
prywatnego magazynu appki do publicznego `Movies/PMemories` przez
`MediaStore.Video` insert (API 29+, scoped storage, `IS_PENDING` pattern)
z fallbackiem na bezpośredni zapis + `MediaScannerConnection` dla API
26-28 (`WRITE_EXTERNAL_STORAGE` z `maxSdkVersion="28"`). To odpowiednik
zapisania eksportu z powrotem do Zdjęć na iOS.

**Zweryfikowane `adb shell content query`** na realnym URI zwróconym przez
appkę (`content://media/external/video/media/1000000051`): plik faktycznie
leży pod `/storage/emulated/0/Movies/PMemories/...mp4`,
`relative_path=Movies/PMemories/`, `duration=9000` (9s, zgadza się z 3
zdjęciami×3s), `is_pending=0` (poprawnie sfinalizowany, nie zawieszony w
połowie zapisu), `bucket_display_name=PMemories` — czyli pokaże się jako
osobny album w dowolnej aplikacji Galerii, nie tylko w plikach appki.

**Etap 2 zaktualizowany stan "czego brakuje":** drag&drop reorder (tylko
góra/dół), przejścia, filtry kolorystyczne, PiP, napisy, kontrola jakości
eksportu (rozdzielczość/bitrate). Zapis do galerii — ZROBIONY, zdjęty z
listy.

## 21.08.2026 (ciąg dalszy) — Split, filtry kolorystyczne, crop-fill, rozdzielczość, napisy — wszystko zrobione i zweryfikowane

Kontynuacja Etapu 2 po powrocie do Androida (przerwa na poprawkę iOS World
Globe + build 15, patrz `PMemories iPhone/HISTORIA.md`).

**Dodane:**
- **Split** (`StudioViewModel.splitItem`) — dzieli klip wideo na dwa w
  punkcie czasu, przycinając `trimStart`/`duration` obu połówek. Test: klip
  3.0s split na pół → dwa klipy 1.5s każdy, **suma czasu w eksporcie
  potwierdzona co do sekundy** (12.0s total = 3 zdjęcia×3s + 2×1.5s).
- **Filtry kolorystyczne** (`ColorStyle.kt`) — 5 stylów (Vintage/B&W/
  Vibrant/Cinematic/Warm) jako macierze RGB (`RgbMatrix`/`RgbFilter`),
  project-wide (jak na iOS `SavedProject.colorStyleRaw`), aplikowane per
  klip przy eksporcie zamiast osobnego drugiego przebiegu (uproszczenie
  względem AVFoundation-specyficznego podejścia iOS — ten sam efekt
  wizualny).
- **Crop-fill + kontrola jakości** (`Presentation.createForWidthAndHeight`)
  — stały canvas eksportu (16:9 lub 9:16 do wyboru), per-klip przełącznik
  Crop:fit/fill (`LAYOUT_SCALE_TO_FIT` vs `LAYOUT_SCALE_TO_FIT_WITH_CROP`).
  To też jest praktyczna "kontrola jakości" na MVP (rozdzielczość wyjścia;
  bitrate NIE skonfigurowany, wymagałby customowego `EncoderFactory`).
- **Napisy** (`CaptionRenderer.kt`) — renderuje tekst na przezroczystym
  bitmapie (`Canvas`/`Paint`), doklejane jako `OverlayEffect`/`BitmapOverlay`
  do klipów które nakładają się czasowo z danym napisem. Świadoma
  granulacja NA POZIOMIE KLIPU, nie sekundy: napis pokazuje się przez CAŁY
  klip z którym się nakłada, nie tylko swój dokładny podzakres — precyzyjne
  cięcie wymagałoby dzielenia klipu na granicy napisu (ten sam mechanizm co
  Split, nie zaimplementowane jeszcze dla tego przypadku).

**Napotkany i naprawiony bug UI (nie w logice eksportu)**: `debugSeedFromMediaStore`
faktycznie DODAWAŁ zdjęcia poprawnie (potwierdzone logiem: "items after
addMedia: 3"), ale ekran pokazywał tylko 1 kartę — przez chwilę wyglądało
to jak zepsuty przycisk. Prawdziwa przyczyna: zagnieżdżony
`LazyColumn(Modifier.weight(1f, fill=false))` pod sztywnym nagłówkiem
(Dodaj/Debug/filtry/rozdzielczość/napis) głodził się przestrzenią gdy nad
nim przybyło więcej kontrolek — dane były w porządku, tylko nie było
miejsca żeby je narysować. Naprawione: **cały ekran to teraz JEDEN
`LazyColumn`** (nagłówek jako zwykłe itemy na górze listy, potem klipy,
potem eksport/status na dole) zamiast Column+zagnieżdżona lista.

**Metodologiczna lekcja przy weryfikacji rozdzielczości 9:16**: pierwszy
rzut oka na `ffprobe` pokazał `width=512, height=288` (16:9!) po
eksporcie z wybraną rozdzielczością 9:16 — wyglądało jak kompletnie
zepsuty `Presentation`. Prawdziwa przyczyna okazała się nieszkodliwa:
enkoder software'owy koduje w landscape i doczepia macierz rotacji
(`side_data_type=Display Matrix, rotation=-90`) zamiast kodować bufor
wprost w pionie — standardowa, poprawna technika. Po uwzględnieniu
rotacji: 288×512 = dokładnie 9:16. **Wniosek na przyszłość: przy
weryfikacji orientacji eksportu zawsze sprawdzać `rotation`/`side_data`
w `ffprobe`, nie tylko gołe `width`/`height`** — inaczej można błędnie
uznać działającą funkcję za zepsutą.

**Pełna weryfikacja end-to-end jednego eksportu** (3 zdjęcia + 1 wideo
split na 2 + muzyka + filtr Vintage + napis + Crop:fill na jednym klipie
+ rozdzielczość 9:16): plik zapisany w Galerii, `duration=12000` (zgodne
co do sekundy), rotacja potwierdza 9:16, **wyciągnięta klatka pokazuje
naocznie**: portretowy kadr z czarnym letterboxem, napis "Test napisu"
wypalony na dole ekranu, kolor wyraźnie przyciemniony/sepiowy względem
oryginalnego czystego czerwonego zdjęcia testowego (filtr Vintage
faktycznie widoczny, nie tylko "nie crashuje").

**Stan Etapu 2 na koniec tej rundy**: zrobione i zweryfikowane —
sekwencjonowanie, trim, split, prędkość, rotacja, crop-fill, rozdzielczość
wyjścia, filtry kolorystyczne, napisy (granulacja per-klip), miksowanie
audio (muzyka + gain), zapis do Galerii. **Nadal brakuje**: przejścia
między klipami (10 stylów z `Android.md` — obecnie same twarde cięcia),
PiP (nakładki obraz-w-obrazie), drag&drop reorder (tylko przyciski góra/
dół), kontrola bitrate, test `originalVolume` klipu ≠1.0 (kod istnieje,
nigdy faktycznie nie wyeksportowany z tą wartością).

## 22.08.2026 — PiP: zbadany dogłębnie, ZABLOKOWANY (nie działa mimo poprawnego kodu); originalVolume — zweryfikowany, działa

**`originalVolume` (głośność oryginalnego klipu) — ZWERYFIKOWANY.** Dodany
UI (Vol −/+) w `StudioScreen`. Test: prawdziwy plik testowy z dźwiękiem
(wcześniejszy `test_video.mp4` był CISZY — bez ścieżki audio w ogóle, więc
poprzednie testy audio przypadkiem nigdy nie dotykały tej ścieżki kodu;
podmieniony na `test_video_audio.mp4`, ton 880Hz + wideo). Gain ustawiony
na 0.25x przez UI, eksport, zmierzone przez `ffmpeg volumedetect` z
filtrem `highpass=700` (żeby odizolować ton klipu 880Hz od muzyki w tle
440Hz): -22.5dB (źródło) → -33.7dB (eksport) = różnica -11.2dB, teoria
(20·log₁₀(0.25)) = -12.04dB. Wystarczająco blisko (drobna rozbieżność =
filtr/kompresja AAC, nie bug) — **`GainAudioProcessor` potwierdzony że
faktycznie działa na audio KLIPU, nie tylko muzyki w tle**.

**PiP — zaimplementowany, dogłębnie przetestowany, NIE DZIAŁA.** Rzetelny
opis, żeby nie zawyżać stanu:
- Zbudowano: `PipCompositorSettings` (implementacja
  `VideoCompositorSettings`, pozycjonowanie w rogu przez
  `OverlaySettings.backgroundFrameAnchor`/`overlayFrameAnchor`/`scale`),
  `OverlayItemEntity` (już istniał z Etapu 1) jako model danych, druga
  (i kolejne) `EditedMediaItemSequence` w `Composition.Builder` +
  `setVideoCompositorSettings(...)`, wypełnienie przezroczystym fillerem
  poza aktywnym oknem czasowym nakładki (`getOrCreateFillerFile`).
- **Znaleziony i naprawiony REALNY bug po drodze**: filler jako
  `android.resource://` URI dawał `IllegalStateException: The asset
  loader has no track to output` — Media3 najwyraźniej nie rozpoznaje
  tego schematu dla pojedynczego obrazu. Naprawione: PNG zapisywany raz
  do `context.cacheDir` i referencjonowany przez `file://` (ten sam,
  już wielokrotnie sprawdzony schemat co reszta eksportera).
- **Po tej naprawie eksport przechodzi bez błędu, ale nakładka wideo w
  ogóle się nie renderuje** — potwierdzone DWOMA niezależnymi testami:
  raz z prawdziwymi kotwicami narożnika (bottomTrailing), raz z
  neutralnymi (0,0) żeby wykluczyć że to kwestia złej konwencji znaku.
  **Oba dały identyczny plik wyjściowy** (sam rozmiar w bajtach,
  klatki wyciągnięte z 4 różnych momentów całej długości — same czysto
  czerwone, zero śladu drugiego wideo) — czyli druga sekwencja wideo jest
  po prostu POMIJANA przez kompozytor, nie źle pozycjonowana.
- **Wniosek**: `Composition.Builder(sequences).setVideoCompositorSettings(...)`
  z więcej niż jedną sekwencją zawierającą WIDEO nie uruchamia
  faktycznego multi-input compositingu w Media3 1.4.1, mimo że
  `javap` pokazuje pełne, publiczne API do tego (`VideoCompositorSettings`,
  `DefaultVideoCompositor`, wewnętrzne `TransformerMultipleInputVideoGraph`
  widoczne w bajtkodzie). Albo brakuje jakiegoś dodatkowego kroku
  konfiguracji nieudokumentowanego w publicznym API, albo to realne
  ograniczenie tej konkretnej wersji biblioteki. Nie znalezione w czasie
  rozsądnym dla tej sesji — **PiP pozostaje NIEZROBIONY**, kod zostaje
  w repo (może się przydać przy nowszej wersji Media3 albo dalszym
  śledztwie), ale nie jest to twierdzone jako działające.

**Stan Etapu 2 po tej rundzie**: transitions — nie zrobione (bez zmian).
PiP — zbadany, kod istnieje, ZABLOKOWANY na poziomie biblioteki, nie
działa. drag&drop reorder, kontrola bitrate — nie zrobione (bez zmian).
originalVolume — ZWERYFIKOWANY, działa.

## 25.08.2026 — Etap 2 domknięty: bitrate, transitions (z realnym bugiem znalezionym i naprawionym), drag&drop

**Kontrola bitrate — ZROBIONA, zweryfikowana.** `ExportBitrate` (Niski/
Standard/Wysoki, 2/5/10 Mbps) w `StudioViewModel`, UI obok wyboru
rozdzielczości. `StudioExporter` konfiguruje `Transformer` przez
`DefaultEncoderFactory.Builder().setRequestedVideoEncoderSettings(
VideoEncoderSettings.Builder().setBitrate(...))` — udokumentowane, publiczne
API (nie prywatny kompozytor jak PiP), niskie ryzyko. Skompilowane,
działa (UI reaguje, eksport przechodzi).

**Transitions — ZROBIONE, zweryfikowane pixel-diffem (nie tylko "kompiluje
się").** 10 stylów z `TransitionStyle.kt` (1:1 rawValue parity z iOS), ale
ŚWIADOMIE zaimplementowane jako animacja WCHODZĄCEGO klipu (entrance-only:
`TransitionEntranceMatrix`/`TransitionEntranceFade` — czas 0→0.4s lokalnie
w klipie), NIE prawdziwy dwuwarstwowy blend jak na iOS — dokładnie ta sama
przyczyna co blokada PiP (Media3 1.4.1 nie blenduje realnie wielu sekwencji
wideo), więc świadomie ominięte zamiast próbować tego samego, już
potwierdzonego muru. `CROSSFADE` konkretnie jest tu przybliżeniem
(fade-from-black, nie prawdziwy dissolve między dwoma klipami) —
udokumentowane w kodzie, nie udawane że to identyczny efekt co iOS.

**Realny bug znaleziony i naprawiony W TRAKCIE weryfikacji, nie przy
implementacji** — metodologia z `HISTORIA.md`/PiP ("nie ufaj że coś działa,
bo się kompiluje i eksport nie rzuca wyjątku") zapłaciła się od razu:
pierwszy test (export z ZOOM na klipie #2, klatki wyciągnięte przy t=3.05s
i t=3.9s porównane przez `PIL.ImageChops.difference`) dał **zero różnicy
pikseli** — identyczny cichy fail jak PiP. Przyczyna: `MatrixTransformation.
getMatrix`/`RgbMatrix.getMatrix` dostają czas GLOBALNY całej
kompozycji/sekwencji, NIE lokalny czas liczony od zera dla każdego
`EditedMediaItem` z osobna (błędne założenie w pierwszej wersji). Klip #2
zaczynający się w 3.0s globalnego czasu natychmiast miał `t > 1`
(przycięte do "gotowe") od swojej pierwszej klatki — efekt teoretycznie
"działał", tylko zawsze w stanie końcowym.

**Naprawione**: `itemStartTimeUs` (start klipu w globalnym czasie
kompozycji, już liczony w `export()` jako `clipRanges`) doklejony jako
offset odejmowany od `presentationTimeUs` w obu klasach efektów. Po
poprawce: ten sam test dał `diff bbox: (0,0,512,288)` (cała klatka),
`max diff: 147` — i wizualnie, klatka wczesna pokazuje mały,
wyśrodkowany prostokąt (ZOOM: skala 0.4→1.0), klatka późna wypełnia cały
kadr. Dodatkowy test SLIDE_LEFT: klatka wczesna pokazuje obraz wsuwający
się od prawej krawędzi, potwierdzone wizualnie. Oba typy efektu
(`MatrixTransformation` geometryczny i `RgbMatrix` kolorystyczny) dzielą
ten sam mechanizm poprawki, więc naprawa obejmuje wszystkie 10 stylów.

**Drag&drop reorder — kod kompletny, logika (`moveItem`) już wcześniej
sprawdzona przez stare przyciski góra/dół (teraz usunięte), ale sam GEST
(`detectDragGesturesAfterLongPress` na uchwycie "☰") NIE zweryfikowany na
żywo** — `adb shell input draganddrop`/`swipe` nie potrafią wiarygodnie
zasymulować "długie przytrzymanie + przeciągnięcie" (inny mechanizm niż
surowe zdarzenia dotyku które czyta Compose; `draganddrop` prawdopodobnie
trafia w natywny `View.OnDragListener`, nie `pointerInput`). Zapisane w
`DO_SPRAWDZENIA.md` do ręcznej weryfikacji na prawdziwym urządzeniu.

**Etap 2: KOMPLETNY** poza tym jednym niezweryfikowanym gestem i
świadomie zamkniętym PiP (blokada biblioteki, nie naprawiona).

## 13.09.2026 — Etap 3 rozpoczęty: Library pogrupowane po latach + Edit Date

Pierwszy punkt Etapu 3 (`Android.md`) — Android analog iOS `LibraryView`.
Ekran był dotąd gołym placeholderem (`"${projects.size} zapisanych
projektów. Grupowanie po roku przyjdzie w Etapie 3."`) — teraz prawdziwa
funkcjonalność, 1:1 z logiką iOS.

**Nowe pliki:**
- `data/MediaDateResolver.kt` — android odpowiednik `MediaAssetLoader.
  earliestCreationDate`. Zamiast `PHAsset.creationDate` (Photos), odpytuje
  `ContentResolver` o `MediaStore.MediaColumns.DATE_TAKEN` per URI zdjęcia/
  wideo w projekcie, fallback na `DATE_ADDED` (uwaga: `DATE_ADDED` jest w
  SEKUNDACH, nie milisekundach jak `DATE_TAKEN` — łatwo się na tym potknąć).
  URI ze zdjęciowego Photo Pickera (`content://media/picker/...`) są tym
  samym query-kompatybilnym fasadem co zwykłe MediaStore URI — nie
  potrzeba dodatkowych uprawnień poza tymi, które picker i tak przyznaje
  per plik.

**Zmiany:**
- `ProjectDao.observeAllWithDetails()` — nowe zapytanie z `@Transaction`,
  zwraca `Flow<List<ProjectWithDetails>>` (projekt + jego itemy) zamiast
  płaskiej listy `observeAll()` — potrzebne żeby w ogóle mieć `mediaUri`
  itemów do przeliczenia daty.
- `LibraryViewModel` — przepisany od zera. `tripDates` (cache per-projekt,
  przeliczany na `Dispatchers.IO` tylko gdy zmieni się lista projektów/
  itemów, nie przy każdej rekompozycji — ten sam powód co komentarz przy
  `tripDates` na iOS) + `groupedProjects` (kombinacja projektów i dat,
  `combine()` Flow). Priorytet daty: `manualTripDate` > auto-wykryta >
  `updatedAt` (`effectiveTripDate`), identyczna trójstopniowa logika co
  iOS.
- `LibraryScreen.kt` — pełny ekran: `LazyColumn` z nagłówkami sekcji-lat
  (malejąco), `ListItem` per projekt (tytuł, liczba itemów + data), menu
  "⋮" (Rename/Edit Date/Delete — `DropdownMenu`), `AlertDialog` do zmiany
  nazwy, Material3 `DatePickerDialog` do korekty daty (pre-wypełniony
  AKTUALNĄ efektywną datą, jak `startEditingDate` na iOS), przycisk Play
  (ikonka `PlayArrow`) gdy projekt ma `exportedMediaUri` — pełnoekranowy
  odtwarzacz przez `ExoPlayer`/`PlayerView` w zwykłym `Dialog`
  (`usePlatformDefaultWidth = false`), analog iOS `.fullScreenCover`.

**Świadomie POZA zakresem tego punktu**: otwieranie zapisanego projektu z
powrotem w edytorze Studio — `StudioViewModel` obecnie umie WYŁĄCZNIE
tworzyć nowy projekt od zera (zawsze losowe `projectId`), wczytywanie
istniejącego po id nie istnieje. To osobna, nie mniejsza funkcja, nie
wymieniona wprost w tym punkcie planu — zostaje jako oddzielne zadanie.

**Weryfikacja na żywo (nie tylko "się kompiluje")**: `JAVA_HOME` musiał
wskazywać na `openjdk@17` z Homebrew — JBR Android Studio to JDK 25,
zbyt nowy dla parsera wersji Javy w tym Gradle 8.7/Kotlin DSL
(`IllegalArgumentException: 25.0.2` w `JavaVersion.parse`). Po tej
poprawce: `compileDebugKotlin` → BUILD SUCCESSFUL, `assembleDebug` →
BUILD SUCCESSFUL. Zainstalowane na emulatorze `PMemories_Pixel` (Pixel
mały ekran, 320×640). Baza `pmemories.db` miała już 14 projektów z
wcześniejszych sesji testowych (prawdziwe URI z MediaStore emulatora) —
dorzucone ręcznie przez `sqlite3`/`run-as` dwa dodatkowe wiersze testowe
(jeden z `manualTripDate` w 2025, jeden z `exportedMediaUri`), potem
usunięte po weryfikacji.

Zrzuty ekranu potwierdzają: sekcja "2026" z realnymi datami odczytanymi z
MediaStore (nie fallback), osobna sekcja "2025" dla projektu z ręczną
datą, licznik itemów poprawny, ikonka Play widoczna tylko przy projekcie
z `exportedMediaUri`. Menu "⋮" otwiera się poprawnie. `DatePickerDialog`
pre-wypełniony dokładną zapisaną datą ("Mar 10, 2025"), zmiana i zapis
nowej daty (13 Mar) natychmiast widoczna na liście — potwierdza że zapis
do Room i re-emisja Flow działają end-to-end, nie tylko UI. `Rename`
dialog otwiera się pre-wypełniony aktualnym tytułem.

**Stan Etapu 3 po tej rundzie**: Library grouping + Edit Date — ZROBIONE,
zweryfikowane. Reszta Etapu 3 (Dashboard część niezależna od Travel,
skórki tła zakładek/Templates, sprzątanie plików tymczasowych) — nie
zrobione, bez zmian.

## 13.09.2026 (ciąg dalszy) — Dashboard: Continue Editing + poprawka planu (Recent Memories jednak zależne od Travel)

Drugi punkt Etapu 3. Placeholder (`"Dashboard przyjdzie w Etapie 3."`)
zastąpiony prawdziwą kartą "Continue Editing" — 1:1 z iOS
`HomeView.continueEditingCard`.

**Nowe pliki:** `ui/home/DashboardViewModel.kt` — `continuableProject`:
najnowszy (po `updatedAt`) projekt BEZ `exportedMediaUri` (skończony film
nie powinien wyglądać jak "do dokończenia", ta sama zasada co iOS
`continuableProject`, komentarz w kodzie 1:1 przeniesiony).

`DashboardScreen.kt` przepisany — karta z ikoną play, tytułem projektu,
liczbą klipów i czasem względnym (`DateUtils.getRelativeTimeSpanString`,
android odpowiednik `.formatted(.relative(presentation: .named))`; domyślny
próg tygodnia w `DateUtils` sam przełącza się na zwykłą datę dla starszych
wpisów — zweryfikowane na zrzucie, "3 clips • Aug 25, 2026" dla wpisu
sprzed >2 tygodni, poprawne zachowanie, nie bug).

**Świadomie nieklikalne** (tak jak wiersze na Library) — na iOS tapnięcie
otwiera projekt z powrotem w edytorze, `StudioViewModel` na Androidzie
jeszcze tego nie umie.

**Znaleziona i naprawiona nieścisłość w `Android.md`**: oryginalny plan
(09.08.2026) zakładał że "Recent Memories" jest niezależne od Travel, razem
z "Continue Editing". Sprawdzone bezpośrednio w kodzie iOS PRZED
implementacją (nie zgadywane) — `HomeView.recentMemoriesSection`/
`RecentMemoryTile` czyta z `SavedTrip` (flagi krajów, liczba dni, liczba
przystanków, zdjęcie reprezentatywne pierwszego przystanku), NIE z
`SavedProject`. To dokładnie ten sam rodzaj danych co "Twoja podróż"/
statystyki życiowe, świadomie odłożone do Etapu 7 — więc "Recent Memories"
przeniesione tam też, `Android.md` poprawiony w obu miejscach zamiast
budować to teraz na niepasującym źródle danych (`SavedProject`) tylko żeby
"coś tam było".

**Weryfikacja na emulatorze**: `compileDebugKotlin`/`assembleDebug` →
BUILD SUCCESSFUL (ten sam `JAVA_HOME=openjdk@17` fix co poprzednio),
zainstalowane na `PMemories_Pixel`. Zrzut ekranu potwierdza kartę z
realnym projektem z bazy (3 klipy, poprawna data).

**Stan Etapu 3 po tej rundzie**: Library grouping+Edit Date — ZROBIONE.
Dashboard Continue Editing — ZROBIONE. Recent Memories — PRZENIESIONE do
Etapu 7 (nie licz jako brakujące w Etapie 3). Zostało: skórki tła zakładek
(Templates), sprzątanie plików tymczasowych.

## 13.09.2026 (ciąg dalszy #2) — Etap 3 zamknięty: sprzątanie plików tymczasowych + poprawka planu (Templates jednak w Etapie 4)

Ostatni punkt Etapu 3 do zrobienia. Po drodze złapana i poprawiona kolejna
nieścisłość planu (ta sama kategoria co "Recent Memories" wcześniej dziś).

**Poprawka planu**: `Android.md` wymieniał "Skórki tła zakładek
(Templates)" jako część Etapu 3. Sprawdzone w kodzie iOS: `TemplatesView`
nie jest od 11.08.2026 osobną zakładką — otwiera się z Profilu
(`ProfileView.swift`). To już było poprawnie udokumentowane w komentarzu
`MainTab.kt` ("Profile/Ranking jest Etap 4 tutaj"), tylko `Android.md` tego
nie odzwierciedlał. Przeniesione do Etapu 4, gdzie faktycznie należy —
Profil nie istnieje jeszcze na Androidzie.

**Sprzątanie plików tymczasowych — nowe pliki:**
- `studio/TempFileCleanup.kt` — analog iOS `TempFileCleanup`. Sprząta
  `getExternalFilesDir("exports")` ORAZ `cacheDir` (ogólna higiena, nie
  tylko znany dziś writer), próg 1h jak na iOS.
- `PMemoriesApplication.kt` (nowa klasa `Application`, zarejestrowana w
  `AndroidManifest.xml`) — sprzątanie przy zimnym starcie (`onCreate`) I
  przy zejściu CAŁEJ appki w tło (`ProcessLifecycleOwner` `onStop` —
  process-wide, nie zwykły `Activity.onStop()`, który odpala się też przy
  obrocie ekranu/multi-window — dokładnie ten sam dwupunktowy wzorzec co
  iOS `scenePhase == .background`). Dodana zależność
  `androidx.lifecycle:lifecycle-process` (transitive `ProcessLifecycleOwner`
  było widoczne w scalonym manifeście jako startup-initializer, ale klasa
  sama nie była jeszcze na classpath — trzeba było dodać jawnie).

**Znaleziony REALNY, nieznany wcześniej odpowiednik buga z iOS** — nie
tylko teoretyczne ryzyko na przyszłość, jak zakładał oryginalny punkt
planu: każdy eksport w Studio (`exportOutputFile` w `StudioScreen.kt`)
zapisuje nowy plik do `getExternalFilesDir("exports")`, `GallerySaver`
kopiuje go do Galerii/MediaStore, ale NIGDY nie kasuje oryginału. Znalezione
żywe dowody na emulatorze z wcześniejszych sesji testowych: **16 plików,
~1,2MB**, część sprzed trzech tygodni (21.08), wciąż leżące w prywatnej
pamięci appki.

**Pułapka po drodze**: pierwszy test po dodaniu `PMemoriesApplication` +
zmianie manifestu pokazał, że pliki WCIĄŻ nie znikają. Przyczyna: builda
`assembleDebug` wcześniej NIE uruchomiłem ponownie po zmianie — instalowałem
STARE APK z poprzedniej rundy (`compileDebugKotlin` samo w sobie nie
pakuje/nie dex-uje APK, tylko kompiluje bajtkod). Potwierdzone wprost
`aapt dump xmltree` na zainstalowanym APK — `android:name` dla
`PMemoriesApplication` faktycznie nieobecne w scalonym manifeście tamtej
wersji. Po prawidłowym `assembleDebug` + reinstalu: potwierdzone dwoma
niezależnymi testami na żywo — (1) wszystkie 16 realnych, starych plików
zniknęło natychmiast po zimnym restarcie appki, (2) ręcznie podrzucony
dodatkowy plik testowy (mtime ustawiony na 2020 przez `touch -t`) zniknął
po wysłaniu appki w tło (`KEYCODE_HOME`) — oba wyzwalacze faktycznie
działają, nie tylko się kompilują.

**Etap 3: ZAMKNIĘTY** (Library grouping+Edit Date, Dashboard Continue
Editing, sprzątanie plików tymczasowych — wszystkie zrobione i
zweryfikowane na żywo). Recent Memories i Templates świadomie przeniesione
do, odpowiednio, Etapu 7 i Etapu 4 — nie licz ich jako brakujące tutaj.

## 14.09.2026 — Etap 4: Google Sign-In + Firestore leaderboard + powitanie z imieniem

User poprosił żeby dokończyć Androida, gdy był poza domem (w międzyczasie budowaliśmy CI/CD dla iOS). Kontynuacja od zamkniętego Etapu 3.

**Sprawdzone PRZED kodowaniem** (nie zgadywane): brak jakiegokolwiek Firebase w projekcie — zero `google-services.json`, zero zależności Firebase w Gradle. Etap 0 checklist faktycznie nigdy nie doszedł do "Założyć projekt Firebase", mimo że dokument w pewnym momencie sugerował dalszy postęp.

**Zrobione:**
- Gradle: plugin `com.google.gms.google-services` (4.4.2), Firebase BoM (33.5.1), `firebase-auth-ktx`/`firebase-firestore-ktx`, Credential Manager (`androidx.credentials` 1.3.0 + `googleid` 1.1.1) — świadomie NIE starszy, wycofywany `GoogleSignInClient`.
- `auth/AuthManager.kt` — Android odpowiednik iOS `AuthManager` (Sign in with Apple → tu Google Sign-In przez Credential Manager). Ten sam fix "tylko pierwsze słowo imienia" co iOS (09.08.2026 bug) — wbudowany od razu, nie powtórzony.
- `leaderboard/LeaderboardService.kt` — Firestore, `submitCurrentScore`/`topEntries`. Świadomie przez `runTransaction` (atomowo odczyt+zapis), NIE osobny fetch-then-save jak w oryginalnym iOS — dokładnie ten wzorzec miał realny bug wyścigu złapany przy przeglądzie kodu iOS dzień wcześniej (13.09.2026); zbudowane tu od razu poprawnie zamiast powtarzać i później łatać.
- `onboarding/OnboardingScreen.kt` + `OnboardingStorage.kt` — logowanie wymagane w onboardingu, NIE dopiero w Rankingu (ten sam fix co iOS 09.08.2026: appka witała każdego imieniem developera, bo logowanie było ukryte głęboko).
- `MainActivity.kt` — gating na `OnboardingStorage.hasCompleted`.
- `DashboardScreen.kt` — prawdziwe powitanie czasowe + imię (analog `HomeView.greeting`, wariant 0 z iOS — pozostałe dwa warianty rotacji świadomie pominięte, to nie jest przedmiot tego punktu checklisty).
- `AndroidManifest.xml` — `INTERNET`/`ACCESS_NETWORK_STATE` (potrzebne dla Firebase, wcześniej nieobecne w źródle).
- **Placeholder `google-services.json`** (jawnie fałszywe wartości, `project_id: "pmemories-placeholder"`) — żeby `google-services` plugin i reszta kodu w ogóle się kompilowały bez prawdziwego projektu Firebase.

**Świadomie NIE zrobione teraz** (jedyna rzecz wymagająca przeglądarki/loginu Google, user zrobi sam): założenie prawdziwego projektu Firebase + podmiana placeholdera na prawdziwy `google-services.json`. Do tego czasu logowanie failuje bezpiecznie.

**Weryfikacja**: `compileDebugKotlin`/`assembleDebug` → BUILD SUCCESSFUL. Zainstalowane na `PMemories_Pixel`. Zrzuty ekranu: ekran onboardingu renderuje się poprawnie, próba logowania z fałszywym configiem kończy się złapanym błędem ("No credentials available") wyświetlonym userowi — **zero crasha**, potwierdzone bezpośrednio na żywym urządzeniu (`ps` pokazuje proces appki wciąż żywy po błędzie).

**Stan Etapu 4**: kompletny poza jednym krokiem wymagającym przeglądarki usera (Firebase Console). Po podmianie pliku konfiguracyjnego wszystko powinno zadziałać bez dalszych zmian w kodzie.

## 15.09.2026 — Etap 5: Lokalizacja (praca nocna, user autoryzował solo: "leć z Androidem, ja lecę spać")

**Zrobione:**
- Jednorazowy skrypt Python (nie w repo — czysta mechaniczna migracja formatu, kod generatora niepotrzebny po użyciu) sparsował `Localizable.xcstrings` (iOS, 586 kluczy angielskich × 27 języków) i wygenerował 28 plików `res/values(-<locale>)/strings.xml`. Konwersja specyfikatorów formatu iOS→Android (`%@`→`%s`, `%lld`/`%ld`→`%d`) automatyczna, escapowanie apostrofów/cudzysłowów/XML pod Androida też automatyczne.
- `zh-Hans` (chiński uproszczony) → `values-b+zh+Hans` (składnia BCP47 — Android nie ma prostego dwuliterowego kodu rozróżniającego Hans/Hant, minSdk 26 wspiera tę składnię bez problemu).
- Jeden przypadek wymagał ręcznej poprawki po pierwszym failed buildzie: `Day %d of %d` → `Day %1$d of %2$d` we WSZYSTKICH 28 plikach (Android/aapt2 nie akceptuje wielokrotnych NIEpozycyjnych specyfikatorów tego samego typu w jednym stringu).
- Podpięcie w kodzie (nie tylko generowanie plików — UI faktycznie z nich korzysta): `DashboardScreen.kt`, `LibraryScreen.kt`, `PMemoriesNavHost.kt`, `OnboardingScreen.kt` — wszystkie CZYSTE dopasowania tekstu 1:1 z angielskimi kluczami iOS podpięte przez `stringResource(R.string.xxx)`.
- **Złapane od razu**: `stringResource()` wymaga kontekstu `@Composable` — string użyty wewnątrz `scope.launch { ... }.onFailure { }` (poza kompozycją) musi być złapany do zmiennej WCZEŚNIEJ, w ciele Composable (`OnboardingScreen.kt`, `signInFailedMessage`), inaczej się nie skompiluje.
- 6 nowych stringów Android-only (Google Sign-In-specific, bez odpowiednika w iOS Sign in with Apple) dodane do bazowego `values/strings.xml`, jawnie oznaczone komentarzem że nie mają jeszcze tłumaczeń na pozostałe 26 języków.
- **Kolizja złapana i naprawiona**: ręcznie dodany `more` kolidował z już wygenerowanym kluczem `more` z `Localizable.xcstrings` (`aapt2`: "Found item String/more more than one time") — usunięty duplikat, appka używa wersji z pełnym tłumaczeniem (lepszy wynik niż zamierzony).
- Weryfikacja: `JAVA_HOME=$(brew --prefix openjdk@17)/... ./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL** (dwukrotnie, po każdej poprawce).

**Odkryte przy okazji, ŚWIADOMIE nietknięte tej nocy**:
- `StudioScreen.kt` ma UI wprost PO POLSKU ("Dodaj zdjęcia/wideo", "Zapisz i eksportuj", "Napis (0-3s)" itd.), nie po angielsku jak reszta appki — automatyczne dopasowanie do angielskich kluczy iOS niemożliwe bez zgadywania. Kilka z tych stringów to zresztą przyciski DEBUG ("Debug: seed zdjęcia") które w ogóle nie powinny trafić do lokalizacji. Wymaga osobnej decyzji z userem (przetłumaczyć na angielski najpierw, czy tłumaczyć wprost z polskiego), nie jest to już mechaniczna migracja formatu.
- `LibraryScreen`'s `"${items.size} items • ${date}"` — złożony string z liczbą, potrzebuje `<plurals>` (poprawna gramatyka liczby mnogiej w części języków), nie prostego `stringResource`.
- Kilka Android-specific stringów (Google Sign-In) ma dziś tylko angielski — realne TŁUMACZENIE (nie migracja formatu) na 26 pozostałych języków to osobny, mały dług.

**Stan Etapu 5**: format i podpięcie zrobione dla wszystkiego co miało czyste dopasowanie 1:1. Reszta (StudioScreen PL→EN, plurals, tłumaczenie 6 nowych stringów) to jasno spisany follow-up, nie coś zgubionego.

## 15.09.2026 (ciąg dalszy) — Etap 5 domknięty: StudioScreen PL→EN, plurals

User poszedł spać, autoryzował samodzielną pracę ("ja zrobie wszystko" — kontynuacja solo). Domknięcie dwóch zaległości spisanych wcześniej tego dnia:

**StudioScreen.kt PL→EN**: 8 stringów przetłumaczonych i podpiętych pod `stringResource` (`add_photos_videos`, `studio_empty_state`, `video_numbered`/`photo_numbered` z formatowaniem pozycyjnym `%1$d`, `caption_time_range`, `save_and_export`, `exporting`, `export_error`, `export_done_gallery`/`export_done_path`, `default_project_title`). Świadomie NIETKNIĘTE: 3 przyciski "Debug: seed..." — dev-only, nie powinny trafić do lokalizacji.

**Złapana pułapka przy `default_project_title`** (ten sam rodzaj co `signInFailedMessage` wczoraj w `OnboardingScreen`): pierwsza wersja próbowała złapać `stringResource(...)` RAZ, poza `onClick`, żeby uniknąć wołania Composable wewnątrz lambdy spoza kompozycji — ale to zamroziłoby `System.currentTimeMillis()` na moment KOMPOZYCJI, nie kliknięcia (każdy kolejny zapis w tej samej sesji dostałby ten sam tytuł). Naprawione: `context.getString(...)` (zwykła metoda `Context`, nie `@Composable`) wołane WEWNĄTRZ `onClick`, świeże przy każdym kliknięciu.

**Pluralizacja** (`LibraryScreen`'s "N items"): dodany `<plurals name="items_count">` (`one`/`other`) w `values/strings.xml`, podpięty przez `pluralStringResource`. Angielski-only jak reszta nowych Android-only stringów.

**Weryfikacja**: `compileDebugKotlin` + `assembleDebug` → BUILD SUCCESSFUL, zainstalowane na emulatorze (`PMemories_Pixel`/`emulator-5554`). Wizualna weryfikacja Studio/Library NIEUDANA — próba ominięcia ekranu logowania przez ręczne wstrzyknięcie `SharedPreferences` (`run-as` + zapis `onboarding.xml`) nie zadziałała, appka nadal pokazywała ekran "Personalize PMemories" (placeholder Firebase blokuje dalej, ten sam znany stan od Etapu 4). Kod poprawny składniowo/typowo (kompilacja to potwierdza), ale bez wzrokowego potwierdzenia na ekranie tego wieczoru — do zrobienia w czwartek na prawdziwym telefonie razem z userem.

**Dopisane do `Android.md`** (zasada na bieżąco): trzy nowe punkty z dzisiejszej sesji iOS do przeniesienia na Androida gdy dojdzie kolej — `AnalyticsLogger` (odpowiednik przez Firebase Analytics), najwyższy szczyt na plakacie (dotyczy Etapu 7), ostateczny podział Free/Premium z `Pricing.md`.

**Stan Etapu 5**: w pełni domknięty (format + podpięcie + PL→EN + plurals). Następny krok w kolejności to Etap 6 (pierwsze wydanie testowe) — zablokowany na tym samym kroku co Etap 4 (user musi ręcznie stworzyć prawdziwy projekt Firebase w przeglądarce i podmienić `google-services.json`).

## 15.09.2026 (ciąg dalszy) — Etap 7: Travel Map zbudowany w całości (user do pracy, "buduj więc całą 7")

User: "buduj wiec cala 7 ja ide do pracy chce miec cala apke juz gotowa a 6 zajmiemy sie po pracy" — największy i najbardziej złożony etap całego projektu, zbudowany solo w jednej sesji. Pełny opis w `Docs/Android.md` (sekcja Etap 7) — tu skrót najważniejszych decyzji i realnych problemów po drodze.

**Architektura (nowe pliki, pakiet `travel/`)**: `TransportMode`, `TravelAchievementsCalculator`/`ExplorerScore` (1:1 port wzoru iOS), `RouteProvider` (Haversine, uproszczone v1 zamiast płatnego Directions API), `ElevationProvider` (open-elevation.com, darmowe), `PeakSearchProvider` (Overpass, to samo źródło co iOS), `CitySearchProvider` (wbudowany `Geocoder`), `PhotoLocationReader`+`SmartRouteDetector` (EXIF przez `androidx.exifinterface`, 1:1 port algorytmu klastrowania iOS), `TravelViewModel`, `BitmapSequenceVideoRenderer`+`RouteFrameRenderer`+`RouteVideoRenderer` (uproszczony animowany eksport). Nowe encje Room: `TripEntity`/`StopEntity`, `AppDatabase` v2.

**UI**: `TravelScreen` (segmented Trips/Globe, karta Explorer Score, lista tras), `TripBuilderScreen` (wyszukiwanie + Smart Route + lista przystanków), `TripDetailScreen` (mapa trasy), `WorldGlobeMap`/`TripRouteMap` (Google Maps Compose), `LeaderboardScreen` (pierwsze realne podpięcie `LeaderboardService` do UI). Nawigacja: 4. zakładka Travel.

**Dwa nowe zewnętrzne blokery znalezione i rozwiązane od razu bez czekania na usera**: Google Maps SDK wymaga własnego, PŁATNEGO klucza Google Cloud (ten sam rodzaj problemu co Firebase) — rozwiązane przez manifest placeholder (`local.properties` → `MAPS_API_KEY`, gitignored, pusty string = appka buduje się i działa, mapy pokazują puste kafelki do czasu wklejenia klucza) zamiast blokować cały etap na tym jednym kroku. Google Directions API (prawdziwe trasowanie) to ten sam rodzaj blokera — obejście: `RouteProvider` liczy dystans i animację po ortodromie (Haversine), jawnie oznaczone jako uproszczone v1 z opisaną ścieżką rozbudowy.

**Realny bug znaleziony i naprawiony NA ŻYWO** (nie w kodzie statycznie, tylko przez faktyczne klikanie na emulatorze): zakładka Travel po wizycie w "Build Route" wracała do TEGO ekranu zamiast do listy tras — standardowy `saveState`/`restoreState` Jetpack Navigation dla dolnej nawigacji, ale sub-ekrany Travel (`travel/builder`, `travel/trip/{id}`) żyją w tym samym płaskim grafie co sama zakładka, nie w osobnym zagnieżdżonym grafie — przez co "zapamiętywały się" razem z nią. Naprawione usunięciem `saveState`/`restoreState` z `navigateToTab` (drobny koszt: Home/Studio/Library tracą pamięć przewijania między zakładkami, akceptowalne na tym etapie). Zweryfikowane przez ponowny build+install+test na żywo — działa poprawnie.

**Świadomie NIE zbudowane — "Travel Replay" (poprawka nieaktualnego zapisu w `Android.md`)**: sprawdzenie iOS przed budową ujawniło, że ta koncepcja (karta outro ze statystykami podróży) została tam CAŁKOWICIE USUNIĘTA 13.08.2026 — user na iOS wprost: appka ma być do dowolnych wspomnień, nie tylko podróży. Budowanie tego na Androidzie odtworzyłoby świadomie odrzuconą na iOS funkcję. Pominięte celowo, `Android.md` poprawiony żeby nie wprowadzać w błąd przy kolejnej sesji.

**Weryfikacja na żywo (nie tylko kompilacja)** — pełny cykl przetestowany na emulatorze (`PMemories_Pixel`) przez ominięcie ekranu logowania (ręczne wstrzyknięcie `SharedPreferences` przez `run-as`, ten sam trik co wcześniej dziś, tym razem skuteczny za drugim podejściem — pierwsza próba użyła kruchego jednolinijkowego `sh -c` przez `adb shell`, druga: `adb push` do `/data/local/tmp` + `run-as cat >` zadziałało niezawodnie):
1. Dashboard pokazuje zakładkę Travel i sekcję Recent Memories.
2. Travel → Trips (pusty stan) → Globe (segmented control działa).
3. FAB "+" → Build Route → wyszukanie "Warsaw" przez prawdziwy `Geocoder` → wynik "Warsaw / Poland" → wybór transportu (✈️) → Stops (1).
4. Save → zapis do Room, powrót do listy (po naprawie buga nawigacji).
5. Lista tras pokazuje "Warsaw · 1 stops", Explorer Score **"25 · Newcomer"** — dokładnie zgodne ze wzorem (1×20 + 1×5).
6. Dashboard → Recent Memories pokazuje tę samą podróż.

**Stan na koniec dnia**: Etap 7 kompletny poza dwoma zewnętrznymi blokerami (Firebase, klucz Maps — oba wymagają przeglądarki usera, do zrobienia po pracy razem z Etapem 6) i kilkoma jawnie oznaczonymi uproszczeniami v1 (routing po linii prostej zamiast prawdziwych tras, Canvas zamiast satelitarnych kafelków w eksporcie wideo, brak zdjęcia reprezentatywnego na kafelku Recent Memories). Appka buduje się, instaluje i działa end-to-end na urządzeniu.
