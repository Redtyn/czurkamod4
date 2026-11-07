package com.example.wavesurvival.config;

import com.example.wavesurvival.WaveSurvivalMod;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ИМЕНА ВРАГОВ И БОССОВ.
 *
 * При первом запуске создаются два файла (по одному имени в строке, строки с # игнорируются):
 *   config/wavesurvival/enemy_names.txt  - имена обычных врагов
 *   config/wavesurvival/boss_names.txt   - имена боссов (босс №1, №2, ... по кругу)
 *
 * Меняйте имена прямо в этих файлах - пересборка мода не нужна.
 * Массивы ниже - это только значения по умолчанию для создания файлов.
 */
public final class NameConfig {
    private static final String[] DEFAULT_ENEMY_NAMES = {
            "Грок", "Мордрек", "Скарн", "Зарнак", "Бракка", "Вольдор", "Нокс", "Гримш",
            "Урзул", "Кракс", "Шнырь", "Гнилозуб", "Хрыч", "Мракус", "Дрогар", "Злобень",
            "Ржавый Бен", "Череп", "Пепельный", "Костогрыз", "Вурдак", "Могильщик", "Тлен",
            "Гарн Одноглазый", "Скрип", "Жуткий Фред", "Пугач", "Лязг", "Гнус", "Мертвяк Олаф",
            "Ворчун", "Холод", "Бродяга Хэл", "Сумрак", "Оскал", "Клык", "Нетопырь", "Гробовщик",
            "Тень Раф", "Черныш", "Костяной Ян", "Ржавчина", "Упырь Дарк", "Мрачный Том"
    };

    private static final String[] DEFAULT_BOSS_NAMES = {
            "Малгар, Король Костей",
            "Нхарот, Владыка Бездны",
            "Зул'Горак Пожиратель",
            "Морвейн Чумной",
            "Ксарон, Кошмар Подземелий",
            "Вергаст Тёмный",
            "Азраил Пепельный",
            "Ульдран, Король-Лич",
            "Изгард, Тёмный Архонт",
            "Кхтул, Древний Ужас"
    };

    private static List<String> enemyNames = new ArrayList<>(Arrays.asList(DEFAULT_ENEMY_NAMES));
    private static List<String> bossNames = new ArrayList<>(Arrays.asList(DEFAULT_BOSS_NAMES));

    private NameConfig() {
    }

    public static void load() {
        enemyNames = loadFile("enemy_names.txt", DEFAULT_ENEMY_NAMES,
                "# Имена обычных врагов. Одно имя в строке. Строки с # игнорируются. Кодировка UTF-8.");
        bossNames = loadFile("boss_names.txt", DEFAULT_BOSS_NAMES,
                "# Имена боссов. Босс волны 10 берёт 1-ю строку, волны 20 - 2-ю и т.д. (по кругу). Кодировка UTF-8.");
    }

    public static String randomEnemyName(RandomSource random) {
        return enemyNames.get(random.nextInt(enemyNames.size()));
    }

    public static String bossName(int index) {
        return bossNames.get(Math.floorMod(index, bossNames.size()));
    }

    private static List<String> loadFile(String fileName, String[] defaults, String header) {
        Path file = ModSettings.configDir().resolve(fileName);
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                List<String> lines = new ArrayList<>();
                lines.add(header);
                lines.addAll(Arrays.asList(defaults));
                Files.write(file, lines, StandardCharsets.UTF_8);
            }
            List<String> result = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    result.add(trimmed);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        } catch (IOException e) {
            WaveSurvivalMod.LOGGER.error("Не удалось прочитать {}, используются имена по умолчанию", fileName, e);
        }
        return new ArrayList<>(Arrays.asList(defaults));
    }
}
