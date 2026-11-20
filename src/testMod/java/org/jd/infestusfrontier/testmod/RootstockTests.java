package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import com.klikli_dev.modonomicon.book.page.BookCraftingRecipePage;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Collecting;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Rootstocking;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.core.mining.Turn;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.mining.RootCellBlock;
import org.jd.infestusfrontier.mining.TunnelRootMenu;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * MINE-3, the Descending Rootstock: a Tunnel Root head with Stairs set before an approved staircase, with a Fracture
 * Jaw and its Work Bed behind it, Collection Cilia on a Mouth sweeping the bed and a Structure Grower on the marker. A
 * bladder feeds the jaw and the cilia through a Vascular Junction; Item Veins carry the cut to filtered Intake Mouths
 * for raw iron and cobblestone, and carry Root Segments and stairs from a stock chest to a hopper that feeds the
 * grower. The tests prove a whole section, the deepest section, every pause and its recovery, unloaded ground,
 * rebuilding and a save to disk, the demo's exhibit and the guide. Each rig stands in a yard of its own in a forced
 * chunk, far from the test structures; the heads face north, so a plan's forward is north and its right east.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class RootstockTests {
    private static final int TOP = 100;
    /** Ticks one staircase block may take: a jaw's cut, a sweep and the grower's look, with a margin. */
    private static final int BLOCK_TICKS = Fracturing.CUT_TICKS + Collecting.SWEEP_TICKS + Surveying.GROW_TICKS;
    /** Ticks the minehead takes to look again and name a change. */
    private static final int LOOK = 2 * Rootstocking.CHECK_TICKS;

    private static final TicketType<ChunkPos> HOLD =
            TicketType.create("infestusfrontier_rootstock_test", Comparator.comparingLong(ChunkPos::toLong));

    // ---- the rig, relative to the head ----

    static final BlockPos HEAD = BlockPos.ZERO;
    static final BlockPos MARKER = new BlockPos(0, -1, -1);
    static final BlockPos GROWER = new BlockPos(0, 0, -1);
    static final BlockPos JAW = new BlockPos(0, 0, 1);
    static final BlockPos BED = new BlockPos(0, 0, 2);
    static final BlockPos CILIA = new BlockPos(1, 0, 2);
    static final BlockPos BLADDER = new BlockPos(2, 0, 1);
    static final BlockPos ORE_CHEST = new BlockPos(7, 0, 3);
    static final BlockPos SPOIL_CHEST = new BlockPos(5, 0, 4);
    static final BlockPos STOCK_CHEST = new BlockPos(3, 0, 4);
    static final BlockPos HOPPER = new BlockPos(1, 0, -1);

    private static final String HEAD_BLOCK =
            "infestusfrontier:tunnel_root[facing=north]{segments:{},output:[{},{},{},{}],"
                    + "tank:{stored:0,capacity:2000},cells:0,ticks:0,steer:0,mutations:[\"stairs\"]}";
    private static final String JAW_BLOCK =
            "infestusfrontier:fracture_jaw[facing=north]{tank:{stored:0,capacity:2000},cuts:0,ticks:0}";
    private static final String CILIA_BLOCK =
            "infestusfrontier:collection_cilia[facing=west]{tank:{stored:0,capacity:1000},sent:0}";
    private static final String GROWER_BLOCK =
            "infestusfrontier:structure_grower{segments:{},treads:{},lights:{},steps:0,ticks:0}";
    private static final String SUBSTRATE = "infestusfrontier:living_substrate[stage=mature]";

    private record Site(GameTestHelper h, ServerLevel level, BlockPos start, int depth) {
        private static final int REACH = 9;

        /** A yard for a staircase {@code depth} deep; its chunks are forced while the test runs. */
        static Site of(GameTestHelper h, int slot, int depth) {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            BlockPos start =
                    new BlockPos((origin.getX() & ~15) + 280008, TOP, (origin.getZ() & ~15) + 310408 + slot * 64);
            Site site = new Site(h, h.getLevel(), start, depth);
            for (ChunkPos chunk : site.chunks()) TestChunks.force(site.level, chunk);
            site.clear();
            return site;
        }

        List<ChunkPos> chunks() {
            List<ChunkPos> chunks = new ArrayList<>();
            for (int z = start.getZ() - depth - REACH; z <= start.getZ() + REACH + 15; z += 16) {
                ChunkPos chunk =
                        new ChunkPos(new BlockPos(start.getX(), start.getY(), Math.min(z, start.getZ() + REACH)));
                if (!chunks.contains(chunk)) chunks.add(chunk);
            }
            return chunks;
        }

        BlockPos low() {
            return start.offset(-4, -depth - 6, -depth - REACH);
        }

        BlockPos high() {
            return start.offset(REACH, 6, REACH);
        }

        void clear() {
            for (BlockPos cell : BlockPos.betweenClosed(low(), high())) {
                level.removeBlockEntity(cell);
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            for (var entity :
                    level.getEntities((net.minecraft.world.entity.Entity) null, yard(), e -> !(e instanceof Player)))
                entity.discard();
        }

        void release() {
            clear();
            for (ChunkPos chunk : chunks()) level.setChunkForced(chunk.x, chunk.z, false);
        }

        AABB yard() {
            return AABB.encapsulatingFullBlocks(low(), high());
        }

        int loose() {
            return level.getEntitiesOfClass(ItemEntity.class, yard()).size();
        }

        BlockPos at(BlockPos offset) {
            return start.offset(offset);
        }

        BlockPos at(int dx, int dy, int dz) {
            return start.offset(dx, dy, dz);
        }

        /** Runs a vanilla command at the yard; a command that fails fails the test. */
        void run(String command) {
            CommandSourceStack source = level.getServer()
                    .createCommandSourceStack()
                    .withLevel(level)
                    .withSuppressedOutput();
            try {
                level.getServer().getCommands().getDispatcher().execute(command, source);
            } catch (CommandSyntaxException e) {
                throw new IllegalStateException(command + ": " + e.getMessage(), e);
            }
        }

        void set(BlockPos offset, String block) {
            BlockPos at = at(offset);
            run("setblock %d %d %d %s".formatted(at.getX(), at.getY(), at.getZ(), block));
        }

        void fill(BlockPos from, BlockPos to, String block) {
            BlockPos a = at(from);
            BlockPos b = at(to);
            run("fill %d %d %d %d %d %d %s"
                    .formatted(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), block));
        }

        void remove(BlockPos offset) {
            level.removeBlockEntity(at(offset));
            level.setBlockAndUpdate(at(offset), Blocks.AIR.defaultBlockState());
        }

        /**
         * Stone for the staircase, as high as {@code hill} blocks over the marker's level, and the whole supplied
         * minehead: {@code bladder} BU for the jaw and cilia, and {@code segments} and {@code treads} in the stock
         * chest, with the torches and Bone Plates the plan's lights and ribs take.
         */
        void rig(int hill, int bladder, int segments, int treads) {
            fill(new BlockPos(-2, -depth - 3, -depth - 3), new BlockPos(2, hill - 1, -2), "stone");
            set(new BlockPos(0, -1, 0), SUBSTRATE);
            set(new BlockPos(0, -1, 1), SUBSTRATE);
            set(new BlockPos(0, -1, 2), SUBSTRATE);
            set(
                    MARKER,
                    ("infestusfrontier:surveyed_tissue[facing=north]{shape:0,length:%d,first_way:0,first_at:0,"
                                    + "second_way:0,second_at:0,lit:0b,approved:1b,built:0}")
                            .formatted(depth));
            set(GROWER, GROWER_BLOCK);
            set(HEAD, HEAD_BLOCK);
            set(JAW, JAW_BLOCK);
            set(BED, "infestusfrontier:work_bed");
            set(new BlockPos(1, -1, 2), "infestusfrontier:mouth[mode=output]");
            set(CILIA, CILIA_BLOCK);
            set(new BlockPos(1, -1, 1), SUBSTRATE);
            set(new BlockPos(2, -1, 1), SUBSTRATE);
            set(new BlockPos(1, 0, 1), "infestusfrontier:vascular_junction[east=intake,west=outlet,south=outlet]");
            set(
                    BLADDER,
                    "infestusfrontier:biomass_bladder{tank:{stored:%d,reserved:0,capacity:4000}}".formatted(bladder));
            fill(new BlockPos(1, -1, 3), new BlockPos(6, -1, 3), "infestusfrontier:item_vein");
            set(new BlockPos(7, -1, 3), filtered("intake", "minecraft:raw_iron"));
            set(ORE_CHEST, "chest[facing=north]");
            set(new BlockPos(5, -1, 4), filtered("intake", "minecraft:cobblestone"));
            set(SPOIL_CHEST, "chest[facing=north]");
            set(new BlockPos(3, -1, 4), "infestusfrontier:mouth[mode=output]");
            List<String> stock = new ArrayList<>();
            if (segments > 0) stock.add("{Slot:0,id:\"infestusfrontier:root_segment\",count:%d}".formatted(segments));
            if (treads > 0) stock.add("{Slot:1,id:\"minecraft:cobblestone_stairs\",count:%d}".formatted(treads));
            stock.add("{Slot:2,id:\"minecraft:torch\",count:%d}"
                    .formatted(plan().needs().lights()));
            stock.add("{Slot:3,id:\"infestusfrontier:bone_plate\",count:%d}"
                    .formatted(plan().needs().ribs()));
            set(STOCK_CHEST, "chest[facing=north]{Items:[%s]}".formatted(String.join(",", stock)));
            fill(new BlockPos(2, -1, -1), new BlockPos(3, -1, -1), "infestusfrontier:item_vein");
            fill(new BlockPos(3, -1, 0), new BlockPos(3, -1, 2), "infestusfrontier:item_vein");
            set(
                    new BlockPos(1, -1, -1),
                    filtered(
                            "intake",
                            "infestusfrontier:root_segment",
                            "minecraft:cobblestone_stairs",
                            "minecraft:torch",
                            "infestusfrontier:bone_plate"));
            set(HOPPER, "hopper[facing=west]");
        }

        static String filtered(String mode, String... items) {
            List<String> kinds = new ArrayList<>();
            for (String item : items) kinds.add("{id:\"%s\",count:1}".formatted(item));
            return "infestusfrontier:mouth[mode=%s]{filter:[%s]}".formatted(mode, String.join(",", kinds));
        }

        /** The staircase the marker holds, as approved. */
        MinePlan plan() {
            return new MinePlan(PlanShape.STAIRCASE, depth, Turn.NONE, Turn.NONE);
        }

        /** Where {@code offset} of the plan lies. */
        BlockPos plan(Offset offset) {
            return at(MARKER)
                    .relative(Direction.NORTH, offset.forward())
                    .relative(Direction.EAST, offset.right())
                    .above(offset.up());
        }

        /** How many blocks of the plan's steps from {@code from} on, counted from 1, are {@code block}. */
        int count(Block block, int from) {
            int found = 0;
            for (PlanStep step : plan().steps().subList(from - 1, plan().steps().size()))
                for (Offset cell : step.cleared())
                    if (level.getBlockState(plan(cell)).is(block)) found++;
            return found;
        }

        BlockEntity entity(BlockPos offset) {
            return level.getBlockEntity(at(offset));
        }

        CompoundTag saved(BlockPos offset) {
            return entity(offset).saveWithFullMetadata(level.registryAccess());
        }

        TunnelRootMenu menu(Player player) {
            return RootstockTests.menu(h, level, at(HEAD), player);
        }

        Optional<String> status(Player player) {
            return menu(player).stopKey();
        }

        int built() {
            return saved(MARKER).getInt("built");
        }

        /** How many {@code item} the container at {@code offset} holds. */
        int held(BlockPos offset, Item item) {
            return entity(offset) instanceof Container container ? container.countItem(item) : -1;
        }

        /** How many {@code item} the grower holds in its stock. */
        int stocked(Item item) {
            IItemHandler stock = level.getCapability(Capabilities.ItemHandler.BLOCK, at(GROWER), null);
            int found = 0;
            for (int slot = 0; slot < stock.getSlots(); slot++)
                if (stock.getStackInSlot(slot).is(item))
                    found += stock.getStackInSlot(slot).getCount();
            return found;
        }

        /** Items the Work Bed holds now. */
        int bed() {
            return saved(BED).getList("slots", 10).stream()
                    .mapToInt(slot -> ((CompoundTag) slot).getInt("count"))
                    .sum();
        }

        void feed(BlockPos offset, int units) {
            var handler = level.getCapability(BiomassCapabilities.BLOCK, at(offset), null);
            if (handler == null
                    || !(handler.insert(Biomass.of(units), false)
                            instanceof org.jd.infestusfrontier.core.foundation.Outcome.Done<?>))
                throw new IllegalStateException("could not feed " + offset);
        }

        /** Makes {@code player} the steward of the minehead's organs, as placing them does. */
        void steward(Player player) {
            for (BlockPos organ : List.of(HEAD, GROWER, JAW, CILIA)) Organs.placedBy(level, at(organ), player);
        }

        /** Asserts every built step of the plan: a root cell, a tread facing back up on it, three clear blocks over. */
        void assertBuilt(int steps) {
            for (PlanStep step : plan().steps().subList(0, steps)) {
                BlockState root = level.getBlockState(plan(step.root()));
                h.assertTrue(root.is(MiningModule.ROOT_CELL.get()), "step " + step.number() + ": a root cell");
                h.assertFalse(root.getValue(RootCellBlock.BEND), "a straight root");
                Offset floor = step.root();
                if (step.tread().isPresent()) {
                    BlockState tread = level.getBlockState(plan(step.tread().get()));
                    h.assertTrue(tread.is(Blocks.COBBLESTONE_STAIRS), "step " + step.number() + ": a tread");
                    h.assertValueEqual(tread.getValue(StairBlock.FACING), Direction.SOUTH, "it climbs back up");
                    floor = step.tread().get();
                }
                for (int up = 1; up <= Surveying.STAIR_HEADROOM; up++)
                    h.assertTrue(
                            open(level, plan(floor.above(up))),
                            "step " + step.number() + ": clear " + up + " over its floor");
            }
        }

        /** A path a zombie would walk from {@code from}, standing, to {@code to}; it must reach it. */
        Path walk(BlockPos from, BlockPos to) {
            Zombie walker = new Zombie(EntityType.ZOMBIE, level);
            walker.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(96);
            walker.moveTo(Vec3.atBottomCenterOf(from));
            walker.setOnGround(true);
            Path path = walker.getNavigation().createPath(to, 0);
            walker.discard();
            return path;
        }

        /** Where a visitor stands on the landing. */
        BlockPos landing() {
            return plan(plan().end().above(1));
        }
    }

    /** The screen of the Rootstock at {@code head}; a missing Rootstock fails the test. */
    static TunnelRootMenu menu(GameTestHelper h, ServerLevel level, BlockPos head, Player player) {
        h.assertTrue(level.getBlockEntity(head) instanceof MenuProvider, "a Rootstock at " + head);
        return (TunnelRootMenu)
                ((MenuProvider) level.getBlockEntity(head)).createMenu(0, player.getInventory(), player);
    }

    /**
     * True for a passage block a player moves through: air, or what a grower grows in a passage with no collision, its
     * ribs, scars and lights. Fluid does not count as open here.
     */
    static boolean open(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty()
                && state.getCollisionShape(level, pos).isEmpty();
    }

    private static String key(Refusal stop) {
        return "infestusfrontier.refusal." + stop.key();
    }

    // ---- a whole section ----

    /**
     * A supplied minehead over a four-deep staircase with two iron ores in it, started and left alone. The jaw cuts
     * every block of each step top down, the cilia send each cut to storage and the grower builds the step behind it;
     * it ends at the landing and says so. Every stone is one cobblestone in the spoil chest and every ore one raw iron
     * in the ore chest; the bed is empty and nothing fell loose. Each organ's history counts its own work, the
     * Rootstock's its steps; the segments and stairs paid came from the stock chest through the veins and hopper, once
     * each. A zombie can walk from the surface to the landing. The steward earns the step and the landing.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 40 * BLOCK_TICKS)
    public static void aSuppliedMineheadCutsExportsAndBuildsAWalkableStaircaseOnItsOwn(GameTestHelper h) {
        Site site = Site.of(h, 0, 4);
        site.rig(0, 3000, 16, 16);
        // two ores: the second step's root cell and the third step's tread
        site.level.setBlockAndUpdate(site.plan(site.plan().steps().get(1).root()), Blocks.IRON_ORE.defaultBlockState());
        site.level.setBlockAndUpdate(
                site.plan(site.plan().steps().get(2).tread().orElseThrow()), Blocks.IRON_ORE.defaultBlockState());
        int stone = site.count(Blocks.STONE, 1);
        int ore = site.count(Blocks.IRON_ORE, 1);
        int steps = site.plan().steps().size();
        ServerPlayer steward = player(h);
        site.steward(steward);
        PendingCredits credits = PendingCredits.get(site.level);
        h.startSequence()
                .thenExecute(() -> {
                    h.assertValueEqual(stone, 16, "sixteen stones to cut");
                    h.assertValueEqual(ore, 2, "and two ores");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(steward), Optional.of(key(Rootstocking.FINISHED)), "it reaches the landing"))
                .thenWaitUntil(() -> {
                    h.assertValueEqual(site.held(SPOIL_CHEST, Items.COBBLESTONE), stone, "every stone in storage");
                    h.assertValueEqual(site.held(ORE_CHEST, Items.RAW_IRON), ore, "every ore in storage");
                })
                .thenExecute(() -> {
                    site.assertBuilt(steps);
                    h.assertValueEqual(site.built(), steps, "the marker counts every step once");
                    h.assertValueEqual(site.bed(), 0, "the bed is empty");
                    h.assertValueEqual(site.held(SPOIL_CHEST, Items.RAW_IRON), 0, "no ore with the spoil");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    h.assertValueEqual(site.saved(JAW).getInt("cuts"), stone + ore, "the jaw cut each block once");
                    h.assertValueEqual(site.saved(CILIA).getInt("sent"), stone + ore, "the cilia sent each once");
                    h.assertValueEqual(site.saved(GROWER).getInt("steps"), steps, "the grower built each step");
                    CompoundTag head = site.saved(HEAD);
                    h.assertValueEqual(head.getInt("descended"), steps, "the Rootstock advanced each step once");
                    h.assertValueEqual(head.getInt("cells"), 0, "it took no step by hand");
                    h.assertTrue(
                            site.level.getBlockState(site.at(HEAD)).is(MiningModule.TUNNEL_ROOT.get()),
                            "and stayed at the minehead");
                    h.assertValueEqual(site.menu(steward).descended(), steps, "its screen counts them");
                    int segments = site.held(STOCK_CHEST, MiningModule.ROOT_SEGMENT.get())
                            + site.held(HOPPER, MiningModule.ROOT_SEGMENT.get())
                            + site.stocked(MiningModule.ROOT_SEGMENT.get());
                    int treads = site.held(STOCK_CHEST, Items.COBBLESTONE_STAIRS)
                            + site.held(HOPPER, Items.COBBLESTONE_STAIRS)
                            + site.stocked(Items.COBBLESTONE_STAIRS);
                    h.assertValueEqual(segments, 16 - steps, "a segment paid for each step");
                    h.assertValueEqual(treads, 16 - (steps - 1), "a tread for each step but the landing");
                    Path path = site.walk(site.at(-1, 0, -2), site.landing());
                    h.assertTrue(path != null && path.canReach(), "a zombie walks down to the landing");
                    h.assertValueEqual(path.getTarget(), site.landing(), "right to it");
                    credits.award(steward);
                    h.assertTrue(done(h, steward, "contact/descending_rootstock"), "a step on its own");
                    h.assertTrue(done(h, steward, "contact/staircase_landing"), "and the landing");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * The deepest section, 32 down. Its first thirty steps stand built, as an earlier run left them; the minehead cuts
     * and builds the last two and the landing 32 blocks below the marker. Every tread has three clear blocks over it,
     * and a zombie walks from the surface down all 32 to the landing.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 20 * BLOCK_TICKS)
    public static void theDeepestSectionDescendsThirtyTwoBlocksToAWalkableLanding(GameTestHelper h) {
        Site site = Site.of(h, 1, Surveying.MAX_DEPTH);
        site.rig(0, 3000, 8, 8);
        int prebuilt = Surveying.MAX_DEPTH - 2;
        for (PlanStep step : site.plan().steps().subList(0, prebuilt)) {
            for (Offset cell : step.passage())
                site.level.setBlock(site.plan(cell), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            site.level.setBlock(site.plan(step.root()), straightRoot(), Block.UPDATE_CLIENTS);
            site.level.setBlock(
                    site.plan(step.tread().orElseThrow()),
                    Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH),
                    Block.UPDATE_CLIENTS);
        }
        CompoundTag marker = site.saved(MARKER);
        marker.putInt("built", prebuilt);
        site.entity(MARKER).loadWithComponents(marker, site.level.registryAccess());
        int stone = site.count(Blocks.STONE, prebuilt + 1);
        Player player = EcologyTests.holding(h, net.minecraft.world.level.GameType.SURVIVAL, Items.AIR, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player), Optional.of(key(Rootstocking.FINISHED)), "it reaches the landing"))
                .thenWaitUntil(() ->
                        h.assertValueEqual(site.held(SPOIL_CHEST, Items.COBBLESTONE), stone, "all of it in storage"))
                .thenExecute(() -> {
                    h.assertValueEqual(stone, 14, "two treads and the landing to cut");
                    site.assertBuilt(Surveying.MAX_DEPTH + 1);
                    h.assertValueEqual(
                            site.plan(site.plan().end()).getY(),
                            site.at(MARKER).getY() - Surveying.MAX_DEPTH,
                            "the landing is 32 below the marker");
                    h.assertValueEqual(site.saved(HEAD).getInt("descended"), 3, "three steps advanced");
                    Path path = site.walk(site.at(-1, 0, -2), site.landing());
                    h.assertTrue(path != null && path.canReach(), "a zombie walks all the way down");
                    h.assertValueEqual(path.getTarget(), site.landing(), "to the landing");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    private static BlockState straightRoot() {
        return MiningModule.ROOT_CELL
                .get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
    }

    // ---- pauses and recovery ----

    /**
     * A two-deep minehead set up without its jaw and bed, an empty bladder and an empty stock chest. Each missing part
     * or supply pauses it with its own reason while the organs that are there do only their own work: with no jaw
     * nothing is cut, with no bladder the jaw waits for biomass, without segments or stairs the cut step stays unbuilt,
     * without cilia the bed is not emptied, without a grower nothing is built. Each fix resumes it, and the section
     * ends with every cut in storage once.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 60 * BLOCK_TICKS)
    public static void missingPartsAndSuppliesPauseWithTheirReasonAndRepairResumes(GameTestHelper h) {
        Site site = Site.of(h, 2, 2);
        site.rig(0, 0, 0, 0);
        site.remove(JAW);
        site.remove(BED);
        int stone = site.count(Blocks.STONE, 1);
        PlanStep first = site.plan().steps().get(0);
        BlockPos root = site.plan(first.root());
        ServerPlayer player = player(h);
        site.steward(player);
        Player hand = EcologyTests.holding(h, net.minecraft.world.level.GameType.SURVIVAL, Items.AIR, 0);
        ItemStack[] away = new ItemStack[2];
        h.startSequence()
                .thenWaitUntil(
                        () -> h.assertValueEqual(site.status(player), Optional.of(key(Rootstocking.NO_JAW)), "no jaw"))
                .thenIdle(Fracturing.CUT_TICKS)
                .thenExecute(() -> {
                    h.assertTrue(site.level.getBlockState(root).is(Blocks.STONE), "nothing cuts in its place");
                    site.set(JAW, JAW_BLOCK.replace("ticks:0", "ticks:" + Fracturing.CUT_TICKS));
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player), Optional.of(key(Rootstocking.NO_BED)), "no bed behind the jaw"))
                .thenExecute(() -> site.set(BED, "infestusfrontier:work_bed"))
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player), Optional.of(key(Fracturing.NO_BIOMASS)), "the jaw has no biomass"))
                .thenExecute(() -> {
                    h.assertTrue(site.level.getBlockState(root).is(Blocks.STONE), "and cuts nothing");
                    site.feed(BLADDER, 3000); // enough to fill the jaw and the cilia
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player),
                        Optional.of(key(Surveying.NO_SEGMENT)),
                        "fed, it cuts the first step; the grower has no segment"))
                .thenExecute(() -> {
                    h.assertTrue(site.level.getBlockState(root).isAir(), "the first step is cut");
                    h.assertValueEqual(site.built(), 0, "and nothing builds it but the grower");
                    ((Container) site.entity(STOCK_CHEST))
                            .setItem(0, new ItemStack(MiningModule.ROOT_SEGMENT.get(), 4));
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player), Optional.of(key(Surveying.NO_TREAD)), "segments arrive; no stairs"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.built(), 0, "still unbuilt");
                    away[0] = takeAway(h, site, hand, CILIA);
                })
                .thenWaitUntil(() ->
                        h.assertValueEqual(site.status(player), Optional.of(key(Rootstocking.NO_CILIA)), "no cilia"))
                .thenExecute(() -> {
                    hand.setYRot(90); // facing west, at the bed
                    putBack(h, site, hand, away[0], CILIA, new BlockPos(1, -1, 2));
                    Organs.placedBy(site.level, site.at(CILIA), player);
                    away[1] = takeAway(h, site, hand, GROWER);
                })
                .thenWaitUntil(() ->
                        h.assertValueEqual(site.status(player), Optional.of(key(Rootstocking.NO_GROWER)), "no grower"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.built(), 0, "nothing built without it");
                    h.assertTrue(
                            site.level.getBlockState(site.plan(first.root())).isAir(), "no root cell either");
                    putBack(h, site, hand, away[1], GROWER, MARKER);
                    Organs.placedBy(site.level, site.at(GROWER), player);
                    ((Container) site.entity(STOCK_CHEST)).setItem(1, new ItemStack(Items.COBBLESTONE_STAIRS, 4));
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player), Optional.of(key(Rootstocking.FINISHED)), "it goes on to the landing"))
                .thenWaitUntil(() ->
                        h.assertValueEqual(site.held(SPOIL_CHEST, Items.COBBLESTONE), stone, "every cut in storage"))
                .thenExecute(() -> {
                    site.assertBuilt(site.plan().steps().size());
                    h.assertValueEqual(site.saved(JAW).getInt("cuts"), stone, "each block cut once");
                    h.assertValueEqual(site.saved(GROWER).getInt("steps"), 3, "the grower kept its work");
                    h.assertValueEqual(site.bed(), 0, "the bed is empty");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * Storage is full and so is the Work Bed: the minehead names the full storage and nothing is cut, dropped or lost.
     * Emptied, the cilia send the bed's load on, the jaw cuts again and the section ends with every cut in storage.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 120 * BLOCK_TICKS)
    public static void fullStoragePausesTheMineheadUntilItIsEmptied(GameTestHelper h) {
        Site site = Site.of(h, 3, 1);
        site.rig(0, 3000, 4, 4);
        int stone = site.count(Blocks.STONE, 1);
        Container spoil = (Container) site.entity(SPOIL_CHEST);
        for (int slot = 0; slot < spoil.getContainerSize(); slot++)
            spoil.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        CompoundTag bed = site.saved(BED);
        net.minecraft.nbt.ListTag slots = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++)
            slots.add(new ItemStack(Items.COBBLESTONE, 64).save(site.level.registryAccess()));
        bed.put("slots", slots);
        site.entity(BED).loadWithComponents(bed, site.level.registryAccess());
        int load = Fracturing.BED_SLOTS * 64;
        Player player = EcologyTests.holding(h, net.minecraft.world.level.GameType.SURVIVAL, Items.AIR, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player), Optional.of(key(Collecting.STORAGE_FULL)), "storage is full"))
                .thenIdle(Fracturing.CUT_TICKS + Fracturing.RETRY_TICKS)
                .thenExecute(() -> {
                    h.assertValueEqual(site.bed(), load, "the bed keeps its load");
                    h.assertValueEqual(site.count(Blocks.STONE, 1), stone, "no stone cut");
                    h.assertValueEqual(site.loose(), 0, "nothing dropped");
                    spoil.clearContent();
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(player), Optional.of(key(Rootstocking.FINISHED)), "emptied, it finishes"))
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.held(SPOIL_CHEST, Items.COBBLESTONE), load + stone, "the load and every cut, once"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.bed(), 0, "the bed is empty");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * Water beside the first step and a cavity under the second step's root cell pause nothing: they are accidents. The
     * minehead cuts on and names no reason; the water runs into the stairs; over the cavity the grower lays neither
     * root cell nor tread and pays for neither, so nothing bridges the gap; and the section goes on to its landing.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 40 * BLOCK_TICKS)
    public static void hazardsAreAccidentsTheMineheadCutsIntoWithoutABridge(GameTestHelper h) {
        Site site = Site.of(h, 4, 3);
        site.rig(4, 3000, 4, 4); // a hillside: the first step is in the rock up to its headroom
        List<PlanStep> steps = site.plan().steps();
        BlockPos beside = site.plan(steps.get(0).tread().orElseThrow()).east();
        BlockPos root = site.plan(steps.get(1).root());
        BlockPos tread = site.plan(steps.get(1).tread().orElseThrow());
        site.level.setBlockAndUpdate(beside, Blocks.WATER.defaultBlockState());
        site.level.setBlockAndUpdate(root.below(), Blocks.AIR.defaultBlockState());
        site.level.setBlockAndUpdate(root.below(2), Blocks.AIR.defaultBlockState());
        Player player = EcologyTests.holding(h, net.minecraft.world.level.GameType.SURVIVAL, Items.AIR, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.built(), 2, "two steps built, the second over the gap"))
                .thenExecute(() -> {
                    h.assertFalse(site.level.getFluidState(beside).isEmpty(), "the water is still there");
                    h.assertFalse(
                            site.level.getBlockState(root).is(MiningModule.ROOT_CELL.get()),
                            "no root cell over the cavity");
                    h.assertFalse(
                            site.level.getBlockState(tread).getBlock() instanceof StairBlock,
                            "and no tread floating on nothing");
                    h.assertTrue(site.level.getBlockState(root.below()).isAir(), "the cavity stays open");
                    h.assertValueEqual(
                            site.stocked(MiningModule.ROOT_SEGMENT.get())
                                    + site.held(STOCK_CHEST, MiningModule.ROOT_SEGMENT.get()),
                            3,
                            "one segment paid, for the first step only");
                })
                .thenWaitUntil(() ->
                        h.assertValueEqual(site.built(), site.plan().steps().size(), "the landing"))
                .thenExecute(() -> {
                    h.assertTrue(
                            site.level
                                    .getBlockState(site.plan(steps.get(3).root()))
                                    .is(MiningModule.ROOT_CELL.get()),
                            "the landing has its root cell");
                    h.assertTrue(
                            site.status(player).isEmpty()
                                    || site.status(player).equals(Optional.of(key(Rootstocking.FINISHED))),
                            "no hazard was ever a reason: " + site.status(player));
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * The next step lies in a chunk that is not loaded: the minehead names it, the jaw cuts nothing and nothing is
     * loaded. Once the chunk is there the jaw cuts at the Rootstock's order. Only the minehead's chunk is held and
     * nothing ticks it, so the test ticks the head and the jaw as the server ticks them in a ticking chunk.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void unloadedGroundPausesTheMineheadUntilItLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        // z ends in 1 and the stairs run north: the first step is in the next chunk
        BlockPos head = new BlockPos((origin.getX() & ~15) + 296008, TOP, (origin.getZ() & ~15) + 296001);
        ChunkPos home = new ChunkPos(head);
        ChunkPos next = new ChunkPos(head.north(2));
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.getChunkSource().addRegionTicket(HOLD, home, 0, home);
        level.getChunkSource().addRegionTicket(HOLD, next, 0, next);
        level.getChunkAt(head);
        level.getChunkAt(head.north(2));
        for (BlockPos cell : BlockPos.betweenClosed(head.offset(-1, -3, -3), head.offset(2, 3, 3))) {
            level.removeBlockEntity(cell);
            level.setBlock(cell, Blocks.STONE.defaultBlockState(), quiet);
        }
        for (BlockPos cell : BlockPos.betweenClosed(head.offset(-1, 0, -1), head.offset(2, 3, 3)))
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), quiet);
        BlockState vein = LogisticsModule.BIOMASS_VEIN.get().defaultBlockState();
        level.setBlock(head.below(), MiningModule.ROOT_CELL.get().defaultBlockState(), quiet);
        level.setBlock(head.south().below(), vein, quiet);
        level.setBlock(head.south(2).below(), vein, quiet);
        level.setBlock(
                head.north().below(),
                MiningModule.SURVEYED_TISSUE
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                quiet);
        level.setBlock(head.north(), MiningModule.STRUCTURE_GROWER.get().defaultBlockState(), quiet);
        level.setBlock(
                head,
                MiningModule.TUNNEL_ROOT
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                quiet);
        level.setBlock(
                head.south(),
                MiningModule.FRACTURE_JAW
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
                quiet);
        level.setBlock(head.south(2), MiningModule.WORK_BED.get().defaultBlockState(), quiet);
        level.setBlock(
                head.south(2).east(),
                MiningModule.COLLECTION_CILIA
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
                quiet);
        BlockEntity marker = level.getBlockEntity(head.north().below());
        CompoundTag plan = marker.saveWithFullMetadata(level.registryAccess());
        plan.putInt("length", 1);
        plan.putBoolean("approved", true);
        marker.loadWithComponents(plan, level.registryAccess());
        BlockEntity rootstock = level.getBlockEntity(head);
        CompoundTag mutated = rootstock.saveWithFullMetadata(level.registryAccess());
        net.minecraft.nbt.ListTag stairs = new net.minecraft.nbt.ListTag();
        stairs.add(net.minecraft.nbt.StringTag.valueOf("stairs"));
        mutated.put("mutations", stairs);
        rootstock.loadWithComponents(mutated, level.registryAccess());
        BlockEntity jaw = level.getBlockEntity(head.south());
        level.getCapability(BiomassCapabilities.BLOCK, head.south(), null).insert(Biomass.of(100), false);
        CompoundTag due = jaw.saveWithFullMetadata(level.registryAccess());
        due.putInt("ticks", Fracturing.CUT_TICKS);
        jaw.loadWithComponents(due, level.registryAccess());
        BlockPos cut = head.north(2).above(2); // the first step's top: the marker's level plus three
        Player player = EcologyTests.holding(h, net.minecraft.world.level.GameType.SURVIVAL, Items.AIR, 0);
        TunnelRootMenu menu = (TunnelRootMenu) ((MenuProvider) rootstock).createMenu(0, player.getInventory(), player);
        h.startSequence()
                .thenExecute(() -> {
                    level.setBlock(cut, Blocks.STONE.defaultBlockState(), quiet);
                    level.getChunkSource().removeRegionTicket(HOLD, next, 0, next);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunk(next.x, next.z), "the next chunk is not loaded"))
                .thenExecute(() -> {
                    tick(level, head, rootstock.getType());
                    tick(level, head.south(), jaw.getType());
                    h.assertTrue(menu.minehead(), "a minehead");
                    h.assertValueEqual(
                            menu.stopKey(), Optional.of(key(Rootstocking.UNLOADED)), "it names the unloaded ground");
                    h.assertFalse(level.hasChunk(next.x, next.z), "and loaded nothing");
                    h.assertValueEqual(
                            jaw.saveWithFullMetadata(level.registryAccess()).getInt("cuts"), 0, "the jaw cut nothing");
                    level.getChunkSource().addRegionTicket(HOLD, next, 0, next);
                    level.getChunkAt(head.north(2));
                    h.assertTrue(level.getBlockState(cut).is(Blocks.STONE), "the step is as it was");
                    for (int wait = 0; wait <= Fracturing.RETRY_TICKS + Rootstocking.CHECK_TICKS; wait++) {
                        tick(level, head, rootstock.getType());
                        tick(level, head.south(), jaw.getType());
                        if (level.getBlockState(cut).isAir()) break;
                    }
                    h.assertTrue(level.getBlockState(cut).isAir(), "loaded, the jaw cuts at its order");
                    h.assertFalse(
                            menu.stopKey().equals(Optional.of(key(Rootstocking.UNLOADED))), "and it no longer waits");
                    for (BlockPos cell : BlockPos.betweenClosed(head.offset(-1, -3, -3), head.offset(2, 3, 3))) {
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

    // ---- rebuilding and restarting ----

    /**
     * A three-deep minehead at work: the Rootstock and then the jaw are broken with the Surgical Blade and placed
     * again, each coming out as one item that carries its contents and history; the chunk is then written to disk,
     * unloaded and read back as new block entities. Nothing is reset: the plan, the Rootstock's mutation and every
     * history count are at least what they were. The section then finishes with each block cut once, each step built
     * once, each segment and tread paid once and the Rootstock's count whole.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "rootstock_unload",
            timeoutTicks = 60 * BLOCK_TICKS)
    public static void rebuildingAndRestartingKeepTheCoreAndLoseNothing(GameTestHelper h) {
        Site site = Site.of(h, 5, 3);
        site.rig(0, 3000, 8, 8);
        int stone = site.count(Blocks.STONE, 1);
        int steps = site.plan().steps().size();
        ServerPlayer steward = player(h);
        site.steward(steward);
        Player hand = EcologyTests.holding(h, net.minecraft.world.level.GameType.SURVIVAL, Items.AIR, 0);
        hand.setYRot(180); // facing north, as the minehead does
        List<BlockPos> kept = List.of(HEAD, JAW, GROWER, MARKER, BED, CILIA);
        List<BlockEntity> before = new ArrayList<>();
        int[] counted = new int[4];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(site.built() >= 1, "a step is built"))
                .thenExecute(() -> {
                    CompoundTag head = site.saved(HEAD);
                    h.assertTrue(head.getInt("descended") >= 1, "the Rootstock has advanced");
                    rebuild(h, site, hand, HEAD, new BlockPos(0, -1, 0));
                    CompoundTag again = site.saved(HEAD);
                    for (String key : List.of("mutations", "descended", "tank", "cells", "segments", "output"))
                        h.assertValueEqual(again.get(key), head.get(key), "the Rootstock keeps its " + key);
                    h.assertValueEqual(
                            site.level.getBlockState(site.at(HEAD)).getValue(HorizontalDirectionalBlock.FACING),
                            Direction.NORTH,
                            "placed facing the plan");
                    CompoundTag jaw = site.saved(JAW);
                    rebuild(h, site, hand, JAW, new BlockPos(0, -1, 1));
                    CompoundTag jawAgain = site.saved(JAW);
                    for (String key : List.of("cuts", "tank"))
                        h.assertValueEqual(jawAgain.get(key), jaw.get(key), "the jaw keeps its " + key);
                    site.steward(steward);
                })
                .thenWaitUntil(() -> h.assertTrue(site.built() >= 2, "rebuilt, it goes on"))
                .thenExecute(() -> {
                    for (BlockPos pos : kept) before.add(site.entity(pos));
                    counted[0] = site.saved(HEAD).getInt("descended");
                    counted[1] = site.built();
                    counted[2] = site.saved(JAW).getInt("cuts");
                    counted[3] = site.saved(GROWER).getInt("steps");
                    site.level.save(null, true, false);
                    for (ChunkPos chunk : site.chunks()) site.level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(site.level.hasChunkAt(site.start), "the chunk is gone"))
                .thenExecute(() -> site.level.save(null, true, false))
                .thenIdle(2 * Fracturing.CUT_TICKS)
                .thenExecute(() -> {
                    for (ChunkPos chunk : site.chunks()) TestChunks.force(site.level, chunk);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        site.level.hasChunkAt(site.start) && site.entity(CILIA) != null,
                        "the chunk and the organs are back"))
                .thenExecute(() -> {
                    for (int n = 0; n < kept.size(); n++)
                        h.assertTrue(
                                site.entity(kept.get(n)) != before.get(n), kept.get(n) + ": read from the saved chunk");
                    CompoundTag head = site.saved(HEAD);
                    h.assertValueEqual(head.getList("mutations", 8).getString(0), "stairs", "still a Rootstock");
                    h.assertTrue(head.getInt("descended") >= counted[0], "its advance is not reset");
                    h.assertTrue(site.built() >= counted[1], "nor the plan's");
                    h.assertTrue(site.saved(JAW).getInt("cuts") >= counted[2], "nor the jaw's history");
                    h.assertTrue(site.saved(GROWER).getInt("steps") >= counted[3], "nor the grower's");
                    CompoundTag marker = site.saved(MARKER);
                    h.assertTrue(marker.getBoolean("approved") && marker.getInt("length") == 3, "the plan is kept");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.status(steward), Optional.of(key(Rootstocking.FINISHED)), "it finishes the section"))
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.held(SPOIL_CHEST, Items.COBBLESTONE), stone, "every cut in storage once"))
                .thenExecute(() -> {
                    site.assertBuilt(steps);
                    h.assertValueEqual(site.built(), steps, "each step built once");
                    h.assertValueEqual(site.saved(JAW).getInt("cuts"), stone, "the jaw's history whole");
                    h.assertValueEqual(site.saved(HEAD).getInt("descended"), steps, "the Rootstock's history whole");
                    h.assertValueEqual(site.saved(GROWER).getInt("steps"), steps, "the grower's history whole");
                    int segments = site.held(STOCK_CHEST, MiningModule.ROOT_SEGMENT.get())
                            + site.held(HOPPER, MiningModule.ROOT_SEGMENT.get())
                            + site.stocked(MiningModule.ROOT_SEGMENT.get());
                    h.assertValueEqual(segments, 8 - steps, "each segment paid once");
                    h.assertValueEqual(site.bed(), 0, "the bed is empty");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    /** Breaks the organ at {@code organ} with the Surgical Blade into one item and places it again. */
    private static void rebuild(GameTestHelper h, Site site, Player hand, BlockPos organ, BlockPos foundation) {
        putBack(h, site, hand, takeAway(h, site, hand, organ), organ, foundation);
    }

    /** Breaks the organ at {@code organ} with the Surgical Blade, as a player does, and returns the one item. */
    private static ItemStack takeAway(GameTestHelper h, Site site, Player hand, BlockPos organ) {
        BlockPos at = site.at(organ);
        List<ItemStack> drops = Block.getDrops(
                site.level.getBlockState(at),
                site.level,
                at,
                site.entity(organ),
                hand,
                new ItemStack(InteractionModule.SURGICAL_BLADE.get()));
        h.assertValueEqual(drops.size(), 1, organ + " comes out as one item: " + drops);
        site.level.removeBlockEntity(at);
        site.level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        return drops.get(0);
    }

    /** Places {@code item} from {@code hand} on the top of {@code foundation}, so it stands at {@code organ}. */
    private static void putBack(
            GameTestHelper h, Site site, Player hand, ItemStack item, BlockPos organ, BlockPos foundation) {
        hand.setItemInHand(InteractionHand.MAIN_HAND, item);
        BlockPos under = site.at(foundation);
        item.useOn(new UseOnContext(
                hand,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(under).add(0, 0.5, 0), Direction.UP, under, false)));
        h.assertTrue(site.entity(organ) != null, organ + " placed again");
    }

    // ---- the guide ----

    /**
     * The guide's entry teaches the minehead from implemented behaviour and renders the organs' recipes from recipe
     * data; the two milestones exist and every part has a survival recipe.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGuideTeachesTheMineheadAndItsRecipesComeFromData(GameTestHelper h) {
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String path : List.of("contact/descending_rootstock", "contact/staircase_landing"))
            h.assertTrue(advancements.get(id(path)) != null, path + " exists");
        var recipes = h.getLevel().getRecipeManager();
        for (String recipe : List.of(
                "fracture_jaw",
                "work_bed",
                "collection_cilia",
                "surveyed_tissue",
                "structure_grower",
                "root_segment",
                "construct/stairs",
                "growth/tunnel_root"))
            h.assertTrue(recipes.byKey(id(recipe)).isPresent(), "a survival recipe: " + recipe);
        var entry = BookDataManager.get()
                .getBook(id("the_waking_genome"))
                .getCategory(id("circulation"))
                .getEntries()
                .get(id("circulation/descending_rootstock"));
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
                List.of(id("fracture_jaw"), id("collection_cilia"), id("surveyed_tissue"), id("structure_grower")),
                "the guide renders the organs' recipes from recipe data");
        var lang = StarterAdvancementTests.lang();
        for (String page : List.of("use", "assembly_text", "supply_text", "work_text", "stops_text", "recover_text"))
            h.assertTrue(
                    lang.has("entry.infestusfrontier.circulation.descending_rootstock." + page),
                    "the guide has " + page);
        for (Refusal stop : TunnelRootMenu.STOPS)
            h.assertTrue(lang.has(key(stop)), "every reason the Rootstock names is worded: " + stop.key());
        h.succeed();
    }

    // ---- the demo ----

    /** The Iron Bars Factory of the demo, whose Gizzard the exhibit's raw iron feeds. */
    private static final BlockPos FACTORY_MIN = new BlockPos(0, -62, -22);

    private static final BlockPos FACTORY_MAX = new BlockPos(31, -59, -15);
    /** The staircase exhibit, south of the factory. */
    private static final BlockPos EXHIBIT_MIN = new BlockPos(16, -61, -14);

    private static final BlockPos EXHIBIT_MAX = new BlockPos(27, -46, 3);
    static final BlockPos DEMO_HEAD = new BlockPos(20, -51, -2);
    private static final BlockPos DEMO_GIZZARD = new BlockPos(8, -60, -16);
    private static final BlockPos DEMO_LARDER = new BlockPos(4, -60, -16);
    private static final BlockPos DEMO_SPOIL = new BlockPos(25, -60, -6);

    /**
     * The demo's staircase exhibit beside the Iron Bars Factory, built from the demo's own commands. Its minehead
     * finishes the eight-deep section on its own from the exhibit's supplies: the spoil arrives in the exhibit's
     * chest and the raw iron it cuts travels the veins into the factory, whose Gizzard grinds it. The factory's own
     * raw iron is taken out first, so every batch the Gizzard finishes is the mine's. A zombie walks from the hill top
     * to the landing.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 40 * BLOCK_TICKS)
    public static void theDemoExhibitDigsItsStaircaseIntoTheOreLine(GameTestHelper h) {
        DemoExhibit factory = new DemoExhibit(FACTORY_MIN, FACTORY_MAX, 70);
        DemoExhibit mine = new DemoExhibit(EXHIBIT_MIN, EXHIBIT_MAX, 70);
        factory.force(h);
        mine.force(h);
        ServerLevel level = h.getLevel();
        Player player = EcologyTests.holding(h, net.minecraft.world.level.GameType.SURVIVAL, Items.AIR, 0);
        int[] stone = new int[1];
        int[] ore = new int[1];
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    factory.tidy(h);
                    mine.tidy(h);
                    factory.place(h);
                    mine.place(h);
                    IItemHandler larder =
                            level.getCapability(Capabilities.ItemHandler.BLOCK, mine.at(DEMO_LARDER), Direction.DOWN);
                    for (int slot = 0; slot < larder.getSlots(); slot++) larder.extractItem(slot, 64, false);
                    h.assertTrue(
                            level.getBlockState(mine.at(DEMO_HEAD)).is(MiningModule.TUNNEL_ROOT.get()),
                            "the Rootstock stands at the minehead");
                    BlockPos marker = mine.at(DEMO_HEAD.north().below());
                    MinePlan plan = new MinePlan(PlanShape.STAIRCASE, 8, Turn.NONE, Turn.NONE);
                    for (PlanStep step : plan.steps().subList(4, plan.steps().size()))
                        for (Offset cell : step.cleared()) {
                            BlockState state = level.getBlockState(
                                    marker.north(cell.forward()).above(cell.up()));
                            if (state.is(Blocks.STONE)) stone[0]++;
                            if (state.is(Blocks.IRON_ORE)) ore[0]++;
                        }
                    h.assertTrue(stone[0] > 0 && ore[0] == 3, "rock and three ores left to cut: " + ore[0]);
                })
                .thenWaitUntil(() -> {
                    TunnelRootMenu menu = menu(h, level, mine.at(DEMO_HEAD), player);
                    h.assertValueEqual(menu.stopKey(), Optional.of(key(Rootstocking.FINISHED)), "it finishes");
                })
                .thenWaitUntil(() -> {
                    h.assertValueEqual(
                            level.getBlockEntity(mine.at(DEMO_SPOIL)) instanceof Container spoil
                                    ? spoil.countItem(Items.COBBLESTONE)
                                    : -1,
                            stone[0],
                            "the spoil in the exhibit's chest");
                    CompoundTag gizzard =
                            level.getBlockEntity(mine.at(DEMO_GIZZARD)).saveWithFullMetadata(level.registryAccess());
                    h.assertValueEqual(
                            gizzard.getCompound("contents")
                                    .getCompound("history")
                                    .getInt("completed_batches"),
                            ore[0],
                            "the factory's Gizzard ground the mine's raw iron");
                })
                .thenExecute(() -> {
                    Path path = new Site(h, level, mine.at(DEMO_HEAD), 8)
                            .walk(mine.at(new BlockPos(19, -51, -3)), mine.at(new BlockPos(20, -59, -12)));
                    h.assertTrue(path != null && path.canReach(), "a zombie walks from the hill top to the landing");
                    h.assertValueEqual(
                            level.getEntitiesOfClass(
                                            ItemEntity.class,
                                            AABB.encapsulatingFullBlocks(mine.at(EXHIBIT_MIN), mine.at(EXHIBIT_MAX))
                                                    .inflate(4))
                                    .size(),
                            0,
                            "nothing loose");
                    mine.tidy(h);
                    factory.tidy(h);
                    mine.forget(h);
                    factory.forget(h);
                })
                .thenSucceed();
    }
}
