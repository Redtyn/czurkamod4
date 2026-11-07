package com.example.wavesurvival.client;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.entity.WaveZombieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

import java.util.HashMap;
import java.util.Map;

/**
 * Рендерер врага. Текстуры берутся из:
 *   assets/wavesurvival/textures/entity/skin_1.png ... skin_N.png  (обычные враги)
 *   assets/wavesurvival/textures/entity/boss.png                   (босс)
 * Формат - обычный скин игрока 64x64 (как у зомби/Steve).
 */
public class WaveZombieRenderer extends ZombieRenderer {
    private static final ResourceLocation BOSS_TEXTURE =
            new ResourceLocation(WaveSurvivalMod.MOD_ID, "textures/entity/boss.png");
    private static final Map<Integer, ResourceLocation> SKIN_CACHE = new HashMap<>();

    public WaveZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        if (zombie instanceof WaveZombieEntity wave) {
            if (wave.isBoss()) {
                return BOSS_TEXTURE;
            }
            int index = Math.max(1, wave.getSkin());
            return SKIN_CACHE.computeIfAbsent(index, i ->
                    new ResourceLocation(WaveSurvivalMod.MOD_ID, "textures/entity/skin_" + i + ".png"));
        }
        return super.getTextureLocation(zombie);
    }
}
