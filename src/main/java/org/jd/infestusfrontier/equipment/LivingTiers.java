package org.jd.infestusfrontier.equipment;

import java.util.function.Supplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.core.equipment.LivingTool;
import org.jd.infestusfrontier.processing.ProcessingModule;

/**
 * The two bone tool tiers (ITEM_CATALOG I015, I016). Both mine what a stone tool mines: a bone head is no harder than
 * stone, and a better harvest tier waits for mineral fusion. A dormant tool is repaired with Bone Plates on an anvil;
 * a living one mends itself from its reserve and takes no anvil repair.
 */
enum LivingTiers implements Tier {
    DORMANT(LivingTool.DORMANT_USES, 4.0F, 1.0F, 5, () -> Ingredient.of(ProcessingModule.BONE_PLATE.get())),
    LIVING(LivingTool.LIVING_USES, 5.0F, 1.5F, 10, Ingredient::of);

    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repair;

    LivingTiers(int uses, float speed, float damage, int enchantmentValue, Supplier<Ingredient> repair) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
        this.repair = repair;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_STONE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }
}
