package com.example.wavesurvival.registry;

import com.example.wavesurvival.WaveSurvivalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WaveSurvivalMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wavesurvival"))
                    .icon(() -> new ItemStack(ModItems.ABYSS_DEVOURER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FEAR_SCROLL.get());
                        output.accept(ModItems.ABYSS_DEVOURER.get());
                        output.accept(ModItems.SPIKE_TRAP.get());
                        output.accept(ModItems.FIRE_TRAP.get());
                        output.accept(ModItems.SLOW_TRAP.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
