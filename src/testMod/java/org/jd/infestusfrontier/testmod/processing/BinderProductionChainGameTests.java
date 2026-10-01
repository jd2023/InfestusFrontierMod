package org.jd.infestusfrontier.testmod.processing;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.testmod.integration.AdvancementRecordingPlayer;

/** Connected survival transfers; only raw ingredients and empty organs are supplied by the fixture. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class BinderProductionChainGameTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty",
            batch = "binder_production_chain", timeoutTicks = 5000)
    public static void rackAndSacSupplyBinderForLumenCharFeedAndSkeletalGraft(GameTestHelper helper) {
        var chain = new Chain(helper);
        var sequence = helper.startSequence().thenExecute(chain::startRack);
        for (int batch = 1; batch <= 20; batch++) {
            sequence.thenExecute(chain::feedSac).thenIdle(45);
            if (batch == 10) sequence.thenExecute(() -> chain.transferBiomass(chain.bowl));
            if (batch == 20) sequence.thenExecute(() -> chain.transferBiomass(chain.loom));
        }
        sequence.thenExecute(chain::startBinderAndPlate)
                .thenIdle(610).thenExecute(chain::reloadBinder)
                .thenIdle(610).thenExecute(chain::startLumenAndGraft)
                .thenIdle(1220).thenExecute(chain::startCharFeed)
                .thenIdle(1220).thenExecute(chain::verifyConservation)
                .thenIdle(100).thenExecute(chain::verifyConservation).thenSucceed();
    }

    private static final class Chain {
        private final GameTestHelper helper;
        private final AdvancementRecordingPlayer player;
        private final BlockPos rack, sac, bowl, loom;
        private final ItemStack wheat = new ItemStack(Items.WHEAT, 20);
        private ItemStack bucket = new ItemStack(Items.BUCKET);
        private ItemStack binder = ItemStack.EMPTY;
        private ItemStack lumen = ItemStack.EMPTY;
        private ItemStack graft = ItemStack.EMPTY;
        private ItemStack charFeed = ItemStack.EMPTY;

        private Chain(GameTestHelper helper) {
            this.helper = helper;
            player = new AdvancementRecordingPlayer(helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "BinderChain"));
            player.setGameMode(GameType.SURVIVAL);
            rack = place("membrane_rack", 0);
            sac = place("digestive_sac", 1);
            bowl = place("culture_bowl", 2);
            loom = place("bone_loom", 3);
        }

        private void startRack() {
            insert(rack, new ItemStack(Items.ROTTEN_FLESH));
            insert(rack, new ItemStack(Items.STRING));
            var returned = use(rack, new ItemStack(Items.WATER_BUCKET));
            helper.assertTrue(returned.is(Items.BUCKET), "Rack returns the water container");
            use(rack, ItemStack.EMPTY);
        }

        private void feedSac() {
            int before = wheat.getCount();
            use(sac, wheat);
            helper.assertTrue(wheat.getCount() == before - 1, "Sac consumes exactly one of the 20 supplied wheat");
        }

        private void transferBiomass(BlockPos destination) {
            helper.assertTrue(fluid(sac, "biomass") == 1000, "Ten completed wheat batches fill one bucket");
            bucket = use(sac, bucket);
            helper.assertTrue(bucket.is(BuiltInRegistries.ITEM.get(id("storage/biomass_bucket")))
                    && fluid(sac, "biomass") == 0, "Withdraw actual Sac output, leaving no duplicate biomass");
            bucket = use(destination, bucket);
            helper.assertTrue(bucket.is(Items.BUCKET) && bucket.getCount() == 1
                    && fluid(destination, "biomass") == 1000, "Transfer the same measured bucket into the consumer");
        }

        private void startBinderAndPlate() {
            var sheet = collect(rack);
            product(sheet, "membrane_sheet", 1);
            insert(bowl, sheet);
            insert(bowl, new ItemStack(BuiltInRegistries.ITEM.get(id("construction/spore_culture"))));
            selectBowl("I004");
            use(bowl, ItemStack.EMPTY);
            insert(loom, new ItemStack(Items.BONE));
            use(loom, ItemStack.EMPTY);
            helper.assertTrue(state(bowl).contains("active") && state(loom).contains("active"),
                    "Rack Sheet and Sac biomass admit real Binder and Plate batches");
        }

        private void reloadBinder() {
            var entity = helper.getLevel().getBlockEntity(bowl);
            var saved = entity.saveWithFullMetadata(helper.getLevel().registryAccess());
            int progress = state(bowl).getCompound("active").getInt("work");
            helper.assertTrue(progress > 0 && progress < 1200, "Reload interrupts a partially completed Binder batch");
            helper.getLevel().removeBlockEntity(bowl);
            helper.getLevel().setBlockEntity(BlockEntity.loadStatic(bowl, helper.getLevel().getBlockState(bowl),
                    saved, helper.getLevel().registryAccess()));
            helper.assertTrue(saved.equals(helper.getLevel().getBlockEntity(bowl)
                    .saveWithFullMetadata(helper.getLevel().registryAccess())), "Reload preserves the actual reservation and progress");
        }

        private void startLumenAndGraft() {
            binder = collect(bowl);
            product(binder, "fusion_binder", 4);
            var plate = collect(loom);
            product(plate, "bone_plate", 1);
            insert(loom, plate);
            insert(loom, binder);
            use(loom, ItemStack.EMPTY);
            insert(bowl, binder);
            insert(bowl, new ItemStack(Items.GLOW_INK_SAC));
            selectBowl("I006");
            use(bowl, ItemStack.EMPTY);
            product(binder, "fusion_binder", 2);
        }

        private void startCharFeed() {
            lumen = collect(bowl);
            graft = collect(loom);
            product(lumen, "lumen_secretion", 2);
            product(graft, "skeletal_graft", 1);
            insert(bowl, binder);
            insert(bowl, new ItemStack(Items.CHARCOAL));
            selectBowl("I029");
            use(bowl, ItemStack.EMPTY);
            product(binder, "fusion_binder", 1);
        }

        private void verifyConservation() {
            if (charFeed.isEmpty()) charFeed = collect(bowl);
            product(charFeed, "char_gland_feed", 1);
            product(lumen, "lumen_secretion", 2);
            product(graft, "skeletal_graft", 1);
            product(binder, "fusion_binder", 1);
            helper.assertTrue(wheat.isEmpty() && bucket.is(Items.BUCKET), "All wheat is spent and its reusable bucket is empty");
            helper.assertTrue(fluid(sac, "biomass") == 0 && fluid(bowl, "biomass") == 850
                            && fluid(loom, "biomass") == 925 && fluid(rack, "water") == 900,
                    "2000 Sac BU = 1775 retained + 100 Binder + 25 Lumen + 25 Char Feed + 50 Plate + 25 Graft; Rack spends 100 water");
            for (var pos : new BlockPos[] {rack, bowl, loom}) {
                var state = state(pos);
                helper.assertTrue(!state.contains("active"), "Every production batch is idle after collection");
                for (var tag : state.getList("items", 10)) helper.assertTrue(((CompoundTag) tag).getInt("count") == 0,
                        "No raw ingredient or duplicate intermediate remains in an organ");
            }
            helper.assertTrue(state(rack).getLong("completed") == 1 && state(bowl).getLong("completed") == 3
                            && state(loom).getLong("completed") == 2 && state(sac).getLong("completed") == 20,
                    "Reload and later idle ticks cannot mint a second batch or history count");
        }

        private void selectBowl(String recipe) {
            for (int i = 0; i < 9 && !state(bowl).getString("selected").equals(recipe); i++) use(bowl, new ItemStack(Items.STICK));
            helper.assertTrue(state(bowl).getString("selected").equals(recipe), "Select Bowl recipe " + recipe);
        }

        private void insert(BlockPos pos, ItemStack stack) {
            int before = stack.getCount();
            use(pos, stack);
            helper.assertTrue(stack.getCount() == before - 1, "Insert consumes exactly one real held ingredient");
        }

        private ItemStack collect(BlockPos pos) {
            player.setShiftKeyDown(true);
            var result = use(pos, ItemStack.EMPTY);
            player.setShiftKeyDown(false);
            return result;
        }

        private ItemStack use(BlockPos pos, ItemStack stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            return player.getMainHandItem();
        }

        private void product(ItemStack stack, String name, int count) {
            helper.assertTrue(stack.is(BuiltInRegistries.ITEM.get(id("processing/" + name)))
                    && stack.getCount() == count, "Collected actual " + count + " x " + name);
        }

        private BlockPos place(String name, int x) {
            var pos = helper.absolutePos(new BlockPos(x, 1, 1));
            helper.getLevel().setBlock(pos.below(), BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
            helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(id("processing/" + name)).defaultBlockState(), 2);
            return pos;
        }

        private CompoundTag state(BlockPos pos) {
            return helper.getLevel().getBlockEntity(pos).saveCustomOnly(helper.getLevel().registryAccess())
                    .getCompound(pos.equals(bowl) ? "bowl" : pos.equals(sac) ? "sac" : "preparation");
        }

        private int fluid(BlockPos pos, String resource) {
            var store = pos.equals(sac) ? state(pos).getCompound("store") : state(pos);
            return store.getList("tanks", 10).stream().map(CompoundTag.class::cast)
                    .filter(tank -> tank.getString("resource").equals(resource)).mapToInt(tank -> tank.getInt("count")).sum();
        }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.parse("infestusfrontier:" + path); }
}
