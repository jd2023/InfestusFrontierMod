package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import com.klikli_dev.modonomicon.book.page.BookCraftingRecipePage;
import com.klikli_dev.modonomicon.data.BookDataManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Collecting;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.logistics.api.ItemAccess;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.mining.WorkBedMenu;
import org.jd.infestusfrontier.mining.WorkOrganMenu;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * The Fracture Jaw, the Work Bed and the Collection Cilia: a stationary jaw cuts the block in front of it once, at
 * stone grade, into the bed behind it, and the cilia send the bed through a Mouth and Item Veins to a chest. What
 * stops each of them, and that nothing drops loose, is lost or doubles while they wait; their state through a save to
 * disk, breaking and an unloaded neighbour; the shared cut limit; recipes, guide and demo. Each rig stands in a yard
 * of its own in a chunk of its own, far from the test structures.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class FractureJawTests {
    private static final int TOP = 100;
    /** Ticks a due cut or a sweep may take: a try, a stop and a look again after the retry, with a margin. */
    private static final int WAIT = 4 * Fracturing.RETRY_TICKS + 2 * Collecting.SWEEP_TICKS;
    /** Ticks that let an Item Vein network discover itself. */
    private static final int SETTLE = 20;

    /**
     * A rig's yard around {@code start}, the jaw's place, in the middle of a chunk the yard forces for itself. The jaw
     * faces north with the block to cut at z - 1 and its bed at z + 1; the cilia stand east of the bed on an Output
     * Mouth, facing it; two Item Veins run east to an Intake Mouth under a chest. A Vascular Junction east of the jaw,
     * fed by a Biomass Bladder east of it, can supply the jaw and the cilia.
     */
    private record Site(ServerLevel level, BlockPos start, ChunkPos chunk) {
        private static final int REACH = 6;

        static Site of(GameTestHelper h, int slot) {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            BlockPos start =
                    new BlockPos((origin.getX() & ~15) + 224008, TOP, (origin.getZ() & ~15) + 270408 + slot * 64);
            Site site = new Site(h.getLevel(), start, new ChunkPos(start));
            TestChunks.force(site.level, site.chunk);
            site.clear();
            return site;
        }

        BlockPos at(int dx, int dy, int dz) {
            return start.offset(dx, dy, dz);
        }

        BlockState state(int dx, int dy, int dz) {
            return level.getBlockState(at(dx, dy, dz));
        }

        void set(int dx, int dy, int dz, BlockState state) {
            level.setBlockAndUpdate(at(dx, dy, dz), state);
        }

        void set(int dx, int dy, int dz, Block block) {
            set(dx, dy, dz, block.defaultBlockState());
        }

        void clear() {
            for (BlockPos cell : BlockPos.betweenClosed(at(-REACH, -3, -REACH), at(REACH, 4, REACH))) {
                level.removeBlockEntity(cell);
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, yard())) item.discard();
        }

        void release() {
            clear();
            level.setChunkForced(chunk.x, chunk.z, false);
        }

        AABB yard() {
            return AABB.encapsulatingFullBlocks(at(-REACH, -3, -REACH), at(REACH, 4, REACH));
        }

        int loose() {
            return level.getEntitiesOfClass(ItemEntity.class, yard()).size();
        }

        static final BlockPos JAW = BlockPos.ZERO;
        static final BlockPos FRONT = new BlockPos(0, 0, -1);
        static final BlockPos BED = new BlockPos(0, 0, 1);
        static final BlockPos CILIA = new BlockPos(1, 0, 1);
        static final BlockPos MOUTH = new BlockPos(1, -1, 1);
        static final BlockPos INTAKE = new BlockPos(4, -1, 1);
        static final BlockPos CHEST = new BlockPos(4, 0, 1);
        static final BlockPos JUNCTION = new BlockPos(1, 0, 0);
        static final BlockPos BLADDER = new BlockPos(2, 0, 0);

        BlockPos at(BlockPos offset) {
            return start.offset(offset);
        }

        /** The jaw, its bed and stone around the block to cut, all on stone with substrate under the organs. */
        void jaw() {
            for (BlockPos cell : BlockPos.betweenClosed(at(-2, -2, -3), at(5, -2, 3)))
                level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (BlockPos cell : BlockPos.betweenClosed(at(-1, -1, -2), at(1, 0, -2)))
                level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            set(-1, 0, -1, Blocks.STONE);
            set(1, 0, -1, Blocks.STONE);
            set(0, -1, -1, Blocks.STONE);
            set(0, 1, -1, Blocks.STONE);
            set(0, -1, 0, EcologyTests.mature());
            set(0, -1, 1, EcologyTests.mature());
            set(
                    0,
                    0,
                    0,
                    MiningModule.FRACTURE_JAW
                            .get()
                            .defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
            set(0, 0, 1, MiningModule.WORK_BED.get());
        }

        /** The cilia on their Mouth, set to {@code mode}, the veins and the chest's Intake Mouth. */
        void cilia(MouthMode mode) {
            mouth(MOUTH, mode);
            set(
                    1,
                    0,
                    1,
                    MiningModule.COLLECTION_CILIA
                            .get()
                            .defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
            set(2, -1, 1, LogisticsModule.ITEM_VEIN.get());
            set(3, -1, 1, LogisticsModule.ITEM_VEIN.get());
            mouth(INTAKE, MouthMode.INTAKE);
            set(4, 0, 1, Blocks.CHEST);
        }

        void mouth(BlockPos offset, MouthMode mode) {
            level.setBlockAndUpdate(
                    at(offset), LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode));
        }

        /** The junction east of the jaw, taking from the bladder east of it and giving to the jaw and the cilia. */
        void junction() {
            set(1, -1, 0, EcologyTests.mature());
            set(2, -1, 0, EcologyTests.mature());
            set(
                    1,
                    0,
                    0,
                    LogisticsModule.VASCULAR_JUNCTION
                            .get()
                            .defaultBlockState()
                            .setValue(VascularJunctionBlock.PORT.get(Direction.EAST), Port.INTAKE)
                            .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.OUTLET)
                            .setValue(VascularJunctionBlock.PORT.get(Direction.SOUTH), Port.OUTLET));
            set(2, 0, 0, StorageModule.BIOMASS_BLADDER.get());
        }

        void steward(Player player) {
            Organs.placedBy(level, at(JAW), player);
            Organs.placedBy(level, at(CILIA), player);
        }

        BlockEntity entity(BlockPos offset) {
            return level.getBlockEntity(at(offset));
        }

        CompoundTag saved(BlockPos offset) {
            return entity(offset).saveWithFullMetadata(level.registryAccess());
        }

        /** Loads the organ's own saved state changed by {@code change}, as a load from disk does. */
        void load(BlockPos offset, Consumer<CompoundTag> change) {
            CompoundTag tag = saved(offset);
            change.accept(tag);
            entity(offset).loadWithComponents(tag, level.registryAccess());
        }

        void feed(BlockPos offset, int units) {
            var handler = level.getCapability(BiomassCapabilities.BLOCK, at(offset), null);
            if (handler == null
                    || !(handler.insert(Biomass.of(units), false)
                            instanceof org.jd.infestusfrontier.core.foundation.Outcome.Done<?>))
                throw new IllegalStateException("could not feed " + offset);
        }

        /** The jaw's wait is over. */
        void due() {
            load(JAW, tag -> tag.putInt("ticks", Fracturing.CUT_TICKS));
        }

        /** The bed holds {@code stacks} from its first slot. */
        void bed(ItemStack... stacks) {
            load(BED, tag -> {
                ListTag slots = new ListTag();
                for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++)
                    slots.add(slot < stacks.length ? stacks[slot].save(level.registryAccess()) : new CompoundTag());
                tag.put("slots", slots);
            });
        }

        WorkOrganMenu menu(BlockPos offset, Player player) {
            return (WorkOrganMenu) ((MenuProvider) entity(offset)).createMenu(0, player.getInventory(), player);
        }

        Optional<String> stop(BlockPos offset, Player player) {
            return menu(offset, player).stopKey();
        }

        ChestBlockEntity chest() {
            return (ChestBlockEntity) entity(CHEST);
        }

        /** How many {@code item} the bed and the chest hold together. */
        int held(Item item) {
            return slots(saved(BED)).stream()
                            .filter(line -> line.startsWith(itemId(item) + " "))
                            .mapToInt(line -> Integer.parseInt(line.substring(line.indexOf(' ') + 1)))
                            .sum()
                    + chest().countItem(item);
        }
    }

    private static String itemId(Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static String key(Refusal stop) {
        return "infestusfrontier.refusal." + stop.key();
    }

    private static int stored(CompoundTag tag) {
        return tag.getCompound("tank").getInt("stored");
    }

    /** The bed's stacks as {@code item count} lines, empty slots left out. */
    private static List<String> slots(CompoundTag tag) {
        return tag.getList("slots", Tag.TAG_COMPOUND).stream()
                .map(slot -> (CompoundTag) slot)
                .filter(slot -> slot.contains("id"))
                .map(slot -> slot.getString("id") + " " + slot.getInt("count"))
                .toList();
    }

    // ---- the cut ----

    /**
     * A supplied jaw cuts the coal ore in front of it once into its bed: the ore's ordinary stone-grade drop, one coal.
     * It stays where it stands, facing the same way, cuts nothing else and pays for one cut; nothing falls loose. With
     * the space in front empty it names that and does not go looking for more. Its steward is credited.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 2 * WAIT)
    public static void aJawCutsTheBlockInFrontOnceIntoItsBedAndStaysWhereItIs(GameTestHelper h) {
        Site site = Site.of(h, 0);
        site.jaw();
        site.set(0, 0, -1, Blocks.COAL_ORE);
        ServerPlayer steward = player(h);
        site.steward(steward);
        site.feed(Site.JAW, 1000);
        site.due();
        PendingCredits credits = PendingCredits.get(h.getLevel());
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(site.state(0, 0, -1).isAir(), "the ore is cut"))
                .thenExecute(() -> {
                    h.assertValueEqual(slots(site.saved(Site.BED)), List.of("minecraft:coal 1"), "one coal, once");
                    h.assertValueEqual(site.saved(Site.BED).getInt("received"), 1, "the bed's history");
                    CompoundTag jaw = site.saved(Site.JAW);
                    h.assertValueEqual(jaw.getInt("cuts"), 1, "one cut in the jaw's history");
                    h.assertValueEqual(stored(jaw), 1000 - Fracturing.CUT_BIOMASS_BU, "one cut paid");
                    BlockState stands = site.state(0, 0, 0);
                    h.assertTrue(stands.is(MiningModule.FRACTURE_JAW.get()), "the jaw has not moved");
                    h.assertValueEqual(
                            stands.getValue(HorizontalDirectionalBlock.FACING), Direction.NORTH, "nor turned");
                    for (BlockPos stays : List.of(site.at(0, 0, -2), site.at(-1, 0, -1), site.at(1, 0, -1)))
                        h.assertTrue(site.level.getBlockState(stays).is(Blocks.STONE), "nothing else is cut");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    h.assertTrue(
                            credits.holdsAutomatic(steward.getUUID(), id("fracture_jaw")), "the steward is credited");
                    site.due();
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(Site.JAW, steward), Optional.of(key(Fracturing.NOTHING)), "nothing left in front"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.saved(Site.JAW).getInt("cuts"), 1, "no other cut");
                    h.assertValueEqual(stored(site.saved(Site.JAW)), 1000 - Fracturing.CUT_BIOMASS_BU, "no pay");
                    h.assertTrue(site.state(0, 0, -2).is(Blocks.STONE), "it does not reach further");
                    site.release();
                })
                .thenSucceed();
    }

    /** One thing in front the jaw must leave, what it is named, and how it is cleared again. */
    private record Leave(String what, Refusal reason, Consumer<Site> place, Consumer<Site> clear) {}

    /**
     * What the jaw may not cut it leaves as it is, names, and pays nothing for: a block above stone grade, one with an
     * inventory, a fluid, and a block that a rail or a torch stands on. Once the torch is gone it cuts the stone under
     * it.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 6 * WAIT)
    public static void theJawLeavesWhatItMayNotCutAndNamesWhy(GameTestHelper h) {
        Site site = Site.of(h, 1);
        site.jaw();
        site.feed(Site.JAW, 1000);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        List<Leave> leaves = List.of(
                new Leave(
                        "obsidian",
                        Fracturing.TOO_HARD,
                        s -> s.set(0, 0, -1, Blocks.OBSIDIAN),
                        s -> s.set(0, 0, -1, Blocks.AIR)),
                new Leave(
                        "diamond ore",
                        Fracturing.TOO_HARD,
                        s -> s.set(0, 0, -1, Blocks.DIAMOND_ORE),
                        s -> s.set(0, 0, -1, Blocks.AIR)),
                new Leave("a chest", Fracturing.UNSUPPORTED, s -> s.set(0, 0, -1, Blocks.CHEST), s -> {
                    s.level.removeBlockEntity(s.at(0, 0, -1));
                    s.level.setBlock(s.at(0, 0, -1), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }),
                new Leave(
                        "water",
                        Fracturing.FLUID,
                        s -> s.set(0, 0, -1, Blocks.WATER),
                        s -> s.set(0, 0, -1, Blocks.STONE)),
                new Leave(
                        "a rail on the stone",
                        Fracturing.FRAGILE,
                        s -> s.set(0, 1, -1, Blocks.RAIL),
                        s -> s.set(0, 1, -1, Blocks.STONE)),
                new Leave("a torch on the stone", Fracturing.FRAGILE, s -> s.set(0, 1, -1, Blocks.TORCH), s -> {}));
        GameTestSequence sequence = h.startSequence();
        for (Leave leave : leaves)
            sequence = sequence.thenExecute(() -> {
                        leave.place().accept(site);
                        site.due();
                    })
                    .thenWaitUntil(() -> h.assertValueEqual(
                            site.stop(Site.JAW, player), Optional.of(key(leave.reason())), leave.what()))
                    .thenExecute(() -> {
                        h.assertValueEqual(slots(site.saved(Site.BED)), List.of(), leave.what() + ": nothing cut");
                        h.assertValueEqual(stored(site.saved(Site.JAW)), 1000, leave.what() + ": nothing paid");
                        h.assertValueEqual(site.loose(), 0, leave.what() + ": nothing loose");
                        leave.clear().accept(site);
                    });
        sequence.thenExecute(() -> {
                    h.assertTrue(site.state(0, 1, -1).is(Blocks.TORCH), "the torch still stands");
                    h.assertTrue(site.state(0, 0, -1).is(Blocks.STONE), "and the stone under it");
                    site.set(0, 1, -1, Blocks.AIR);
                    site.due();
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        slots(site.saved(Site.BED)), List.of("minecraft:cobblestone 1"), "the torch gone, it cuts"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.loose(), 0, "nothing loose");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * A bed that cannot take the cut stops the jaw: the block stays, nothing is paid or dropped. Taking one stack out
     * at the bed's screen lets the jaw cut.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * WAIT)
    public static void aFullBedStopsTheJawUntilSomethingIsTakenOut(GameTestHelper h) {
        Site site = Site.of(h, 2);
        site.jaw();
        site.set(0, 0, -1, Blocks.STONE);
        site.feed(Site.JAW, 1000);
        ItemStack[] full = new ItemStack[Fracturing.BED_SLOTS];
        for (int slot = 0; slot < full.length; slot++) full[slot] = new ItemStack(Items.DIRT, 64);
        site.bed(full);
        site.due();
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(Site.JAW, player), Optional.of(key(Fracturing.BED_FULL)), "the bed is full"))
                .thenExecute(() -> {
                    h.assertTrue(site.state(0, 0, -1).is(Blocks.STONE), "the stone stays");
                    h.assertValueEqual(stored(site.saved(Site.JAW)), 1000, "nothing paid");
                    h.assertValueEqual(site.loose(), 0, "nothing loose");
                    WorkBedMenu menu = (WorkBedMenu)
                            ((MenuProvider) site.entity(Site.BED)).createMenu(0, player.getInventory(), player);
                    h.assertValueEqual(menu.held(), 9 * 64, "the screen shows what it holds");
                    menu.quickMoveStack(player, 0);
                    h.assertTrue(player.getInventory().countItem(Items.DIRT) == 64, "one stack taken by hand");
                })
                .thenWaitUntil(() -> h.assertTrue(site.state(0, 0, -1).isAir(), "emptied, the jaw cuts"))
                .thenExecute(() -> {
                    h.assertTrue(
                            slots(site.saved(Site.BED)).contains("minecraft:cobblestone 1"), "into the freed slot");
                    h.assertValueEqual(site.loose(), 0, "nothing loose");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- collection ----

    /**
     * The whole chain on vein biomass: without biomass the jaw and the cilia both stop and nothing moves; once the
     * bladder holds biomass the junction supplies them, the jaw cuts and the cilia send the cut material and what the
     * bed already held through the Mouths and Item Veins to the chest. The bed ends empty, nothing is loose, and the
     * steward earns the cut-to-storage milestone.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 8 * WAIT)
    public static void veinBiomassRunsTheChainFromTheFaceToTheChest(GameTestHelper h) {
        Site site = Site.of(h, 3);
        site.jaw();
        site.cilia(MouthMode.OUTPUT);
        site.junction();
        site.set(0, 0, -1, Blocks.STONE);
        site.bed(new ItemStack(Items.COBBLESTONE, 4));
        ServerPlayer steward = player(h);
        site.steward(steward);
        site.due();
        PendingCredits credits = PendingCredits.get(h.getLevel());
        h.startSequence()
                .thenIdle(SETTLE)
                .thenWaitUntil(() -> {
                    h.assertValueEqual(
                            site.stop(Site.JAW, steward), Optional.of(key(Fracturing.NO_BIOMASS)), "jaw unsupplied");
                    h.assertValueEqual(
                            site.stop(Site.CILIA, steward),
                            Optional.of(key(Collecting.NO_BIOMASS)),
                            "cilia unsupplied");
                })
                .thenExecute(() -> {
                    h.assertTrue(site.state(0, 0, -1).is(Blocks.STONE), "nothing is cut");
                    h.assertValueEqual(slots(site.saved(Site.BED)), List.of("minecraft:cobblestone 4"), "nor sent");
                    h.assertValueEqual(site.chest().countItem(Items.COBBLESTONE), 0, "the chest is empty");
                    site.feed(Site.BLADDER, 2000);
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(site.state(0, 0, -1).isAir(), "supplied, the jaw cuts");
                    h.assertValueEqual(site.chest().countItem(Items.COBBLESTONE), 5, "and all of it reaches the chest");
                })
                .thenExecute(() -> {
                    h.assertValueEqual(slots(site.saved(Site.BED)), List.of(), "the bed is empty");
                    h.assertValueEqual(site.saved(Site.CILIA).getInt("sent"), 5, "the cilia's history");
                    h.assertTrue(stored(site.saved(Site.CILIA)) > 0, "the vein filled the cilia");
                    h.assertValueEqual(site.loose(), 0, "nothing loose");
                    h.assertTrue(
                            credits.holdsAutomatic(steward.getUUID(), id("collection_cilia")),
                            "the steward is credited");
                    credits.award(steward);
                    h.assertTrue(done(h, steward, "contact/cut_to_storage"), "Face to Store is earned");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * Collection waits without losing or doubling anything: a Mouth that does not give and a full chest each stop the
     * cilia with their reason while the cut material stays in the bed; once the chest has room it all arrives, once.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 8 * WAIT)
    public static void aClosedMouthOrFullStorageKeepsTheCutInTheBed(GameTestHelper h) {
        Site site = Site.of(h, 4);
        site.jaw();
        site.cilia(MouthMode.CLOSED);
        site.feed(Site.CILIA, 1000);
        site.bed(new ItemStack(Items.COBBLESTONE, 10), new ItemStack(Items.COAL, 3));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        h.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    for (int slot = 0; slot < 27; slot++) site.chest().setItem(slot, new ItemStack(Items.DIRT, 64));
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(Site.CILIA, player), Optional.of(key(ItemAccess.NOT_GIVING)), "the Mouth is closed"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.held(Items.COBBLESTONE), 10, "all ten still in the bed");
                    site.mouth(Site.MOUTH, MouthMode.OUTPUT);
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(Site.CILIA, player), Optional.of(key(Collecting.STORAGE_FULL)), "the chest is full"))
                .thenExecute(() -> {
                    h.assertValueEqual(
                            slots(site.saved(Site.BED)),
                            List.of("minecraft:cobblestone 10", "minecraft:coal 3"),
                            "the bed keeps it all");
                    h.assertValueEqual(stored(site.saved(Site.CILIA)), 1000, "a sweep that moved nothing is free");
                    h.assertValueEqual(site.loose(), 0, "nothing loose");
                    site.chest().setItem(0, ItemStack.EMPTY);
                    site.chest().setItem(1, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> h.assertValueEqual(slots(site.saved(Site.BED)), List.of(), "room: all sent"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.chest().countItem(Items.COBBLESTONE), 10, "ten cobblestone, once");
                    h.assertValueEqual(site.chest().countItem(Items.COAL), 3, "three coal, once");
                    h.assertValueEqual(site.saved(Site.CILIA).getInt("sent"), 13, "sent counts what left");
                    h.assertValueEqual(stored(site.saved(Site.CILIA)), 1000 - Collecting.SWEEP_BIOMASS_BU, "one paid");
                    h.assertValueEqual(site.loose(), 0, "nothing loose");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- saved state ----

    /**
     * The jaw, the bed and the cilia keep everything through their chunk being written to disk, unloaded and read
     * back: new block entities with the same biomass, contents, histories and steward; no work while they were gone;
     * and they carry on, the jaw cutting into the bed and the cilia sending to the chest.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "cutting_unload",
            timeoutTicks = 1200)
    public static void theOrgansKeepTheirStateThroughASaveToDiskAndAReload(GameTestHelper h) {
        Site site = Site.of(h, 5);
        site.jaw();
        site.cilia(MouthMode.CLOSED);
        ServerPlayer steward = player(h);
        site.steward(steward);
        site.feed(Site.JAW, 700);
        site.feed(Site.CILIA, 500);
        site.load(Site.JAW, tag -> tag.putInt("cuts", 3));
        site.load(Site.CILIA, tag -> tag.putInt("sent", 9));
        site.bed(new ItemStack(Items.COBBLESTONE, 5));
        site.load(Site.BED, tag -> tag.putInt("received", 7));
        List<BlockPos> organs = List.of(Site.JAW, Site.BED, Site.CILIA);
        List<BlockEntity> before = new ArrayList<>();
        List<CompoundTag> saved = new ArrayList<>();
        h.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    for (BlockPos organ : organs) {
                        before.add(site.entity(organ));
                        saved.add(site.saved(organ));
                    }
                    site.level.save(null, true, false);
                    site.level.setChunkForced(site.chunk.x, site.chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(site.level.hasChunkAt(site.start), "the chunk is gone"))
                .thenExecute(() -> site.level.save(null, true, false))
                .thenIdle(2 * Fracturing.RETRY_TICKS)
                .thenExecute(() -> TestChunks.force(site.level, site.chunk))
                .thenWaitUntil(() -> h.assertTrue(
                        site.level.hasChunkAt(site.start) && site.entity(Site.CILIA) != null,
                        "the chunk and the organs are back"))
                .thenExecute(() -> {
                    for (int n = 0; n < organs.size(); n++) {
                        BlockPos organ = organs.get(n);
                        h.assertTrue(site.entity(organ) != before.get(n), organ + ": read from the saved chunk");
                        CompoundTag back = site.saved(organ);
                        for (String key : List.of("tank", "cuts", "slots", "received", "sent", "steward"))
                            if (saved.get(n).contains(key))
                                h.assertValueEqual(back.get(key), saved.get(n).get(key), organ + " keeps " + key);
                    }
                    int waited =
                            site.saved(Site.JAW).getInt("ticks") - saved.get(0).getInt("ticks");
                    h.assertTrue(waited >= 0 && waited <= 5, "the jaw did not wait while gone: " + waited);
                    site.set(0, 0, -1, Blocks.STONE);
                    site.due();
                    site.mouth(Site.MOUTH, MouthMode.OUTPUT);
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.chest().countItem(Items.COBBLESTONE), 6, "it carries on: five held and one cut"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.saved(Site.JAW).getInt("cuts"), 4, "from where it was");
                    h.assertValueEqual(site.saved(Site.CILIA).getInt("sent"), 15, "and the cilia too");
                    h.assertValueEqual(site.saved(Site.BED).getInt("received"), 8, "and the bed");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * Each organ broken with the Surgical Blade comes out as one item carrying its biomass, contents and history; placed
     * again it has all of it. Nothing spills.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void brokenOrgansComeOutWholeWithTheirContentsAndHistory(GameTestHelper h) {
        Site site = Site.of(h, 6);
        site.jaw();
        site.cilia(MouthMode.OUTPUT);
        site.feed(Site.JAW, 300);
        site.feed(Site.CILIA, 200);
        site.load(Site.JAW, tag -> tag.putInt("cuts", 12));
        site.load(Site.CILIA, tag -> tag.putInt("sent", 40));
        site.bed(new ItemStack(Items.RAW_IRON, 3), new ItemStack(Items.COBBLESTONE, 30));
        site.load(Site.BED, tag -> tag.putInt("received", 33));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        record Organ(BlockPos offset, Item item, List<String> keys) {}
        for (Organ organ : List.of(
                new Organ(Site.JAW, MiningModule.FRACTURE_JAW_ITEM.get(), List.of("tank", "cuts")),
                new Organ(Site.BED, MiningModule.WORK_BED_ITEM.get(), List.of("slots", "received")),
                new Organ(Site.CILIA, MiningModule.COLLECTION_CILIA_ITEM.get(), List.of("tank", "sent")))) {
            CompoundTag held = site.saved(organ.offset());
            BlockPos at = site.at(organ.offset());
            List<ItemStack> drops = Block.getDrops(
                    site.level.getBlockState(at),
                    site.level,
                    at,
                    site.entity(organ.offset()),
                    player,
                    new ItemStack(InteractionModule.SURGICAL_BLADE.get()));
            h.assertValueEqual(drops.size(), 1, organ.item() + ": one item, nothing spilled: " + drops);
            ItemStack carried = drops.get(0);
            h.assertTrue(carried.is(organ.item()), "the organ itself");
            site.level.removeBlockEntity(at);
            site.level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            BlockPos ground = at.below();
            player.setItemInHand(InteractionHand.MAIN_HAND, carried);
            carried.useOn(new UseOnContext(
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
            h.assertTrue(site.level.getBlockState(at).getBlock().asItem() == organ.item(), "placed again");
            CompoundTag again = site.saved(organ.offset());
            for (String key : organ.keys())
                h.assertValueEqual(again.get(key), held.get(key), organ.item() + ": " + key + " travels on the item");
        }
        h.assertValueEqual(site.loose(), 0, "nothing spilled");
        site.release();
        h.succeed();
    }

    /** Impossible saved numbers are kept as they were; the jaw then cuts nothing and the cilia send nothing. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * WAIT)
    public static void unreadableOrgansKeepTheirSavedBytesAndDoNothing(GameTestHelper h) {
        Site site = Site.of(h, 7);
        site.jaw();
        site.cilia(MouthMode.OUTPUT);
        site.set(0, 0, -1, Blocks.STONE);
        site.feed(Site.JAW, 1000);
        site.feed(Site.CILIA, 1000);
        site.bed(new ItemStack(Items.COBBLESTONE, 2));
        site.load(Site.JAW, tag -> tag.putInt("ticks", Fracturing.CUT_TICKS + 1));
        site.load(Site.CILIA, tag -> tag.putInt("sent", -1));
        h.startSequence()
                .thenIdle(SETTLE + 2 * WAIT)
                .thenExecute(() -> {
                    h.assertTrue(site.state(0, 0, -1).is(Blocks.STONE), "the jaw cuts nothing");
                    h.assertValueEqual(
                            site.saved(Site.JAW).getInt("ticks"), Fracturing.CUT_TICKS + 1, "its bytes stay");
                    h.assertValueEqual(
                            slots(site.saved(Site.BED)), List.of("minecraft:cobblestone 2"), "the cilia send nothing");
                    h.assertValueEqual(site.saved(Site.CILIA).getInt("sent"), -1, "their bytes stay");
                    h.assertValueEqual(site.chest().countItem(Items.COBBLESTONE), 0, "the chest gets nothing");
                    site.release();
                })
                .thenSucceed();
    }

    /** Holds one chunk loaded and nothing around it: a radius-0 region ticket keeps only that chunk at full status. */
    private static final TicketType<ChunkPos> HOLD =
            TicketType.create("infestusfrontier_fracture_jaw_test", Comparator.comparingLong(ChunkPos::toLong));

    /**
     * Ground next to the cut is not loaded: the jaw names it and changes nothing, and cuts once the chunk is there.
     * Only the jaw's own chunk is held loaded, and nothing ticks it, so the test ticks the jaw itself as the server
     * ticks one in a ticking chunk.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void unloadedGroundStopsTheJawUntilItLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        // x ends in 14: the cut is at the chunk's edge, and the ground its updates reach is in the next chunk
        BlockPos jaw = new BlockPos((origin.getX() & ~15) + 248014, TOP, (origin.getZ() & ~15) + 248008);
        ChunkPos home = new ChunkPos(jaw);
        ChunkPos next = new ChunkPos(jaw.east(2));
        level.getChunkSource().addRegionTicket(HOLD, home, 0, home);
        level.getChunkAt(jaw);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (BlockPos cell : BlockPos.betweenClosed(jaw.offset(-2, -1, -1), jaw.offset(1, 1, 1)))
            level.setBlock(cell, Blocks.STONE.defaultBlockState(), quiet);
        // veins, not substrate: placed substrate looks around
        level.setBlock(jaw.below(), LogisticsModule.BIOMASS_VEIN.get().defaultBlockState(), quiet);
        level.setBlock(jaw.west().below(), LogisticsModule.BIOMASS_VEIN.get().defaultBlockState(), quiet);
        level.setBlock(
                jaw,
                MiningModule.FRACTURE_JAW
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST),
                quiet);
        level.setBlock(jaw.west(), MiningModule.WORK_BED.get().defaultBlockState(), quiet);
        BlockEntity entity = level.getBlockEntity(jaw);
        level.getCapability(BiomassCapabilities.BLOCK, jaw, null).insert(Biomass.of(100), false);
        CompoundTag due = entity.saveWithFullMetadata(level.registryAccess());
        due.putInt("ticks", Fracturing.CUT_TICKS);
        entity.loadWithComponents(due, level.registryAccess());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        WorkOrganMenu menu = (WorkOrganMenu) ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(level.hasChunk(next.x, next.z), "the next chunk is not loaded"))
                .thenExecute(() -> {
                    h.assertTrue(level.getBlockEntity(jaw) == entity, "the jaw's chunk stays");
                    tick(level, jaw, entity.getType());
                    h.assertValueEqual(
                            menu.stopKey(), Optional.of(key(Fracturing.UNLOADED)), "it names the unloaded ground");
                    h.assertFalse(level.hasChunk(next.x, next.z), "and loaded nothing");
                    h.assertTrue(level.getBlockState(jaw.east()).is(Blocks.STONE), "and cut nothing");
                    level.getChunkSource().addRegionTicket(HOLD, next, 0, next);
                    level.getChunkAt(jaw.east(2));
                    for (int wait = 0;
                            wait <= Fracturing.RETRY_TICKS
                                    && level.getBlockState(jaw.east()).is(Blocks.STONE);
                            wait++) tick(level, jaw, entity.getType());
                    h.assertTrue(level.getBlockState(jaw.east()).isAir(), "loaded, it cuts");
                    h.assertValueEqual(menu.count(), 1, "one cut");
                    for (BlockPos cell : BlockPos.betweenClosed(jaw.offset(-2, -1, -1), jaw.offset(1, 1, 1))) {
                        level.removeBlockEntity(cell);
                        level.setBlock(cell, Blocks.AIR.defaultBlockState(), quiet);
                    }
                    level.getChunkSource().removeRegionTicket(HOLD, home, 0, home);
                    level.getChunkSource().removeRegionTicket(HOLD, next, 0, next);
                })
                .thenSucceed();
    }

    /** Runs the server's ticker of the block entity of {@code type} at {@code pos} once. */
    private static <T extends BlockEntity> void tick(ServerLevel level, BlockPos pos, BlockEntityType<T> type) {
        T entity = type.getBlockEntity(level, pos);
        BlockEntityTicker<T> ticker = level.getBlockState(pos).getTicker(level, type);
        if (entity != null && ticker != null) ticker.tick(level, pos, level.getBlockState(pos), entity);
    }

    // ---- limits ----

    /**
     * The jaws of a dimension share {@value Fracturing#CUTS_PER_TICK} cuts a tick: of one more jaw than that, all due
     * at once and ticked in one tick, one is left for the next tick. Alone in its batch, so no other jaw shares the
     * limit.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "jaw_cut_limit",
            timeoutTicks = 100)
    public static void theJawsOfADimensionShareTheirCutsEachTick(GameTestHelper h) {
        Site site = Site.of(h, 8);
        int jaws = Fracturing.CUTS_PER_TICK + 1;
        BlockState jaw = MiningModule.FRACTURE_JAW
                .get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        for (int n = 0; n < jaws; n++) {
            int dx = n - jaws / 2;
            site.set(dx, -1, 0, EcologyTests.mature());
            site.set(dx, -1, 1, EcologyTests.mature());
            site.set(dx, -1, -1, Blocks.STONE);
            site.set(dx, 0, -1, Blocks.STONE);
            site.set(dx, 0, 0, jaw);
            site.set(dx, 0, 1, MiningModule.WORK_BED.get());
            site.feed(new BlockPos(dx, 0, 0), 100);
        }
        h.startSequence()
                .thenExecute(() -> {
                    for (int n = 0; n < jaws; n++) {
                        BlockPos at = new BlockPos(n - jaws / 2, 0, 0);
                        site.load(at, tag -> tag.putInt("ticks", Fracturing.CUT_TICKS));
                        tick(site.level, site.at(at), site.entity(at).getType());
                    }
                    int cut = 0;
                    for (int n = 0; n < jaws; n++)
                        if (site.state(n - jaws / 2, 0, -1).isAir()) cut++;
                    h.assertValueEqual(cut, Fracturing.CUTS_PER_TICK, "the limit, in one tick");
                })
                .thenWaitUntil(() -> {
                    for (int n = 0; n < jaws; n++)
                        h.assertTrue(site.state(n - jaws / 2, 0, -1).isAir(), "the last one cuts on a later tick");
                })
                .thenExecute(site::release)
                .thenSucceed();
    }

    // ---- discovery ----

    /**
     * Each organ is crafted from an Organ Bud and Overworld materials; the guide's entry teaches them and shows their
     * recipes; the discovery flags and the milestone exist; every stop the jaw names has its short label and its
     * sentence.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void recipesMakeTheOrgansAndTheGuideTeachesThem(GameTestHelper h) {
        ItemStack bud = new ItemStack(OrganModule.ORGAN_BUD_ITEM.get());
        ItemStack plate = new ItemStack(ProcessingModule.BONE_PLATE.get());
        ItemStack sheet = new ItemStack(ProcessingModule.MEMBRANE_SHEET.get());
        M1RouteTests.craft(
                h,
                List.of(
                        bud.copy(),
                        new ItemStack(Items.STONE_PICKAXE),
                        plate.copy(),
                        plate.copy(),
                        new ItemStack(Items.FLINT)),
                MiningModule.FRACTURE_JAW_ITEM.get());
        M1RouteTests.craft(
                h,
                List.of(bud.copy(), new ItemStack(Items.CHEST), sheet.copy(), sheet.copy()),
                MiningModule.WORK_BED_ITEM.get());
        M1RouteTests.craft(
                h,
                List.of(bud.copy(), new ItemStack(Items.STRING), new ItemStack(Items.STRING), sheet.copy()),
                MiningModule.COLLECTION_CILIA_ITEM.get());
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String organ : List.of("fracture_jaw", "work_bed", "collection_cilia"))
            for (String path : List.of("acquired/" + organ, "recipes/" + organ))
                h.assertTrue(advancements.get(id(path)) != null, path + " exists");
        h.assertTrue(advancements.get(id("contact/cut_to_storage")) != null, "the milestone exists");
        var entry = BookDataManager.get()
                .getBook(id("the_waking_genome"))
                .getCategory(id("circulation"))
                .getEntries()
                .get(id("circulation/fracture_jaw"));
        h.assertTrue(entry != null, "the guide entry loaded");
        List<net.minecraft.resources.ResourceLocation> shown = new ArrayList<>();
        entry.getPages().stream()
                .filter(page -> page instanceof BookCraftingRecipePage)
                .map(page -> (BookCraftingRecipePage) page)
                .forEach(page -> {
                    shown.add(page.getRecipeId1());
                    if (page.getRecipeId2() != null) shown.add(page.getRecipeId2());
                });
        h.assertValueEqual(
                shown,
                List.of(id("fracture_jaw"), id("work_bed"), id("collection_cilia")),
                "the guide renders all three recipes from recipe data");
        var lang = StarterAdvancementTests.lang();
        for (Refusal stop : Fracturing.STOPS) {
            String label = "gui.infestusfrontier.fracture_jaw.stop."
                    + stop.key().substring(stop.key().indexOf('.') + 1).toLowerCase(Locale.ROOT);
            for (String wording : List.of(label, key(stop)))
                h.assertTrue(
                        lang.has(wording) && !lang.get(wording).getAsString().isBlank(),
                        "the jaw's stop is worded: " + wording);
        }
        h.succeed();
    }

    // ---- the demo ----

    /**
     * The demo's exhibit: a supplied jaw under a chute of gravel, its bed, cilia on an Output Mouth and veins to a chest
     * on an Intake Mouth. It cuts on its own and the gravel's drops reach the chest; nothing falls loose.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void theDemoExhibitCutsAChuteOfGravelIntoTheChest(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(new BlockPos(122, -62, -62), new BlockPos(150, -50, -50), 63);
        yard.force(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        BlockPos jaw = yard.at(new BlockPos(126, -60, -56));
        BlockPos chest = yard.at(new BlockPos(132, -60, -55));
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> yard.build(h))
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(h.getLevel().getBlockState(jaw).is(MiningModule.FRACTURE_JAW.get()), "the jaw");
                    h.assertTrue(h.getLevel().getBlockState(jaw.south()).is(MiningModule.WORK_BED.get()), "its bed");
                    h.assertTrue(
                            h.getLevel()
                                    .getBlockState(yard.at(new BlockPos(127, -60, -55)))
                                    .is(MiningModule.COLLECTION_CILIA.get()),
                            "the cilia");
                })
                .thenWaitUntil(() -> {
                    var box = (ChestBlockEntity) h.getLevel().getBlockEntity(chest);
                    h.assertTrue(
                            box != null && box.countItem(Items.GRAVEL) + box.countItem(Items.FLINT) >= 2,
                            "cut gravel reaches the chest");
                })
                .thenExecute(() -> {
                    WorkOrganMenu menu =
                            (WorkOrganMenu) ((MenuProvider) h.getLevel().getBlockEntity(jaw))
                                    .createMenu(0, player.getInventory(), player);
                    h.assertTrue(menu.count() >= 2, "the jaw counts its cuts");
                    h.assertValueEqual(
                            h.getLevel()
                                    .getEntitiesOfClass(ItemEntity.class, new AABB(jaw).inflate(8))
                                    .size(),
                            0,
                            "nothing loose");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }
}
