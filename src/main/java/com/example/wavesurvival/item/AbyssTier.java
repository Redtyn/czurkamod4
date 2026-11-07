package com.example.wavesurvival.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Материал легендарного меча: урон выше незерита, но без «ваншотов» (итого ~11 урона). */
public final class AbyssTier implements Tier {
    public static final AbyssTier INSTANCE = new AbyssTier();

    private AbyssTier() {
    }

    @Override
    public int getUses() {
        return 2500;
    }

    @Override
    public float getSpeed() {
        return 8.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 3.0F;
    }

    @Override
    public int getLevel() {
        return 4;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHERITE_INGOT);
    }
}
