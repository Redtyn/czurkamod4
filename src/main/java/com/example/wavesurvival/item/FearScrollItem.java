package com.example.wavesurvival.item;

import com.example.wavesurvival.config.ModSettings;
import com.example.wavesurvival.entity.WaveZombieEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;
import java.util.List;

/**
 * «Свиток страха» (аналог «Билета на депортацию» из исходного ТЗ, но без привязки к реальным группам людей).
 * Как только свиток оказывается в ГЛАВНОЙ руке игрока (и не на перезарядке) - все враги волны
 * в радиусе ModSettings.fearRadius впадают в панику и разбегаются.
 */
public class FearScrollItem extends Item {

    public FearScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        // Именно главная рука (проверка по ссылке, т.к. флаг selected бывает true и для слота оффхенда)
        if (!selected || player.getMainHandItem() != stack) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }

        ServerLevel serverLevel = player.serverLevel();
        double radius = ModSettings.fearRadius;
        List<WaveZombieEntity> mobs = serverLevel.getEntitiesOfClass(
                WaveZombieEntity.class,
                player.getBoundingBox().inflate(radius),
                mob -> mob.isAlive() && mob.distanceToSqr(player) <= radius * radius);

        if (mobs.isEmpty()) {
            return;
        }

        for (WaveZombieEntity mob : mobs) {
            mob.scare(player, ModSettings.fearDurationTicks);
        }

        serverLevel.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0D, player.getZ(),
                40, radius / 3.0D, 0.6D, radius / 3.0D, 0.02D);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 0.8F, 0.7F);

        player.getCooldowns().addCooldown(this, ModSettings.fearCooldownTicks);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wavesurvival.fear_scroll").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wavesurvival.fear_scroll.cooldown").withStyle(ChatFormatting.DARK_GRAY));
    }
}
