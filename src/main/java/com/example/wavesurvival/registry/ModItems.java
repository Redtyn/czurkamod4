package com.example.wavesurvival.registry;

import com.example.wavesurvival.WaveSurvivalMod;
import com.example.wavesurvival.item.AbyssDevourerItem;
import com.example.wavesurvival.item.FearScrollItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WaveSurvivalMod.MOD_ID);

    /** Свиток страха - заменяет «Билет на депортацию» из исходного ТЗ. */
    public static final RegistryObject<Item> FEAR_SCROLL = ITEMS.register("fear_scroll",
            () -> new FearScrollItem(new Item.Properties().durability(24).rarity(Rarity.UNCOMMON)));

    /** Легендарное оружие босса. Название меняется в lang-файле (см. README). */
    public static final RegistryObject<Item> ABYSS_DEVOURER = ITEMS.register("abyss_devourer",
            () -> new AbyssDevourerItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> SPIKE_TRAP = ITEMS.register("spike_trap",
            () -> new BlockItem(ModBlocks.SPIKE_TRAP.get(), new Item.Properties()));
    public static final RegistryObject<Item> FIRE_TRAP = ITEMS.register("fire_trap",
            () -> new BlockItem(ModBlocks.FIRE_TRAP.get(), new Item.Properties()));
    public static final RegistryObject<Item> SLOW_TRAP = ITEMS.register("slow_trap",
            () -> new BlockItem(ModBlocks.SLOW_TRAP.get(), new Item.Properties()));

    private ModItems() {
    }
}
