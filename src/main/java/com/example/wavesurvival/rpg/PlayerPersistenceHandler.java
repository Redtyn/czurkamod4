package com.example.wavesurvival.rpg;

import com.example.wavesurvival.WaveSurvivalMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Гарантирует, что постоянные баффы и RPG-прогресс НЕ пропадают после смерти:
 *  1) при PlayerEvent.Clone копируем весь Player.PERSISTED_NBT_TAG старого игрока в нового;
 *  2) при респавне / входе / смене измерения заново накладываем модификаторы атрибутов.
 */
@Mod.EventBusSubscriber(modid = WaveSurvivalMod.MOD_ID)
public final class PlayerPersistenceHandler {

    private PlayerPersistenceHandler() {
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }
        Player original = event.getOriginal();
        CompoundTag oldPersisted = original.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!oldPersisted.isEmpty()) {
            newPlayer.getPersistentData().put(Player.PERSISTED_NBT_TAG, oldPersisted.copy());
        }
        RPGManager.applyBuffs(newPlayer);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RPGManager.applyBuffs(player);
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RPGManager.applyBuffs(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RPGManager.applyBuffs(player);
        }
    }
}
