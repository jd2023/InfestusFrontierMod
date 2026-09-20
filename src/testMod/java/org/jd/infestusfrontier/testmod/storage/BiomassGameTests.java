package org.jd.infestusfrontier.testmod.storage;

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
import net.minecraft.world.item.context.UseOnContext;
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
public final class BiomassGameTests {
    private static final ResourceLocation BUD = id("construction/organ_bud");
    private static final ResourceLocation SUBSTRATE = id("ecology/living_substrate");
    private static final ResourceLocation SAC = id("processing/digestive_sac");
    private static final ResourceLocation BLADDER = id("storage/biomass_bladder");
    private static final ResourceLocation BUCKET = id("storage/biomass_bucket");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void constructsDigestsAndTransfersExactBuckets(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var sacPos = placeBud(helper, new BlockPos(0, 1, 0));
        player.getInventory().setItem(1, new ItemStack(Items.ROTTEN_FLESH, 2));
        use(helper, sacPos, player, new ItemStack(Items.BOWL));
        helper.assertTrue(helper.getLevel().getBlockState(sacPos).is(block(SAC)),
                "Bud plus bowl and two real rotten flesh constructs a Digestive Sac");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:storage.digestive_sac.obtain");

        for (int batch = 0; batch < 10; batch++) {
            use(helper, sacPos, player, new ItemStack(Items.WHEAT));
            helper.assertTrue(player.getMainHandItem().isEmpty(), "Accepted wheat is held by one active Sac batch");
            for (int tick = 0; tick < 40; tick++) tick(helper.getLevel().getBlockEntity(sacPos));
        }
        helper.assertTrue(biomass(helper, sacPos, "sac") == 1_000,
                "Ten real wheat batches produce exactly 1000 BU / mB");
        var full = data(helper, sacPos);
        use(helper, sacPos, player, new ItemStack(Items.ROTTEN_FLESH));
        helper.assertTrue(player.getMainHandItem().is(Items.ROTTEN_FLESH)
                        && full.equals(data(helper, sacPos)),
                "Full Sac refuses rotten flesh without consuming it or changing retained output");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:storage.digestive_sac.use");

        var bladderPos = placeBud(helper, new BlockPos(2, 1, 0));
        player.getInventory().clearContent();
        player.getInventory().setItem(1, new ItemStack(Items.GLASS, 2));
        use(helper, bladderPos, player, new ItemStack(Items.SLIME_BALL));
        helper.assertTrue(helper.getLevel().getBlockState(bladderPos).is(block(BLADDER)),
                "Bud plus slime and two glass constructs the 4000 mB Bladder");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:storage.biomass_bladder.obtain");

        use(helper, sacPos, player, new ItemStack(Items.BUCKET));
        helper.assertTrue(player.getMainHandItem().is(item(BUCKET)) && biomass(helper, sacPos, "sac") == 0,
                "Empty bucket withdraws exactly 1000 mB and becomes I019");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:storage.biomass_bucket.obtain");

        var placement = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(placement, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(placement), Direction.UP, placement, false)));
        helper.assertTrue(helper.getLevel().getBlockState(placement.above()).isAir()
                        && player.getMainHandItem().is(item(BUCKET)),
                "Biomass Bucket has no world placement before finite containment");

        use(helper, bladderPos, player, player.getMainHandItem());
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET)
                        && biomass(helper, bladderPos, "bladder") == 1_000
                        && helper.getLevel().getBlockState(bladderPos).toString().contains("fill=1"),
                "I019 empties exactly into the Bladder");
        var beforeEquipment = data(helper, bladderPos);
        use(helper, bladderPos, player, new ItemStack(Items.STICK));
        helper.assertTrue(beforeEquipment.equals(data(helper, bladderPos)) && player.getMainHandItem().is(Items.STICK),
                "The fueling port refuses non-equipment without inventing or consuming armor");

        use(helper, bladderPos, player, new ItemStack(Items.BUCKET, 2));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET)
                        && player.getMainHandItem().getCount() == 2
                        && biomass(helper, bladderPos, "bladder") == 1_000,
                "A stacked empty bucket refuses instead of losing the unfilled remainder");

        use(helper, bladderPos, player, new ItemStack(Items.BUCKET));
        helper.assertTrue(player.getMainHandItem().is(item(BUCKET)) && biomass(helper, bladderPos, "bladder") == 0,
                "Bladder fills one exact I019");
        use(helper, sacPos, player, player.getMainHandItem());
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && biomass(helper, sacPos, "sac") == 1_000,
                "The same bucket empties into a second compatible tank");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:storage.biomass_bladder.use");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:storage.biomass_bucket.use");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fullReceiverRetainsPortableOutput(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(pos.below(), block(SUBSTRATE).defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, block(BLADDER).defaultBlockState(), 2);
        var saved = data(helper, pos);
        var tank = saved.getCompound("bladder").getList("tanks", 10).getCompound(0);
        tank.putString("resource", "biomass");
        tank.putInt("count", 4_000);
        helper.getLevel().getBlockEntity(pos).loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(biomass(helper, pos, "bladder") == 4_000, "Starter Bladder has a hard 4000 mB capacity");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var held = new ItemStack(item(BUCKET));
        use(helper, pos, player, held);
        helper.assertTrue(player.getMainHandItem().is(item(BUCKET)) && biomass(helper, pos, "bladder") == 4_000,
                "Full receiving store leaves rejected bucket output held");
        helper.succeed();
    }

    private static BlockPos placeBud(GameTestHelper helper, BlockPos relative) {
        var pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos.below(), block(SUBSTRATE).defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, block(BUD).defaultBlockState(), 2);
        return pos;
    }
    private static void use(GameTestHelper helper, BlockPos pos,
            net.minecraft.world.entity.player.Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(BlockEntity entity) {
        var ticker = entity.getBlockState().getTicker(entity.getLevel(), entity.getType());
        ((net.minecraft.world.level.block.entity.BlockEntityTicker) ticker)
                .tick(entity.getLevel(), entity.getBlockPos(), entity.getBlockState(), entity);
    }
    private static int biomass(GameTestHelper helper, BlockPos pos, String owner) {
        var saved = data(helper, pos).getCompound(owner);
        var store = owner.equals("sac") ? saved.getCompound("store") : saved;
        return store.getList("tanks", 10).getCompound(0).getInt("count");
    }
    private static CompoundTag data(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(pos).saveCustomOnly(helper.getLevel().registryAccess());
    }
    private static Block block(ResourceLocation id) { return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(); }
    private static Item item(ResourceLocation id) { return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path); }
}
