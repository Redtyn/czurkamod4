package com.example.wavesurvival.client;

import net.minecraft.client.renderer.entity.EntityRenderers;
// Убедись, что импортируешь правильный класс эвента для Forge:
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "vlorp", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Здесь регистрируются кастомные рендереры мобов (например, директора или Адрика)
        // Пример: event.registerEntityRenderer(ModEntities.DIRECTOR, DirectorRenderer::new);
    }
}
