package com.example.wavesurvival.rpg;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * RPG-система. Все данные игрока лежат в Player.PERSISTED_NBT_TAG -> "wavesurvival"
 * и копируются при смерти в PlayerPersistenceHandler, поэтому НЕ пропадают после смерти.
 *
 * Валюта - очки за убийства. Улучшения (покупаются командой /rpg &lt;ключ&gt;):
 *   weapon     - уровень оружия, до 10:  +5% к урону за уровень
 *   armor      - уровень брони, до 10:   -3% входящего урона за уровень
 *   sharpness  - зачарование, до 5:      +0.6 урона за уровень
 *   knockback  - зачарование, до 5:      +0.25 отбрасывания за уровень
 *   protection - зачарование, до 5:      -2% входящего урона за уровень
 *   resistance - постоянный бафф, до 3:  -6% входящего урона за уровень
 *   speed      - постоянный бафф, до 3:  +5% скорости за уровень
 *   health     - постоянный бафф, до 5:  +2 макс. здоровья (1 сердце) за уровень
 *
 * Баланс: суммарное снижение урона ограничено 60%, усиленный удар не может превысить 2x от исходного.
 */
@Mod.EventBusSubscriber(modid = WaveSurvivalMod.MOD_ID)
public final class RPGManager {
    private static final String ROOT_KEY = "wavesurvival";
    private static final String POINTS_KEY = "points";
    public static final int SCROLL_COST = 40;

    private static final UUID HEALTH_MODIFIER = UUID.fromString("5f1d7c0e-2a4b-4c8e-9a31-7b6e0d2c4a11");
    private static final UUID SPEED_MODIFIER = UUID.fromString("a3c9e5b2-61d4-4f7a-8e02-1c5d9b7f3a22");

    public enum Upgrade {
        WEAPON("weapon", "Уровень оружия", 10, 25, true),
        ARMOR("armor", "Уровень брони", 10, 30, true),
        SHARPNESS("sharpness", "Острота (RPG)", 5, 60, false),
        KNOCKBACK("knockback", "Отбрасывание (RPG)", 5, 50, false),
        PROTECTION("protection", "Защита (RPG)", 5, 60, false),
        RESISTANCE("resistance", "Постоянное сопротивление урону", 3, 300, false),
        SPEED("speed", "Постоянная скорость", 3, 250, false),
        HEALTH("health", "Постоянное макс. здоровье", 5, 350, false);

        private final String key;
        private final String displayName;
        private final int maxLevel;
        private final int baseCost;
        private final boolean exponential;

        Upgrade(String key, String displayName, int maxLevel, int baseCost, boolean exponential) {
            this.key = key;
            this.displayName = displayName;
            this.maxLevel = maxLevel;
            this.baseCost = baseCost;
            this.exponential = exponential;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        /** Цена перехода на уровень nextLevel (динамически растёт). */
        public int costFor(int nextLevel) {
            if (exponential) {
                return (int) Math.round(baseCost * Math.pow(1.35D, nextLevel - 1) * nextLevel);
            }
            return baseCost * nextLevel * nextLevel;
        }
    }

    private RPGManager() {
    }

    // ------------------------------------------------------------- данные

    /** Возвращает (создавая при необходимости) наш подтег в PERSISTED_NBT_TAG игрока. */
    public static CompoundTag getData(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persisted.put(ROOT_KEY, new CompoundTag());
        }
        return persisted.getCompound(ROOT_KEY);
    }

    public static int getPoints(Player player) {
        return getData(player).getInt(POINTS_KEY);
    }

    public static void addPoints(ServerPlayer player, int amount) {
        getData(player).putInt(POINTS_KEY, getPoints(player) + amount);
        player.displayClientMessage(Component.literal("+" + amount + " очков (всего " + getPoints(player) + ")")
                .withStyle(ChatFormatting.YELLOW), true);
    }

    public static int getLevel(Player player, Upgrade upgrade) {
        return getData(player).getInt("lvl_" + upgrade.getKey());
    }

    // ---------------------------------------------------------- покупки

    public static Component tryUpgrade(ServerPlayer player, Upgrade upgrade) {
        int current = getLevel(player, upgrade);
        if (current >= upgrade.getMaxLevel()) {
            return Component.literal(upgrade.getDisplayName() + ": уже максимальный уровень.").withStyle(ChatFormatting.RED);
        }
        int cost = upgrade.costFor(current + 1);
        int points = getPoints(player);
        if (points < cost) {
            return Component.literal("Не хватает очков: нужно " + cost + ", у вас " + points + ".").withStyle(ChatFormatting.RED);
        }
        getData(player).putInt(POINTS_KEY, points - cost);
        getData(player).putInt("lvl_" + upgrade.getKey(), current + 1);
        applyBuffs(player);
        return Component.literal(upgrade.getDisplayName() + " -> уровень " + (current + 1) + "/" + upgrade.getMaxLevel()
                + " (потрачено " + cost + ")").withStyle(ChatFormatting.GREEN);
    }

    public static Component tryBuyScroll(ServerPlayer player) {
        int points = getPoints(player);
        if (points < SCROLL_COST) {
            return Component.literal("Не хватает очков: нужно " + SCROLL_COST + ", у вас " + points + ".").withStyle(ChatFormatting.RED);
        }
        getData(player).putInt(POINTS_KEY, points - SCROLL_COST);
        ItemStack stack = new ItemStack(ModItems.FEAR_SCROLL.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return Component.literal("Куплен Свиток страха (-" + SCROLL_COST + " очков).").withStyle(ChatFormatting.GREEN);
    }

    public static Component describeStats(Player player) {
        net.minecraft.network.chat.MutableComponent text = Component.literal("=== RPG: очки " + getPoints(player) + " ===")
                .withStyle(ChatFormatting.GOLD);
        for (Upgrade upgrade : Upgrade.values()) {
            int level = getLevel(player, upgrade);
            String next = level >= upgrade.getMaxLevel() ? "MAX" : "след. " + upgrade.costFor(level + 1);
            text.append(Component.literal("\n/rpg " + upgrade.getKey() + " - " + upgrade.getDisplayName()
                    + ": " + level + "/" + upgrade.getMaxLevel() + " (" + next + ")").withStyle(ChatFormatting.GRAY));
        }
        text.append(Component.literal("\n/rpg scroll - купить Свиток страха (" + SCROLL_COST + ")").withStyle(ChatFormatting.GRAY));
        return text;
    }

    // ------------------------------------------------- применение эффектов

    /** Накладывает постоянные модификаторы атрибутов (здоровье, скорость). Вызывать после входа/респавна/покупки. */
    public static void applyBuffs(ServerPlayer player) {
        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER, "wavesurvival_health",
                2.0D * getLevel(player, Upgrade.HEALTH), AttributeModifier.Operation.ADDITION);
        setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER, "wavesurvival_speed",
                0.05D * getLevel(player, Upgrade.SPEED), AttributeModifier.Operation.MULTIPLY_BASE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void setModifier(AttributeInstance instance, UUID id, String name, double amount,
                                    AttributeModifier.Operation operation) {
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount > 0.0D) {
            instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        float amount = event.getAmount();

        // Исходящий урон игрока
        if (source.getEntity() instanceof ServerPlayer attacker) {
            int weapon = getLevel(attacker, Upgrade.WEAPON);
            int sharpness = getLevel(attacker, Upgrade.SHARPNESS);
            float boosted = amount * (1.0F + 0.05F * weapon) + 0.6F * sharpness;
            amount = Math.min(boosted, amount * 2.0F); // защита от «ваншотов»

            int knockback = getLevel(attacker, Upgrade.KNOCKBACK);
            if (knockback > 0 && source.getDirectEntity() == attacker) {
                victim.knockback(0.25D * knockback, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
            }
        }

        // Входящий урон игрока
        if (victim instanceof ServerPlayer player && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            double reduction = 0.03D * getLevel(player, Upgrade.ARMOR)
                    + 0.02D * getLevel(player, Upgrade.PROTECTION)
                    + 0.06D * getLevel(player, Upgrade.RESISTANCE);
            reduction = Math.min(0.60D, reduction);
            amount *= (float) (1.0D - reduction);
        }

        event.setAmount(amount);
    }
}
