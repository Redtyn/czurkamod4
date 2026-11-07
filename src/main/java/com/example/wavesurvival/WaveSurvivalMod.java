package com.example.wavesurvival;

import com.example.wavesurvival.config.ModSettings;
import com.example.wavesurvival.config.NameConfig;
import com.example.wavesurvival.registry.ModBlocks;
import com.example.wavesurvival.registry.ModCreativeTabs;
import com.example.wavesurvival.registry.ModEntities;
import com.example.wavesurvival.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(WaveSurvivalMod.MOD_ID)
public class WaveSurvivalMod {
    public static final String MOD_ID = "wavesurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WaveSurvivalMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Файлы config/wavesurvival/*.txt и settings.properties создаются при первом запуске
        ModSettings.load();
        NameConfig.load();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }
}
