package org.jd.infestusfrontier.testmod;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.equipment.EquipmentModule;

/**
 * The living armor is vanilla armor: each piece is in its slot's armor tag, so armor enchantments and trims take it;
 * it takes dye as leather does and a water cauldron washes the dye off; a smithing table trims it.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ArmorCompatTests {
    private ArmorCompatTests() {}

    private static Map<Item, TagKey<Item>> pieces() {
        return Map.of(
                EquipmentModule.DORMANT_HOOD.get(), ItemTags.HEAD_ARMOR,
                EquipmentModule.DORMANT_THORAX.get(), ItemTags.CHEST_ARMOR,
                EquipmentModule.DORMANT_LEGWRAPS.get(), ItemTags.LEG_ARMOR,
                EquipmentModule.DORMANT_TREADS.get(), ItemTags.FOOT_ARMOR,
                EquipmentModule.AWAKENED_HOOD.get(), ItemTags.HEAD_ARMOR,
                EquipmentModule.AWAKENED_THORAX.get(), ItemTags.CHEST_ARMOR,
                EquipmentModule.AWAKENED_LEGWRAPS.get(), ItemTags.LEG_ARMOR,
                EquipmentModule.AWAKENED_TREADS.get(), ItemTags.FOOT_ARMOR);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyPieceTakesArmorEnchantmentsTrimsAndDye(GameTestHelper h) {
        var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Enchantment protection =
                enchantments.getOrThrow(Enchantments.PROTECTION).value();
        Enchantment unbreaking =
                enchantments.getOrThrow(Enchantments.UNBREAKING).value();
        Enchantment mending = enchantments.getOrThrow(Enchantments.MENDING).value();
        pieces().forEach((item, slot) -> {
            ItemStack stack = new ItemStack(item);
            h.assertTrue(stack.is(slot), item + " is in " + slot.location());
            h.assertTrue(stack.is(ItemTags.TRIMMABLE_ARMOR), item + " takes trims");
            h.assertTrue(stack.is(ItemTags.DYEABLE), item + " takes dye");
            h.assertTrue(protection.canEnchant(stack), "Protection fits " + item);
            h.assertTrue(unbreaking.canEnchant(stack), "Unbreaking fits " + item);
            h.assertTrue(mending.canEnchant(stack), "Mending fits " + item);
            h.assertValueEqual(stack.getEnchantmentValue(), 5, item + " enchants at the table as the design says");
            h.assertTrue(
                    ((ArmorItem) item).getMaterial().value().layers().getFirst().dyeable(),
                    item + "'s worn layer takes the dye");
        });
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void dyeColoursAPieceAndAWaterCauldronWashesItOff(GameTestHelper h) {
        ItemStack dyed = DyedItemColor.applyDyes(
                new ItemStack(EquipmentModule.AWAKENED_THORAX.get()), List.of((DyeItem) Items.BLUE_DYE));
        h.assertTrue(dyed.has(DataComponents.DYED_COLOR), "blue dye colours the thorax");
        BlockPos cauldron = new BlockPos(2, 1, 2);
        h.setBlock(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, dyed);
        EcologyTests.use(h, player, cauldron, Direction.UP);
        h.assertFalse(player.getMainHandItem().has(DataComponents.DYED_COLOR), "the cauldron washes the dye off");
        h.assertValueEqual(
                h.getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL), 2, "and spends a level of water");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSmithingTableTrimsAPiece(GameTestHelper h) {
        var input = new SmithingRecipeInput(
                new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE),
                new ItemStack(EquipmentModule.DORMANT_HOOD.get()),
                new ItemStack(Items.GOLD_INGOT));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "vanilla's trim recipe takes the hood");
        ItemStack trimmed = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(trimmed.has(DataComponents.TRIM), "the hood comes out trimmed");
        h.succeed();
    }
}
