package com.example.wavesurvival.config;

import com.example.wavesurvival.WaveSurvivalMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Простые настройки мода. Файл создаётся автоматически:
 * &lt;папка игры&gt;/config/wavesurvival/settings.properties
 */
public final class ModSettings {
    /** Сколько скинов в пуле (skin_1.png ... skin_N.png). */
    public static int skinCount = 8;
    /** Радиус действия Свитка страха (блоки). */
    public static int fearRadius = 12;
    /** Сколько тиков враги убегают после срабатывания свитка (20 тиков = 1 секунда). */
    public static int fearDurationTicks = 100;
    /** Перезарядка свитка (тики). */
    public static int fearCooldownTicks = 400;
    /** Босс появляется каждые N волн. */
    public static int bossEveryWaves = 10;

    private static final String DEFAULT_FILE = """
            # Настройки Wave Survival (сохраняйте файл в UTF-8)
            # Сколько скинов в пуле. Файлы: assets/wavesurvival/textures/entity/skin_1.png ... skin_N.png
            skinCount=8
            # Радиус Свитка страха в блоках
            fearRadius=12
            # Длительность паники врагов в тиках (20 тиков = 1 сек)
            fearDurationTicks=100
            # Перезарядка Свитка страха в тиках
            fearCooldownTicks=400
            # Босс каждые N волн
            bossEveryWaves=10
            """;

    private ModSettings() {
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get().resolve("wavesurvival");
    }

    public static void load() {
        Path file = configDir().resolve("settings.properties");
        Properties props = new Properties();
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, DEFAULT_FILE, StandardCharsets.UTF_8);
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException e) {
            WaveSurvivalMod.LOGGER.error("Не удалось прочитать settings.properties, используются значения по умолчанию", e);
        }
        skinCount = readInt(props, "skinCount", 8, 1, 64);
        fearRadius = readInt(props, "fearRadius", 12, 4, 48);
        fearDurationTicks = readInt(props, "fearDurationTicks", 100, 20, 1200);
        fearCooldownTicks = readInt(props, "fearCooldownTicks", 400, 20, 12000);
        bossEveryWaves = readInt(props, "bossEveryWaves", 10, 2, 100);
    }

    private static int readInt(Properties props, String key, int def, int min, int max) {
        try {
            int value = Integer.parseInt(props.getProperty(key, String.valueOf(def)).trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
