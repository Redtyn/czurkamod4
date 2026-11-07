package com.example.wavesurvival.registry;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.entity.WaveZombieEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = WaveSurvivalMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WaveSurvivalMod.MOD_ID);

  public static final RegistryObject<EntityType<DirectorEntity>> DIRECTOR = ENTITIES.register("director",
    () -> EntityType.Builder.of(DirectorEntity::new, MobCategory.MONSTER)
        .sized(0.6F, 1.95F)
        .build("director"));

    private ModEntities() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WAVE_ZOMBIE.get(), Zombie.createAttributes().build());
    }
}
