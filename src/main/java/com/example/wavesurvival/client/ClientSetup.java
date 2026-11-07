package com.example.wavesurvival.client;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.registry.ModEntities;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WaveSurvivalMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DIRECTOR.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.WAVE_ZOMBIE.get(), ZombieRenderer::new);
    }
}
