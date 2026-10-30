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
 * The two bone tool tiers (ITEM_CATALOG I015, I016). A dormant tool has fewer uses and slower mining than wood,
 * with the same harvest eligibility. A living one mines what a stone tool mines, and a better harvest
 * tier waits for mineral fusion. A dormant tool is repaired with Bone Plates on an anvil; a living one mends itself
 * from its reserve and takes no anvil repair.
 */
enum LivingTiers implements Tier {
    DORMANT(
            LivingTool.DORMANT_USES,
            1.0F,
            0.0F,
            BlockTags.INCORRECT_FOR_WOODEN_TOOL,
            5,
            () -> Ingredient.of(ProcessingModule.BONE_PLATE.get())),
    LIVING(LivingTool.LIVING_USES, 5.0F, 1.5F, BlockTags.INCORRECT_FOR_STONE_TOOL, 10, Ingredient::of);

    private final int uses;
    private final float speed;
    private final float damage;
    private final TagKey<Block> incorrectForDrops;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repair;

    LivingTiers(
            int uses,
            float speed,
            float damage,
            TagKey<Block> incorrectForDrops,
            int enchantmentValue,
            Supplier<Ingredient> repair) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.incorrectForDrops = incorrectForDrops;
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
        return incorrectForDrops;
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
