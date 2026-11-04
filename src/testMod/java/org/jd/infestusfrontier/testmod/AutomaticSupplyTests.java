package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.holder;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.cultivation.CultivationModule;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.ItemStores;

/**
 * The automatic farm branch of the advancement tab: a Planting Proboscis that plants, a Harvest Corolla that cuts a
 * ripe crop and a Digestive Sac that finishes a batch on piped food. Each credits the organ's steward, online or
 * through the offline credit, and nobody else; placing, holding and failed work earn nothing.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class AutomaticSupplyTests {
    private static final BlockPos CENTER = new BlockPos(3, 1, 3);
    private static final BlockPos ORGAN = CENTER.above();
    private static final BlockPos SAC_GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos SAC = SAC_GROUND.above();
    private static final int WHEAT_TICKS = 40;
    private static final List<String> BRANCH =
            List.of("planting_proboscis", "harvest_corolla", "supplied_sac", "automatic_supply");

    private static void bedsAround(GameTestHelper h) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                h.setBlock(CENTER.offset(dx, 0, dz), CultivationModule.CULTIVATION_TISSUE.get());
        h.setBlock(CENTER, EcologyTests.mature());
    }

    /** Puts an organ on the plot and makes {@code steward} its placer, as setPlacedBy does after a placement. */
    private static void place(GameTestHelper h, BlockPos pos, BlockState organ, Player steward) {
        h.setBlock(pos, organ);
        if (steward != null)
            organ.getBlock().setPlacedBy(h.getLevel(), h.absolutePos(pos), organ, steward, ItemStack.EMPTY);
    }

    private static void fuel(GameTestHelper h, BlockPos organ) {
        Player bucket = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, bucket, organ, Direction.UP);
    }

    private static void sacOnGround(GameTestHelper h, Player steward) {
        h.setBlock(SAC_GROUND, EcologyTests.mature());
        place(h, SAC, ProcessingModule.DIGESTIVE_SAC.get().defaultBlockState(), steward);
    }

    private static BlockState mouth(MouthMode mode) {
        return LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode);
    }

    /** Puts {@code stack} into the Sac the way an intake Mouth does: through the store rule, not a slot. */
    private static void byMouth(GameTestHelper h, BlockPos sac, ItemStack stack) {
        IItemHandler port = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(sac), null);
        h.assertTrue(port != null, "the item capability answers at " + sac);
        h.assertTrue(ItemStores.insert(port, stack, false).isEmpty(), "the Sac takes what a Mouth offers");
    }

    private static void fillTank(GameTestHelper h, BlockPos organ, int units) {
        BiomassTank tank = BiomassTank.empty(Biomass.of(1000));
        if (units > 0) tank = ((Outcome.Done<BiomassTank>) tank.fill(Biomass.of(units))).value();
        h.getBlockEntity(organ)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(StorageModule.TANK.get(), tank)
                                .build(),
                        DataComponentPatch.EMPTY);
    }

    /** The player puts one seed into the planter at {@code organ} by hand, as a right-click does. */
    private static void giveSeeds(GameTestHelper h, Player player, BlockPos organ) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 4));
        EcologyTests.use(h, player, organ, Direction.UP);
    }

    private static void setOutput(GameTestHelper h, int slot, ItemStack stack) {
        MenuProvider provider = (MenuProvider) h.getBlockEntity(ORGAN);
        AbstractContainerMenu menu = provider.createMenu(
                1, EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0).getInventory(), null);
        menu.getSlot(slot).set(stack);
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 200)
    public static void theProboscisCreditsTheStewardWhoLoadedItAndNobodyElse(GameTestHelper h) {
        ServerPlayer steward = player(h);
        Player bystander = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        Player opener = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        bedsAround(h);
        place(h, ORGAN, CultivationModule.PLANTING_PROBOSCIS.get().defaultBlockState(), null);
        fuel(h, ORGAN);
        Organs.openScreen(h.getLevel(), h.absolutePos(ORGAN), opener);
        h.assertTrue(
                ((Stewarded) h.getBlockEntity(ORGAN)).steward().player().equals(Optional.of(opener.getUUID())),
                "an organ nobody tended is claimed by the first player to open it");
        giveSeeds(h, steward, ORGAN);
        h.assertTrue(
                ((Stewarded) h.getBlockEntity(ORGAN)).steward().player().equals(Optional.of(steward.getUUID())),
                "loading it by hand makes the loader its steward");
        Organs.openScreen(h.getLevel(), h.absolutePos(ORGAN), bystander);
        h.assertTrue(
                ((Stewarded) h.getBlockEntity(ORGAN)).steward().player().equals(Optional.of(steward.getUUID())),
                "opening the screen of a tended organ changes nothing");
        h.runAfterDelay(80, () -> {
            h.assertBlockPresent(Blocks.WHEAT, ORGAN.offset(-1, 0, -1));
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("planting_proboscis")), "the steward waits");
            for (Player other : List.of(bystander, opener))
                h.assertFalse(credits.holdsAutomatic(other.getUUID(), id("planting_proboscis")), "not the other");
            h.assertFalse(done(h, steward, "contact/planting_proboscis"), "not earned while away");
            credits.award(steward);
            h.assertTrue(done(h, steward, "contact/planting_proboscis"), "earned on login");
            h.assertFalse(credits.holdsAutomatic(steward.getUUID(), id("planting_proboscis")), "and spent");
            h.assertFalse(done(h, steward, "contact/harvest_corolla"), "only the planting");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 200)
    public static void aProboscisThatCannotPlantEarnsNothing(GameTestHelper h) {
        ServerPlayer steward = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        h.setBlock(CENTER, EcologyTests.mature());
        place(h, ORGAN, CultivationModule.PLANTING_PROBOSCIS.get().defaultBlockState(), steward);
        giveSeeds(h, steward, ORGAN); // a plot with no bed and no biomass: nothing can happen
        h.runAtTickTime(60, () -> {
            h.assertFalse(
                    credits.holdsAutomatic(steward.getUUID(), id("planting_proboscis")),
                    "no biomass, no bed, no credit");
            fuel(h, ORGAN);
        });
        h.runAtTickTime(120, () -> {
            h.assertFalse(
                    credits.holdsAutomatic(steward.getUUID(), id("planting_proboscis")),
                    "biomass without a bed to plant in is still no credit");
            for (int dx = -1; dx <= 1; dx++)
                h.setBlock(CENTER.offset(dx, 0, 1), CultivationModule.CULTIVATION_TISSUE.get());
        });
        h.runAtTickTime(190, () -> {
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("planting_proboscis")), "a real planting counts");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 200)
    public static void theCorollaCreditsOnlyAHarvestThatHappened(GameTestHelper h) {
        ServerPlayer steward = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        bedsAround(h);
        place(h, ORGAN, CultivationModule.HARVEST_COROLLA.get().defaultBlockState(), steward);
        h.setBlock(ORGAN.north(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
        fuel(h, ORGAN);
        h.runAtTickTime(50, () -> {
            h.assertFalse(
                    credits.holdsAutomatic(steward.getUUID(), id("harvest_corolla")), "a young crop is not harvested");
            h.setBlock(ORGAN.north(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
            for (int slot = 0; slot < 4; slot++) setOutput(h, slot, new ItemStack(Items.STONE, 64));
        });
        h.runAtTickTime(110, () -> {
            h.assertBlockPresent(Blocks.WHEAT, ORGAN.north());
            h.assertFalse(
                    credits.holdsAutomatic(steward.getUUID(), id("harvest_corolla")),
                    "a full output leaves the crop standing and earns nothing");
            setOutput(h, 2, ItemStack.EMPTY); // wheat and seeds each need a slot
            setOutput(h, 3, ItemStack.EMPTY);
        });
        h.runAtTickTime(170, () -> {
            h.assertBlockPresent(Blocks.AIR, ORGAN.north());
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("harvest_corolla")), "the cut counts");
            credits.award(steward);
            h.assertTrue(done(h, steward, "contact/harvest_corolla"), "earned on login");
            h.assertFalse(done(h, steward, "contact/planting_proboscis"), "and only the harvest");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 200)
    public static void anUntendedOrgansWorkEarnsNobodyAnything(GameTestHelper h) {
        ServerPlayer neighbour = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        bedsAround(h);
        place(h, ORGAN, CultivationModule.HARVEST_COROLLA.get().defaultBlockState(), null);
        h.setBlock(ORGAN.north(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        fuel(h, ORGAN);
        h.runAtTickTime(80, () -> {
            h.assertBlockPresent(Blocks.AIR, ORGAN.north());
            h.assertTrue(
                    ((Stewarded) h.getBlockEntity(ORGAN)).steward().player().isEmpty(), "nobody tends it");
            h.assertFalse(
                    credits.holdsAutomatic(neighbour.getUUID(), id("harvest_corolla")), "no credit for the neighbour");
            credits.award(neighbour);
            h.assertFalse(done(h, neighbour, "contact/harvest_corolla"), "a bystander is not credited");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 200)
    public static void twoStewardsAreCreditedEachForTheirOwnOrgan(GameTestHelper h) {
        ServerPlayer first = player(h);
        ServerPlayer second = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        BlockPos otherCenter = CENTER.east(4);
        bedsAround(h);
        h.setBlock(otherCenter, EcologyTests.mature());
        h.setBlock(otherCenter.north(), CultivationModule.CULTIVATION_TISSUE.get());
        place(h, ORGAN, CultivationModule.HARVEST_COROLLA.get().defaultBlockState(), first);
        place(h, otherCenter.above(), CultivationModule.PLANTING_PROBOSCIS.get().defaultBlockState(), second);
        h.setBlock(ORGAN.north(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        fuel(h, ORGAN);
        fuel(h, otherCenter.above());
        player(h); // a third player stands about and does nothing
        giveSeeds(h, second, otherCenter.above());
        h.runAtTickTime(100, () -> {
            h.assertTrue(credits.holdsAutomatic(first.getUUID(), id("harvest_corolla")), "the first has the harvest");
            h.assertFalse(credits.holdsAutomatic(first.getUUID(), id("planting_proboscis")), "not the planting");
            h.assertTrue(credits.holdsAutomatic(second.getUUID(), id("planting_proboscis")), "the second the planting");
            h.assertFalse(
                    credits.holdsAutomatic(second.getUUID(), id("harvest_corolla")), "the second not the harvest");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 300)
    public static void aSacCreditsPipedFoodToItsStewardButNotHandFeeding(GameTestHelper h) {
        ServerPlayer piped = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        sacOnGround(h, piped);
        byMouth(h, SAC, new ItemStack(Items.WHEAT));
        h.runAtTickTime(WHEAT_TICKS - 10, () -> {
            h.assertFalse(credits.holdsAutomatic(piped.getUUID(), id("digestive_sac")), "not before it finishes");
        });
        h.runAtTickTime(WHEAT_TICKS + 30, () -> {
            h.assertTrue(credits.holdsAutomatic(piped.getUUID(), id("digestive_sac")), "piped food counts");
            h.assertTrue(credits.holds(piped.getUUID(), id("digestive_sac")), "and it is a batch of its steward too");
            credits.award(piped);
            h.assertTrue(done(h, piped, "contact/supplied_sac"), "the supplied milestone");
            h.assertTrue(done(h, piped, "contact/digestive_sac"), "and First Biomass");
            h.assertFalse(done(h, piped, "contact/harvest_corolla"), "the milestones stand apart");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 300)
    public static void aHopperFedSacBatchIsItsStewardsButNotAutomatic(GameTestHelper h) {
        ServerPlayer steward = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        sacOnGround(h, steward);
        h.setBlock(SAC.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((HopperBlockEntity) h.getBlockEntity(SAC.above())).setItem(0, new ItemStack(Items.WHEAT, 2));
        h.runAtTickTime(WHEAT_TICKS + 30, () -> {
            h.assertTrue(credits.holds(steward.getUUID(), id("digestive_sac")), "the hopper's batch is the steward's");
            h.assertFalse(
                    credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")),
                    "but a hopper is not the item transport system");
            credits.award(steward);
            h.assertTrue(done(h, steward, "contact/digestive_sac"), "First Biomass follows the batch");
            h.assertFalse(done(h, steward, "contact/supplied_sac"), "the supplied milestone does not");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 300)
    public static void wheatCarriedByMouthsAndVeinsEarnsFedThroughTheVeins(GameTestHelper h) {
        ServerPlayer steward = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        BlockPos out = new BlockPos(1, 1, 1);
        BlockPos in = new BlockPos(4, 1, 1);
        h.setBlock(out, mouth(MouthMode.OUTPUT));
        h.setBlock(out.above(), Blocks.CHEST);
        ((ChestBlockEntity) h.getBlockEntity(out.above())).setItem(0, new ItemStack(Items.WHEAT, 1));
        h.setBlock(out.east(), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(out.east(2), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(in, mouth(MouthMode.INTAKE));
        place(h, in.above(), ProcessingModule.DIGESTIVE_SAC.get().defaultBlockState(), steward);
        h.runAtTickTime(WHEAT_TICKS + 80, () -> {
            h.assertTrue(
                    credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")),
                    "wheat carried by Mouths and veins is automatic work");
            credits.award(steward);
            h.assertTrue(done(h, steward, "contact/supplied_sac"), "earned on login");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 200)
    public static void aSlotBatchIsItsStewardsButFoodFromTheScreenIsNotDelivered(GameTestHelper h) {
        ServerPlayer slotted = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        sacOnGround(h, slotted);
        MenuProvider provider = (MenuProvider) h.getBlockEntity(SAC);
        provider.createMenu(
                        1,
                        EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0).getInventory(),
                        null)
                .getSlot(0)
                .set(new ItemStack(Items.WHEAT, 2));
        h.runAtTickTime(WHEAT_TICKS + 30, () -> {
            h.assertTrue(credits.holds(slotted.getUUID(), id("digestive_sac")), "a slot batch is its steward's");
            h.assertFalse(
                    credits.holdsAutomatic(slotted.getUUID(), id("digestive_sac")),
                    "food put in at the screen is not delivered food");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 200)
    public static void aSacBatchByHandIsNeverAutomatic(GameTestHelper h) {
        ServerPlayer hand = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        sacOnGround(h, null);
        hand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 1));
        EcologyTests.use(h, hand, SAC, Direction.UP);
        h.runAtTickTime(WHEAT_TICKS + 30, () -> {
            h.assertTrue(credits.holds(hand.getUUID(), id("digestive_sac")), "the feeder's batch");
            h.assertFalse(credits.holdsAutomatic(hand.getUUID(), id("digestive_sac")), "but not an automatic one");
            credits.award(hand);
            h.assertTrue(done(h, hand, "contact/digestive_sac"), "First Biomass");
            h.assertFalse(done(h, hand, "contact/supplied_sac"), "not the supplied milestone");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 300)
    public static void aFullSacEarnsNothingUntilItDigests(GameTestHelper h) {
        ServerPlayer steward = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        sacOnGround(h, steward);
        fillTank(h, SAC, 1000);
        byMouth(h, SAC, new ItemStack(Items.WHEAT));
        h.runAtTickTime(WHEAT_TICKS + 60, () -> {
            h.assertFalse(
                    credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")),
                    "a Sac with no room starts nothing and credits nothing");
            fillTank(h, SAC, 0);
        });
        h.runAtTickTime(WHEAT_TICKS * 2 + 80, () -> {
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")), "once there is room");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 300)
    public static void reloadingMidBatchKeepsTheCreditAndMakesNoneEarly(GameTestHelper h) {
        ServerPlayer steward = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        sacOnGround(h, steward);
        byMouth(h, SAC, new ItemStack(Items.WHEAT));
        h.runAtTickTime(15, () -> {
            h.assertFalse(credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")), "still digesting");
            BlockEntity running = h.getBlockEntity(SAC);
            var registries = h.getLevel().registryAccess();
            CompoundTag saved = running.saveWithFullMetadata(registries);
            BlockEntity reloaded = BlockEntity.loadStatic(h.absolutePos(SAC), h.getBlockState(SAC), saved, registries);
            h.assertTrue(reloaded != null, "the saved Sac reads back");
            h.getLevel().setBlockEntity(reloaded);
            h.assertTrue(
                    ((Stewarded) reloaded).steward().player().equals(Optional.of(steward.getUUID())),
                    "the steward survives the reload");
            h.assertFalse(credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")), "reloading credits nothing");
        });
        h.runAtTickTime(WHEAT_TICKS + 50, () -> {
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")), "the batch still counts");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            batch = "automatic_supply",
            template = "empty",
            timeoutTicks = 300)
    public static void pipedFoodWaitingInTheSlotStillCountsAfterAReload(GameTestHelper h) {
        ServerPlayer steward = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        sacOnGround(h, steward);
        fillTank(h, SAC, 1000);
        byMouth(h, SAC, new ItemStack(Items.WHEAT));
        h.runAtTickTime(10, () -> {
            var registries = h.getLevel().registryAccess();
            BlockEntity reloaded = BlockEntity.loadStatic(
                    h.absolutePos(SAC),
                    h.getBlockState(SAC),
                    h.getBlockEntity(SAC).saveWithFullMetadata(registries),
                    registries);
            h.getLevel().setBlockEntity(reloaded);
            fillTank(h, SAC, 0);
        });
        h.runAtTickTime(WHEAT_TICKS + 70, () -> {
            h.assertTrue(
                    credits.holdsAutomatic(steward.getUUID(), id("digestive_sac")),
                    "the food was delivered before the reload");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", batch = "automatic_supply", template = "empty")
    public static void aBrokenOrganForgetsItsStewardAndAPlotOrganKeepsItAcrossAReload(GameTestHelper h) {
        ServerPlayer steward = player(h);
        bedsAround(h);
        place(h, ORGAN, CultivationModule.HARVEST_COROLLA.get().defaultBlockState(), steward);
        var registries = h.getLevel().registryAccess();
        CompoundTag saved = h.getBlockEntity(ORGAN).saveWithFullMetadata(registries);
        BlockEntity reloaded = BlockEntity.loadStatic(h.absolutePos(ORGAN), h.getBlockState(ORGAN), saved, registries);
        h.assertTrue(
                ((Stewarded) reloaded).steward().player().equals(Optional.of(steward.getUUID())),
                "the steward is saved with the organ");
        CompoundTag carried = h.getBlockEntity(ORGAN).saveCustomOnly(registries);
        h.getBlockEntity(ORGAN).removeComponentsFromTag(carried);
        h.assertFalse(carried.contains("steward"), "a picked-up organ carries no steward");
        ServerPlayer newcomer = player(h);
        h.setBlock(ORGAN, Blocks.AIR);
        place(h, ORGAN, CultivationModule.HARVEST_COROLLA.get().defaultBlockState(), null);
        h.assertTrue(((Stewarded) h.getBlockEntity(ORGAN)).steward().player().isEmpty(), "a new organ has no steward");
        h.assertTrue(
                ((Stewarded) h.getBlockEntity(ORGAN)).steward().claim(newcomer), "and the first to open it claims it");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", batch = "automatic_supply", template = "empty")
    public static void holdingAndPlacingTheMachinesCompletesNoOperationMilestone(GameTestHelper h) {
        ServerPlayer player = player(h);
        for (var item : List.of(
                CultivationModule.PLANTING_PROBOSCIS_ITEM.get(),
                CultivationModule.HARVEST_COROLLA_ITEM.get(),
                ProcessingModule.DIGESTIVE_SAC_ITEM.get())) {
            ItemStack stack = new ItemStack(item);
            player.getInventory().add(stack.copy());
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
        }
        bedsAround(h);
        place(h, ORGAN, CultivationModule.PLANTING_PROBOSCIS.get().defaultBlockState(), player);
        place(h, ORGAN.east(2), CultivationModule.HARVEST_COROLLA.get().defaultBlockState(), player);
        sacOnGround(h, player);
        CriteriaTriggers.TICK.trigger(player);
        PendingCredits.get(h.getLevel()).award(player);
        for (String path : BRANCH) h.assertFalse(done(h, player, "contact/" + path), path + " waits for work");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", batch = "automatic_supply", template = "empty")
    public static void theBranchHangsUnderFirstBiomassAndTheGoalNeedsAllThree(GameTestHelper h) {
        record Step(String path, String parent, Item icon, String organ) {}
        for (Step step : List.of(
                new Step(
                        "planting_proboscis",
                        "digestive_sac",
                        CultivationModule.PLANTING_PROBOSCIS_ITEM.get(),
                        "planting_proboscis"),
                new Step(
                        "harvest_corolla",
                        "planting_proboscis",
                        CultivationModule.HARVEST_COROLLA_ITEM.get(),
                        "harvest_corolla"),
                new Step("supplied_sac", "harvest_corolla", LogisticsModule.MOUTH_ITEM.get(), "digestive_sac"))) {
            var advancement = holder(h, "contact/" + step.path()).value();
            h.assertValueEqual(advancement.parent().orElseThrow(), id("contact/" + step.parent()), "parent");
            h.assertValueEqual(
                    advancement.display().orElseThrow().getIcon().getItem(), step.icon(), step.path() + " icon");
            h.assertValueEqual(advancement.display().orElseThrow().getType(), AdvancementType.TASK, "a task");
            h.assertValueEqual(advancement.criteria().size(), 1, "one criterion");
            var criterion = advancement.criteria().values().iterator().next();
            h.assertValueEqual(
                    BuiltInRegistries.TRIGGER_TYPES.getKey(criterion.trigger()).toString(),
                    "infestusfrontier:automatic_work",
                    step.path() + " uses the automatic work trigger");
            var written =
                    Criterion.CODEC.encodeStart(JsonOps.INSTANCE, criterion).getOrThrow();
            h.assertValueEqual(
                    written.getAsJsonObject()
                            .getAsJsonObject("conditions")
                            .get("organ")
                            .getAsString(),
                    "infestusfrontier:" + step.organ(),
                    step.path() + " names its organ");
        }
        AdvancementHolder goal = holder(h, "contact/automatic_supply");
        h.assertValueEqual(goal.value().parent().orElseThrow(), id("contact/supplied_sac"), "the goal's parent");
        h.assertValueEqual(goal.value().display().orElseThrow().getType(), AdvancementType.GOAL, "a goal");
        h.assertTrue(
                goal.value().rewards().recipes().isEmpty()
                        && goal.value().rewards().loot().isEmpty()
                        && goal.value().rewards().experience() == 0,
                "no reward");
        ServerPlayer player = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        credits.rememberAutomatic(player.getUUID(), id("planting_proboscis"));
        credits.rememberAutomatic(player.getUUID(), id("digestive_sac"));
        credits.award(player);
        CriteriaTriggers.TICK.trigger(player);
        h.assertFalse(done(h, player, "contact/automatic_supply"), "two of three is not the goal");
        credits.rememberAutomatic(player.getUUID(), id("harvest_corolla"));
        credits.award(player);
        CriteriaTriggers.TICK.trigger(player);
        h.assertTrue(done(h, player, "contact/automatic_supply"), "the three earn the goal");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", batch = "automatic_supply", template = "empty")
    public static void batchAndAutomaticCreditsDoNotShadowEachOther(GameTestHelper h) {
        ServerPlayer player = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        credits.rememberAutomatic(player.getUUID(), id("digestive_sac"));
        h.assertTrue(credits.holdsAutomatic(player.getUUID(), id("digestive_sac")), "remembered");
        h.assertFalse(credits.holds(player.getUUID(), id("digestive_sac")), "not as a batch");
        credits.award(player);
        h.assertTrue(done(h, player, "contact/supplied_sac"), "an automatic credit earns the supplied milestone");
        h.assertFalse(done(h, player, "contact/digestive_sac"), "and not First Biomass");
        h.succeed();
    }
}
