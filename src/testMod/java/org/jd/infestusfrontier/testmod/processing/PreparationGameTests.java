package org.jd.infestusfrontier.testmod.processing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class PreparationGameTests {
    private static final ResourceLocation RACK = id("processing/membrane_rack");
    private static final ResourceLocation LOOM = id("processing/bone_loom");
    private static final ResourceLocation SHEET = id("processing/membrane_sheet");
    private static final ResourceLocation PLATE = id("processing/bone_plate");
    private static final ResourceLocation BIOMASS_BUCKET = id("storage/biomass_bucket");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void exactCraftingRecipesProduceOnePreparationOrgan(GameTestHelper helper) {
        var rack = craft(helper, RACK, List.of(new ItemStack(Items.STICK, 2), new ItemStack(Items.BONE, 2),
                new ItemStack(item(id("construction/spore_culture")))));
        helper.assertTrue(rack.is(item(RACK)) && rack.getCount() == 1, "Rack recipe produces one real block item");
        var loom = craft(helper, LOOM, List.of(new ItemStack(Items.STICK, 2), new ItemStack(Items.BONE, 2),
                new ItemStack(item(id("construction/organ_bud")))));
        helper.assertTrue(loom.is(item(LOOM)) && loom.getCount() == 1, "Loom recipe produces one real block item");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.membrane_rack.obtain");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.bone_loom.obtain");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void rackRoutesPreserveRefusalsAndProduceOneSheet(GameTestHelper helper) {
        var pos = place(helper, RACK, new BlockPos(1, 1, 1));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        use(helper, pos, player, new ItemStack(Items.ROTTEN_FLESH));
        use(helper, pos, player, new ItemStack(Items.STRING));
        var dry = data(helper, pos);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(dry.equals(data(helper, pos)), "Missing water preserves both Rack inputs");
        use(helper, pos, player, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Rack returns the water bucket");
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(active(data(helper, pos)).getInt("required") == 400, "Flesh route takes 20 loaded seconds");
        tick(helper, pos, 400);
        helper.assertTrue(count(data(helper, pos), "membrane_sheet") == 1
                        && count(data(helper, pos), "rotten_flesh") == 0 && count(data(helper, pos), "string") == 0,
                "Flesh route commits exactly one Sheet");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.membrane_sheet.obtain");

        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        player.setShiftKeyDown(false);
        helper.assertTrue(player.getMainHandItem().is(item(SHEET)) && player.getMainHandItem().getCount() == 1,
                "Collected Rack output is one readable I002 item");
        use(helper, pos, player, new ItemStack(Items.LEATHER));
        use(helper, pos, player, new ItemStack(Items.STRING));
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(active(data(helper, pos)).getInt("required") == 800, "Leather route doubles duration");
        tick(helper, pos, 400);
        helper.assertTrue(data(helper, pos).getCompound("preparation").contains("active"), "Leather is not complete at flesh time");
        tick(helper, pos, 400);
        helper.assertTrue(count(data(helper, pos), "membrane_sheet") == 1,
                "Leather route still produces only one Sheet");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.membrane_rack.use");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.membrane_sheet.use");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void loomConsumesAccountedBoneAndNeverAcceptsBoneBlocks(GameTestHelper helper) {
        var pos = place(helper, LOOM, new BlockPos(1, 1, 1));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        use(helper, pos, player, new ItemStack(Items.BONE_BLOCK));
        helper.assertTrue(player.getMainHandItem().is(Items.BONE_BLOCK), "Loom must not unpack a bone block");
        var before = data(helper, pos);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(before.equals(data(helper, pos)), "Bone-block-only start refuses unchanged");
        use(helper, pos, player, new ItemStack(Items.BONE));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Loom accepts one measured biomass bucket");
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(active(data(helper, pos)).getInt("required") == 400, "Plate route takes 20 loaded seconds");
        tick(helper, pos, 400);
        var complete = data(helper, pos);
        helper.assertTrue(count(complete, "bone_plate") == 1 && count(complete, "bone") == 0
                        && tank(complete, "biomass") == 950,
                "Loom consumes one bone and exactly 50 BU for one Plate");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.bone_plate.obtain");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.bone_loom.use");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.bone_plate.use");
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, ResourceLocation id, List<ItemStack> ingredients) {
        var slots = new ArrayList<ItemStack>();
        ingredients.forEach(stack -> { for (int i = 0; i < stack.getCount(); i++) slots.add(stack.copyWithCount(1)); });
        while (slots.size() < 9) slots.add(ItemStack.EMPTY);
        var input = CraftingInput.of(3, 3, slots);
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        helper.assertTrue(holder.value() instanceof CraftingRecipe, "Expected crafting recipe " + id);
        var recipe = (CraftingRecipe) holder.value();
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Exact ingredients must match " + id);
        return recipe.assemble(input, helper.getLevel().registryAccess());
    }

    private static BlockPos place(GameTestHelper helper, ResourceLocation block, BlockPos relative) {
        var pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos.below(), BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(block).defaultBlockState(), 2);
        return pos;
    }
    private static void use(GameTestHelper helper, BlockPos pos, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (stack.isEmpty()) {
            player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
            ((net.minecraft.server.level.ServerPlayer) player).gameMode.useItemOn(
                    (net.minecraft.server.level.ServerPlayer) player, helper.getLevel(), stack,
                    InteractionHand.MAIN_HAND, hit);
        } else {
            helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player,
                    InteractionHand.MAIN_HAND, hit);
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(GameTestHelper helper, BlockPos pos, int ticks) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        var ticker = entity.getBlockState().getTicker(entity.getLevel(), entity.getType());
        for (int tick = 0; tick < ticks; tick++) ((net.minecraft.world.level.block.entity.BlockEntityTicker) ticker)
                .tick(entity.getLevel(), entity.getBlockPos(), entity.getBlockState(), entity);
    }
    private static CompoundTag data(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(pos).saveCustomOnly(helper.getLevel().registryAccess());
    }
    private static CompoundTag active(CompoundTag data) { return data.getCompound("preparation").getCompound("active"); }
    private static int count(CompoundTag data, String resource) {
        var items = data.getCompound("preparation").getList("items", 10);
        int count = 0;
        for (int index = 0; index < items.size(); index++) if (items.getCompound(index).getString("resource").equals(resource))
            count += items.getCompound(index).getInt("count");
        return count;
    }
    private static int tank(CompoundTag data, String resource) {
        var tank = data.getCompound("preparation").getList("tanks", 10).getCompound(0);
        return tank.getString("resource").equals(resource) ? tank.getInt("count") : 0;
    }
    private static Item item(ResourceLocation id) { return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path); }
}
