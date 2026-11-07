package com.example.wavesurvival.client;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.registry.ModEntities; // твой путь к мобам
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "wavesurvival", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Привязываем стандартный рендерер зомби к твоему мобу, чтобы игра знала, как его отрисовать
        event.registerEntityRenderer(ModEntities.DIRECTOR.get(), ZombieRenderer::new);
    }
}
