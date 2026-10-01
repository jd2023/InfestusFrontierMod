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
    private static final ResourceLocation BINDER = id("processing/fusion_binder");
    private static final ResourceLocation GRAFT = id("processing/skeletal_graft");
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

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void loomConsumesPlateBinderAndExactBiomassForSkeletalGraft(GameTestHelper helper) {
        var pos = place(helper, LOOM, new BlockPos(1, 1, 1));
        var player = player(helper, "SkeletalGraft");
        use(helper, pos, player, new ItemStack(item(PLATE)));
        use(helper, pos, player, new ItemStack(item(BINDER)));
        var beforeBiomass = data(helper, pos);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(beforeBiomass.equals(data(helper, pos)),
                "Missing graft biomass refuses without reserving the Plate or Binder");
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Loom returns the biomass bucket");
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(active(data(helper, pos)).getString("recipe").equals("I050")
                        && active(data(helper, pos)).getInt("required") == 400,
                "Plate and Binder select the 20-second graft route");
        tick(helper, pos, 137);
        var saved = helper.getLevel().getBlockEntity(pos).saveWithId(helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(pos);
        helper.getLevel().setBlockEntity(BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos),
                saved, helper.getLevel().registryAccess()));
        tick(helper, pos, 263);
        var complete = data(helper, pos);
        helper.assertTrue(count(complete, "skeletal_graft") == 1 && count(complete, "bone_plate") == 0
                        && count(complete, "fusion_binder") == 0 && tank(complete, "biomass") == 975
                        && complete.getCompound("preparation").getLong("completed") == 1,
                "Reloaded graft consumes one Plate, one Binder and exactly 25 BU once");
        tick(helper, pos, 400);
        helper.assertTrue(complete.equals(data(helper, pos)), "Completed graft cannot replay after reload");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.skeletal_graft.obtain");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.skeletal_graft.use");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void idleInputsCanBeRecoveredAfterOutputSpaceRefusal(GameTestHelper helper) {
        var pos = place(helper, LOOM, new BlockPos(1, 1, 1));
        var player = player(helper, "LoomRecovery");
        for (int i = 0; i < 193; i++) use(helper, pos, player, new ItemStack(Items.BONE));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        var full = data(helper, pos);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(full.equals(data(helper, pos)), "Full output admission preserves all 193 bones and biomass");
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.BONE) && player.getMainHandItem().getCount() == 64
                        && count(data(helper, pos), "bone") == 129,
                "Idle collection recovers an input stack when output is absent");
        player.setShiftKeyDown(false);
        use(helper, pos, player, ItemStack.EMPTY);
        var active = data(helper, pos);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().isEmpty() && active.equals(data(helper, pos)),
                "Collection cannot extract reserved inputs while work is active");
        tick(helper, pos, 400);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(item(PLATE)) && count(data(helper, pos), "bone") == 128,
                "Finished output takes collection priority over remaining inputs");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "preparation_transfers")
    public static void loomSharesPortableAdmissionAndAwardsOnlyCommittedTransfers(GameTestHelper helper) {
        var loom = place(helper, LOOM, new BlockPos(1, 1, 1));
        var bladder = place(helper, id("storage/biomass_bladder"), new BlockPos(2, 1, 1));
        var sac = place(helper, id("processing/digestive_sac"), new BlockPos(3, 1, 1));
        var player = player(helper, "SharedTransfers");
        for (int i = 0; i < 3; i++) use(helper, bladder, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, sac, player, new ItemStack(item(BIOMASS_BUCKET)));
        var before = data(helper, loom);
        use(helper, loom, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(before.equals(data(helper, loom)) && player.getMainHandItem().is(item(BIOMASS_BUCKET)),
                "The fifth portable transfer refuses at the Loom after Bladder/Sac contention");
        var successful = player(helper, "LoomBucketCredit");
        use(helper, loom, successful, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(successful.getMainHandItem().is(Items.BUCKET)
                        && successful.wasAwarded(id("discovery/biomass_bucket")) && tank(data(helper, loom), "biomass") == 1000,
                "Committed Loom bucket transfer consumes exactly one bucket and awards discovery");
        var refused = player(helper, "FullLoomCredit");
        var full = data(helper, loom);
        use(helper, loom, refused, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(full.equals(data(helper, loom)) && refused.getMainHandItem().is(item(BIOMASS_BUCKET))
                        && !refused.wasAwarded(id("discovery/biomass_bucket")),
                "Full Loom refusal preserves resources and grants no discovery");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "preparation_server_quota")
    public static void loomSharesServerPortableBudget(GameTestHelper helper) {
        var loom = place(helper, LOOM, new BlockPos(1, 1, 1));
        var bladder = place(helper, id("storage/biomass_bladder"), new BlockPos(2, 1, 1));
        for (int i = 0; i < 16; i++) {
            var player = player(helper, "ServerBudget" + i);
            for (int transfer = 0; transfer < 4; transfer++) {
                use(helper, bladder, player, transfer % 2 == 0
                        ? new ItemStack(item(BIOMASS_BUCKET)) : new ItemStack(Items.BUCKET));
                helper.assertTrue(player.getMainHandItem().is(transfer % 2 == 0 ? Items.BUCKET : item(BIOMASS_BUCKET)),
                        "The first 64 server transfers must commit");
            }
        }
        var player = player(helper, "ServerLimitedLoom");
        var before = data(helper, loom);
        use(helper, loom, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(before.equals(data(helper, loom)) && player.getMainHandItem().is(item(BIOMASS_BUCKET))
                        && !player.wasAwarded(id("discovery/biomass_bucket")),
                "A fresh player's Loom transfer shares the 64-operation server bound");
        helper.runAfterDelay(2, () -> {
            use(helper, loom, player, new ItemStack(item(BIOMASS_BUCKET)));
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && tank(data(helper, loom), "biomass") == 1000,
                    "Server quota refusal queues nothing and permits a later explicit transfer");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "preparation_persistence")
    public static void everyRouteRetainsActiveWorkThroughReloadAndRecoveredPlacement(GameTestHelper helper) {
        for (int route = 0; route < 3; route++) {
            var organ = route == 2 ? LOOM : RACK;
            var pos = place(helper, organ, new BlockPos(route, 1, 1));
            var player = player(helper, "PreparationRestore" + route);
            var input = route == 0 ? Items.ROTTEN_FLESH : route == 1 ? Items.LEATHER : Items.BONE;
            int required = route == 1 ? 800 : 400;
            String output = route == 2 ? "bone_plate" : "membrane_sheet";
            use(helper, pos, player, new ItemStack(input));
            if (route != 2) use(helper, pos, player, new ItemStack(Items.STRING));
            use(helper, pos, player, new ItemStack(route == 2 ? item(BIOMASS_BUCKET) : Items.WATER_BUCKET));
            use(helper, pos, player, ItemStack.EMPTY);
            tick(helper, pos, 137);
            var before = data(helper, pos);
            var saved = helper.getLevel().getBlockEntity(pos).saveWithId(helper.getLevel().registryAccess());
            helper.getLevel().removeBlockEntity(pos);
            helper.getLevel().setBlockEntity(BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos),
                    saved, helper.getLevel().registryAccess()));
            helper.assertTrue(before.equals(data(helper, pos)) && active(data(helper, pos)).getInt("work") == 137,
                    "Codec reload retains the exact active recipe, reservation, progress, quantities and history");
            recover(helper, pos, player);
            helper.assertTrue(before.equals(data(helper, pos)), "Recovered placement restores the identical active state");
            tick(helper, pos, required - 138);
            helper.assertTrue(active(data(helper, pos)).getInt("work") == required - 1 && count(data(helper, pos), output) == 0,
                    "Reload and replacement perform no offline work or early output");
            tick(helper, pos, 1);
            var completed = data(helper, pos);
            helper.assertTrue(!completed.getCompound("preparation").contains("active")
                            && completed.getCompound("preparation").getLong("completed") == 1
                            && completed.getCompound("preparation").getLong("lastCompleted") == 1
                            && count(completed, output) == 1
                            && count(completed, route == 0 ? "rotten_flesh" : route == 1 ? "leather" : "bone") == 0
                            && count(completed, "string") == 0
                            && tank(completed, route == 2 ? "biomass" : "water") == (route == 2 ? 950 : 900),
                    "Completion after restoration consumes exact inputs and fluid, creates one output and one history event");
            recover(helper, pos, player);
            tick(helper, pos, required);
            helper.assertTrue(completed.equals(data(helper, pos)), "Recovered completed work never replays its output or history");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void malformedPreparationSavesRemainFrozenThroughRecovery(GameTestHelper helper) {
        for (var organ : List.of(RACK, LOOM)) {
            for (int fault = 0; fault < 4; fault++) {
                var pos = place(helper, organ, new BlockPos(1, 1, 1));
                var player = player(helper, "RejectedPreparation");
                var invalid = data(helper, pos);
                var state = invalid.getCompound("preparation");
                switch (fault) {
                    case 0 -> state.putInt("schema", 99);
                    case 1 -> state.getList("items", 10).getCompound(0).putString("count", "malformed");
                    case 2 -> state.getList("tanks", 10).getCompound(0).putInt("capacity", 1001);
                    default -> state.getList("items", 10).add(state.getList("items", 10).getCompound(0).copy());
                }
                helper.getLevel().getBlockEntity(pos).loadWithComponents(invalid, helper.getLevel().registryAccess());
                var input = organ.equals(RACK) ? Items.ROTTEN_FLESH : Items.BONE;
                use(helper, pos, player, new ItemStack(input));
                helper.assertTrue(player.getMainHandItem().is(input), "Rejected save refuses input payment");
                tick(helper, pos, 800);
                helper.assertTrue(invalid.equals(data(helper, pos)), "Malformed save retains exact original data without ticking");
                recover(helper, pos, player);
                use(helper, pos, player, new ItemStack(input));
                helper.assertTrue(player.getMainHandItem().is(input)
                                && invalid.getCompound("preparation").equals(data(helper, pos).getCompound("preparation")),
                        "Recovered malformed save stays locked and preserves its original payload");
                helper.getLevel().removeBlock(pos, false);
            }
        }
        helper.succeed();
    }

    private static void recover(GameTestHelper helper, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                helper.getLevel().getBlockEntity(pos));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1,
                "Breaking yields exactly one stateful core and no separate contents");
        helper.getLevel().removeBlock(pos, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst());
        player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
        drops.getFirst().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(player.getMainHandItem().isEmpty() && helper.getLevel().getBlockEntity(pos) != null,
                "Survival replacement consumes the single recovered core");
    }

    private static org.jd.infestusfrontier.testmod.integration.AdvancementRecordingPlayer player(
            GameTestHelper helper, String name) {
        var player = new org.jd.infestusfrontier.testmod.integration.AdvancementRecordingPlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL);
        return player;
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
