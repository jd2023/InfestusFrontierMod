package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.mining.RootCellBlock;
import org.jd.infestusfrontier.mining.Stride;
import org.jd.infestusfrontier.mining.TunnelRootBlock;
import org.jd.infestusfrontier.mining.TunnelRootMenu;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * The Tunnel Root: how a bud grows a head, how the head cuts a level 1x2 tunnel with a turn and lays its root, what
 * stops it and how the player starts it again, its limits, and its state through moving, unloading, a save to disk
 * and breaking. Each head works in a yard of its own in a chunk of its own, far from the test structures. Most tests
 * make the next step due by loading the head's own saved state with the wait finished; one waits the twenty seconds.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class TunnelRootTests {
    private static final int TOP = 100;
    /** Ticks a due step may take: one try, a stop, and one more look after the retry delay, with a margin. */
    private static final int STEP_WAIT = 4 * Tunnelling.RETRY_TICKS;

    private static final int SEGMENTS = 16;
    private static final int BUCKET_BU = 1000;

    /** A head's yard: rock around {@code start}, the middle of a chunk the yard forces for itself. */
    private record Site(ServerLevel level, BlockPos start, ChunkPos chunk) {
        private static final int REACH = 6;

        static Site of(GameTestHelper h, int slot) {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            BlockPos start =
                    new BlockPos((origin.getX() & ~15) + 208008, TOP, (origin.getZ() & ~15) + 262408 + slot * 64);
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

        /** Everything in the yard to air, block entities and loose items included. */
        void clear() {
            for (BlockPos cell : BlockPos.betweenClosed(at(-REACH, -2, -REACH), at(REACH, 4, REACH))) {
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
            return AABB.encapsulatingFullBlocks(at(-REACH, -2, -REACH), at(REACH, 4, REACH));
        }

        /**
         * Stone from the floor layer under the start to the layer over the tunnel, with the head on Living Substrate
         * at {@code start}, facing {@code facing}, and air over it.
         */
        void plant(Direction facing) {
            for (BlockPos cell : BlockPos.betweenClosed(at(-REACH + 1, -1, -REACH + 1), at(REACH - 1, 2, REACH - 1)))
                level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            set(0, -1, 0, EcologyTests.mature());
            set(0, 1, 0, Blocks.AIR);
            level.setBlockAndUpdate(
                    start, MiningModule.TUNNEL_ROOT.get().defaultBlockState().setValue(TunnelRootBlock.FACING, facing));
        }

        /** Where the head is now; it moves. */
        BlockPos head() {
            for (BlockPos cell : BlockPos.betweenClosed(at(-REACH, -1, -REACH), at(REACH, 2, REACH)))
                if (level.getBlockState(cell).is(MiningModule.TUNNEL_ROOT.get())) return cell.immutable();
            throw new IllegalStateException("No head in the yard");
        }

        BlockEntity entity() {
            return level.getBlockEntity(head());
        }

        CompoundTag saved() {
            return entity().saveWithFullMetadata(level.registryAccess());
        }

        /** Loads the head's own saved state changed by {@code change}, as a load from disk does. */
        void load(Consumer<CompoundTag> change) {
            CompoundTag tag = saved();
            change.accept(tag);
            entity().loadWithComponents(tag, level.registryAccess());
        }

        /** The wait for the next step is over. */
        void due() {
            load(tag -> tag.putInt("ticks", Tunnelling.STEP_TICKS));
        }

        TunnelRootMenu menu(Player player) {
            return (TunnelRootMenu) ((MenuProvider) entity()).createMenu(0, player.getInventory(), player);
        }

        Optional<String> stop(Player player) {
            return menu(player).stopKey();
        }

        /** {@code player} right-clicks the head holding {@code stack}. */
        void feed(Player player, ItemStack stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockPos head = head();
            level.getBlockState(head)
                    .useItemOn(
                            stack,
                            level,
                            player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(head), Direction.UP, head, false));
        }

        /** Sixteen segments and one bucket of biomass, by hand. */
        void supply(Player player) {
            feed(player, new ItemStack(MiningModule.ROOT_SEGMENT.get(), SEGMENTS));
            feed(player, new ItemStack(StorageModule.BIOMASS_BUCKET.get()));
        }

        int loose() {
            return level.getEntitiesOfClass(ItemEntity.class, yard()).size();
        }
    }

    private static String key(Refusal stop) {
        return "infestusfrontier.refusal." + stop.key();
    }

    private static int segments(CompoundTag tag) {
        return tag.getCompound("segments").getInt("count");
    }

    private static int stored(CompoundTag tag) {
        return tag.getCompound("tank").getInt("stored");
    }

    /** The output's stacks as {@code item count} lines, empty slots left out. */
    private static List<String> output(CompoundTag tag) {
        ListTag slots = tag.getList("output", Tag.TAG_COMPOUND);
        return slots.stream()
                .map(slot -> (CompoundTag) slot)
                .filter(slot -> slot.contains("id"))
                .map(slot -> slot.getString("id") + " " + slot.getInt("count"))
                .toList();
    }

    /** True when a standing player fits in {@code cell} with nothing in the way. */
    private static boolean walkable(ServerLevel level, BlockPos cell) {
        return level.noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(cell)));
    }

    // ---- the tunnel ----

    /**
     * A head cuts north, is steered left and cuts west twice: a level 1x2 tunnel with a turn that a player can walk,
     * a flush root cell under every step, a bend cell where it turned, and every cut's ordinary drops once in its
     * output. The same organ keeps its supplies, output, cells and steward through every move, and its steward earns
     * the first cut and, walking through the bend, the turn.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            timeoutTicks = 3 * STEP_WAIT + 60 + Tunnelling.CRAWL_TICKS) // and one crawl watched to its end
    public static void aHeadCutsALevelTunnelWithATurnAndLaysAFlushRoot(GameTestHelper h) {
        Site site = Site.of(h, 0);
        site.plant(Direction.NORTH);
        site.set(0, 1, -1, Blocks.IRON_ORE); // stone grade cuts iron
        site.set(-1, 0, -1, Blocks.COAL_ORE);
        ServerPlayer steward = player(h);
        site.supply(steward);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        site.due();
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(0, 0, -1), "the first step, north"))
                .thenExecute(() -> {
                    h.assertValueEqual(
                            headState(site).getValue(TunnelRootBlock.STRIDE), Stride.STRAIGHT, "it crawls in straight");
                    TunnelRootMenu menu = site.menu(steward);
                    h.assertTrue(menu.clickMenuButton(steward, TunnelRootMenu.LEFT_BUTTON), "the screen steers");
                    h.assertValueEqual(site.menu(steward).steer(), Steer.LEFT, "the next step goes left");
                    h.assertValueEqual(
                            headState(site).getValue(TunnelRootBlock.STEER), Stride.LEFT, "the head leans left");
                    site.due();
                })
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(-1, 0, -1), "the turned step, west"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.menu(steward).steer(), Steer.STRAIGHT, "after a turn, straight again");
                    BlockState turned = headState(site);
                    h.assertValueEqual(turned.getValue(TunnelRootBlock.STRIDE), Stride.LEFT, "it crawls round left");
                    h.assertValueEqual(turned.getValue(TunnelRootBlock.STEER), Stride.STRAIGHT, "steered on");
                    h.assertValueEqual(turned.getValue(TunnelRootBlock.FACING), Direction.WEST, "facing west");
                    h.assertTrue(site.saved().getInt("ticks") < Tunnelling.CRAWL_TICKS, "still crawling in");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        headState(site).getValue(TunnelRootBlock.STRIDE), Stride.REST, "the crawl ends at rest"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.head(), site.at(-1, 0, -1), "at rest where it crawled to");
                    int waited = site.saved().getInt("ticks");
                    h.assertTrue(
                            waited >= Tunnelling.CRAWL_TICKS && waited < Tunnelling.STEP_TICKS,
                            "it rests once its crawl's time is up, before the next step: " + waited);
                    site.due();
                })
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(-2, 0, -1), "straight on, west"))
                .thenExecute(() -> {
                    ServerLevel level = site.level;
                    h.assertValueEqual(
                            site.state(-2, 0, -1).getValue(TunnelRootBlock.FACING), Direction.WEST, "it faces west");
                    for (BlockPos cell : List.of(
                            site.at(0, 0, 0),
                            site.at(0, 1, 0),
                            site.at(0, 0, -1),
                            site.at(0, 1, -1),
                            site.at(-1, 0, -1),
                            site.at(-1, 1, -1),
                            site.at(-2, 1, -1)))
                        h.assertTrue(level.getBlockState(cell).isAir(), "the tunnel is open at " + cell);
                    for (BlockPos cell : List.of(site.at(0, 0, 0), site.at(0, 0, -1), site.at(-1, 0, -1)))
                        h.assertTrue(walkable(level, cell), "a player walks the tunnel at " + cell);
                    h.assertTrue(site.state(0, 2, -1).is(Blocks.STONE), "the roof stays");
                    h.assertTrue(site.state(-1, 0, -2).is(Blocks.STONE), "the walls stay");
                    h.assertTrue(site.state(0, 0, -2).is(Blocks.STONE), "nothing is cut past the turn");
                    h.assertTrue(site.state(0, -1, 0).is(EcologyModule.LIVING_SUBSTRATE.get()), "the start stays");
                    BlockState bend = site.state(0, -1, -1);
                    h.assertTrue(bend.is(MiningModule.ROOT_CELL.get()) && bend.getValue(RootCellBlock.BEND), "a bend");
                    h.assertValueEqual(bend.getValue(RootCellBlock.FACING), Direction.SOUTH, "opening south and west");
                    for (int dx : new int[] {-1, -2}) {
                        BlockState cell = site.state(dx, -1, -1);
                        h.assertTrue(
                                cell.is(MiningModule.ROOT_CELL.get())
                                        && !cell.getValue(RootCellBlock.BEND)
                                        && cell.getValue(RootCellBlock.FACING) == Direction.WEST,
                                "a straight cell running west at " + dx);
                        h.assertTrue(
                                cell.isCollisionShapeFullBlock(level, site.at(dx, -1, -1)),
                                "flush: a full block in the floor");
                    }
                    CompoundTag tag = site.saved();
                    h.assertValueEqual(tag.getInt("cells"), 3, "three cells laid");
                    h.assertValueEqual(segments(tag), SEGMENTS - 3, "one segment a step");
                    h.assertValueEqual(stored(tag), BUCKET_BU - 3 * Tunnelling.STEP_BIOMASS_BU, "biomass a step");
                    h.assertValueEqual(
                            output(tag),
                            List.of("minecraft:raw_iron 1", "minecraft:cobblestone 4", "minecraft:coal 1"),
                            "each cut's ordinary drops, once");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    h.assertValueEqual(tag.getUUID("steward"), steward.getUUID(), "the same steward moved with it");
                    h.assertTrue(
                            credits.holdsAutomatic(steward.getUUID(), id("tunnel_root")), "the steward is credited");
                    credits.award(steward);
                    h.assertTrue(done(h, steward, "contact/tunnel_root"), "Root Bites Rock is earned");
                    stand(steward, site.at(-1, 0, -1));
                    CriteriaTriggers.LOCATION.trigger(steward);
                    h.assertFalse(done(h, steward, "contact/root_bend"), "a straight cell is no bend");
                    stand(steward, site.at(0, 0, -1));
                    CriteriaTriggers.LOCATION.trigger(steward);
                    h.assertTrue(done(h, steward, "contact/root_bend"), "walking through the bend earns the turn");
                    site.release();
                })
                .thenSucceed();
    }

    /** The head's block state, which the client animates. */
    private static BlockState headState(Site site) {
        return site.level.getBlockState(site.head());
    }

    private static void stand(ServerPlayer player, BlockPos cell) {
        Vec3 feet = Vec3.atBottomCenterOf(cell);
        player.moveTo(feet.x, feet.y, feet.z, 0F, 0F);
    }

    /** A head steps twenty seconds of loaded ticks after it was planted, not before. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            timeoutTicks = Tunnelling.STEP_TICKS + STEP_WAIT)
    public static void aStepComesEveryTwentySeconds(GameTestHelper h) {
        Site site = Site.of(h, 1);
        site.plant(Direction.EAST);
        site.supply(EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0));
        long planted = h.getLevel().getGameTime();
        long[] stepped = new long[1];
        h.startSequence()
                .thenIdle(Tunnelling.STEP_TICKS - 10)
                .thenExecute(() -> h.assertValueEqual(site.head(), site.start, "it waits"))
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(1, 0, 0), "then it steps east"))
                .thenExecute(() -> {
                    stepped[0] = h.getLevel().getGameTime() - planted;
                    h.assertTrue(
                            stepped[0] >= Tunnelling.STEP_TICKS && stepped[0] <= Tunnelling.STEP_TICKS + 5,
                            "twenty seconds, not " + stepped[0] + " ticks");
                    h.assertValueEqual(site.saved().getInt("ticks"), 0, "and the wait starts over");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * {@code place} puts a pair into the cut where one block depends on the other. One step takes both, and each
     * block's ordinary drops arrive in the output once: none is dropped a second time, loose, by the removal of its
     * partner.
     */
    private static void cutsAPairOnce(GameTestHelper h, int slot, Consumer<Site> place, List<String> drops) {
        Site site = Site.of(h, slot);
        site.plant(Direction.NORTH);
        site.supply(EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0));
        place.accept(site);
        site.due();
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(0, 0, -1), "it steps into the cut"))
                .thenExecute(() -> {
                    h.assertValueEqual(output(site.saved()), drops, "each block's drops, once");
                    h.assertValueEqual(site.loose(), 0, "nothing dropped loose");
                    h.assertTrue(site.state(0, 1, -1).isAir(), "the cut is open");
                    site.release();
                })
                .thenSucceed();
    }

    /** Placed without updates, so a pair the yard's stone would not hold stays as placed until the head cuts it. */
    private static void quietly(Site site, int dx, int dy, int dz, BlockState state) {
        site.level.setBlock(site.at(dx, dy, dz), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = STEP_WAIT)
    public static void aDoorInTheCutGivesOneDoor(GameTestHelper h) {
        cutsAPairOnce(
                h,
                18,
                site -> {
                    quietly(site, 0, 0, -1, Blocks.OAK_DOOR.defaultBlockState());
                    quietly(
                            site,
                            0,
                            1,
                            -1,
                            Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                },
                List.of("minecraft:oak_door 1"));
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = STEP_WAIT)
    public static void aLanternHangingFromTheUpperCutGivesOneLantern(GameTestHelper h) {
        cutsAPairOnce(
                h,
                19,
                site -> quietly(
                        site, 0, 0, -1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true)),
                List.of("minecraft:cobblestone 1", "minecraft:lantern 1"));
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = STEP_WAIT)
    public static void aTorchOnTheLowerCutGivesOneTorch(GameTestHelper h) {
        cutsAPairOnce(
                h,
                20,
                site -> quietly(site, 0, 1, -1, Blocks.TORCH.defaultBlockState()),
                List.of("minecraft:torch 1", "minecraft:cobblestone 1"));
    }

    // ---- stops and recovery ----

    /**
     * {@code spoil} makes the next step impossible: the head stops with {@code stop}, changes nothing and keeps its
     * supplies and output. {@code fix} removes the cause and the head takes the same step, paying once.
     */
    private static void stopsThenResumes(
            GameTestHelper h, int slot, Refusal stop, Consumer<Site> spoil, Consumer<Site> fix, List<String> drops) {
        Site site = Site.of(h, slot);
        site.plant(Direction.NORTH);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        site.supply(player);
        spoil.accept(site);
        site.due();
        CompoundTag spoiled = site.saved();
        CompoundTag[] fixed = new CompoundTag[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.stop(player), Optional.of(key(stop)), "it names " + stop))
                .thenIdle(2 * Tunnelling.RETRY_TICKS)
                .thenExecute(() -> {
                    h.assertValueEqual(site.head(), site.start, "it stays");
                    h.assertValueEqual(site.stop(player), Optional.of(key(stop)), "and still names " + stop);
                    CompoundTag stopped = site.saved();
                    for (String key : List.of("segments", "output", "tank", "cells"))
                        h.assertValueEqual(stopped.get(key), spoiled.get(key), "stopped, it keeps its " + key);
                    h.assertValueEqual(site.loose(), 0, "nothing dropped");
                    fix.accept(site);
                    fixed[0] = site.saved();
                })
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(0, 0, -1), "fixed, it steps"))
                .thenExecute(() -> {
                    CompoundTag after = site.saved();
                    h.assertValueEqual(after.getInt("cells"), 1, "one cell");
                    h.assertValueEqual(segments(after), segments(fixed[0]) - 1, "one segment paid, none lost");
                    h.assertValueEqual(
                            stored(after), stored(fixed[0]) - Tunnelling.STEP_BIOMASS_BU, "one step's biomass");
                    h.assertValueEqual(output(after), drops, "the cut's drops, once");
                    h.assertValueEqual(site.stop(player), Optional.empty(), "no stop shown");
                    h.assertValueEqual(site.loose(), 0, "nothing loose");
                    h.assertTrue(site.state(0, -1, -1).is(MiningModule.ROOT_CELL.get()), "a root cell under it");
                    site.release();
                })
                .thenSucceed();
    }

    private static final List<String> TWO_STONE = List.of("minecraft:cobblestone 2");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void noSegmentStopsUntilSegmentsAreLoaded(GameTestHelper h) {
        Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        stopsThenResumes(
                h,
                2,
                Tunnelling.NO_SEGMENT,
                site -> site.load(tag -> tag.put("segments", new CompoundTag())),
                site -> site.feed(loader, new ItemStack(MiningModule.ROOT_SEGMENT.get(), 3)),
                TWO_STONE);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void noBiomassStopsUntilABucketIsLoaded(GameTestHelper h) {
        Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        stopsThenResumes(
                h,
                3,
                Tunnelling.NO_BIOMASS,
                site -> site.load(tag -> tag.getCompound("tank").putInt("stored", Tunnelling.STEP_BIOMASS_BU - 1)),
                site -> {
                    site.feed(loader, new ItemStack(StorageModule.BIOMASS_BUCKET.get()));
                    h.assertTrue(loader.getMainHandItem().is(Items.BUCKET), "the bucket comes back empty");
                },
                TWO_STONE);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aFullOutputStopsUntilTheDropsAreTakenOut(GameTestHelper h) {
        Player taker = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        stopsThenResumes(
                h,
                4,
                Tunnelling.OUTPUT_FULL,
                site -> site.load(tag -> {
                    ListTag output = tag.getList("output", Tag.TAG_COMPOUND);
                    CompoundTag full = new CompoundTag();
                    full.putString("id", "minecraft:dirt");
                    full.putInt("count", 63);
                    output.set(0, full);
                }),
                site -> {
                    TunnelRootMenu menu = site.menu(taker);
                    h.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.DIRT)), "the output takes nothing in");
                    menu.quickMoveStack(taker, 1);
                    h.assertValueEqual(taker.getInventory().countItem(Items.DIRT), 63, "the dirt is the player's");
                },
                TWO_STONE);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aBlockAboveStoneGradeStopsUntilItIsGone(GameTestHelper h) {
        stopsThenResumes(
                h,
                5,
                Tunnelling.TOO_HARD,
                site -> site.set(0, 0, -1, Blocks.DIAMOND_ORE),
                site -> site.set(0, 0, -1, Blocks.AIR),
                List.of("minecraft:cobblestone 1"));
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aBlockWithAnInventoryStopsUntilItIsGone(GameTestHelper h) {
        stopsThenResumes(
                h,
                6,
                Tunnelling.UNSUPPORTED,
                site -> site.set(0, 1, -1, Blocks.CHEST),
                site -> {
                    site.level.removeBlockEntity(site.at(0, 1, -1));
                    site.set(0, 1, -1, Blocks.AIR);
                },
                List.of("minecraft:cobblestone 1"));
    }

    /** Ore in the floor under the cut: a root cell may not replace it, so the head waits until it is mined out. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void oreUnderTheCutStopsUntilItIsGone(GameTestHelper h) {
        stopsThenResumes(
                h,
                9,
                Tunnelling.NO_FLOOR,
                site -> site.set(0, -1, -1, Blocks.IRON_ORE),
                site -> site.set(0, -1, -1, Blocks.STONE),
                TWO_STONE);
    }

    // ---- accidents ----

    /**
     * {@code spoil} puts a hazard by the next step. The head does not stop for it: it takes the step as steered,
     * names no reason, and the world does what it does. {@code after} checks what the accident left.
     */
    private static void takesTheStepAnyway(
            GameTestHelper h, int slot, Consumer<Site> spoil, List<String> drops, Consumer<Site> after) {
        Site site = Site.of(h, slot);
        site.plant(Direction.NORTH);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        site.supply(player);
        spoil.accept(site);
        site.due();
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(0, 0, -1), "it steps all the same"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.stop(player), Optional.empty(), "and names no stop");
                    CompoundTag tag = site.saved();
                    h.assertValueEqual(tag.getInt("cells"), 1, "one step");
                    h.assertValueEqual(segments(tag), SEGMENTS - 1, "one segment, laid as the floor");
                    h.assertValueEqual(output(tag), drops, "the cut's ordinary drops, once");
                })
                .thenIdle(Tunnelling.RETRY_TICKS)
                .thenExecute(() -> {
                    after.accept(site);
                    site.release();
                })
                .thenSucceed();
    }

    /** Water beside the cut is no stop: the head cuts, and the water flows into the tunnel behind it. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void waterBesideTheCutFloodsTheTunnel(GameTestHelper h) {
        takesTheStepAnyway(h, 7, site -> site.set(1, 1, -1, Blocks.WATER), TWO_STONE, site -> {
            h.assertTrue(site.state(1, 1, -1).is(Blocks.WATER), "the source is still there");
            h.assertFalse(site.level.getFluidState(site.at(0, 1, -1)).isEmpty(), "and water runs into the cut");
            h.assertTrue(site.state(0, 0, -1).is(MiningModule.TUNNEL_ROOT.get()), "the head lives on, wet");
        });
    }

    /** Lava in the upper cut is no stop: there is nothing in it to cut, so only the lower block drops. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void lavaInTheCutIsCutInto(GameTestHelper h) {
        takesTheStepAnyway(h, 22, site -> site.set(0, 1, -1, Blocks.LAVA), List.of("minecraft:cobblestone 1"), site -> {
            h.assertFalse(site.level.getFluidState(site.at(0, 1, -1)).isEmpty(), "the lava is left to flow");
            h.assertValueEqual(site.loose(), 0, "nothing dropped loose");
        });
    }

    /**
     * Gravel on top of the cut is no stop: the head cuts and the gravel falls into the tunnel, landing as vanilla lands
     * it: on the head it cannot settle, so it breaks into its item. The head lives.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aLooseBlockOnTheCutFallsIntoTheTunnel(GameTestHelper h) {
        takesTheStepAnyway(h, 8, site -> site.set(0, 2, -1, Blocks.GRAVEL), TWO_STONE, site -> {
            h.assertFalse(site.state(0, 2, -1).is(Blocks.GRAVEL), "the gravel has left the roof");
            int fallen = site.state(0, 1, -1).is(Blocks.GRAVEL) ? 1 : 0;
            for (ItemEntity item : site.level.getEntitiesOfClass(ItemEntity.class, site.yard()))
                if (item.getItem().is(Items.GRAVEL)) fallen += item.getItem().getCount();
            h.assertValueEqual(fallen, 1, "one gravel, in the tunnel as a block or as its item");
            h.assertTrue(site.state(0, 0, -1).is(MiningModule.TUNNEL_ROOT.get()), "the head lives");
        });
    }

    /**
     * A hole under the cut is no stop and gets no bridge: the head cuts the two blocks, lays no root cell over the
     * hole and pays no segment for one, crawls onto nothing and dies. It leaves nothing behind: no head, no item, no
     * floor over the hole. The cell where it stood and the root behind it are what the accident leaves.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aHeadThatStepsOverAHoleDiesAndLeavesNothing(GameTestHelper h) {
        Site site = Site.of(h, 23);
        site.plant(Direction.NORTH);
        site.supply(EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0));
        site.set(0, -1, -1, Blocks.AIR);
        site.set(0, -2, -1, Blocks.AIR);
        site.due();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(site.state(0, 1, -1).isAir(), "it cuts"))
                .thenExecute(() -> {
                    h.assertTrue(site.state(0, 0, -1).isAir(), "the head died where it stepped");
                    h.assertTrue(site.state(0, 0, 0).isAir(), "it left the cell behind it");
                    h.assertTrue(site.state(0, -1, -1).isAir(), "no root cell bridges the hole");
                    for (BlockPos cell : BlockPos.betweenClosed(site.at(-6, -1, -6), site.at(6, 2, 6)))
                        h.assertFalse(site.level.getBlockState(cell).is(MiningModule.TUNNEL_ROOT.get()), "no head");
                    h.assertTrue(site.state(0, -1, 0).is(EcologyModule.LIVING_SUBSTRATE.get()), "the start stays");
                    h.assertValueEqual(site.loose(), 0, "nothing dropped: not the head, its segments or its cut");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * Losing the block under it kills a head, working or waiting: broken by a player, it leaves no item, no segments
     * and no output; the head is destroyed, not paused.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void aHeadWhoseGroundIsBrokenDiesWithoutDrops(GameTestHelper h) {
        Site site = Site.of(h, 24);
        site.plant(Direction.NORTH);
        site.supply(EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0));
        site.load(tag -> {
            ListTag output = tag.getList("output", Tag.TAG_COMPOUND);
            CompoundTag coal = new CompoundTag();
            coal.putString("id", "minecraft:coal");
            coal.putInt("count", 5);
            output.set(0, coal);
        });
        ServerPlayer miner = TestDrops.miner(h, new ItemStack(Items.DIAMOND_SHOVEL));
        h.assertTrue(miner.gameMode.destroyBlock(site.at(0, -1, 0)), "a player digs out the substrate under it");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(site.state(0, 0, 0).isAir(), "the head dies"))
                .thenExecute(() -> {
                    for (ItemEntity item : site.level.getEntitiesOfClass(ItemEntity.class, site.yard()))
                        h.assertFalse(
                                item.getItem().is(MiningModule.TUNNEL_ROOT_ITEM.get())
                                        || item.getItem().is(MiningModule.ROOT_SEGMENT.get())
                                        || item.getItem().is(Items.COAL),
                                "it drops nothing of its own: " + item.getItem());
                    site.release();
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aHeadOffItsRootStopsUntilItStandsOnOneAgain(GameTestHelper h) {
        stopsThenResumes(
                h,
                10,
                Tunnelling.UNROOTED,
                site -> site.set(0, -1, 0, Blocks.STONE),
                site -> site.set(0, -1, 0, EcologyTests.mature()),
                TWO_STONE);
    }

    /**
     * An extended piston pushing up into the upper cut: removing its arm would break the piston with drops, so the head
     * refuses it until both are gone.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aPistonArmInTheCutStopsUntilItIsGone(GameTestHelper h) {
        stopsThenResumes(
                h,
                21,
                Tunnelling.UNSUPPORTED,
                site -> {
                    site.set(1, 0, -1, Blocks.REDSTONE_BLOCK); // holds the piston out
                    site.set(
                            0,
                            0,
                            -1,
                            Blocks.PISTON
                                    .defaultBlockState()
                                    .setValue(PistonBaseBlock.FACING, Direction.UP)
                                    .setValue(PistonBaseBlock.EXTENDED, true));
                    site.set(
                            0,
                            1,
                            -1,
                            Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, Direction.UP));
                },
                site -> {
                    quietly(site, 0, 0, -1, Blocks.AIR.defaultBlockState());
                    quietly(site, 0, 1, -1, Blocks.AIR.defaultBlockState());
                },
                List.of());
    }

    /** Holds one chunk loaded and nothing around it: a radius-0 region ticket keeps only that chunk at full status. */
    private static final TicketType<ChunkPos> HOLD =
            TicketType.create("infestusfrontier_tunnel_root_test", Comparator.comparingLong(ChunkPos::toLong));

    /**
     * The next chunk is not loaded: the head names it and changes nothing, and steps once the chunk is there. Only the
     * head's own chunk is held loaded, and nothing ticks it, so the test ticks the head itself as the server ticks a
     * head in a ticking chunk.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void unloadedGroundAheadStopsTheHeadUntilItLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        // x ends in 14: the cut is at the chunk's edge, and its neighbour in the next chunk, which nothing holds
        BlockPos head = new BlockPos((origin.getX() & ~15) + 240014, TOP, (origin.getZ() & ~15) + 240008);
        ChunkPos home = new ChunkPos(head);
        ChunkPos next = new ChunkPos(head.east(2));
        level.getChunkSource().addRegionTicket(HOLD, home, 0, home);
        level.getChunkAt(head);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (BlockPos cell : BlockPos.betweenClosed(head.offset(-2, -1, -1), head.offset(1, 2, 1)))
            level.setBlock(cell, Blocks.STONE.defaultBlockState(), quiet);
        // a root cell, not substrate: placed substrate looks around
        level.setBlock(head.below(), MiningModule.ROOT_CELL.get().defaultBlockState(), quiet);
        level.setBlock(head.above(), Blocks.AIR.defaultBlockState(), quiet);
        level.setBlock(
                head,
                MiningModule.TUNNEL_ROOT.get().defaultBlockState().setValue(TunnelRootBlock.FACING, Direction.EAST),
                quiet);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        for (ItemStack supply : List.of(
                new ItemStack(MiningModule.ROOT_SEGMENT.get(), 4), new ItemStack(StorageModule.BIOMASS_BUCKET.get()))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, supply);
            level.getBlockState(head)
                    .useItemOn(
                            supply,
                            level,
                            player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(head), Direction.UP, head, false));
        }
        BlockEntity entity = level.getBlockEntity(head);
        CompoundTag due = entity.saveWithFullMetadata(level.registryAccess());
        due.putInt("ticks", Tunnelling.STEP_TICKS);
        entity.loadWithComponents(due, level.registryAccess());
        TunnelRootMenu menu = (TunnelRootMenu) ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(level.hasChunk(next.x, next.z), "the next chunk is not loaded"))
                .thenExecute(() -> {
                    h.assertTrue(level.getBlockEntity(head) == entity, "the head's chunk stays");
                    tick(level, head, entity.getType());
                    h.assertValueEqual(
                            menu.stopKey(), Optional.of(key(Tunnelling.UNLOADED)), "it names the unloaded ground");
                    h.assertTrue(
                            level.getBlockState(head).is(MiningModule.TUNNEL_ROOT.get()),
                            "unloaded ground ahead is unknown, not missing: the head lives");
                    h.assertFalse(level.hasChunk(next.x, next.z), "and loaded nothing");
                    h.assertTrue(level.getBlockState(head.east()).is(Blocks.STONE), "and cut nothing");
                    h.assertValueEqual(menu.cells(), 0, "and laid nothing");
                    level.getChunkSource().addRegionTicket(HOLD, next, 0, next);
                    level.getChunkAt(head.east(2));
                    for (int wait = 0;
                            wait <= Tunnelling.RETRY_TICKS
                                    && level.getBlockState(head).is(MiningModule.TUNNEL_ROOT.get());
                            wait++) tick(level, head, entity.getType());
                    h.assertTrue(
                            level.getBlockState(head.east()).is(MiningModule.TUNNEL_ROOT.get()), "loaded, it steps");
                    CompoundTag after = level.getBlockEntity(head.east()).saveWithFullMetadata(level.registryAccess());
                    h.assertValueEqual(after.getInt("cells"), 1, "one cell");
                    h.assertValueEqual(segments(after), 3, "one segment");
                    for (BlockPos cell : BlockPos.betweenClosed(head.offset(-2, -1, -1), head.offset(1, 2, 1))) {
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

    /** The 64th cell is the last: the head then stops with a full-length root, cuts nothing and pays nothing. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void aRootStopsAtSixtyFourCells(GameTestHelper h) {
        Site site = Site.of(h, 11);
        site.plant(Direction.NORTH);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        site.supply(player);
        site.load(tag -> tag.putInt("cells", Tunnelling.MAX_CELLS - 1));
        site.due();
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(0, 0, -1), "the last cell"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.saved().getInt("cells"), Tunnelling.MAX_CELLS, "sixty-four");
                    site.due();
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(player), Optional.of(key(Tunnelling.FULL_LENGTH)), "the root is full length"))
                .thenExecute(() -> {
                    CompoundTag tag = site.saved();
                    h.assertValueEqual(site.head(), site.at(0, 0, -1), "it moves no further");
                    h.assertTrue(site.state(0, 0, -2).is(Blocks.STONE), "and cuts nothing");
                    h.assertValueEqual(tag.getInt("cells"), Tunnelling.MAX_CELLS, "still sixty-four");
                    h.assertValueEqual(segments(tag), SEGMENTS - 1, "and pays nothing");
                    site.release();
                })
                .thenSucceed();
    }

    /** Segments past sixteen stay in the hand; a full tank refuses a bucket and the bucket stays full. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theHeadHoldsSixteenSegmentsAndTwoBucketsAndNoMore(GameTestHelper h) {
        Site site = Site.of(h, 12);
        site.plant(Direction.NORTH);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        site.feed(player, new ItemStack(MiningModule.ROOT_SEGMENT.get(), 20));
        h.assertValueEqual(player.getMainHandItem().getCount(), 4, "sixteen go in, four stay in the hand");
        site.feed(player, new ItemStack(MiningModule.ROOT_SEGMENT.get(), 4));
        h.assertValueEqual(player.getMainHandItem().getCount(), 4, "a full head takes none");
        for (int bucket = 0; bucket < 2; bucket++) site.feed(player, new ItemStack(StorageModule.BIOMASS_BUCKET.get()));
        site.feed(player, new ItemStack(StorageModule.BIOMASS_BUCKET.get()));
        h.assertTrue(player.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "a third bucket stays full");
        CompoundTag tag = site.saved();
        h.assertValueEqual(segments(tag), Tunnelling.MAX_SEGMENTS, "sixteen segments");
        h.assertValueEqual(stored(tag), Tunnelling.BIOMASS_CAPACITY_BU, "two buckets");
        site.release();
        h.succeed();
    }

    /**
     * The screen steers straight, left and right and nothing else; its segment slot takes only segments and its
     * output only gives. No vein, hopper or pipe reaches the head: it has no item, fluid or biomass capability.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theScreenSteersAndNothingButAHandFeedsTheHead(GameTestHelper h) {
        Site site = Site.of(h, 13);
        site.plant(Direction.NORTH);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        TunnelRootMenu menu = site.menu(player);
        h.assertTrue(menu.clickMenuButton(player, TunnelRootMenu.RIGHT_BUTTON), "right");
        h.assertValueEqual(site.menu(player).steer(), Steer.RIGHT, "steered right");
        h.assertTrue(menu.clickMenuButton(player, TunnelRootMenu.STRAIGHT_BUTTON), "straight");
        h.assertValueEqual(site.menu(player).steer(), Steer.STRAIGHT, "steered straight");
        h.assertFalse(menu.clickMenuButton(player, 3), "no fourth way");
        h.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(MiningModule.ROOT_SEGMENT.get())), "segments go in");
        h.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "nothing else");
        for (int slot = 1; slot <= Tunnelling.OUTPUT_SLOTS; slot++)
            h.assertFalse(menu.getSlot(slot).mayPlace(new ItemStack(Items.COBBLESTONE)), "the output only gives");
        for (Direction side : Direction.values()) {
            h.assertTrue(
                    site.level.getCapability(Capabilities.ItemHandler.BLOCK, site.start, side) == null, "no items");
            h.assertTrue(
                    site.level.getCapability(Capabilities.FluidHandler.BLOCK, site.start, side) == null, "no fluid");
            h.assertTrue(site.level.getCapability(BiomassCapabilities.BLOCK, site.start, side) == null, "no biomass");
        }
        site.release();
        h.succeed();
    }

    // ---- saved state ----

    /**
     * A head that has cut a step keeps everything through its chunk being written to disk, unloaded and read back:
     * it is a new block entity with the same supplies, output, cells, steering and steward; it did not work while it
     * was gone; and it carries on from there.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "tunnel_root_unload",
            timeoutTicks = 1200)
    public static void aHeadKeepsItsStateThroughASaveToDiskAndAReload(GameTestHelper h) {
        savedSteering(h, false);
    }

    /** A pre-animation save keeps its NBT steering through an actual chunk save and reload. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "tunnel_root_legacy_unload",
            timeoutTicks = 1200)
    public static void aLegacyHeadKeepsItsSteeringThroughASaveToDiskAndAReload(GameTestHelper h) {
        savedSteering(h, true);
    }

    private static void savedSteering(GameTestHelper h, boolean legacy) {
        Site site = Site.of(h, legacy ? 32 : 14);
        site.plant(Direction.NORTH);
        ServerPlayer steward = player(h);
        site.supply(steward);
        site.set(0, 0, -1, Blocks.COAL_ORE);
        site.due();
        BlockEntity[] before = new BlockEntity[1];
        CompoundTag[] saved = new CompoundTag[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(0, 0, -1), "one step"))
                .thenExecute(() -> {
                    site.menu(steward).clickMenuButton(steward, TunnelRootMenu.RIGHT_BUTTON);
                    before[0] = site.entity();
                    saved[0] = site.saved();
                    if (legacy) {
                        saved[0].putInt("steer", Steer.RIGHT.ordinal());
                        site.level.setBlock(
                                site.head(),
                                site.level.getBlockState(site.head()).setValue(TunnelRootBlock.STEER, Stride.STRAIGHT),
                                Block.UPDATE_CLIENTS);
                        site.entity().loadWithComponents(saved[0], site.level.registryAccess());
                    }
                    site.level.save(null, true, false);
                    site.level.setChunkForced(site.chunk.x, site.chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(site.level.hasChunkAt(site.start), "the chunk is gone"))
                .thenExecute(() -> site.level.save(null, true, false))
                .thenIdle(2 * Tunnelling.RETRY_TICKS)
                .thenExecute(() -> TestChunks.force(site.level, site.chunk))
                .thenWaitUntil(() -> h.assertTrue(
                        site.level.hasChunkAt(site.start) && site.level.getBlockEntity(site.at(0, 0, -1)) != null,
                        "the chunk and the head are back"))
                .thenExecute(() -> {
                    h.assertTrue(site.entity() != before[0], "a new block entity, read from the saved chunk");
                    CompoundTag back = site.saved();
                    for (String key : List.of("segments", "output", "tank", "cells", "steward"))
                        h.assertValueEqual(back.get(key), saved[0].get(key), key + " survives");
                    h.assertValueEqual(site.menu(steward).steer(), Steer.RIGHT, "the steering survives in its state");
                    h.assertValueEqual(output(back), List.of("minecraft:cobblestone 1", "minecraft:coal 1"), "drops");
                    int waited = back.getInt("ticks") - saved[0].getInt("ticks");
                    h.assertTrue(waited >= 0 && waited <= 5, "it did not work while gone: " + waited);
                    site.due();
                })
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.at(1, 0, -1), "it carries on, steered right"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.saved().getInt("cells"), 2, "from where it was");
                    site.release();
                })
                .thenSucceed();
    }

    /** Impossible saved numbers are kept as they were; the head then does nothing and takes nothing. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 3 * STEP_WAIT)
    public static void anUnreadableHeadKeepsItsSavedBytesAndDoesNothing(GameTestHelper h) {
        Site site = Site.of(h, 15);
        site.plant(Direction.NORTH);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        site.supply(player);
        site.load(tag -> {
            tag.putInt("cells", Tunnelling.MAX_CELLS + 1);
            tag.putInt("ticks", Tunnelling.STEP_TICKS);
        });
        h.startSequence()
                .thenIdle(2 * Tunnelling.RETRY_TICKS)
                .thenExecute(() -> {
                    h.assertValueEqual(site.head(), site.start, "it does not move");
                    h.assertTrue(site.state(0, 0, -1).is(Blocks.STONE), "it cuts nothing");
                    site.feed(player, new ItemStack(MiningModule.ROOT_SEGMENT.get(), 2));
                    h.assertValueEqual(player.getMainHandItem().getCount(), 2, "it takes nothing");
                    site.menu(player).clickMenuButton(player, TunnelRootMenu.LEFT_BUTTON);
                    CompoundTag kept = site.saved();
                    h.assertValueEqual(kept.getInt("cells"), Tunnelling.MAX_CELLS + 1, "the saved bytes stay");
                    h.assertValueEqual(segments(kept), SEGMENTS, "all of them");
                    h.assertValueEqual(
                            site.state(0, 0, 0).getValue(TunnelRootBlock.STEER).name(),
                            "STRAIGHT",
                            "unchanged by the screen");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * A broken head comes out whole as one item carrying its supplies, output and cells; nothing spills. Placed on
     * substrate it faces away from the player and has all of it again, so its root can never pass 64 cells.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenHeadComesOutWholeAndKeepsItsRoot(GameTestHelper h) {
        Site site = Site.of(h, 16);
        site.plant(Direction.NORTH);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        site.supply(player);
        site.load(tag -> tag.putInt("cells", 5));
        site.menu(player).clickMenuButton(player, TunnelRootMenu.LEFT_BUTTON);
        CompoundTag held = site.saved();
        List<ItemStack> drops = Block.getDrops(
                site.level.getBlockState(site.start),
                site.level,
                site.start,
                site.entity(),
                player,
                new ItemStack(InteractionModule.SURGICAL_BLADE.get()));
        h.assertValueEqual(drops.size(), 1, "one item, nothing spilled: " + drops);
        ItemStack carried = drops.get(0);
        h.assertTrue(carried.is(MiningModule.TUNNEL_ROOT_ITEM.get()), "the head itself");
        site.set(0, 0, 0, Blocks.AIR);
        site.set(2, 0, 0, Blocks.AIR);
        site.set(2, 1, 0, Blocks.AIR);
        site.set(2, -1, 0, EcologyTests.mature());
        player.setYRot(-90F); // facing east
        player.setItemInHand(InteractionHand.MAIN_HAND, carried);
        carried.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(site.at(2, -1, 0)).add(0, 0.5, 0), Direction.UP, site.at(2, -1, 0), false)));
        BlockState placed = site.state(2, 0, 0);
        h.assertTrue(placed.is(MiningModule.TUNNEL_ROOT.get()), "planted again");
        h.assertValueEqual(placed.getValue(TunnelRootBlock.FACING), Direction.EAST, "facing away from the player");
        CompoundTag again = site.saved();
        for (String key : List.of("segments", "output", "tank", "cells", "steer"))
            h.assertValueEqual(again.get(key), held.get(key), key + " travels on the item");
        h.assertValueEqual(again.getInt("cells"), 5, "the cells it laid");
        site.release();
        h.succeed();
    }

    /** A bud on substrate grows a head from survival materials, facing away from the player who grew it. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBudGrowsAHeadFacingAwayFromTheGrower(GameTestHelper h) {
        Site site = Site.of(h, 17);
        site.set(0, -1, 0, EcologyTests.mature());
        site.level.setBlockAndUpdate(site.start, OrganModule.ORGAN_BUD.get().defaultBlockState());
        Player grower = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        grower.setYRot(-90F); // facing east
        // the pickaxe first: only this growth takes one, so the bud's choice is fixed before the bones
        for (ItemStack material : List.of(
                new ItemStack(Items.STONE_PICKAXE),
                new ItemStack(Items.BONE, 4),
                new ItemStack(Items.ROTTEN_FLESH, 4))) {
            h.assertTrue(site.state(0, 0, 0).is(OrganModule.ORGAN_BUD.get()), "still a bud");
            grower.setItemInHand(InteractionHand.MAIN_HAND, material);
            site.level
                    .getBlockState(site.start)
                    .useItemOn(
                            material,
                            site.level,
                            grower,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(site.start), Direction.UP, site.start, false));
            h.assertTrue(grower.getMainHandItem().isEmpty(), "the bud takes " + material);
        }
        BlockState head = site.state(0, 0, 0);
        h.assertTrue(head.is(MiningModule.TUNNEL_ROOT.get()), "a head has grown");
        h.assertValueEqual(head.getValue(TunnelRootBlock.FACING), Direction.EAST, "facing away from the grower");
        CompoundTag fresh = site.saved();
        h.assertValueEqual(fresh.getInt("cells"), 0, "no cells yet");
        h.assertValueEqual(segments(fresh), 0, "and no supplies");
        site.release();
        h.succeed();
    }

    // ---- discovery ----

    /** The segment recipe makes four from starter materials; the guide teaches growing, segments and recovery. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void segmentsAreCraftedAndTheGuideTeachesTheRoot(GameTestHelper h) {
        ItemStack segments = M1RouteTests.craft(
                h,
                List.of(
                        new ItemStack(ProcessingModule.BONE_PLATE.get()),
                        new ItemStack(Items.ROTTEN_FLESH),
                        new ItemStack(Items.STICK)),
                MiningModule.ROOT_SEGMENT.get());
        h.assertTrue(segments.is(MiningModule.ROOT_SEGMENT.get()) && segments.getCount() == 4, "four segments");
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String path : List.of("acquired/tunnel_root", "contact/tunnel_root", "contact/root_bend"))
            h.assertTrue(advancements.get(id(path)) != null, path + " exists");
        h.assertTrue(
                h.getLevel().getRecipeManager().byKey(id("growth/tunnel_root")).isPresent(), "the growth exists");
        h.succeed();
    }

    // ---- the demo ----

    /** A demo head at {@code x} in the row of stops, and the reason it shows. */
    private record DemoStop(int x, Refusal reason) {}

    /**
     * The demo's Tunnel Root exhibit: a walkable tunnel with a turn and a head ready to cut on, a row of heads each
     * stopped for a named reason, one of which carries on once its output is emptied, two accidents that do not stop
     * a head (water it floods its tunnel with, and a hole it dies over), supplies, and a bud that a stone pickaxe turns
     * into a head.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void theDemoExhibitShowsATunnelWithATurnAndEveryStop(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(new BlockPos(66, -62, -62), new BlockPos(118, -50, -50), 62);
        yard.force(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> yard.build(h))
                .thenIdle(2)
                .thenExecute(() -> {
                    ServerLevel level = h.getLevel();
                    BlockPos head = yard.at(new BlockPos(74, -60, -60));
                    h.assertTrue(level.getBlockState(head).is(MiningModule.TUNNEL_ROOT.get()), "the tunnel's head");
                    h.assertValueEqual(
                            level.getBlockState(head).getValue(TunnelRootBlock.FACING), Direction.EAST, "facing east");
                    for (int z = -55; z >= -60; z--)
                        h.assertTrue(walkable(level, yard.at(new BlockPos(68, -60, z))), "walk north at z " + z);
                    for (int x = 69; x <= 73; x++)
                        h.assertTrue(walkable(level, yard.at(new BlockPos(x, -60, -60))), "walk east at x " + x);
                    h.assertTrue(
                            level.getBlockState(yard.at(new BlockPos(68, -61, -60)))
                                    .getValue(RootCellBlock.BEND),
                            "the bend in the root");
                    TunnelRootMenu menu = (TunnelRootMenu)
                            ((MenuProvider) level.getBlockEntity(head)).createMenu(0, player.getInventory(), player);
                    h.assertValueEqual(menu.cells(), 11, "eleven cells laid");
                    var chest = (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(114, -60, -52)));
                    for (var supply : List.of(
                            MiningModule.ROOT_SEGMENT.get(),
                            StorageModule.BIOMASS_BUCKET.get(),
                            ProcessingModule.BONE_PLATE.get(),
                            Items.STONE_PICKAXE))
                        h.assertTrue(chest != null && chest.countItem(supply) > 0, "the chest holds " + supply);
                })
                .thenWaitUntil(() -> {
                    for (DemoStop stop : List.of(
                            new DemoStop(86, Tunnelling.NO_SEGMENT),
                            new DemoStop(90, Tunnelling.NO_BIOMASS),
                            new DemoStop(94, Tunnelling.OUTPUT_FULL),
                            new DemoStop(98, Tunnelling.TOO_HARD),
                            new DemoStop(102, Tunnelling.NO_FLOOR))) {
                        BlockPos at = yard.at(new BlockPos(stop.x(), -60, -52));
                        TunnelRootMenu menu =
                                (TunnelRootMenu) ((MenuProvider) h.getLevel().getBlockEntity(at))
                                        .createMenu(0, player.getInventory(), player);
                        h.assertValueEqual(
                                menu.stopKey(), Optional.of(key(stop.reason())), "the head at x " + stop.x());
                    }
                })
                .thenExecute(() -> {
                    BlockPos full = yard.at(new BlockPos(94, -60, -52));
                    TunnelRootMenu menu =
                            (TunnelRootMenu) ((MenuProvider) h.getLevel().getBlockEntity(full))
                                    .createMenu(0, player.getInventory(), player);
                    menu.quickMoveStack(player, 1);
                    BlockPos bud = yard.at(new BlockPos(118, -60, -52));
                    player.setYRot(180F);
                    ItemStack pick = new ItemStack(Items.STONE_PICKAXE);
                    player.setItemInHand(InteractionHand.MAIN_HAND, pick);
                    h.getLevel()
                            .getBlockState(bud)
                            .useItemOn(
                                    pick,
                                    h.getLevel(),
                                    player,
                                    InteractionHand.MAIN_HAND,
                                    new BlockHitResult(Vec3.atCenterOf(bud), Direction.UP, bud, false));
                    h.assertTrue(
                            h.getLevel().getBlockState(bud).is(MiningModule.TUNNEL_ROOT.get()),
                            "the fed bud grows a head");
                })
                .thenWaitUntil(() -> {
                    ServerLevel level = h.getLevel();
                    h.assertTrue(
                            level.getBlockState(yard.at(new BlockPos(94, -60, -53)))
                                    .is(MiningModule.TUNNEL_ROOT.get()),
                            "emptied, the stopped head cuts on");
                    h.assertTrue(
                            level.getBlockState(yard.at(new BlockPos(106, -60, -53)))
                                    .is(MiningModule.TUNNEL_ROOT.get()),
                            "the head beside the water cut into it");
                    h.assertTrue(
                            level.getBlockState(yard.at(new BlockPos(107, -59, -53)))
                                    .is(Blocks.WATER),
                            "with the water still beside its cut, which stopped nothing");
                    for (int z = -52; z >= -54; z--)
                        h.assertFalse(
                                level.getBlockState(yard.at(new BlockPos(110, -60, z)))
                                        .is(MiningModule.TUNNEL_ROOT.get()),
                                "the head over the hole crawled onto nothing and died");
                    h.assertTrue(
                            level.getBlockState(yard.at(new BlockPos(110, -59, -53)))
                                    .isAir(),
                            "after its cut");
                })
                .thenExecute(() -> {
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }
}
