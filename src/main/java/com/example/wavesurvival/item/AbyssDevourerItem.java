package com.example.wavesurvival.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Легендарный меч «Пожиратель бездны» - 100% дроп с босса.
 * Название предмета меняется в assets/wavesurvival/lang/ru_ru.json (ключ item.wavesurvival.abyss_devourer).
 *
 * Характеристики: ~11 урона (7 + 3 материал + 1 база), скорость атаки 1.6,
 * сильное отбрасывание, иссушение цели, визуальный эффект душ и небольшое восстановление здоровья.
 */
public class AbyssDevourerItem extends SwordItem {

    public AbyssDevourerItem(Properties properties) {
        super(AbyssTier.INSTANCE, 7, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        // Сильное отбрасывание: направление «от атакующего»
        target.knockback(1.1D, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        attacker.heal(1.0F);

        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY(0.5D), target.getZ(), 18, 0.3D, 0.5D, 0.3D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                    target.getX(), target.getY(0.5D), target.getZ(), 6, 0.2D, 0.4D, 0.2D, 0.03D);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wavesurvival.abyss_devourer").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
