package com.example.wavesurvival.wave;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.config.ModSettings;
import com.example.wavesurvival.config.NameConfig;
import com.example.wavesurvival.entity.WaveZombieEntity;
import com.example.wavesurvival.registry.ModEntities;
import com.example.wavesurvival.registry.ModItems;
import com.example.wavesurvival.rpg.RPGManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Управляет волнами: спавн, масштабирование сложности, боссы каждые N волн, награды.
 * Запуск/остановка - команды /wavearena start | stop | wave N.
 */
@Mod.EventBusSubscriber(modid = WaveSurvivalMod.MOD_ID)
public final class WaveManager {
    /** Половина стороны арены: 25 => внутренняя площадь 51x51. */
    public static final int ARENA_HALF = 25;

    private static final int BREAK_TICKS = 100;
    private static final double PARTICIPANT_RADIUS = 45.0D;

    private static boolean active;
    private static ServerLevel activeLevel;
    private static BlockPos center = BlockPos.ZERO;

    private static ServerLevel registeredLevel;
    private static BlockPos registeredCenter;

    private static int wave;
    private static int pendingSpawns;
    private static boolean bossPending;
    private static int spawnCooldown;
    private static int breakTimer;
    private static int idleTicks;
    private static final Set<UUID> ALIVE = new HashSet<>();

    private WaveManager() {
    }

    // -------------------------------------------------------------- API

    public static boolean isActive() {
        return active;
    }

    public static int getWave() {
        return wave;
    }

    /** Вызывается командой постройки арены, чтобы волны шли именно на ней. */
    public static void registerArena(ServerLevel level, BlockPos floorCenter) {
        registeredLevel = level;
        registeredCenter = floorCenter;
    }

    public static void start(ServerPlayer player) {
        stop(true);
        activeLevel = player.serverLevel();
        if (registeredLevel == activeLevel && registeredCenter != null) {
            center = registeredCenter;
        } else {
            center = player.blockPosition().below();
        }
        wave = 0;
        pendingSpawns = 0;
        bossPending = false;
        breakTimer = BREAK_TICKS;
        idleTicks = 0;
        active = true;
        broadcast(Component.literal("Волны начнутся через 5 секунд. Приготовьтесь!").withStyle(ChatFormatting.GOLD));
    }

    public static void stop(boolean clearMobs) {
        if (clearMobs && activeLevel != null) {
            for (UUID id : new ArrayList<>(ALIVE)) {
                Entity entity = activeLevel.getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
        }
        ALIVE.clear();
        pendingSpawns = 0;
        bossPending = false;
        active = false;
        activeLevel = null;
    }

    /** Перейти сразу к волне n (для тестов боссов: /wavearena wave 10). */
    public static boolean setWave(int n) {
        if (!active || activeLevel == null) {
            return false;
        }
        for (UUID id : new ArrayList<>(ALIVE)) {
            Entity entity = activeLevel.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
        ALIVE.clear();
        pendingSpawns = 0;
        bossPending = false;
        wave = Math.max(0, n - 1);
        breakTimer = 40;
        return true;
    }

    public static boolean isBossWave(int w) {
        return w > 0 && w % ModSettings.bossEveryWaves == 0;
    }

    /** Босс призывает прислужников. */
    public static void spawnMinions(ServerLevel level, Vec3 near, int count) {
        if (!active || level != activeLevel) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            WaveZombieEntity mob = ModEntities.WAVE_ZOMBIE.get().create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(near.x + (random.nextDouble() - 0.5D) * 6.0D, near.y, near.z + (random.nextDouble() - 0.5D) * 6.0D,
                    random.nextFloat() * 360.0F, 0.0F);
            setupMob(mob, false, 1);
            level.addFreshEntity(mob);
            ALIVE.add(mob.getUUID());
        }
    }

    // ------------------------------------------------------------ события

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active || activeLevel == null) {
            return;
        }
        tick();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        stop(false);
        registeredLevel = null;
        registeredCenter = null;
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof WaveZombieEntity mob) || mob.level().isClientSide()) {
            return;
        }
        ALIVE.remove(mob.getUUID());

        if (mob.isBoss()) {
            // 100% дроп легендарного оружия
            ItemEntity drop = mob.spawnAtLocation(new ItemStack(ModItems.ABYSS_DEVOURER.get()));
            if (drop != null) {
                drop.setGlowingTag(true);
                drop.setUnlimitedLifetime();
            }
            int reward = 250 + wave * 25;
            for (ServerPlayer player : participants()) {
                RPGManager.addPoints(player, reward);
            }
            broadcast(Component.literal("Босс повержен! Из него выпало легендарное оружие. Награда: +" + reward + " очков каждому.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            RPGManager.addPoints(killer, 5 + wave);
        }
    }

    // ---------------------------------------------------------- логика волн

    private static void tick() {
        ServerLevel level = activeLevel;
        List<ServerPlayer> players = participants();
        if (players.isEmpty()) {
            idleTicks++;
            if (idleTicks > 1200) {
                stop(true);
            }
            return;
        }
        idleTicks = 0;

        if (breakTimer > 0) {
            breakTimer--;
            if (breakTimer == 0) {
                beginWave(players.size());
            }
            return;
        }

        if (pendingSpawns > 0) {
            if (--spawnCooldown <= 0) {
                spawnCooldown = 8;
                for (int i = 0; i < 2 && pendingSpawns > 0; i++) {
                    spawnWaveMob(players, bossPending && pendingSpawns == 1);
                    pendingSpawns--;
                }
            }
            return;
        }

        if (level.getGameTime() % 20 == 0) {
            ALIVE.removeIf(id -> {
                Entity entity = level.getEntity(id);
                return entity == null || !entity.isAlive();
            });
        }
        if (ALIVE.isEmpty()) {
            onWaveCleared(players);
        }
    }

    private static void beginWave(int playerCount) {
        wave++;
        boolean boss = isBossWave(wave);
        bossPending = boss;
        pendingSpawns = boss ? 1 : Math.min(60, 4 + wave * 2 + (playerCount - 1) * 4);
        spawnCooldown = 0;
        if (boss) {
            broadcast(Component.literal("=== ВОЛНА " + wave + ": БОСС! ===").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        } else {
            broadcast(Component.literal("Волна " + wave + " - врагов: " + pendingSpawns).withStyle(ChatFormatting.RED));
        }
    }

    private static void onWaveCleared(List<ServerPlayer> players) {
        broadcast(Component.literal("Волна " + wave + " отбита! Следующая через 5 секунд.").withStyle(ChatFormatting.GREEN));
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        }
        breakTimer = BREAK_TICKS;
    }

    private static void spawnWaveMob(List<ServerPlayer> players, boolean boss) {
        ServerLevel level = activeLevel;
        WaveZombieEntity mob = ModEntities.WAVE_ZOMBIE.get().create(level);
        if (mob == null) {
            return;
        }
        RandomSource random = level.getRandom();
        BlockPos pos = boss ? surface(center.getX(), center.getZ()) : randomSpawnPos(players, random);
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        setupMob(mob, boss, players.size());
        level.addFreshEntity(mob);
        ALIVE.add(mob.getUUID());
    }

    private static BlockPos surface(int x, int z) {
        int y = activeLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static BlockPos randomSpawnPos(List<ServerPlayer> players, RandomSource random) {
        int range = ARENA_HALF - 2;
        BlockPos best = surface(center.getX(), center.getZ());
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = center.getX() + random.nextInt(range * 2 + 1) - range;
            int z = center.getZ() + random.nextInt(range * 2 + 1) - range;
            BlockPos candidate = surface(x, z);
            best = candidate;
            boolean farEnough = true;
            for (ServerPlayer player : players) {
                if (player.distanceToSqr(x + 0.5D, candidate.getY(), z + 0.5D) < 64.0D) {
                    farEnough = false;
                    break;
                }
            }
            if (farEnough) {
                break;
            }
        }
        return best;
    }

    /** Имя, скин, характеристики и экипировка врага в зависимости от номера волны. */
    private static void setupMob(WaveZombieEntity mob, boolean boss, int playerCount) {
        RandomSource random = mob.getRandom();
        mob.setSkin(1 + random.nextInt(ModSettings.skinCount));
        mob.setCustomName(Component.literal(NameConfig.randomEnemyName(random)));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();

        double scale = 1.0D + wave * 0.06D;
        setBase(mob, Attributes.MAX_HEALTH, 20.0D * scale);
        setBase(mob, Attributes.ATTACK_DAMAGE, Math.min(12.0D, 2.5D + wave * 0.15D));
        setBase(mob, Attributes.MOVEMENT_SPEED, Math.min(0.30D, 0.22D + wave * 0.002D + random.nextFloat() * 0.02D));
        setBase(mob, Attributes.FOLLOW_RANGE, 64.0D);
        mob.setHealth(mob.getMaxHealth());

        if (wave >= 3) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(wave >= 6 ? Items.IRON_HELMET : Items.LEATHER_HELMET));
        }
        if (wave >= 8) {
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(wave >= 14 ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
        }
        if (wave >= 12) {
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }

        if (boss) {
            int index = wave / ModSettings.bossEveryWaves - 1;
            mob.makeBoss(wave, playerCount, NameConfig.bossName(index));
        }
    }

    private static void setBase(WaveZombieEntity mob, Attribute attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // --------------------------------------------------------------- прочее

    private static List<ServerPlayer> participants() {
        List<ServerPlayer> result = new ArrayList<>();
        if (activeLevel == null) {
            return result;
        }
        for (ServerPlayer player : activeLevel.players()) {
            double dx = player.getX() - center.getX();
            double dz = player.getZ() - center.getZ();
            if (player.isAlive() && !player.isSpectator() && dx * dx + dz * dz <= PARTICIPANT_RADIUS * PARTICIPANT_RADIUS) {
                result.add(player);
            }
        }
        return result;
    }

    private static void broadcast(Component message) {
        for (ServerPlayer player : participants()) {
            player.sendSystemMessage(message);
        }
    }
}
