package org.jd.infestusfrontier.testmod.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.testmod.kit.OffhandUse;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class BioFurnaceGameTests {
    private static final ResourceLocation FURNACE = id("processing/bio_furnace");
    private static final ResourceLocation BIOMASS_BUCKET = id("storage/biomass_bucket");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void smeltsRawIronInSixteenSecondsForFortyBiomass(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "A complete biomass bucket is returned");
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        helper.assertTrue(work(helper, pos).state().quantities().itemCount("minecraft:iron_ingot") == 1
                        && work(helper, pos).state().quantities().fluidAmount("biomass") == 960,
                "One iron ingot completes after 320 work units and consumes exactly 40 BU");
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.IRON_INGOT), "One raw iron smelts to one iron ingot");
        player.setShiftKeyDown(false);
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "960 BU leaves room for one measured bucket");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void refusesNonSmeltableAndPassesItThrough(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        var diamond = new ItemStack(Items.DIAMOND);
        var result = useResult(helper, pos, player, diamond);
        helper.assertTrue(result == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                        && player.getMainHandItem().is(Items.DIAMOND),
                "A non-smeltable item passes through unchanged");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void refusalLeavesInputAndBiomassUnchanged(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        var work = fillOutputSlot(helper, pos);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        var beforeStart = work.state();
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(beforeStart.equals(work.state()), "An output-full start retains its loaded input and biomass");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bucketReturnsOnlyWhenAllThousandFits(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "First full bucket is accepted");
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Second full bucket exactly fills the tank");
        var full = work(helper, pos).state();
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(item(BIOMASS_BUCKET)) && full.equals(work(helper, pos).state()),
                "A bucket is retained when all 1000 BU cannot fit");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void offhandPlacementPassesThrough(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        place(helper, relative);
        InteractionResult result = OffhandUse.useWithOffhand(helper, relative, new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(result.consumesAction(), "Bio-Furnace leaves empty-main-hand placement to the offhand");
        OffhandUse.assertPlacedBeside(helper, relative, Blocks.COBBLESTONE);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void idleFurnaceDoesNoWork(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        var beforeTicks = work(helper, pos).state();
        tick(helper, pos, 100);
        helper.assertTrue(beforeTicks.equals(work(helper, pos).state()), "An idle furnace state does not change over 100 ticks");
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.RAW_IRON), "An idle furnace never advances an unstarted batch");
        helper.succeed();
    }

    private static BatchWork fillOutputSlot(GameTestHelper helper, BlockPos pos) {
        var work = work(helper, pos);
        work.insertItem("minecraft:raw_iron", 1, 64, work.revision());
        work.insertItem("minecraft:iron_ingot", 64, 64, work.revision());
        work.extractItemSlot(0, work.revision());
        return work;
    }

    private static BatchWork work(GameTestHelper helper, BlockPos pos) {
        try {
            var field = helper.getLevel().getBlockEntity(pos).getClass().getDeclaredField("work");
            field.setAccessible(true);
            return (BatchWork) field.get(helper.getLevel().getBlockEntity(pos));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Bio-Furnace work store is unavailable", exception);
        }
    }

    private static BlockPos place(GameTestHelper helper, BlockPos relative) {
        var pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos.below(), BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(FURNACE).defaultBlockState(), 2);
        return pos;
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BioFurnaceTest"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void use(GameTestHelper helper, BlockPos pos, net.minecraft.server.level.ServerPlayer player, ItemStack stack) {
        useResult(helper, pos, player, stack);
    }

    private static net.minecraft.world.ItemInteractionResult useResult(GameTestHelper helper, BlockPos pos,
            net.minecraft.server.level.ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (stack.isEmpty()) {
            player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        return helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(GameTestHelper helper, BlockPos pos, int ticks) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        var ticker = entity.getBlockState().getTicker(entity.getLevel(), entity.getType());
        for (int tick = 0; tick < ticks; tick++) ((net.minecraft.world.level.block.entity.BlockEntityTicker) ticker)
                .tick(entity.getLevel(), entity.getBlockPos(), entity.getBlockState(), entity);
    }

    private static net.minecraft.world.item.Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }
}
