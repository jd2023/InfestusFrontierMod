package org.jd.infestusfrontier.testmod.processing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;
import org.jd.infestusfrontier.testmod.integration.AdvancementRecordingPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class BowlGameTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bowlAndItsProducedMaterialsAreRegistered(GameTestHelper helper) {
        for (String name : new String[] {"culture_bowl", "fusion_binder", "elastic_gel", "lumen_secretion",
                "nutrient_mash", "char_gland_feed", "honey_culture", "rooting_gel", "skeletal_graft"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("infestusfrontier:processing/" + name)),
                    "Missing playable Bowl content: " + name);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bowlCraftsAndPlacesOnlyOnSubstrate(GameTestHelper helper) {
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(id("processing/culture_bowl")).orElseThrow().value();
        var ingredients = new ArrayList<>(List.of(new ItemStack(Items.FLOWER_POT), stack("spore_culture", 1), new ItemStack(Items.WHEAT_SEEDS)));
        while (ingredients.size() < 9) ingredients.add(ItemStack.EMPTY);
        var input = CraftingInput.of(3, 3, ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Exact Bowl crafting ingredients");
        var crafted = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(crafted.is(BuiltInRegistries.ITEM.get(id("processing/culture_bowl"))) && crafted.getCount() == 1, "Craft one Bowl");
        var player = new AdvancementRecordingPlayer(helper.getLevel(), new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("70bd1925-1784-48bf-9706-14128474beef"), "BowlOperator"));
        player.setGameMode(GameType.SURVIVAL);
        var support = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlock(support, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, crafted);
        var hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
        crafted.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(crafted.getCount() == 1 && helper.getLevel().getBlockState(support.above()).isAir(), "Refuse non-substrate without payment");
        helper.getLevel().setBlock(support, BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        crafted.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(player.getMainHandItem().isEmpty() && helper.getLevel().getBlockEntity(support.above()) != null, "Paid Bowl placement creates server state");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.culture_bowl.obtain");
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1350, batch = "bowl_recipes")
    public static void allNineRecipesRunOnLoadedServerTicks(GameTestHelper helper) {
        String[][] inputs = {{"red_mushroom", "wheat_seeds"}, {"spore_culture", "rotten_flesh", "rotten_flesh", "bone_meal"},
                {"membrane_sheet", "spore_culture"}, {"slime_ball", "spore_culture"},
                {"glow_ink_sac", "fusion_binder"}, {"wheat", "carrot"},
                {"charcoal", "fusion_binder"}, {"honey_bottle", "spore_culture"},
                {"wheat_seeds", "spore_culture"}};
        String[] outputs = {"spore_culture", "organ_bud", "fusion_binder", "elastic_gel", "lumen_secretion",
                "nutrient_mash", "char_gland_feed", "honey_culture", "rooting_gel"};
        int[] outputCounts = {1, 1, 4, 2, 2, 2, 1, 2, 2};
        int[] water = {100, 0, 0, 50, 0, 100, 0, 0, 50};
        int[] biomass = {0, 0, 100, 0, 25, 0, 25, 0, 0};
        var positions = new ArrayList<BlockPos>();
        var player = new AdvancementRecordingPlayer(helper.getLevel(), new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("68fa29bc-e0a5-4a66-98af-adfb3ad57b77"), "BatchOperator"));
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i < outputs.length; i++) {
            var pos = place(helper, new BlockPos(i, 1, 0)); positions.add(pos);
            ((org.jd.infestusfrontier.interaction.api.ProbeTarget) helper.getLevel().getBlockEntity(pos))
                    .claimProbeOwner(player.getUUID());
            for (int cycle = 0; cycle < i; cycle++) use(helper, pos, player, new ItemStack(Items.STICK), false);
            for (String ingredient : inputs[i]) use(helper, pos, player, stack(ingredient, 1), false);
            if (water[i] > 0) {
                var before = data(helper, pos);
                use(helper, pos, player, ItemStack.EMPTY, false);
                helper.assertTrue(before.equals(data(helper, pos)), "Insufficient water must preserve every balance/count");
                use(helper, pos, player, new ItemStack(Items.WATER_BUCKET), false);
                helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Return water bucket");
            }
            if (biomass[i] > 0) {
                var before = data(helper, pos);
                use(helper, pos, player, ItemStack.EMPTY, false);
                helper.assertTrue(before.equals(data(helper, pos)), "Insufficient BU must preserve every balance/count");
                use(helper, pos, player, stack("biomass_bucket", 1), false);
                helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Return biomass bucket");
            }
            use(helper, pos, player, ItemStack.EMPTY, false);
            helper.assertTrue(data(helper, pos).getCompound("bowl").contains("active"), "Batch started: " + outputs[i]);
            var before = data(helper, pos);
            use(helper, pos, player, ItemStack.EMPTY, false);
            helper.assertTrue(before.equals(data(helper, pos)), "Duplicate start leaves all state unchanged");
        }
        helper.runAfterDelay(600, () -> {
            var pos = positions.get(2);
            var before = data(helper, pos);
            helper.getLevel().removeBlockEntity(pos);
            var restored = BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos), before, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(restored);
            helper.assertTrue(before.equals(data(helper, pos)), "Midway serialized round trip preserves progress, reservation and counts");
        });
        helper.runAfterDelay(1220, () -> {
            helper.assertTrue(!player.wasAwarded(id("discovery/culture_bowl_batch")),
                    "Offline initiator must not receive awards through a stale player reference");
            var reconnected = new AdvancementRecordingPlayer(helper.getLevel(), player.getGameProfile());
            reconnected.setGameMode(GameType.SURVIVAL);
            for (int i = 0; i < outputs.length; i++) {
                var pos = positions.get(i);
                var receipt = data(helper, pos);
                if (i == 1) {
                    var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                            helper.getLevel().getBlockEntity(pos));
                    helper.assertTrue(drops.size() == 1, "Pending Bud credit has one recovery item");
                    helper.getLevel().removeBlock(pos, false);
                    reconnected.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst());
                    drops.getFirst().getItem().useOn(new UseOnContext(reconnected, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
                } else {
                    helper.getLevel().removeBlockEntity(pos);
                    helper.getLevel().setBlockEntity(BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos),
                            receipt, helper.getLevel().registryAccess()));
                }
                use(helper, pos, reconnected, new ItemStack(Items.STICK), false);
            }
            helper.assertTrue(reconnected.wasAwarded(id("discovery/culture_bowl_batch")),
                    "Reloaded completion evidence credits the current player after reconnect");
            helper.assertTrue(reconnected.wasAwarded(id("discovery/organ_bud")),
                    "Growing a Bud completes T0-16");
            for (int i = 0; i < outputs.length; i++) {
                var pos = positions.get(i); var saved = data(helper, pos).getCompound("bowl");
                helper.assertTrue(!saved.contains("active") && saved.getLong("completed") == 1, "One completion, one count: " + outputs[i]);
                helper.assertTrue(count(saved, outputs[i]) == outputCounts[i], "Exact recipe output: " + outputs[i]);
                for (String ingredient : inputs[i]) helper.assertTrue(count(saved, ingredient) == 0, "Consumed recipe input: " + ingredient);
                helper.assertTrue(saved.getList("tanks", 10).getCompound(0).getInt("count") == (water[i] > 0 ? 1000 - water[i] : 0), "Exact water debit");
                helper.assertTrue(saved.getList("tanks", 10).getCompound(1).getInt("count") == (biomass[i] > 0 ? 1000 - biomass[i] : 0), "Exact biomass debit");
                if (i == 7) helper.assertTrue(count(saved, "glass_bottle") == 1, "Honey returns exactly one bottle after reload");
                for (int slot = 0; slot < 9; slot++) use(helper, pos, player, ItemStack.EMPTY, true);
                helper.assertTrue(count(data(helper, pos).getCompound("bowl"), outputs[i]) == 0, "Manual collection removes products");
                if (i >= 2) {
                    // Each produced material is a non-food preparation, and can be retained/retrieved intact.
                    var product = stack(outputs[i], outputCounts[i]);
                    var productItem = product.getItem();
                    helper.assertTrue(!product.has(DataComponents.FOOD), "Prepared material is not player food");
                    use(helper, pos, player, product, false);
                    helper.assertTrue(player.getMainHandItem().getCount() == outputCounts[i] - 1,
                            "Deposit one produced dose");
                    use(helper, pos, player, ItemStack.EMPTY, true);
                    helper.assertTrue(player.getMainHandItem().is(productItem) && player.getMainHandItem().getCount() == 1,
                            "Retrieve same dose: " + outputs[i]);

                }
            }
            for (int i = 2; i < outputs.length; i++) {
                ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing." + outputs[i] + ".obtain");
                ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing." + outputs[i] + ".use");
            }
            ContentAssertion.passGameTest(helper, "infestusfrontier_tests:processing.culture_bowl.use");
            var milestone = helper.getLevel().getServer().getAdvancements().get(
                    id("discovery/culture_bowl_batch"));
            helper.assertTrue(reconnected.wasAwarded(milestone.id()),
                    "A completed rooted Bowl batch earns server-owned T0-02 progress");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void recoveryHasOneHistoryAndMalformedSavesStayFrozen(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(0, 1, 0));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        var saved = data(helper, pos);
        saved.getCompound("bowl").putLong("completed", 512);
        saved.getCompound("bowl").putLong("lastCompleted", 512);
        saved.getCompound("bowl").putLong("nextBatch", 513);
        helper.getLevel().getBlockEntity(pos).loadWithComponents(saved, helper.getLevel().registryAccess());
        use(helper, pos, player, new ItemStack(Items.CLOCK), true);
        use(helper, pos, player, new ItemStack(Items.CLOCK), true);
        use(helper, pos, player, new ItemStack(Items.CLOCK), true);
        var capped = data(helper, pos);
        use(helper, pos, player, new ItemStack(Items.GLASS_BOTTLE), true);
        helper.assertTrue(capped.equals(data(helper, pos)), "L3 caps choices at three");
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1, "One recovery core, no duplicate contents/history drop");
        helper.getLevel().removeBlock(pos, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        drops.getFirst().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Recovery placement consumes the one core");
        helper.assertTrue(capped.getCompound("bowl").equals(data(helper, pos).getCompound("bowl"))
                        && data(helper, pos).hasUUID("owner"),
                "Dismantling retains exactly one history and claims an unowned legacy core");
        var invalid = data(helper, pos); invalid.getCompound("bowl").putInt("schema", 99);
        helper.getLevel().getBlockEntity(pos).loadWithComponents(invalid, helper.getLevel().registryAccess());
        use(helper, pos, player, stack("wheat_seeds", 1), false);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && invalid.equals(data(helper, pos)), "Unsupported save refuses payment and preserves data");
        helper.succeed();
    }


    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bowl_quota", timeoutTicks = 80)
    public static void completionAdmissionIsSharedAcrossDimensions(GameTestHelper helper) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        var bowls = new ArrayList<BlockEntity>();
        var nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var remotePos = new BlockPos(0, 100, 0);
        // Explicit disposable fixture loading; production ticking performs no chunk request.
        boolean wasForced = nether.getForcedChunks().contains(new net.minecraft.world.level.ChunkPos(remotePos).toLong());
        nether.setChunkForced(0, 0, true);
        nether.getChunkAt(remotePos);
        var oldRemote = nether.getBlockState(remotePos);
        var oldSupport = nether.getBlockState(remotePos.below());
        nether.setBlock(remotePos.below(), BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        nether.setBlock(remotePos, BuiltInRegistries.BLOCK.get(id("processing/culture_bowl")).defaultBlockState(), 2);
        for (int i = 0; i < 17; i++) {
            var pos = place(helper, new BlockPos(i % 5, 1, i / 5));
            for (int cycle = 0; cycle < 7; cycle++) use(helper, pos, player, new ItemStack(Items.STICK), false);
            use(helper, pos, player, stack("honey_bottle", 1), false);
            use(helper, pos, player, stack("spore_culture", 1), false);
            use(helper, pos, player, ItemStack.EMPTY, false);
            var saved = data(helper, pos);
            saved.getCompound("bowl").getCompound("active").putInt("work", 1199);
            BlockEntity bowl = helper.getLevel().getBlockEntity(pos);
            if (i == 16) {
                bowl = nether.getBlockEntity(remotePos);
                helper.getLevel().removeBlock(pos, false);
            }
            bowl.loadWithComponents(saved, helper.getLevel().registryAccess());
            bowls.add(bowl);
        }
        for (var bowl : bowls) tick(bowl);
        helper.assertTrue(completed(bowls) == 16, "Only sixteen completions admitted across both dimensions in one tick");
        var remote = bowls.getLast();
        helper.assertTrue(remote.saveCustomOnly(nether.registryAccess()).getCompound("bowl").contains("active"), "Deferred Bowl retains reservation");
        helper.runAfterDelay(2, () -> {
            tick(remote);
            helper.assertTrue(completed(bowls) == 17, "Deferred completion admitted on a later server tick exactly once");
            tick(remote);
            helper.assertTrue(completed(bowls) == 17, "No replay count");
            nether.setBlock(remotePos, oldRemote, 2);
            nether.setBlock(remotePos.below(), oldSupport, 2);
            nether.setChunkForced(0, 0, wasForced);
            helper.succeed();
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(BlockEntity bowl) {
        var ticker = bowl.getBlockState().getTicker(bowl.getLevel(), bowl.getType());
        ((net.minecraft.world.level.block.entity.BlockEntityTicker) ticker).tick(bowl.getLevel(), bowl.getBlockPos(), bowl.getBlockState(), bowl);
    }
    private static long completed(List<BlockEntity> bowls) {
        long total = 0;
        for (var bowl : bowls) total += bowl.saveCustomOnly(bowl.getLevel().registryAccess()).getCompound("bowl").getLong("completed");
        return total;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fullBottleDestinationAndMalformedReservationsNeverConsume(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(0, 1, 0));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        for (int cycle = 0; cycle < 7; cycle++) use(helper, pos, player, new ItemStack(Items.STICK), false);
        var saved = data(helper, pos);
        var slots = saved.getCompound("bowl").getList("items", 10);
        String[] keys = {"honey_bottle", "spore_culture", "honey_culture", "glass_bottle", "wheat", "carrot", "bone_meal", "slime_ball", "red_mushroom"};
        for (int i = 0; i < keys.length; i++) {
            slots.getCompound(i).putString("resource", keys[i]);
            slots.getCompound(i).putInt("count", i < 2 ? 1 : i == 2 ? 62 : 64);
            slots.getCompound(i).putInt("capacity", i == 0 ? 16 : 64);
        }
        helper.getLevel().getBlockEntity(pos).loadWithComponents(saved, helper.getLevel().registryAccess());
        use(helper, pos, player, ItemStack.EMPTY, false);
        helper.assertTrue(saved.equals(data(helper, pos)), "Full bottle slot refuses before consumption/counts");
        use(helper, pos, player, new ItemStack(Items.WATER_BUCKET), false);
        var valid = data(helper, pos);
        var tooMany = valid.copy();
        tooMany.getCompound("bowl").getList("items", 10).add(slots.getCompound(0).copy());
        helper.getLevel().getBlockEntity(pos).loadWithComponents(tooMany, helper.getLevel().registryAccess());
        use(helper, pos, player, ItemStack.EMPTY, false);
        helper.assertTrue(tooMany.equals(data(helper, pos)), "Oversized store is preserved but cannot operate");
        helper.succeed();
    }


    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bowl_refill")
    public static void waterEconomyRemainderAcceptsTheNextWholeBucket(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(0, 1, 0));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        var saved = data(helper, pos);
        saved.getCompound("bowl").putLong("completed", 512);
        saved.getCompound("bowl").putLong("lastCompleted", 512);
        saved.getCompound("bowl").putLong("nextBatch", 513);
        helper.getLevel().getBlockEntity(pos).loadWithComponents(saved, helper.getLevel().registryAccess());
        for (int i = 0; i < 3; i++) use(helper, pos, player, new ItemStack(Items.GLASS_BOTTLE), true);
        for (int i = 0; i < 8; i++) use(helper, pos, player, new ItemStack(Items.STICK), false);
        use(helper, pos, player, stack("spore_culture", 1), false);
        use(helper, pos, player, stack("wheat_seeds", 1), false);
        use(helper, pos, player, new ItemStack(Items.WATER_BUCKET), false);
        use(helper, pos, player, ItemStack.EMPTY, false);
        for (int i = 0; i < 1200; i++) tick(helper.getLevel().getBlockEntity(pos));
        helper.assertTrue(data(helper, pos).getCompound("bowl").getList("tanks", 10).getCompound(0).getInt("count") == 965,
                "Pure L3 water economy consumes exactly 35 mB");
        use(helper, pos, player, new ItemStack(Items.WATER_BUCKET), false);
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "A paid whole bucket must fit after water economy leaves a remainder");
        helper.assertTrue(data(helper, pos).getCompound("bowl").getList("tanks", 10).getCompound(0).getInt("count") == 1965,
                "Refill retains the remainder and the full paid bucket");
        var full = data(helper, pos);
        use(helper, pos, player, new ItemStack(Items.WATER_BUCKET), false);
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && full.equals(data(helper, pos)),
                "Overflow refuses without discarding water or bucket");
        helper.succeed();
    }

    private static int count(CompoundTag bowl, String key) {
        int count = 0;
        for (var raw : bowl.getList("items", 10)) {
            var slot = (CompoundTag) raw;
            if (slot.getString("resource").equals(key)) count += slot.getInt("count");
        }
        return count;
    }
    private static CompoundTag data(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(pos).saveWithFullMetadata(helper.getLevel().registryAccess());
    }
    private static BlockPos place(GameTestHelper helper, BlockPos relative) {
        var pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos.below(), BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(id("processing/culture_bowl")).defaultBlockState(), 2);
        return pos;
    }
    private static void use(GameTestHelper helper, BlockPos pos, Player player, ItemStack stack, boolean sneak) {
        player.setShiftKeyDown(sneak); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
        ((net.minecraft.server.level.ServerPlayer) player).gameMode.useItemOn(
                (net.minecraft.server.level.ServerPlayer) player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }
    private static ItemStack stack(String key, int count) {
        String path = switch (key) {
            case "spore_culture", "organ_bud" -> "infestusfrontier:construction/" + key;
            case "membrane_sheet", "fusion_binder", "elastic_gel", "lumen_secretion", "nutrient_mash",
                    "char_gland_feed", "honey_culture", "rooting_gel", "skeletal_graft" ->
                    "infestusfrontier:processing/" + key;
            case "biomass_bucket" -> "infestusfrontier:storage/biomass_bucket";
            default -> "minecraft:" + key;
        };
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(path)), count);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.parse("infestusfrontier:" + path); }
}
