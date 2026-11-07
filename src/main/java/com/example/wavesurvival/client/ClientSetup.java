package com.example.wavesurvival.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
// Импортируй свой файл со списком сущностей (проверь название пакета):
// import com.example.wavesurvival.registry.ModEntities; 

@Mod.EventBusSubscriber(modid = "vlorp", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Регистрируем заглушки рендереров для мобов, чтобы игра не падала с NullPointerException:
        // event.registerEntityRenderer(ModEntities.DIRECTOR.get(), NoopRenderer::new);
        // event.registerEntityRenderer(ModEntities.ADRIK.get(), NoopRenderer::new);
    }
}
