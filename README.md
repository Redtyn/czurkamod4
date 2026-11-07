# Wave Survival (Forge 1.20.1)

Выживание против волн: враги с именами и скинами, боссы каждые 10 волн, легендарный меч,
ловушки, RPG-прокачка, постоянные баффы (не пропадают после смерти) и арена 51x51.

## Как собрать
1. Скачайте Forge MDK 1.20.1 (47.x) с files.minecraftforge.net.
2. Скопируйте из этого архива в папку MDK: `src/`, `build.gradle`, `settings.gradle`, `gradle.properties`
   (или откройте этот проект в IntelliJ IDEA и добавьте Gradle wrapper командой `gradle wrapper --gradle-version 8.1.1`).
3. `./gradlew build` -> готовый мод: `build/libs/wavesurvival-1.0.0.jar` (кладите в `mods/`).
   Для теста: `./gradlew runClient`.

## ГДЕ МЕНЯТЬ НАЗВАНИЯ
| Что | Где | Нужна пересборка? |
|---|---|---|
| Имена обычных врагов | `<папка игры>/config/wavesurvival/enemy_names.txt` (создаётся при первом запуске, по имени в строке) | нет |
| Имена боссов (по порядку: волна 10, 20, 30...) | `<папка игры>/config/wavesurvival/boss_names.txt` | нет |
| Название оружия «Пожиратель бездны», Свитка страха, ловушек, моба | `src/main/resources/assets/wavesurvival/lang/ru_ru.json` (и `en_us.json`) | да (или через ресурспак) |
| Значения имён по умолчанию (для первого создания файлов) | `config/NameConfig.java` | да |

Пример: чтобы переименовать меч, в `ru_ru.json` поменяйте строку
`"item.wavesurvival.abyss_devourer": "Пожиратель бездны"`.

## ГДЕ ЛЕЖАТ СКИНЫ МОБОВ
* В исходниках: `src/main/resources/assets/wavesurvival/textures/entity/`
  `skin_1.png ... skin_8.png` (обычные враги, выбираются случайно) и `boss.png` (босс).
  Формат: скин игрока 64x64 PNG. Сейчас там простые цветные заглушки - замените своими файлами с теми же именами.
* Без пересборки: папка `resourcepack_example/` - готовый ресурспак. Положите свои PNG в
  `resourcepack_example/assets/wavesurvival/textures/entity/`, скопируйте папку целиком в `resourcepacks/`
  и включите пак в настройках игры. Через этот же пак можно переопределить названия (`assets/wavesurvival/lang/ru_ru.json`).
* Больше 8 скинов: добавьте `skin_9.png`, `skin_10.png`... и поставьте `skinCount=N` в `config/wavesurvival/settings.properties`.

## Игра
* `/wavearena` (оператор) - построить арену 51x51 вокруг вас (стены 10 блоков, 4 угловые башни, свет, ровный пол).
* `/wavearena start` / `stop` / `wave N` - запуск, остановка, переход к волне N (например `wave 10` - сразу босс).
* Враги: имя над головой + случайный скин. Каждые 10 волн - босс (полоса здоровья, удар по земле, призыв прислужников),
  100% дроп меча «Пожиратель бездны» (урон ~11, мощное отбрасывание, иссушение, частицы душ).
* Свиток страха: возьмите в главную руку - враги в радиусе 12 блоков (боссы - нет) бросают цель и разбегаются на 5 секунд.
  Перезарядка 20 с, 24 использования. Купить: `/rpg scroll`.
* Ловушки (креатив-вкладка Wave Survival или крафт): шипы, огонь, замедление. Срабатывают, когда на них подан
  редстоун-сигнал (рычаг рядом, кнопка, плита, провод). Игроков не трогают.
* RPG: очки за убийства. `/rpg` - список и цены; `/rpg weapon|armor|sharpness|knockback|protection|resistance|speed|health`.
  Все улучшения хранятся в `Player.PERSISTED_NBT_TAG` и копируются в `PlayerEvent.Clone`, поэтому после смерти остаются.
* Настройки: `config/wavesurvival/settings.properties` (радиус свитка, перезарядка, частота боссов, число скинов).

## Структура кода
`wave/WaveManager` - волны и боссы · `rpg/RPGManager` - прокачка и баффы · `rpg/PlayerPersistenceHandler` - сохранение при смерти ·
`item/FearScrollItem` - свиток страха · `command/ArenaCommand` - арена · `command/RPGCommand` - покупки ·
`entity/WaveZombieEntity` - враг/босс · `block/TrapBlock` - ловушки.
