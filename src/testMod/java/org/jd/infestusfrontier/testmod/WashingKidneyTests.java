package org.jd.infestusfrontier.testmod;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.mineral.Grinding;
import org.jd.infestusfrontier.core.mineral.Metal;
import org.jd.infestusfrontier.core.mineral.Parcel;
import org.jd.infestusfrontier.core.mineral.Washing;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.mineral.MineralModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.OrganBatch;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * The Washing Kidney: a Tailings of four residual units gives one more dust and a Tailings of three, for 250 mB of clean
 * water and 30 BU in 8 seconds; the water stays as dirty water in the Kidney's own tank, and a full tank settles into
 * one Rock Residue for 20 BU in 20 seconds. Tests count every unit, refuse the washed parcel and every foreign input,
 * pause on every full tray and on a full dirty tank, and keep one copy of everything through saves, breaking, an
 * unloaded chunk and a line with no hand in it. Organs stand at y=2 on substrate at y=1.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class WashingKidneyTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int Y = 1;
    private static final int WASH_TICKS = 160;
    private static final int SETTLE_TICKS = 400;
    private static final int WASH_BU = 30;
    private static final int SETTLE_BU = 20;
    /** Slots of the Kidney's ports: the Tailings, then dust, washed Tailings, Rock Residue. */
    private static final int DUST = 1;

    private static final int TAILINGS = 2;
    private static final int RESIDUE = 3;
    /** The {@code blocked} bits: 1, 2 and 4 for the three trays, 8 for a dirty tank with no room. */
    private static final int DIRTY_FULL = 8;

    private WashingKidneyTests() {}

    // --- washing ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 260)
    public static void everyMetalWashesToOneDustAndAThreeUnitParcelAndTheWaterGoesDirty(GameTestHelper h) {
        List<Metal> metals = List.of(Metal.values());
        for (int i = 0; i < metals.size(); i++) {
            BlockPos at = kidney(h, 1 + 2 * i, 1000, 1000);
            ports(h, at).insertItem(0, tailings(metals.get(i), Grinding.TAILINGS_UNITS, 5), false);
        }
        h.runAfterDelay(2, () -> {
            for (int i = 0; i < metals.size(); i++) {
                Metal metal = metals.get(i);
                BlockPos at = new BlockPos(1 + 2 * i, Y + 1, 1);
                BatchOrganContents started = contents(h, at);
                h.assertTrue(started.batch().isPresent(), metal.id() + ": a wash starts on its own");
                h.assertValueEqual(
                        started.batch().orElseThrow().batch().recipe(),
                        "infestusfrontier:washing/" + metal.id(),
                        metal.id() + ": the recipe");
                h.assertValueEqual(started.batch().orElseThrow().batch().ticksRequired(), WASH_TICKS, "8 seconds");
                h.assertValueEqual(started.ingredients().get(0).getCount(), 4, "one Tailings taken, the rest waits");
                h.assertValueEqual(started.waterMb(), 1000 - Washing.CLEAN_PER_WASH, "250 mB clean water spent");
                h.assertValueEqual(started.biomass().stored().units(), 1000 - WASH_BU, "30 BU spent at the start");
                h.assertValueEqual(started.dirtyMb(), Washing.DIRTY_PER_WASH, "250 mB dirty water, reserved at once");
                h.assertValueEqual(started.batch().orElseThrow().results().size(), 2, "a dust and a Tailings promised");
                IFluidHandler fluid = fluid(h, at);
                h.assertValueEqual(
                        fluid.drain(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.SIMULATE)
                                .getAmount(),
                        1000 - Washing.CLEAN_PER_WASH,
                        metal.id() + ": a pipe can take only the clean water, never the dirty");
            }
        });
        h.runAtTickTime(WASH_TICKS + 6, () -> {
            for (int i = 0; i < metals.size(); i++) {
                Metal metal = metals.get(i);
                BlockPos at = new BlockPos(1 + 2 * i, Y + 1, 1);
                Washing.Washed rule = Washing.wash(Parcel.ground(metal)).orElseThrow();
                BatchOrganContents done = contents(h, at);
                h.assertValueEqual(done.history().completedBatches(), 1, metal.id() + ": one wash counted once");
                ItemStack dust = done.outputs().get(0);
                h.assertTrue(dust.is(MineralModule.dust(metal)), metal.id() + ": its own dust");
                h.assertValueEqual(dust.getCount(), rule.dust(), metal.id() + ": one more dust");
                ItemStack washed = done.outputs().get(1);
                h.assertTrue(washed.is(MineralModule.MINERAL_TAILINGS.get()), metal.id() + ": a Tailings");
                h.assertValueEqual(washed.getCount(), 1, metal.id() + ": one Tailings");
                h.assertValueEqual(
                        washed.get(MineralModule.PARCEL.get()),
                        rule.remaining(),
                        metal.id() + ": three residual units of the same metal");
                h.assertTrue(done.outputs().get(2).isEmpty(), metal.id() + ": no residue from a wash");
                // the same residual state cannot be washed again, from the tray or by hand
                IItemHandler ports = ports(h, at);
                ItemStack taken = ports.extractItem(TAILINGS, 64, false);
                h.assertValueEqual(taken.get(MineralModule.PARCEL.get()), rule.remaining(), "the washed parcel");
                h.assertFalse(ports.isItemValid(0, taken), metal.id() + ": the Kidney refuses the washed parcel");
                h.assertValueEqual(
                        ports.insertItem(0, taken, true).getCount(), 1, metal.id() + ": no way back in for 3 units");
                Player hand = EcologyTests.holding(h, GameType.SURVIVAL, MineralModule.MINERAL_TAILINGS.get(), 1);
                hand.getMainHandItem().set(MineralModule.PARCEL.get(), rule.remaining());
                EcologyTests.use(h, hand, at, Direction.UP);
                h.assertValueEqual(hand.getMainHandItem().getCount(), 1, metal.id() + ": it stays in the hand");
                h.assertValueEqual(
                        contents(h, at).ingredients().get(0).getCount(),
                        3,
                        metal.id() + ": the slot still holds only the three fresh Tailings");
            }
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theKidneyRefusesEverythingButAFreshTailings(GameTestHelper h) {
        BlockPos at = kidney(h, 1, 1000, 1000);
        IItemHandler ports = ports(h, at);
        List<ItemStack> refused = new ArrayList<>();
        for (Metal metal : Metal.values()) {
            for (int units = 1; units < Grinding.TAILINGS_UNITS; units++) refused.add(tailings(metal, units, 3));
            refused.add(new ItemStack(raw(metal), 3));
            refused.add(new ItemStack(MineralModule.dust(metal), 3));
        }
        refused.add(new ItemStack(MineralModule.MINERAL_TAILINGS.get(), 3)); // no parcel
        refused.add(new ItemStack(MineralModule.ROCK_RESIDUE.get(), 3));
        for (Item item : List.of(
                Items.IRON_INGOT,
                Items.GOLD_NUGGET,
                Items.COPPER_BLOCK,
                Items.IRON_ORE,
                Items.IRON_PICKAXE,
                Items.COBBLESTONE,
                Items.WATER_BUCKET,
                Items.SAND,
                Items.GRAVEL)) refused.add(new ItemStack(item, 3));
        for (ItemStack stack : refused)
            h.assertTrue(
                    ports.insertItem(0, stack, true).getCount() == 3 && !ports.isItemValid(0, stack),
                    "the Kidney refuses " + stack);
        for (Metal metal : Metal.values())
            h.assertTrue(
                    ports.isItemValid(0, tailings(metal, Grinding.TAILINGS_UNITS, 1)),
                    metal.id() + " Tailings of four units go in");
        h.assertTrue(ports.insertItem(0, tailings(Metal.IRON, 4, 5), false).isEmpty(), "iron Tailings fill the slot");
        h.assertValueEqual(
                ports.insertItem(0, tailings(Metal.GOLD, 4, 5), false).getCount(),
                5,
                "gold waits while the slot holds iron");
        // by hand a refused item opens the screen and stays in the hand
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, Items.RAW_COPPER, 5);
        EcologyTests.use(h, hand, at, Direction.UP);
        h.assertValueEqual(hand.getMainHandItem().getCount(), 5, "raw copper stays in the hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1500)
    public static void fourWashesFillTheDirtyTankOneSettleClearsItAndTheLedgerIsExact(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = kidney(h, 1, 1000, 1000);
        IItemHandler ports = ports(h, at);
        int washes = (int) Washing.line(4).washes();
        ports.insertItem(0, tailings(Metal.IRON, Grinding.TAILINGS_UNITS, washes + 1), false);
        long[] marks = new long[4];
        List<String> starts = new ArrayList<>();
        List<Integer> dirtyAtStart = new ArrayList<>();
        BatchOrganContents[] last = new BatchOrganContents[1];
        h.startSequence()
                .thenExecute(() -> marks[0] = level.getGameTime())
                .thenWaitUntil(() -> {
                    // every tick: note each batch the Kidney starts, with the dirty tank as it stood
                    BatchOrganContents now = contents(h, at);
                    if (now.batch().isPresent()
                            && (last[0] == null
                                    || last[0].batch().isEmpty()
                                    || last[0].batch().get().batch().ticksDone()
                                            > now.batch().get().batch().ticksDone())) {
                        starts.add(now.batch().get().batch().recipe());
                        dirtyAtStart.add(now.dirtyMb());
                    }
                    last[0] = now;
                    h.assertTrue(
                            now.history().completedBatches() == 5 && now.batch().isEmpty(),
                            "four washes and a settle are done");
                })
                .thenExecute(() -> {
                    marks[1] = level.getGameTime();
                    BatchOrganContents settled = contents(h, at);
                    h.assertValueEqual(
                            starts,
                            List.of(
                                    "infestusfrontier:washing/iron",
                                    "infestusfrontier:washing/iron",
                                    "infestusfrontier:washing/iron",
                                    "infestusfrontier:washing/iron",
                                    "infestusfrontier:settling"),
                            "the Kidney settles before it washes a fifth time");
                    h.assertValueEqual(
                            dirtyAtStart,
                            List.of(250, 500, 750, 1000, 0),
                            "the dirty tank fills in 250 mB steps and the settle empties it");
                    Washing.Line line = Washing.line(4);
                    h.assertValueEqual(settled.dirtyMb(), (int) line.dirtyLeft(), "no dirty water left");
                    h.assertValueEqual(settled.waterMb(), 1000 - (int) line.cleanMb(), "1000 mB of clean water used");
                    h.assertValueEqual(
                            settled.biomass().stored().units(),
                            1000 - 4 * WASH_BU - SETTLE_BU,
                            "140 BU: four washes and a settle");
                    h.assertValueEqual((long) settled.outputs().get(0).getCount(), line.dust(), "four dust");
                    h.assertValueEqual(
                            (long) settled.outputs().get(1).getCount(), line.washes(), "four washed Tailings");
                    h.assertValueEqual((long) settled.outputs().get(2).getCount(), line.residue(), "one Rock Residue");
                    h.assertValueEqual(
                            settled.outputs().get(1).get(MineralModule.PARCEL.get()),
                            new Parcel(Metal.IRON, Washing.WASHED_UNITS),
                            "all of them three units");
                    h.assertValueEqual(
                            settled.ingredients().get(0).getCount(),
                            1,
                            "the fifth Tailings waits, but there is no water");
                    h.assertTrue(
                            ports.extractItem(0, 1, true).isEmpty(), "and the raw slot never gives it back to a pipe");
                    h.assertValueEqual(menu(h, at).blocked(), 0, "a short supply is not a full tray");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    BatchOrganContents waiting = contents(h, at);
                    h.assertTrue(waiting.batch().isEmpty(), "without 250 mB of water the fifth wash does not start");
                    h.assertValueEqual(waiting.biomass().stored().units(), 860, "and nothing is spent");
                    h.assertValueEqual(
                            fluid(h, at).fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE),
                            250,
                            "the supply returns");
                })
                .thenExecuteAfter(2, () -> {
                    BatchOrganContents again = contents(h, at);
                    h.assertTrue(again.batch().isPresent(), "the supply restored, washing resumes");
                    h.assertValueEqual(again.dirtyMb(), Washing.DIRTY_PER_WASH, "250 mB dirty once more");
                    h.assertValueEqual(again.waterMb(), 0, "250 mB clean spent");
                    LOGGER.info(
                            "KIDNEY LEDGER 4 washes + 1 settle: {} ticks ({} s) loaded; 1000 mB clean water in, 1000 mB "
                                    + "dirty water settled to 1 Rock Residue; 140 BU; 4 Iron Dust, 4 Tailings of 3 units, "
                                    + "1 Rock Residue for 4 Tailings of 4 units. Per wash: 160 ticks, 250 mB, 30 BU, 1 dust.",
                            marks[1] - marks[0],
                            (marks[1] - marks[0]) / 20);
                    h.assertTrue(
                            marks[1] - marks[0] >= 4L * WASH_TICKS + SETTLE_TICKS - 5,
                            "52 s: four 8 s washes and one 20 s settle");
                    h.assertTrue(
                            marks[1] - marks[0] <= 4L * WASH_TICKS + SETTLE_TICKS + 30,
                            "and no more than a few ticks over");
                })
                .thenSucceed();
    }

    // --- waste ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1000)
    public static void aFullDirtyTankAndAFullResidueTrayPauseTheKidneyAndRoomResumesIt(GameTestHelper h) {
        BlockPos at = kidney(h, 1, 1000, 1000);
        BatchOrganContents like = contents(h, at);
        preset(
                h,
                at,
                new BatchOrganContents(
                        List.of(tailings(Metal.GOLD, Grinding.TAILINGS_UNITS, 3)),
                        List.of(
                                new ItemStack(MineralModule.GOLD_DUST.get(), 5),
                                tailings(Metal.GOLD, Washing.WASHED_UNITS, 2),
                                new ItemStack(MineralModule.ROCK_RESIDUE.get(), 64)),
                        1000,
                        like.biomass(),
                        OrganHistory.NONE,
                        Optional.empty(),
                        Optional.empty(),
                        0f,
                        1000),
                List.of(tailings(Metal.GOLD, Grinding.TAILINGS_UNITS, 3)),
                List.of(
                        new ItemStack(MineralModule.GOLD_DUST.get(), 5),
                        tailings(Metal.GOLD, Washing.WASHED_UNITS, 2),
                        new ItemStack(MineralModule.ROCK_RESIDUE.get(), 64)),
                Optional.empty());
        h.runAfterDelay(20, () -> {
            BatchOrganContents held = contents(h, at);
            h.assertTrue(held.batch().isEmpty(), "a full dirty tank and a full residue tray: nothing can start");
            h.assertValueEqual(held.dirtyMb(), 1000, "the dirty water is kept");
            h.assertValueEqual(held.waterMb(), 1000, "no clean water spent");
            h.assertValueEqual(held.biomass().stored().units(), 1000, "no biomass spent");
            h.assertValueEqual(held.ingredients().get(0).getCount(), 3, "the Tailings stay");
            h.assertValueEqual(
                    menu(h, at).blocked(), DIRTY_FULL | 4, "the screen names the dirty tank and the residue tray");
            // freeing room in the residue tray lets the settle start
            ItemStack residue = ports(h, at).extractItem(RESIDUE, 64, false);
            h.assertValueEqual(residue.getCount(), 64, "the residue is taken out");
        });
        h.runAfterDelay(26, () -> {
            BatchOrganContents settling = contents(h, at);
            h.assertTrue(settling.batch().isPresent(), "room for the residue starts the settle");
            h.assertValueEqual(
                    settling.batch().orElseThrow().batch().recipe(),
                    "infestusfrontier:settling",
                    "a settle, not a wash");
            h.assertValueEqual(settling.dirtyMb(), 0, "1000 mB of dirty water leave the tank at the start");
            h.assertValueEqual(settling.biomass().stored().units(), 1000 - SETTLE_BU, "20 BU");
            h.assertValueEqual(settling.waterMb(), 1000, "a settle spends no clean water");
            h.assertValueEqual(settling.outputs().get(0).getCount(), 5, "the dust tray is untouched");
            h.assertValueEqual(settling.outputs().get(1).getCount(), 2, "so is the washed Tailings tray");
            h.assertValueEqual(settling.ingredients().get(0).getCount(), 3, "and the Tailings waiting");
            h.assertValueEqual(menu(h, at).blocked(), 0, "nothing waits for room now");
        });
        h.runAfterDelay(26 + SETTLE_TICKS, () -> {
            BatchOrganContents settled = contents(h, at);
            h.assertValueEqual(settled.outputs().get(2).getCount(), 1, "one Rock Residue for the thousand mB");
            h.assertValueEqual(settled.outputs().get(0).getCount(), 5, "no dust came out of the waste");
            h.assertValueEqual(settled.history().completedBatches(), 1, "one batch, counted once");
            h.assertTrue(settled.batch().isPresent(), "and the washing goes on with room in the tank");
            h.assertValueEqual(settled.dirtyMb(), Washing.DIRTY_PER_WASH, "250 mB, from the first wash");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void everyFullTrayPausesAWashWithNothingSpentAndNamesIt(GameTestHelper h) {
        ItemStack gold3 = tailings(Metal.GOLD, Washing.WASHED_UNITS, 1);
        List<List<ItemStack>> trays = List.of(
                List.of(new ItemStack(MineralModule.IRON_DUST.get(), 64), ItemStack.EMPTY, ItemStack.EMPTY),
                List.of(ItemStack.EMPTY, tailings(Metal.IRON, Washing.WASHED_UNITS, 64), ItemStack.EMPTY),
                List.of(ItemStack.EMPTY, gold3, ItemStack.EMPTY),
                List.of(new ItemStack(MineralModule.GOLD_DUST.get(), 1), ItemStack.EMPTY, ItemStack.EMPTY));
        int[] masks = {1, 2, 2, 1};
        for (int i = 0; i < trays.size(); i++) {
            BlockPos at = kidney(h, 1 + 2 * i, 1000, 1000);
            preset(
                    h,
                    at,
                    contents(h, at),
                    List.of(tailings(Metal.IRON, Grinding.TAILINGS_UNITS, 1)),
                    trays.get(i),
                    Optional.empty());
        }
        BlockPos residueFull = kidney(h, 9, 1000, 1000);
        preset(
                h,
                residueFull,
                contents(h, residueFull),
                List.of(tailings(Metal.IRON, Grinding.TAILINGS_UNITS, 1)),
                List.of(ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(MineralModule.ROCK_RESIDUE.get(), 64)),
                Optional.empty());
        h.runAfterDelay(20, () -> {
            for (int i = 0; i < trays.size(); i++) {
                BlockPos at = new BlockPos(1 + 2 * i, Y + 1, 1);
                BatchOrganContents held = contents(h, at);
                h.assertTrue(held.batch().isEmpty(), "tray case " + i + " pauses");
                h.assertValueEqual(held.ingredients().get(0).getCount(), 1, i + ": the Tailings stays");
                h.assertValueEqual(held.biomass().stored().units(), 1000, i + ": no biomass spent");
                h.assertValueEqual(held.waterMb(), 1000, i + ": no water spent");
                h.assertValueEqual(held.dirtyMb(), 0, i + ": and none made dirty");
                h.assertValueEqual(menu(h, at).blocked(), masks[i], i + ": the screen names the full tray");
            }
            h.assertTrue(
                    contents(h, residueFull).batch().isPresent(),
                    "a full residue tray does not stop a wash: a wash makes none");
            for (int i = 0; i < trays.size(); i++) {
                IItemHandler ports = ports(h, new BlockPos(1 + 2 * i, Y + 1, 1));
                for (int slot = DUST; slot <= RESIDUE; slot++) ports.extractItem(slot, 64, false);
            }
        });
        h.runAfterDelay(26, () -> {
            for (int i = 0; i < trays.size(); i++) {
                BlockPos at = new BlockPos(1 + 2 * i, Y + 1, 1);
                BatchOrganContents resumed = contents(h, at);
                h.assertTrue(resumed.batch().isPresent(), i + ": emptying the tray resumes the wash");
                h.assertValueEqual(resumed.biomass().stored().units(), 1000 - WASH_BU, i + ": 30 BU, now");
                h.assertValueEqual(resumed.waterMb(), 1000 - Washing.CLEAN_PER_WASH, i + ": 250 mB, now");
                h.assertValueEqual(menu(h, at).blocked(), 0, i + ": nothing waits");
            }
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void aFinishedWashWaitsForRoomAndDeliversItsDustAndParcelOnce(GameTestHelper h) {
        BlockPos at = kidney(h, 1, 1000, 1000);
        var nearlyDone = new OrganBatch(
                new org.jd.infestusfrontier.core.processing.Batch(
                        "infestusfrontier:washing/copper", WASH_TICKS, WASH_TICKS - 2),
                new ItemStack(MineralModule.COPPER_DUST.get(), 1),
                List.of(tailings(Metal.COPPER, Washing.WASHED_UNITS, 1)),
                Optional.empty(),
                0f);
        BatchOrganContents like = contents(h, at);
        preset(
                h,
                at,
                new BatchOrganContents(
                        List.of(ItemStack.EMPTY),
                        List.of(new ItemStack(MineralModule.COPPER_DUST.get(), 64), ItemStack.EMPTY, ItemStack.EMPTY),
                        750,
                        like.biomass(),
                        OrganHistory.NONE,
                        Optional.of(nearlyDone),
                        Optional.empty(),
                        0f,
                        250),
                List.of(ItemStack.EMPTY),
                List.of(new ItemStack(MineralModule.COPPER_DUST.get(), 64), ItemStack.EMPTY, ItemStack.EMPTY),
                Optional.of(nearlyDone));
        h.runAfterDelay(8, () -> {
            BatchOrganContents waiting = contents(h, at);
            h.assertTrue(waiting.batch().isPresent(), "the finished wash is kept, not dropped");
            h.assertTrue(waiting.outputs().get(1).isEmpty(), "no partial delivery of the parcel");
            h.assertValueEqual(waiting.dirtyMb(), 250, "the dirty water it made stays in the tank");
            h.assertValueEqual(menu(h, at).blocked(), 1, "the screen says the dust tray is full");
            ports(h, at).extractItem(DUST, 10, false);
        });
        h.runAfterDelay(14, () -> {
            BatchOrganContents delivered = contents(h, at);
            h.assertTrue(delivered.batch().isEmpty(), "delivered once room opened");
            h.assertValueEqual(delivered.outputs().get(0).getCount(), 55, "54 left in the tray plus one");
            h.assertValueEqual(delivered.outputs().get(1).getCount(), 1, "one washed Tailings");
            h.assertValueEqual(delivered.history().completedBatches(), 1, "counted exactly once");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void withoutCleanWaterOrBiomassNothingStartsAndEachSupplyResumesIt(GameTestHelper h) {
        BlockPos thirsty = kidney(h, 1, Washing.CLEAN_PER_WASH - 1, 1000);
        BlockPos hungry = kidney(h, 3, 1000, WASH_BU - 1);
        for (BlockPos at : List.of(thirsty, hungry))
            ports(h, at).insertItem(0, tailings(Metal.COPPER, Grinding.TAILINGS_UNITS, 1), false);
        h.runAfterDelay(10, () -> {
            h.assertTrue(contents(h, thirsty).batch().isEmpty(), "249 mB cannot pay for a wash");
            h.assertValueEqual(contents(h, thirsty).ingredients().get(0).getCount(), 1, "the Tailings stays");
            h.assertValueEqual(contents(h, thirsty).biomass().stored().units(), 1000, "and the biomass");
            h.assertTrue(contents(h, hungry).batch().isEmpty(), "29 BU cannot pay for a wash");
            h.assertValueEqual(contents(h, hungry).waterMb(), 1000, "the water stays");
            fluid(h, thirsty).fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
            h.getLevel()
                    .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(hungry), null)
                    .insert(Biomass.of(1), false);
        });
        h.runAfterDelay(14, () -> {
            h.assertTrue(contents(h, thirsty).batch().isPresent(), "the last mB of water starts it");
            h.assertValueEqual(contents(h, thirsty).waterMb(), 0, "all 250 mB spent");
            h.assertTrue(contents(h, hungry).batch().isPresent(), "the last BU starts it");
            h.assertValueEqual(contents(h, hungry).biomass().stored().units(), 0, "all 30 BU spent");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void dirtyWaterOnlyBecomesOneRockResidueAndNothingElseUsesOrMakesIt(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        int settles = 0;
        int dirtyMakers = 0;
        for (var holder : h.getLevel().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof OrganBatchRecipe recipe)) continue;
            if (recipe.dirtyIn() > 0) {
                settles++;
                h.assertValueEqual(recipe.organ(), "washing_kidney", holder.id() + " is the Kidney's");
                h.assertValueEqual(recipe.dirtyIn(), Washing.DIRTY_PER_SETTLE, holder.id() + " takes 1000 mB");
                h.assertValueEqual(recipe.dirtyOut(), 0, holder.id() + " makes no dirty water");
                h.assertTrue(recipe.ingredients().isEmpty(), holder.id() + " takes no item with it");
                h.assertValueEqual(recipe.results().size(), 1, holder.id() + " has one result");
                h.assertTrue(
                        recipe.result().is(MineralModule.ROCK_RESIDUE.get())
                                && recipe.result().getCount() == 1,
                        holder.id() + " makes one Rock Residue and nothing of value");
                h.assertValueEqual(recipe.waterMb(), 0, holder.id() + " refunds and spends no clean water");
                h.assertValueEqual(recipe.slot(), 2, holder.id() + " puts it in the residue tray");
            }
            if (recipe.dirtyOut() > 0) {
                dirtyMakers++;
                h.assertValueEqual(recipe.organ(), "washing_kidney", holder.id() + " is the Kidney's");
                h.assertValueEqual(recipe.dirtyOut(), Washing.DIRTY_PER_WASH, holder.id() + " makes 250 mB");
                h.assertValueEqual(recipe.waterMb(), Washing.CLEAN_PER_WASH, holder.id() + " spends 250 mB clean");
                h.assertValueEqual(recipe.biomass().units(), WASH_BU, holder.id() + " costs 30 BU");
                h.assertValueEqual(recipe.ticks(), WASH_TICKS, holder.id() + " takes 8 s");
                h.assertValueEqual(recipe.ingredients().size(), 1, holder.id() + " takes one Tailings");
                h.assertValueEqual(recipe.result().getCount(), 1, holder.id() + " gives one dust");
                var metal = Metal.of(holder.id().getPath().substring("washing/".length()))
                        .orElseThrow();
                h.assertTrue(recipe.result().is(MineralModule.dust(metal)), holder.id() + " gives its metal's dust");
                h.assertValueEqual(
                        recipe.byproducts().get(0).get(MineralModule.PARCEL.get()),
                        Washing.wash(Parcel.ground(metal)).orElseThrow().remaining(),
                        holder.id() + " leaves the parcel the core rule says");
                h.assertTrue(
                        recipe.ingredients().get(0).test(tailings(metal, Grinding.TAILINGS_UNITS, 1))
                                && Metal.values().length == 3,
                        holder.id() + " takes a fresh Tailings of its metal");
                for (Metal other : Metal.values())
                    for (int units = 1; units <= Grinding.TAILINGS_UNITS; units++)
                        h.assertValueEqual(
                                recipe.ingredients().get(0).test(tailings(other, units, 1)),
                                other == metal && units == Grinding.TAILINGS_UNITS,
                                holder.id() + " and " + other.id() + " Tailings of " + units + " units");
                h.assertFalse(
                        recipe.ingredients().get(0).test(new ItemStack(MineralModule.MINERAL_TAILINGS.get())),
                        holder.id() + " refuses a Tailings with no parcel");
            }
            for (ItemStack made : recipe.results())
                h.assertFalse(
                        made.is(Items.WATER_BUCKET) || made.is(Items.POTION) || made.is(Items.GLASS_BOTTLE),
                        holder.id() + " makes no water item");
        }
        h.assertValueEqual(settles, 1, "one recipe takes dirty water");
        h.assertValueEqual(dirtyMakers, 3, "three washes make it");
        // no handler of any Kidney offers dirty water; the registry knows no dirty water fluid
        BlockPos at = kidney(h, 1, 1000, 1000);
        BlockEntity kidney = h.getBlockEntity(at);
        h.assertTrue(
                kidney.saveWithoutMetadata(registries).getCompound("contents").getInt("dirty") == 0,
                "a fresh Kidney holds none");
        for (Direction side : Direction.values()) {
            IFluidHandler handler =
                    h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), side);
            h.assertTrue(handler != null, "a fluid handler on " + side);
            for (int tank = 0; tank < handler.getTanks(); tank++)
                h.assertTrue(
                        handler.getFluidInTank(tank).isEmpty()
                                || handler.getFluidInTank(tank).getFluid() == Fluids.WATER
                                || BiomassFluids.isBiomass(handler.getFluidInTank(tank)),
                        "only clean water and biomass show on " + side);
        }
        h.succeed();
    }

    // --- crafting and the guide's data ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theKidneyIsCraftedFromABudTwoSheetsSandAndABottle(GameTestHelper h) {
        Item bud = OrganModule.ORGAN_BUD_ITEM.get();
        Item sheet = ProcessingModule.MEMBRANE_SHEET.get();
        MineralWorkshopTests.craft(
                h,
                List.of(
                        new ItemStack(bud),
                        new ItemStack(sheet),
                        new ItemStack(sheet),
                        new ItemStack(Items.SAND),
                        new ItemStack(Items.GLASS_BOTTLE)),
                Optional.of(new ItemStack(ProcessingModule.WASHING_KIDNEY_ITEM.get())));
        MineralWorkshopTests.craft(
                h,
                List.of(
                        new ItemStack(bud),
                        new ItemStack(sheet),
                        new ItemStack(Items.SAND),
                        new ItemStack(Items.GLASS_BOTTLE)),
                Optional.empty());
        // no electricity, no genome and no Nether material in it
        h.succeed();
    }

    // --- saves, breaking and the world ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 520)
    public static void aSaveInTheMiddleOfAWashAndOfASettleGivesTheProductsOnce(GameTestHelper h) {
        BlockPos washing = kidney(h, 1, 1000, 1000);
        ports(h, washing).insertItem(0, tailings(Metal.GOLD, Grinding.TAILINGS_UNITS, 2), false);
        BlockPos settling = kidney(h, 3, 0, 1000);
        BatchOrganContents like = contents(h, settling);
        preset(
                h,
                settling,
                new BatchOrganContents(
                        List.of(ItemStack.EMPTY),
                        List.of(new ItemStack(MineralModule.IRON_DUST.get(), 3), ItemStack.EMPTY, ItemStack.EMPTY),
                        0,
                        like.biomass(),
                        OrganHistory.NONE,
                        Optional.empty(),
                        Optional.empty(),
                        0f,
                        1000),
                List.of(ItemStack.EMPTY),
                List.of(new ItemStack(MineralModule.IRON_DUST.get(), 3), ItemStack.EMPTY, ItemStack.EMPTY),
                Optional.empty());
        h.runAfterDelay(80, () -> {
            for (BlockPos at : List.of(washing, settling)) {
                BatchOrganContents mid = contents(h, at);
                h.assertTrue(mid.batch().isPresent(), "a batch runs");
                reload(h, at); // through real NBT bytes, as a chunk writes and reads it
                h.assertValueEqual(contents(h, at), mid, "saved and read back, nothing changed");
            }
            h.assertValueEqual(contents(h, washing).dirtyMb(), 250, "the wash's dirty water is part of the save");
            h.assertValueEqual(contents(h, settling).dirtyMb(), 0, "the settle's thousand mB already left the tank");
        });
        h.runAtTickTime(SETTLE_TICKS + 8, () -> {
            BatchOrganContents settled = contents(h, settling);
            h.assertValueEqual(settled.outputs().get(2).getCount(), 1, "one Rock Residue, not two");
            h.assertValueEqual(settled.outputs().get(0).getCount(), 3, "the dust beside it untouched");
            h.assertValueEqual(settled.history().completedBatches(), 1, "one settle counted");
            BatchOrganContents washed = contents(h, washing);
            h.assertValueEqual(washed.history().completedBatches(), 2, "two washes done after the reload");
            h.assertValueEqual(washed.outputs().get(0).getCount(), 2, "two gold dust, not four");
            h.assertValueEqual(washed.outputs().get(1).getCount(), 2, "two washed Tailings");
            h.assertValueEqual(washed.dirtyMb(), 2 * Washing.DIRTY_PER_WASH, "500 mB dirty, once each");
            reload(h, washing);
            h.assertValueEqual(contents(h, washing), washed, "a second reload keeps every tray and the tank");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedDirtyTankBeyondTheKindIsUnreadableAndAnOldSaveHasNone(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        BlockPos kidney = kidney(h, 1, 1000, 1000);
        BlockEntity organ = h.getBlockEntity(kidney);
        CompoundTag tag = organ.saveWithoutMetadata(registries);
        tag.getCompound("contents").putInt("dirty", 1001);
        organ.loadWithComponents(tag, registries);
        h.assertTrue(
                h.getBlockEntity(kidney).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "1001 mB cannot be a Kidney's dirty water");
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, MineralModule.MINERAL_TAILINGS.get(), 1);
        player.getMainHandItem().set(MineralModule.PARCEL.get(), Parcel.ground(Metal.IRON));
        EcologyTests.use(h, player, kidney, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "an unreadable Kidney takes nothing");

        BlockPos bowl = new BlockPos(3, Y + 1, 1);
        h.setBlock(bowl.below(), EcologyTests.mature());
        h.setBlock(bowl, ProcessingModule.CULTURE_BOWL.get());
        BlockEntity culture = h.getBlockEntity(bowl);
        CompoundTag saved = culture.saveWithoutMetadata(registries);
        saved.getCompound("contents").putInt("dirty", 1);
        culture.loadWithComponents(saved, registries);
        h.assertTrue(
                h.getBlockEntity(bowl).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "a Bowl has no dirty tank, so one mB of it is unreadable");

        BlockPos old = kidney(h, 5, 1000, 1000);
        BlockEntity before = h.getBlockEntity(old);
        CompoundTag legacy = before.saveWithoutMetadata(registries);
        legacy.getCompound("contents").remove("dirty");
        before.loadWithComponents(legacy, registries);
        h.assertTrue(
                h.getBlockEntity(old).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) == null,
                "a save with no dirty field is not unreadable");
        h.assertValueEqual(contents(h, old).dirtyMb(), 0, "and holds none");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void breakingAKidneyMidWashCarriesItsDirtyWaterAndPlacingItResumes(GameTestHelper h) {
        BlockPos at = kidney(h, 1, 1000, 1000);
        ports(h, at).insertItem(0, tailings(Metal.COPPER, Grinding.TAILINGS_UNITS, 3), false);
        h.runAfterDelay(60, () -> {
            BatchOrganContents before = contents(h, at);
            h.assertTrue(before.batch().isPresent(), "a wash runs");
            h.assertValueEqual(before.dirtyMb(), 250, "with its dirty water in the tank");
            h.getLevel().destroyBlock(h.absolutePos(at), true);
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, at, 3.0);
            h.assertValueEqual(drops.size(), 1, "one drop, the organ itself");
            ItemStack dropped = drops.get(0).getItem();
            h.assertTrue(dropped.is(ProcessingModule.WASHING_KIDNEY_ITEM.get()), "it is the Kidney");
            BatchOrganContents carried = dropped.get(ProcessingModule.BATCH_CONTENTS.get());
            h.assertTrue(carried != null, "carrying its contents");
            h.assertValueEqual(carried, before, "the batch, the Tailings, the water and the dirty water as they were");
            drops.get(0).discard();
            Player placer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            placer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dropped.copy());
            h.placeAt(placer, placer.getMainHandItem(), at.below(), Direction.UP);
            h.assertBlockPresent(ProcessingModule.WASHING_KIDNEY.get(), at);
            h.assertValueEqual(contents(h, at), before, "placed back with everything it carried");
        });
        h.runAtTickTime(WASH_TICKS + 12, () -> {
            BatchOrganContents done = contents(h, at);
            h.assertValueEqual(done.history().completedBatches(), 1, "one wash, finished once");
            h.assertValueEqual(done.outputs().get(0).getCount(), 1, "one dust");
            h.assertTrue(h.getEntities(EntityType.ITEM, at, 3.0).isEmpty(), "nothing was left lying on the ground");
            h.succeed();
        });
    }

    /**
     * The demo's line with no hand in it: a chest of iron Tailings over a hopper over the Kidney; two sources of water
     * drunk by two Junctions that push into it; a Reservoir with exactly the biomass the run costs behind a Junction;
     * a Yield Lobe on an output Mouth, an Item Vein and an intake Mouth under a chest taking everything the Kidney
     * gives. Eight Tailings are washed: eight washes and two settles, and the Kidney is saved and read back in the
     * middle. The water, the biomass, the dust, the residue and the duration are measured.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3400)
    public static void aChestOfTailingsBecomesDustAndResidueThroughTheWholeLineWithNoHand(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int tailings = 8;
        int biomassNeeded = tailings * WASH_BU + 2 * SETTLE_BU;
        BlockPos kidney = new BlockPos(3, Y + 1, 4);
        h.setBlock(kidney.below(), EcologyTests.mature());
        h.setBlock(kidney, ProcessingModule.WASHING_KIDNEY.get());
        // tailings: chest, hopper, Kidney
        h.setBlock(kidney.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(kidney.above(2), Blocks.CHEST);
        ChestBlockEntity store = (ChestBlockEntity) h.getBlockEntity(kidney.above(2));
        store.setItem(0, tailings(Metal.IRON, Grinding.TAILINGS_UNITS, tailings));
        // water: one source each side, a Junction drinking it and pushing it into the Kidney
        BlockPos westSource = kidney.west(2);
        BlockPos westPump = kidney.west();
        h.setBlock(westSource.below(), Blocks.STONE);
        h.setBlock(westSource, Blocks.WATER);
        h.setBlock(westPump.below(), EcologyTests.mature());
        h.setBlock(westPump, junction(Direction.WEST, Direction.EAST));
        BlockPos northSource = kidney.north(2);
        BlockPos northPump = kidney.north();
        h.setBlock(northSource.below(), Blocks.STONE);
        h.setBlock(northSource, Blocks.WATER);
        h.setBlock(northPump.below(), EcologyTests.mature());
        h.setBlock(northPump, junction(Direction.NORTH, Direction.SOUTH));
        for (BlockPos wall : List.of(
                westSource.north(),
                westSource.south(),
                westSource.west(),
                northSource.west(),
                northSource.east(),
                northSource.north())) h.setBlock(wall, Blocks.STONE);
        // biomass: a Reservoir holding exactly the run's cost, a Junction between it and the Kidney
        BlockPos pump = kidney.east();
        BlockPos reservoir = kidney.east(2);
        h.setBlock(pump.below(), EcologyTests.mature());
        h.setBlock(reservoir.below(), EcologyTests.mature());
        h.setBlock(pump, junction(Direction.EAST, Direction.WEST));
        h.setBlock(reservoir, StorageModule.RESERVOIR_CELL.get());
        h.assertValueEqual(
                level.getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(reservoir), Direction.UP)
                        .fill(BiomassFluids.of(Biomass.of(biomassNeeded)), IFluidHandler.FluidAction.EXECUTE),
                biomassNeeded,
                "the Reservoir takes the run's biomass");
        // the yield: a lobe south of the Kidney on an output Mouth, veins east, an intake Mouth under a sink chest
        BlockPos lobe = kidney.south();
        mouth(h, lobe.below(), MouthMode.OUTPUT);
        MineralWorkshopTests.lobe(h, lobe, Direction.NORTH);
        for (int x = 1; x <= 4; x++) h.setBlock(lobe.below().east(x), LogisticsModule.ITEM_VEIN.get());
        BlockPos sink = lobe.east(5);
        mouth(h, sink.below(), MouthMode.INTAKE);
        h.setBlock(sink, Blocks.CHEST);
        ChestBlockEntity out = (ChestBlockEntity) h.getBlockEntity(sink);
        long[] marks = new long[2];
        int[] washStarts = {0};
        int[] settleStarts = {0};
        int[] mostDirty = {0};
        BatchOrganContents[] last = new BatchOrganContents[1];
        h.startSequence()
                .thenExecute(() -> marks[0] = level.getGameTime())
                .thenWaitUntil(() -> {
                    BatchOrganContents now = contents(h, kidney);
                    mostDirty[0] = Math.max(mostDirty[0], now.dirtyMb());
                    if (now.batch().isPresent()
                            && (last[0] == null
                                    || last[0].batch().isEmpty()
                                    || last[0].batch().get().batch().ticksDone()
                                            > now.batch().get().batch().ticksDone())) {
                        if (now.batch().get().batch().recipe().endsWith("settling")) settleStarts[0]++;
                        else washStarts[0]++;
                    }
                    last[0] = now;
                    h.assertTrue(now.history().completedBatches() >= 5, "four washes and a settle are done");
                })
                .thenExecute(() -> {
                    BatchOrganContents mid = contents(h, kidney);
                    reload(h, kidney); // a chunk saved and read back in the middle of the line
                    h.assertValueEqual(contents(h, kidney), mid, "the line's Kidney is the same after a reload");
                    last[0] = contents(h, kidney);
                })
                .thenWaitUntil(() -> {
                    BatchOrganContents now = contents(h, kidney);
                    mostDirty[0] = Math.max(mostDirty[0], now.dirtyMb());
                    if (now.batch().isPresent()
                            && (last[0] == null
                                    || last[0].batch().isEmpty()
                                    || last[0].batch().get().batch().ticksDone()
                                            > now.batch().get().batch().ticksDone())) {
                        if (now.batch().get().batch().recipe().endsWith("settling")) settleStarts[0]++;
                        else washStarts[0]++;
                    }
                    last[0] = now;
                    h.assertTrue(
                            now.history().completedBatches() == tailings + 2
                                    && now.batch().isEmpty(),
                            "eight washes and two settles are done");
                })
                .thenExecute(() -> marks[1] = level.getGameTime())
                .thenWaitUntil(() -> {
                    h.assertTrue(
                            ((HopperBlockEntity) h.getBlockEntity(kidney.above())).isEmpty() && store.isEmpty(),
                            "the chest and the hopper are empty");
                    BatchOrganContents now = contents(h, kidney);
                    h.assertTrue(
                            now.outputs().stream().allMatch(ItemStack::isEmpty),
                            "the Yield Lobe and the Mouths carried every product out");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        out.countItem(MineralModule.IRON_DUST.get()), tailings, "one dust per Tailings in the sink"))
                .thenExecute(() -> {
                    Washing.Line line = Washing.line(tailings);
                    BatchOrganContents ended = contents(h, kidney);
                    h.assertValueEqual(washStarts[0] + settleStarts[0], tailings + 2, "ten batches, each started once");
                    h.assertValueEqual(settleStarts[0], (int) line.settles(), "two settles");
                    h.assertValueEqual(mostDirty[0], 1000, "the dirty tank never exceeded its 1000 mB");
                    h.assertValueEqual(ended.dirtyMb(), (int) line.dirtyLeft(), "all the dirty water settled");
                    h.assertValueEqual(ended.waterMb(), 0, "all 2000 mB of clean water went through the Kidney");
                    h.assertBlockPresent(Blocks.AIR, westSource);
                    h.assertBlockPresent(Blocks.AIR, northSource);
                    h.assertValueEqual(
                            ended.biomass().stored().units()
                                    + stored(level, h.absolutePos(reservoir))
                                    + stored(level, h.absolutePos(pump))
                                    + stored(level, h.absolutePos(westPump))
                                    + stored(level, h.absolutePos(northPump)),
                            0,
                            "every BU of the Reservoir's " + biomassNeeded + " was spent: 8 x 30 and 2 x 20");
                    h.assertValueEqual(
                            (long) out.countItem(MineralModule.IRON_DUST.get()), line.dust(), "eight dust in the sink");
                    int washed = 0;
                    int residue = 0;
                    for (int slot = 0; slot < out.getContainerSize(); slot++) {
                        ItemStack stack = out.getItem(slot);
                        if (stack.is(MineralModule.MINERAL_TAILINGS.get())) {
                            h.assertValueEqual(
                                    stack.get(MineralModule.PARCEL.get()),
                                    new Parcel(Metal.IRON, Washing.WASHED_UNITS),
                                    "every collected Tailings is washed");
                            washed += stack.getCount();
                        }
                        if (stack.is(MineralModule.ROCK_RESIDUE.get())) residue += stack.getCount();
                    }
                    h.assertValueEqual((long) washed, line.washes(), "eight washed Tailings in the sink");
                    h.assertValueEqual((long) residue, line.residue(), "two Rock Residue in the sink");
                    h.assertValueEqual(
                            out.countItem(MineralModule.IRON_DUST.get()) + washed * Washing.WASHED_UNITS,
                            tailings * (Washing.DUST_PER_WASH + Washing.WASHED_UNITS),
                            "every unit of the Tailings' metal is dust or a residual unit");
                    long ticks = marks[1] - marks[0];
                    LOGGER.info(
                            "KIDNEY LINE LEDGER, {} iron Tailings, no hand: {} ticks ({} s) loaded for {} washes and {} "
                                    + "settles; {} mB clean water drawn, {} BU, {} dust, {} washed Tailings of 3 units, "
                                    + "{} Rock Residue; dirty tank peak {} mB, left {} mB.",
                            tailings,
                            ticks,
                            ticks / 20,
                            washStarts[0],
                            settleStarts[0],
                            line.cleanMb(),
                            biomassNeeded,
                            out.countItem(MineralModule.IRON_DUST.get()),
                            washed,
                            residue,
                            mostDirty[0],
                            ended.dirtyMb());
                })
                .thenSucceed();
    }

    /** A wash in a chunk that unloads and loads again keeps its one batch, its dirty water and its products. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "kidney_unload",
            timeoutTicks = 1200)
    public static void aWashInAChunkThatUnloadsIsKeptAndFinishesOnce(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos at = new BlockPos((origin.getX() & ~15) + 240002, 80, (origin.getZ() & ~15) + 240008);
        ChunkPos chunk = new ChunkPos(at);
        TestChunks.force(level, chunk);
        level.setBlockAndUpdate(at.below(), EcologyTests.mature());
        level.setBlockAndUpdate(at, ProcessingModule.WASHING_KIDNEY.get().defaultBlockState());
        level.getCapability(Capabilities.ItemHandler.BLOCK, at, Direction.UP)
                .insertItem(0, tailings(Metal.COPPER, Grinding.TAILINGS_UNITS, 2), false);
        level.getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.UP)
                .fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        level.getCapability(BiomassCapabilities.BLOCK, at, null).insert(Biomass.of(1000), false);
        BatchOrganContents[] before = new BatchOrganContents[1];
        h.startSequence()
                .thenWaitUntil(() -> {
                    BatchOrganContents now = read(level, at);
                    h.assertTrue(
                            now.batch().isPresent() && now.batch().get().batch().ticksDone() >= 60,
                            "a wash is running");
                })
                .thenExecute(() -> {
                    before[0] = read(level, at);
                    h.assertValueEqual(before[0].dirtyMb(), 250, "with its dirty water");
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(at), "the chunk unloaded"))
                .thenExecute(() -> TestChunks.force(level, chunk))
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(at), "the chunk is back"))
                .thenExecute(() -> {
                    BatchOrganContents back = read(level, at);
                    h.assertValueEqual(back.dirtyMb(), 250, "the dirty water came back from the disk");
                    h.assertValueEqual(back.ingredients().get(0).getCount(), 1, "the second Tailings waits");
                    h.assertTrue(back.batch().isPresent(), "the batch is the same one");
                    h.assertTrue(
                            back.batch().get().batch().ticksDone()
                                            >= before[0].batch().get().batch().ticksDone()
                                    && back.batch()
                                            .get()
                                            .batch()
                                            .recipe()
                                            .equals(before[0]
                                                    .batch()
                                                    .get()
                                                    .batch()
                                                    .recipe()),
                            "unloaded time was not worked, and none was lost");
                    h.assertValueEqual(back.waterMb(), 750, "the water spent once");
                    h.assertValueEqual(back.biomass().stored().units(), 1000 - WASH_BU, "and the biomass");
                })
                .thenWaitUntil(() -> h.assertTrue(
                        read(level, at).history().completedBatches() >= 1, "the wash finished after the reload"))
                .thenExecute(() -> {
                    BatchOrganContents done = read(level, at);
                    h.assertValueEqual(done.outputs().get(0).getCount(), 1, "one dust, not two");
                    h.assertValueEqual(done.outputs().get(1).getCount(), 1, "one washed Tailings");
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenSucceed();
    }

    // --- fixtures ---

    /** A Kidney at (x, 2, 1) on substrate with {@code water} mB and {@code biomass} BU. */
    private static BlockPos kidney(GameTestHelper h, int x, int water, int biomass) {
        BlockPos at = new BlockPos(x, Y + 1, 1);
        h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at, ProcessingModule.WASHING_KIDNEY.get());
        if (water > 0) fluid(h, at).fill(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
        if (biomass > 0)
            h.getLevel()
                    .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(at), null)
                    .insert(Biomass.of(biomass), false);
        return at;
    }

    private static ItemStack tailings(Metal metal, int units, int count) {
        return MineralWorkshopTests.withCount(MineralModule.tailings(new Parcel(metal, units)), count);
    }

    private static Item raw(Metal metal) {
        return switch (metal) {
            case IRON -> Items.RAW_IRON;
            case GOLD -> Items.RAW_GOLD;
            case COPPER -> Items.RAW_COPPER;
        };
    }

    private static IFluidHandler fluid(GameTestHelper h, BlockPos at) {
        IFluidHandler handler =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), Direction.UP);
        if (handler == null) h.fail("no fluid handler", at);
        return handler;
    }

    /** The biomass a block holds in its own tanks, in BU; 0 for a block with none. */
    private static int stored(ServerLevel level, BlockPos absolute) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, absolute, null);
        if (handler == null) handler = level.getCapability(Capabilities.FluidHandler.BLOCK, absolute, Direction.UP);
        if (handler == null) return 0;
        int held = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++)
            if (BiomassFluids.isBiomass(handler.getFluidInTank(tank)))
                held += handler.getFluidInTank(tank).getAmount();
        return held;
    }

    private static BlockState junction(Direction intake, Direction outlet) {
        return LogisticsModule.VASCULAR_JUNCTION
                .get()
                .defaultBlockState()
                .setValue(VascularJunctionBlock.PORT.get(intake), Port.INTAKE)
                .setValue(VascularJunctionBlock.PORT.get(outlet), Port.OUTLET);
    }

    /** The contents of a Kidney at an absolute position. */
    private static BatchOrganContents read(ServerLevel level, BlockPos absolute) {
        return level.getBlockEntity(absolute).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
    }

    private static void preset(
            GameTestHelper h,
            BlockPos at,
            BatchOrganContents like,
            List<ItemStack> ingredients,
            List<ItemStack> outputs,
            Optional<OrganBatch> batch) {
        MineralWorkshopTests.preset(h, at, like, ingredients, outputs, batch);
    }

    private static IItemHandler ports(GameTestHelper h, BlockPos at) {
        return MineralWorkshopTests.ports(h, at);
    }

    private static BatchOrganContents contents(GameTestHelper h, BlockPos at) {
        return MineralWorkshopTests.contents(h, at);
    }

    private static org.jd.infestusfrontier.processing.BatchOrganMenu menu(GameTestHelper h, BlockPos at) {
        return MineralWorkshopTests.menu(h, at);
    }

    private static void reload(GameTestHelper h, BlockPos at) {
        MineralWorkshopTests.reload(h, at);
    }

    private static void mouth(GameTestHelper h, BlockPos at, MouthMode mode) {
        MineralWorkshopTests.mouth(h, at, mode);
    }
}
