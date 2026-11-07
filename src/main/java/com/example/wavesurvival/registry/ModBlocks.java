package com.example.wavesurvival.registry;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.block.TrapBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, WaveSurvivalMod.MOD_ID);

    public static final RegistryObject<Block> SPIKE_TRAP = BLOCKS.register("spike_trap",
            () -> new TrapBlock(TrapBlock.Kind.SPIKE, trapProperties()));
    public static final RegistryObject<Block> FIRE_TRAP = BLOCKS.register("fire_trap",
            () -> new TrapBlock(TrapBlock.Kind.FIRE, trapProperties()));
    public static final RegistryObject<Block> SLOW_TRAP = BLOCKS.register("slow_trap",
            () -> new TrapBlock(TrapBlock.Kind.SLOW, trapProperties()));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties trapProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }
}
