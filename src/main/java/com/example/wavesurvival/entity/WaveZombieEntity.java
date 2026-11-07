package com.example.wavesurvival.entity;

import com.example.wavesurvival.wave.WaveManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Враг волны: зомби-гуманоид с именем над головой, случайным скином из пула
 * и поддержкой «паники» (Свиток страха). Босс - тот же класс с флагом BOSS.
 */
public class WaveZombieEntity extends Zombie {
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(WaveZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOSS =
            SynchedEntityData.defineId(WaveZombieEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("Boss"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    private int fearTicks;
    @Nullable
    private LivingEntity fearSource;
    private int bossTicks;

    public WaveZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setPersistenceRequired();
        AttributeInstance reinforcements = this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.setBaseValue(0.0D);
        }
    }

    // ---------------------------------------------------------------- данные

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN, 1);
        this.entityData.define(DATA_BOSS, false);
    }

    public int getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkin(int skin) {
        this.entityData.set(DATA_SKIN, skin);
    }

    public boolean isBoss() {
        return this.entityData.get(DATA_BOSS);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("WsSkin", getSkin());
        tag.putBoolean("WsBoss", isBoss());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSkin(Math.max(1, tag.getInt("WsSkin")));
        this.entityData.set(DATA_BOSS, tag.getBoolean("WsBoss"));
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    // ------------------------------------------------------------- поведение

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FearFleeGoal(this));
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Пока враг напуган, он игнорирует любые попытки выбрать цель. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.fearTicks > 0 && target != null) {
            return;
        }
        super.setTarget(target);
    }

    /** Паника: враг бросает цель и убегает от source на ticks тиков. Боссы к страху иммунны. */
    public void scare(LivingEntity source, int ticks) {
        if (isBoss()) {
            return;
        }
        this.fearTicks = ticks;
        this.fearSource = source;
        this.setTarget(null);
        this.getNavigation().stop();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.fearTicks > 0) {
            this.fearTicks--;
            if (this.fearTicks == 0) {
                this.fearSource = null;
            }
        }

        // Если волна уже не идёт (например, перезапуск сервера) - убираем «осиротевших» врагов
        if (this.tickCount % 100 == 0 && !WaveManager.isActive()) {
            this.discard();
            return;
        }

        if (isBoss()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            bossAbilities();
        }
    }

    // ------------------------------------------------------------------ босс

    public void makeBoss(int wave, int playerCount, String name) {
        this.entityData.set(DATA_BOSS, true);

        double health = (400.0D + 150.0D * (wave / 10.0D)) * (1.0D + 0.6D * (playerCount - 1));
        setBase(Attributes.MAX_HEALTH, health);
        setBase(Attributes.ATTACK_DAMAGE, 5.0D + wave * 0.2D);
        setBase(Attributes.MOVEMENT_SPEED, 0.27D);
        setBase(Attributes.ARMOR, 6.0D);
        setBase(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
        setBase(Attributes.FOLLOW_RANGE, 64.0D);
        this.setHealth(this.getMaxHealth());

        this.setCustomName(Component.literal(name).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        this.setCustomNameVisible(true);

        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    private void bossAbilities() {
        this.bossTicks++;
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean enraged = this.getHealth() < this.getMaxHealth() * 0.5F;
        int slamInterval = enraged ? 90 : 140;

        LivingEntity target = this.getTarget();
        if (target != null && this.bossTicks % slamInterval == 0 && this.distanceToSqr(target) < 100.0D) {
            groundSlam(serverLevel);
        }
        if (this.bossTicks % 500 == 0) {
            WaveManager.spawnMinions(serverLevel, this.position(), enraged ? 5 : 3);
        }
    }

    /** Удар по земле: урон + отбрасывание + замедление всем игрокам рядом. */
    private void groundSlam(ServerLevel serverLevel) {
        for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0D, 2.0D, 8.0D))) {
            player.hurt(this.damageSources().mobAttack(this), 8.0F);
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            double len = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
            player.push(dx / len * 1.2D, 0.6D, dz / len * 1.2D);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3D, this.getZ(),
                8, 3.0D, 0.2D, 3.0D, 0.0D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (isBoss()) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    private void setBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // ------------------------------------------------------------ AI испуга

    /** Цель «убегать от источника страха» пока fearTicks > 0. */
    private static final class FearFleeGoal extends Goal {
        private final WaveZombieEntity mob;

        FearFleeGoal(WaveZombieEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mob.fearTicks > 0 && mob.fearSource != null && mob.fearSource.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            flee();
        }

        @Override
        public void tick() {
            if (mob.getNavigation().isDone()) {
                flee();
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        private void flee() {
            if (mob.fearSource == null) {
                return;
            }
            Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 7, mob.fearSource.position());
            if (away != null) {
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.4D);
            }
        }
    }
}
