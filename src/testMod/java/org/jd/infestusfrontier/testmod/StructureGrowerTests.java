package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import com.klikli_dev.modonomicon.book.page.BookCraftingRecipePage;
import com.klikli_dev.modonomicon.data.BookDataManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanEdit;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.core.mining.Turn;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.mining.RootCellBlock;
import org.jd.infestusfrontier.mining.StructureGrowerMenu;
import org.jd.infestusfrontier.mining.SurveyMenu;
import org.jd.infestusfrontier.mining.SurveyedTissueBlock;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;

/**
 * Surveyed Tissue and the Structure Grower: the player shapes a staircase or a level route at the marker, sees it as
 * plan numbers only, and approves it; the grower then builds each cleared step whole from its stock, and only that.
 * What stops it, with nothing placed, paid or lost while it waits; its state and the plan's through a save to disk;
 * the shared step limit; unloaded ground; breaking; unreadable state; recipes, guide and demo. Each rig stands in a yard
 * of its own in a chunk of its own, far from the test structures. The markers face north, so a plan's forward is
 * north and its right is east.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class StructureGrowerTests {
    private static final int TOP = 100;
    /** Ticks a due step may take: a try, a stop and a look again after the retry, with a margin. */
    private static final int WAIT = Surveying.GROW_TICKS + 3 * Surveying.RETRY_TICKS;

    private static final MinePlan STAIRS = new MinePlan(PlanShape.STAIRCASE, 3, Turn.NONE, Turn.NONE, false);
    /** Right at its third cell, left at its sixth, lit: its eighth cell gets a torch. */
    private static final MinePlan GALLERY =
            new MinePlan(PlanShape.ROUTE, 10, new Turn(Steer.RIGHT, 3), new Turn(Steer.LEFT, 6), true);

    /**
     * A rig's yard around {@code start}, the first marker's place, in the middle of a chunk the yard forces. Stone is
     * the ground up to a height the rig chooses; markers sit flush in it with their growers on top.
     */
    private record Site(ServerLevel level, BlockPos start, ChunkPos chunk) {
        private static final int REACH = 14;
        private static final int DEPTH = 8;

        static Site of(GameTestHelper h, int slot) {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            BlockPos start =
                    new BlockPos((origin.getX() & ~15) + 264008, TOP, (origin.getZ() & ~15) + 294408 + slot * 64);
            Site site = new Site(h.getLevel(), start, new ChunkPos(start));
            TestChunks.force(site.level, site.chunk);
            site.clear();
            return site;
        }

        void clear() {
            for (BlockPos cell :
                    BlockPos.betweenClosed(start.offset(-REACH, -DEPTH, -REACH), start.offset(REACH, DEPTH, REACH))) {
                level.removeBlockEntity(cell);
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            for (var entity :
                    level.getEntities((net.minecraft.world.entity.Entity) null, yard(), e -> !(e instanceof Player)))
                entity.discard();
        }

        void release() {
            clear();
            level.setChunkForced(chunk.x, chunk.z, false);
        }

        AABB yard() {
            return AABB.encapsulatingFullBlocks(
                    start.offset(-REACH, -DEPTH, -REACH), start.offset(REACH, DEPTH, REACH));
        }

        int loose() {
            return level.getEntitiesOfClass(ItemEntity.class, yard()).size();
        }

        /** Stone from the yard's floor up to {@code top} blocks above the markers' level. */
        void ground(int top) {
            for (BlockPos cell : BlockPos.betweenClosed(
                    start.offset(-REACH + 2, -DEPTH + 1, -REACH + 2), start.offset(REACH - 2, top, 2)))
                level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        /** A marker facing north at {@code marker}, holding {@code plan}, with a grower on it and room above. */
        void rig(BlockPos marker, MinePlan plan, boolean approved, int built) {
            for (int up = 1; up <= 3; up++) level.setBlock(marker.above(up), Blocks.AIR.defaultBlockState(), 3);
            level.setBlockAndUpdate(
                    marker,
                    MiningModule.SURVEYED_TISSUE
                            .get()
                            .defaultBlockState()
                            .setValue(SurveyedTissueBlock.FACING, Direction.NORTH));
            plan(marker, plan, approved, built);
            level.setBlockAndUpdate(
                    marker.above(), MiningModule.STRUCTURE_GROWER.get().defaultBlockState());
        }

        /** Loads the marker's saved plan, as a load from disk does. */
        void plan(BlockPos marker, MinePlan plan, boolean approved, int built) {
            load(marker, tag -> {
                tag.putInt("shape", plan.shape().ordinal());
                tag.putInt("length", plan.length());
                tag.putInt("first_way", plan.first().way().ordinal());
                tag.putInt("first_at", plan.first().at());
                tag.putInt("second_way", plan.second().way().ordinal());
                tag.putInt("second_at", plan.second().at());
                tag.putBoolean("lit", plan.lit());
                tag.putBoolean("approved", approved);
                tag.putInt("built", built);
            });
        }

        /** Where {@code offset} of the plan of the north-facing marker at {@code marker} lies. */
        static BlockPos at(BlockPos marker, Offset offset) {
            return marker.relative(Direction.NORTH, offset.forward())
                    .relative(Direction.EAST, offset.right())
                    .above(offset.up());
        }

        /** Clears the blocks of steps {@code from} to {@code to}, counted from 1, as a cut or a pick would. */
        void carve(BlockPos marker, MinePlan plan, int from, int to) {
            for (PlanStep step : plan.steps().subList(from - 1, to))
                for (Offset cell : step.cleared())
                    level.setBlock(at(marker, cell), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        BlockEntity entity(BlockPos pos) {
            return level.getBlockEntity(pos);
        }

        CompoundTag saved(BlockPos pos) {
            return entity(pos).saveWithFullMetadata(level.registryAccess());
        }

        void load(BlockPos pos, Consumer<CompoundTag> change) {
            CompoundTag tag = saved(pos);
            change.accept(tag);
            entity(pos).loadWithComponents(tag, level.registryAccess());
        }

        IItemHandler stock(BlockPos grower) {
            return level.getCapability(Capabilities.ItemHandler.BLOCK, grower, null);
        }

        /** Hands {@code stack} to the grower through its item handler, as a hopper does. */
        void supply(BlockPos grower, ItemStack stack) {
            IItemHandler handler = stock(grower);
            for (int slot = 0; slot < handler.getSlots(); slot++)
                if (handler.isItemValid(slot, stack)) {
                    ItemStack left = handler.insertItem(slot, stack.copy(), false);
                    if (!left.isEmpty()) throw new IllegalStateException("did not take " + stack);
                    return;
                }
            throw new IllegalStateException("no slot takes " + stack);
        }

        int held(BlockPos grower, int slot) {
            return stock(grower).getStackInSlot(slot).getCount();
        }

        StructureGrowerMenu growerMenu(BlockPos grower, Player player) {
            return (StructureGrowerMenu) ((MenuProvider) entity(grower)).createMenu(0, player.getInventory(), player);
        }

        SurveyMenu surveyMenu(BlockPos marker, Player player) {
            return (SurveyMenu) ((MenuProvider) entity(marker)).createMenu(0, player.getInventory(), player);
        }

        Optional<String> stop(BlockPos grower, Player player) {
            return growerMenu(grower, player).stopKey();
        }

        int built(BlockPos marker) {
            return saved(marker).getInt("built");
        }

        BlockState state(BlockPos pos) {
            return level.getBlockState(pos);
        }
    }

    private static String key(Refusal stop) {
        return "infestusfrontier.refusal." + stop.key();
    }

    private static ItemStack segments(int count) {
        return new ItemStack(MiningModule.ROOT_SEGMENT.get(), count);
    }

    /** Asserts that every step from {@code from} on, counted from 1, has nothing of the grower's in it. */
    private static void untouched(GameTestHelper h, Site site, BlockPos marker, MinePlan plan, int from) {
        for (PlanStep step : plan.steps().subList(from - 1, plan.steps().size()))
            for (Offset cell : step.cleared()) {
                BlockState state = site.state(Site.at(marker, cell));
                h.assertFalse(
                        state.is(MiningModule.ROOT_CELL.get())
                                || state.getBlock() instanceof StairBlock
                                || state.is(Blocks.WALL_TORCH),
                        "step " + step.number() + " holds nothing grown: " + state);
            }
    }

    // ---- the staircase ----

    /**
     * The player shapes a three-deep staircase at the marker's screen and approves it; until then the grower builds
     * nothing. It then builds each step the moment it is clear, a root cell with a tread on it, and waits at the first
     * step still in the ground, then at missing stock, naming each and paying nothing; loaded by hand, it finishes
     * with the landing three below the marker. It never cuts, every step's headroom stays clear, the steward is
     * credited and nothing falls loose.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 12 * WAIT)
    public static void aStaircaseIsShapedApprovedAndGrownIntoItsClearedSteps(GameTestHelper h) {
        Site site = Site.of(h, 0);
        BlockPos marker = site.start;
        BlockPos grower = marker.above();
        site.ground(0);
        site.rig(marker, MinePlan.START, false, 0);
        site.carve(marker, STAIRS, 1, 2);
        ServerPlayer steward = player(h);
        SurveyMenu menu = site.surveyMenu(marker, steward);
        for (int n = 0; n < MinePlan.START.length() - STAIRS.length(); n++)
            h.assertTrue(menu.clickMenuButton(steward, PlanEdit.SHORTER.ordinal()), "the screen asks");
        h.assertValueEqual(menu.survey().map(Survey::plan), Optional.of(STAIRS), "a three-deep draft");
        h.assertValueEqual(menu.end(), Optional.of(Site.at(marker, new Offset(4, 0, -3))), "it ends three down");
        load(h, site, grower, steward, segments(2));
        load(h, site, grower, steward, new ItemStack(Items.COBBLESTONE_STAIRS, 8));
        PendingCredits credits = PendingCredits.get(h.getLevel());
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(grower, steward), Optional.of(key(Surveying.NOT_APPROVED)), "a draft is not built"))
                .thenExecute(() -> {
                    untouched(h, site, marker, STAIRS, 1);
                    h.assertTrue(menu.clickMenuButton(steward, SurveyMenu.APPROVE), "approve");
                    h.assertTrue(menu.survey().orElseThrow().approved(), "approved");
                    menu.clickMenuButton(steward, PlanEdit.LONGER.ordinal());
                    h.assertValueEqual(
                            menu.survey().map(Survey::plan), Optional.of(STAIRS), "an approved plan does not change");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(grower, steward),
                        Optional.of(key(Surveying.NOT_CLEAR)),
                        "the third step is still in the ground"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.built(marker), 2, "the two cleared steps are built");
                    untouched(h, site, marker, STAIRS, 3);
                    h.assertTrue(
                            site.state(Site.at(marker, STAIRS.steps().get(2).root()))
                                    .is(Blocks.STONE),
                            "the grower cuts nothing");
                    site.carve(marker, STAIRS, 3, 4);
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(grower, steward), Optional.of(key(Surveying.NO_SEGMENT)), "out of segments"))
                .thenExecute(() -> {
                    untouched(h, site, marker, STAIRS, 3);
                    h.assertValueEqual(site.held(grower, 1), 6, "two treads paid, no more");
                    load(h, site, grower, steward, segments(5));
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(grower, steward), Optional.of(key(Surveying.FINISHED)), "the plan is built"))
                .thenExecute(() -> {
                    for (PlanStep step : STAIRS.steps()) {
                        BlockState root = site.state(Site.at(marker, step.root()));
                        h.assertTrue(root.is(MiningModule.ROOT_CELL.get()), "step " + step.number() + ": root cell");
                        h.assertValueEqual(
                                root.getValue(HorizontalDirectionalBlock.FACING), Direction.NORTH, "along the stairs");
                        if (step.tread().isPresent()) {
                            BlockState tread =
                                    site.state(Site.at(marker, step.tread().get()));
                            h.assertTrue(tread.is(Blocks.COBBLESTONE_STAIRS), "step " + step.number() + ": tread");
                            h.assertValueEqual(
                                    tread.getValue(StairBlock.FACING), Direction.SOUTH, "it climbs back to the marker");
                            h.assertValueEqual(tread.getValue(StairBlock.HALF), Half.BOTTOM, "set on its cell");
                        }
                        for (Offset clear : step.passage())
                            h.assertTrue(site.state(Site.at(marker, clear)).isAir(), "three clear over each tread");
                    }
                    h.assertValueEqual(STAIRS.steps().size(), 4, "three treads and the landing");
                    h.assertValueEqual(site.built(marker), 4, "the marker counts every step once");
                    h.assertValueEqual(site.held(grower, 0), 3, "four segments paid of seven");
                    h.assertValueEqual(site.held(grower, 1), 5, "three treads paid of eight");
                    h.assertValueEqual(site.saved(grower).getInt("steps"), 4, "the grower's history");
                    StructureGrowerMenu shown = site.growerMenu(grower, steward);
                    h.assertValueEqual(shown.built(), 4, "its screen shows the plan built");
                    h.assertValueEqual(shown.steps(), 4, "of its four steps");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    h.assertTrue(
                            credits.holdsAutomatic(steward.getUUID(), id("structure_grower")),
                            "the steward is credited");
                    site.release();
                })
                .thenSucceed();
    }

    /** Loads {@code stack} into the grower by hand, as a right click with it does. */
    private static void load(GameTestHelper h, Site site, BlockPos grower, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        site.state(grower)
                .useItemOn(
                        stack,
                        site.level,
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(grower), Direction.UP, grower, false));
        h.assertTrue(player.getMainHandItem().isEmpty(), "the grower takes " + stack);
    }

    // ---- the route ----

    /**
     * An approved, lit route of ten cells that turns right at its third cell and left at its sixth: once every cell
     * is clear the grower lays a level root along it, bent at both turns, and hangs one torch on a wall at head height
     * over the eighth cell. It pays one segment a cell and one torch, and the passage stays open.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 14 * WAIT)
    public static void aLitRouteWithTwoTurnsIsGrownLevelWithBendsAndALight(GameTestHelper h) {
        Site site = Site.of(h, 1);
        BlockPos marker = site.start;
        BlockPos grower = marker.above();
        site.ground(3);
        site.rig(marker, GALLERY, true, 0);
        site.carve(marker, GALLERY, 1, GALLERY.length());
        site.supply(grower, segments(12));
        site.supply(grower, new ItemStack(Items.TORCH, 2));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(grower, player), Optional.of(key(Surveying.FINISHED)), "the route is built"))
                .thenExecute(() -> {
                    List<PlanStep> steps = GALLERY.steps();
                    Direction[] heading = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                    for (PlanStep step : steps) {
                        BlockPos at = Site.at(marker, step.root());
                        h.assertValueEqual(at.getY(), marker.getY(), "cell " + step.number() + " is level");
                        BlockState root = site.state(at);
                        h.assertTrue(root.is(MiningModule.ROOT_CELL.get()), "cell " + step.number() + ": root cell");
                        h.assertValueEqual(root.getValue(RootCellBlock.BEND), step.bend(), "bent only at a turn");
                        if (!step.bend())
                            h.assertValueEqual(
                                    root.getValue(HorizontalDirectionalBlock.FACING),
                                    heading[step.leaving()],
                                    "cell " + step.number() + " runs with the route");
                        for (Offset clear : step.passage())
                            if (step.light().isEmpty()
                                    || !clear.equals(step.light().get()))
                                h.assertTrue(site.state(Site.at(marker, clear)).isAir(), "two clear over the root");
                    }
                    h.assertTrue(steps.get(2).bend() && steps.get(5).bend(), "two bends");
                    // a bend opens towards its facing and the side clockwise of it: north-then-east opens to the
                    // south and east, so it faces east; east-then-north opens to the west and north, so it faces west
                    h.assertValueEqual(
                            site.state(Site.at(marker, steps.get(2).root()))
                                    .getValue(HorizontalDirectionalBlock.FACING),
                            Direction.EAST,
                            "the right turn's bend");
                    h.assertValueEqual(
                            site.state(Site.at(marker, steps.get(5).root()))
                                    .getValue(HorizontalDirectionalBlock.FACING),
                            Direction.WEST,
                            "the left turn's bend");
                    h.assertValueEqual(
                            Site.at(marker, steps.get(9).root()), marker.offset(3, 0, -7), "three east, seven north");
                    PlanStep lit = steps.get(7);
                    BlockState torch = site.state(Site.at(marker, lit.light().orElseThrow()));
                    h.assertTrue(torch.is(Blocks.WALL_TORCH), "a torch over the eighth cell");
                    h.assertValueEqual(
                            torch.getValue(WallTorchBlock.FACING), Direction.EAST, "on the wall left of the way");
                    h.assertValueEqual(Site.at(marker, lit.light().get()).getY() - marker.getY(), 2, "at head height");
                    h.assertValueEqual(site.held(grower, 0), 2, "a segment a cell");
                    h.assertValueEqual(site.held(grower, 2), 1, "one torch");
                    h.assertValueEqual(site.built(marker), 10, "all ten counted once");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- the plan and its preview ----

    /**
     * The marker's screen keeps a staircase to 32 blocks deep and a route to 64 cells with two turns at most, inside
     * it and in order. A plan that would leave the world is not approved. Clients get the plan's numbers and nothing
     * else, so a marker over hidden ore sends the same as one over plain stone: the preview shows geometry only.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theMarkerKeepsThePlanInItsLimitsAndShowsOnlyItsShape(GameTestHelper h) {
        Site site = Site.of(h, 2);
        BlockPos marker = site.start;
        site.ground(0);
        site.rig(marker, MinePlan.START, false, 0);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        SurveyMenu menu = site.surveyMenu(marker, player);
        int far = SurveyMenu.EDITS;
        for (int n = 0; n < 8; n++) menu.clickMenuButton(player, far + PlanEdit.LONGER.ordinal());
        MinePlan deepest = menu.survey().orElseThrow().plan();
        h.assertValueEqual(deepest.length(), Surveying.MAX_DEPTH, "a staircase stops at 32 deep");
        h.assertValueEqual(deepest.end().up(), -32, "its landing is 32 below the marker");
        menu.clickMenuButton(player, PlanEdit.FIRST_WAY.ordinal());
        h.assertValueEqual(menu.survey().orElseThrow().plan().first(), Turn.NONE, "a staircase does not turn");
        menu.clickMenuButton(player, PlanEdit.SHAPE.ordinal());
        for (int n = 0; n < 8; n++) menu.clickMenuButton(player, far + PlanEdit.LONGER.ordinal());
        MinePlan longest = menu.survey().orElseThrow().plan();
        h.assertValueEqual(longest.shape(), PlanShape.ROUTE, "a route");
        h.assertValueEqual(longest.length(), Surveying.MAX_ROUTE_LENGTH, "it stops at 64 cells");
        menu.clickMenuButton(player, PlanEdit.FIRST_WAY.ordinal());
        menu.clickMenuButton(player, PlanEdit.SECOND_WAY.ordinal());
        menu.clickMenuButton(player, PlanEdit.SECOND_WAY.ordinal());
        menu.clickMenuButton(player, far + PlanEdit.FIRST_LATER.ordinal());
        for (int n = 0; n < 8; n++) menu.clickMenuButton(player, far + PlanEdit.SECOND_LATER.ordinal());
        MinePlan turned = menu.survey().orElseThrow().plan();
        h.assertValueEqual(turned.first(), new Turn(Steer.LEFT, 40), "the first turn moved later");
        h.assertValueEqual(turned.second(), new Turn(Steer.RIGHT, 63), "the second stays before the end");
        h.assertFalse(menu.clickMenuButton(player, SurveyMenu.APPROVE + 1), "no other button");

        BlockPos low = new BlockPos(marker.getX() - 6, site.level.getMinBuildHeight() + 4, marker.getZ());
        site.level.setBlockAndUpdate(
                low,
                MiningModule.SURVEYED_TISSUE
                        .get()
                        .defaultBlockState()
                        .setValue(SurveyedTissueBlock.FACING, Direction.NORTH));
        SurveyMenu deep = site.surveyMenu(low, player);
        deep.clickMenuButton(player, SurveyMenu.APPROVE);
        h.assertFalse(
                deep.survey().orElseThrow().approved(), "an eight-deep staircase four above the floor stays a draft");
        for (int n = 0; n < 5; n++) deep.clickMenuButton(player, PlanEdit.SHORTER.ordinal());
        deep.clickMenuButton(player, SurveyMenu.APPROVE);
        h.assertTrue(deep.survey().orElseThrow().approved(), "a three-deep one fits and is approved");
        site.level.removeBlockEntity(low);
        site.level.setBlock(low, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

        site.plan(marker, GALLERY, false, 0);
        CompoundTag over = site.entity(marker).getUpdateTag(site.level.registryAccess());
        h.assertValueEqual(
                over.getAllKeys(),
                Set.of(
                        "shape",
                        "length",
                        "first_way",
                        "first_at",
                        "second_way",
                        "second_at",
                        "lit",
                        "approved",
                        "built"),
                "clients get the plan's numbers and nothing else");
        for (PlanStep step : GALLERY.steps())
            site.level.setBlock(
                    Site.at(marker, step.root()).below(), Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
        h.assertValueEqual(
                site.entity(marker).getUpdateTag(site.level.registryAccess()),
                over,
                "ore under the route changes nothing a client sees");
        site.release();
        h.succeed();
    }

    // ---- what stops it ----

    /** One thing in the way of a step, the reason the grower names, and how the player clears it. */
    private record Obstacle(String what, Refusal reason, Consumer<Site> place, Consumer<Site> clear) {}

    /**
     * A route's first cell, clear and supplied, with something in the way: a block the player put in its passage,
     * water, a hole under the root cell, a cow where it goes. For each the grower names the reason, places nothing,
     * pays nothing and leaves the player's block as it is; once the player clears it, it builds.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 12 * WAIT)
    public static void theGrowerRefusesOccupiedOrUnsafeStepsAndKeepsWhatThePlayerPut(GameTestHelper h) {
        Site site = Site.of(h, 3);
        BlockPos marker = site.start;
        BlockPos grower = marker.above();
        MinePlan route = new MinePlan(PlanShape.ROUTE, 2, Turn.NONE, Turn.NONE, false);
        site.ground(3);
        site.rig(marker, route, true, 0);
        site.carve(marker, route, 1, 1);
        site.supply(grower, segments(4));
        BlockPos root = Site.at(marker, route.steps().get(0).root());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        List<Obstacle> obstacles = List.of(
                new Obstacle(
                        "a player's block",
                        Surveying.NOT_CLEAR,
                        s -> s.level.setBlockAndUpdate(root.above(2), Blocks.OAK_PLANKS.defaultBlockState()),
                        s -> {
                            h.assertTrue(s.state(root.above(2)).is(Blocks.OAK_PLANKS), "the planks stay");
                            s.level.setBlockAndUpdate(root.above(2), Blocks.AIR.defaultBlockState());
                        }),
                new Obstacle(
                        "water",
                        Surveying.FLUID,
                        s -> s.level.setBlockAndUpdate(root, Blocks.WATER.defaultBlockState()),
                        s -> {
                            for (BlockPos wet : BlockPos.betweenClosed(root.offset(-1, 0, -2), root.offset(1, 2, 1)))
                                if (!s.state(wet).getFluidState().isEmpty())
                                    s.level.setBlock(wet, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }),
                new Obstacle(
                        "a hole",
                        Surveying.NO_GROUND,
                        s -> s.level.setBlockAndUpdate(root.below(), Blocks.AIR.defaultBlockState()),
                        s -> s.level.setBlockAndUpdate(root.below(), Blocks.STONE.defaultBlockState())),
                new Obstacle(
                        "a cow",
                        Surveying.CREATURE,
                        s -> {
                            Cow cow = EntityType.COW.create(s.level);
                            cow.setNoAi(true);
                            cow.moveTo(Vec3.atBottomCenterOf(root));
                            s.level.addFreshEntity(cow);
                        },
                        s -> s.level.getEntitiesOfClass(Cow.class, s.yard()).forEach(Cow::discard)));
        var sequence = h.startSequence();
        for (Obstacle obstacle : obstacles)
            sequence.thenExecute(() -> obstacle.place().accept(site))
                    .thenWaitUntil(() -> h.assertValueEqual(
                            site.stop(grower, player),
                            Optional.of(key(obstacle.reason())),
                            obstacle.what() + " is named"))
                    .thenExecute(() -> {
                        h.assertFalse(site.state(root).is(MiningModule.ROOT_CELL.get()), obstacle.what() + ": no cell");
                        h.assertValueEqual(site.built(marker), 0, obstacle.what() + ": nothing counted");
                        h.assertValueEqual(site.held(grower, 0), 4, obstacle.what() + ": nothing paid");
                        obstacle.clear().accept(site);
                    });
        sequence.thenWaitUntil(() -> h.assertTrue(site.state(root).is(MiningModule.ROOT_CELL.get()), "then it builds"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.built(marker), 1, "one step");
                    h.assertValueEqual(site.held(grower, 0), 3, "paid once");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * Each kind of stock missing pauses the grower on the step that needs it, named, with nothing placed; supplied
     * through its item handler, as a hopper would, it goes on. A light with no wall to hold it pauses the step too.
     * The item handler takes and never gives.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 14 * WAIT)
    public static void missingStockPausesTheStepWithItsReasonAndResumesWhenSupplied(GameTestHelper h) {
        Site site = Site.of(h, 4);
        BlockPos stairs = site.start;
        BlockPos stairsGrower = stairs.above();
        BlockPos route = site.start.offset(-6, 0, 0);
        BlockPos routeGrower = route.above();
        MinePlan lit = new MinePlan(PlanShape.ROUTE, 8, Turn.NONE, Turn.NONE, true);
        MinePlan shallow = new MinePlan(PlanShape.STAIRCASE, 1, Turn.NONE, Turn.NONE, false);
        site.ground(0);
        site.rig(stairs, shallow, true, 0);
        site.carve(stairs, shallow, 1, 2);
        site.rig(route, lit, true, 0);
        site.carve(route, lit, 1, 8);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        BlockPos light = Site.at(route, lit.steps().get(7).light().orElseThrow());
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertValueEqual(
                            site.stop(stairsGrower, player), Optional.of(key(Surveying.NO_SEGMENT)), "no segment");
                    h.assertValueEqual(
                            site.stop(routeGrower, player), Optional.of(key(Surveying.NO_SEGMENT)), "nor here");
                })
                .thenExecute(() -> {
                    untouched(h, site, stairs, shallow, 1);
                    untouched(h, site, route, lit, 1);
                    IItemHandler handler = site.stock(stairsGrower);
                    h.assertFalse(handler.isItemValid(0, new ItemStack(Items.DIRT)), "it takes no dirt");
                    site.supply(stairsGrower, segments(4));
                    site.supply(routeGrower, segments(10));
                    h.assertValueEqual(handler.extractItem(0, 4, false), ItemStack.EMPTY, "and gives nothing back");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(stairsGrower, player), Optional.of(key(Surveying.NO_TREAD)), "no tread"))
                .thenExecute(() -> {
                    untouched(h, site, stairs, shallow, 1);
                    h.assertValueEqual(site.held(stairsGrower, 0), 4, "nothing paid for the step");
                    site.supply(stairsGrower, new ItemStack(Items.STONE_BRICK_STAIRS, 3));
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(stairsGrower, player), Optional.of(key(Surveying.FINISHED)), "the stairs are built"))
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(routeGrower, player), Optional.of(key(Surveying.NO_LIGHT)), "no torch"))
                .thenExecute(() -> {
                    h.assertValueEqual(site.built(route), 7, "seven cells, then the lit one waits");
                    untouched(h, site, route, lit, 8);
                    h.assertValueEqual(site.held(routeGrower, 0), 3, "seven segments paid");
                    site.supply(routeGrower, new ItemStack(Items.TORCH, 1));
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(routeGrower, player), Optional.of(key(Surveying.NO_WALL)), "nothing holds it"))
                .thenExecute(() -> {
                    untouched(h, site, route, lit, 8);
                    h.assertValueEqual(site.held(routeGrower, 2), 1, "the torch is kept");
                    site.level.setBlockAndUpdate(light.east(), Blocks.COBBLESTONE.defaultBlockState());
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(routeGrower, player), Optional.of(key(Surveying.FINISHED)), "the route is built"))
                .thenExecute(() -> {
                    h.assertTrue(site.state(light).is(Blocks.WALL_TORCH), "the torch hangs on the new wall");
                    h.assertValueEqual(site.state(light).getValue(WallTorchBlock.FACING), Direction.WEST, "facing in");
                    h.assertValueEqual(site.held(routeGrower, 2), 0, "paid");
                    h.assertValueEqual(site.held(stairsGrower, 1), 2, "one tread paid");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- saved state ----

    /**
     * A staircase and a route, each two steps built, go through their chunk being written to disk, unloaded and read
     * back: new block entities with the same plans, counts, stock, histories and steward; no step built while they
     * were gone. A tread the player swapped for planks stays planks. Cleared, the rest is built once each and paid once.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "grower_unload",
            timeoutTicks = 30 * WAIT)
    public static void plansAndProgressSurviveASaveToDiskAndAReload(GameTestHelper h) {
        Site site = Site.of(h, 5);
        BlockPos stairs = site.start;
        BlockPos route = site.start.offset(-5, 0, 0);
        MinePlan straight = new MinePlan(PlanShape.ROUTE, 4, new Turn(Steer.LEFT, 2), Turn.NONE, false);
        site.ground(0);
        site.rig(stairs, STAIRS, true, 0);
        site.rig(route, straight, true, 0);
        site.carve(stairs, STAIRS, 1, 2);
        site.carve(route, straight, 1, 2);
        ServerPlayer steward = player(h);
        for (BlockPos grower : List.of(stairs.above(), route.above())) {
            org.jd.infestusfrontier.organ.api.Organs.placedBy(site.level, grower, steward);
            site.supply(grower, segments(9));
        }
        site.supply(stairs.above(), new ItemStack(Items.COBBLESTONE_STAIRS, 5));
        List<BlockPos> kept = List.of(stairs, stairs.above(), route, route.above());
        List<BlockEntity> before = new ArrayList<>();
        List<CompoundTag> saved = new ArrayList<>();
        BlockPos swapped = Site.at(stairs, STAIRS.steps().get(0).tread().orElseThrow());
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertValueEqual(site.built(stairs), 2, "two stairs built");
                    h.assertValueEqual(site.built(route), 2, "two cells built");
                })
                .thenExecute(() -> {
                    site.level.setBlockAndUpdate(swapped, Blocks.OAK_PLANKS.defaultBlockState());
                    for (BlockPos pos : kept) {
                        before.add(site.entity(pos));
                        saved.add(site.saved(pos));
                    }
                    site.level.save(null, true, false);
                    site.level.setChunkForced(site.chunk.x, site.chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(site.level.hasChunkAt(site.start), "the chunk is gone"))
                .thenExecute(() -> site.level.save(null, true, false))
                .thenIdle(2 * Surveying.RETRY_TICKS)
                .thenExecute(() -> TestChunks.force(site.level, site.chunk))
                .thenWaitUntil(() -> h.assertTrue(
                        site.level.hasChunkAt(site.start) && site.entity(route.above()) != null,
                        "the chunk and the organs are back"))
                .thenExecute(() -> {
                    for (int n = 0; n < kept.size(); n++) {
                        BlockPos pos = kept.get(n);
                        h.assertTrue(site.entity(pos) != before.get(n), pos + ": read from the saved chunk");
                        CompoundTag back = site.saved(pos);
                        for (String key : saved.get(n).getAllKeys())
                            if (!key.equals("ticks"))
                                h.assertValueEqual(back.get(key), saved.get(n).get(key), pos + " keeps " + key);
                    }
                    h.assertValueEqual(site.built(stairs), 2, "no stair built while gone");
                    h.assertValueEqual(site.built(route), 2, "no cell built while gone");
                    site.carve(stairs, STAIRS, 3, 4);
                    site.carve(route, straight, 3, 4);
                })
                .thenWaitUntil(() -> {
                    h.assertValueEqual(site.built(stairs), 4, "the staircase goes on");
                    h.assertValueEqual(site.built(route), 4, "the route goes on");
                })
                .thenExecute(() -> {
                    h.assertTrue(site.state(swapped).is(Blocks.OAK_PLANKS), "the player's planks stay");
                    h.assertValueEqual(site.held(stairs.above(), 0), 5, "four segments paid, once each");
                    h.assertValueEqual(site.held(stairs.above(), 1), 2, "three treads paid, once each");
                    h.assertValueEqual(site.held(route.above(), 0), 5, "four cells paid, once each");
                    h.assertValueEqual(site.saved(stairs.above()).getInt("steps"), 4, "its history");
                    h.assertValueEqual(site.saved(route.above()).getInt("steps"), 4, "and this one's");
                    h.assertTrue(
                            site.state(Site.at(route, straight.steps().get(1).root()))
                                    .getValue(RootCellBlock.BEND),
                            "the bend built before the save");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * The grower broken with the Surgical Blade comes out as one item carrying its stock and history, and has them when
     * placed again; the marker comes out blank, its plan left with the place.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGrowerComesOutWholeAndTheMarkerBlank(GameTestHelper h) {
        Site site = Site.of(h, 6);
        BlockPos marker = site.start;
        BlockPos grower = marker.above();
        site.ground(0);
        site.rig(marker, STAIRS, true, 2);
        site.supply(grower, segments(7));
        site.supply(grower, new ItemStack(Items.MOSSY_COBBLESTONE_STAIRS, 3));
        site.supply(grower, new ItemStack(Items.TORCH, 5));
        site.load(grower, tag -> tag.putInt("steps", 11));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        CompoundTag held = site.saved(grower);
        List<ItemStack> drops = Block.getDrops(
                site.state(grower),
                site.level,
                grower,
                site.entity(grower),
                player,
                new ItemStack(InteractionModule.SURGICAL_BLADE.get()));
        h.assertValueEqual(drops.size(), 1, "one item, nothing spilled: " + drops);
        site.level.removeBlockEntity(grower);
        site.level.setBlock(grower, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        player.setItemInHand(InteractionHand.MAIN_HAND, drops.get(0));
        h.assertValueEqual(
                site.state(marker)
                        .useItemOn(
                                drops.get(0),
                                site.level,
                                player,
                                InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(marker), Direction.UP, marker, false)),
                ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                "a block in hand goes on the marker, not into its screen");
        drops.get(0)
                .useOn(new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(marker).add(0, 0.5, 0), Direction.UP, marker, false)));
        h.assertTrue(site.state(grower).is(MiningModule.STRUCTURE_GROWER.get()), "placed again on the marker");
        CompoundTag again = site.saved(grower);
        for (String key : List.of("segments", "treads", "lights", "steps"))
            h.assertValueEqual(again.get(key), held.get(key), key + " travels on the item");
        List<ItemStack> marked =
                Block.getDrops(site.state(marker), site.level, marker, site.entity(marker), player, ItemStack.EMPTY);
        h.assertValueEqual(marked.size(), 1, "the marker drops one item");
        h.assertTrue(marked.get(0).is(MiningModule.SURVEYED_TISSUE_ITEM.get()), "Surveyed Tissue");
        h.assertTrue(marked.get(0).getComponentsPatch().isEmpty(), "blank: the plan stays behind");
        site.release();
        h.succeed();
    }

    /**
     * A marker and a grower whose saved numbers are impossible keep their bytes; the marker refuses edits and offers
     * no plan, so the grower on it names that and builds nothing; the broken grower builds nothing either.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 4 * WAIT)
    public static void unreadablePlansAndGrowersKeepTheirBytesAndBuildNothing(GameTestHelper h) {
        Site site = Site.of(h, 7);
        BlockPos marker = site.start;
        BlockPos other = site.start.offset(-5, 0, 0);
        site.ground(0);
        site.rig(marker, STAIRS, true, 0);
        site.rig(other, STAIRS, true, 0);
        site.carve(marker, STAIRS, 1, 4);
        site.carve(other, STAIRS, 1, 4);
        for (BlockPos grower : List.of(marker.above(), other.above())) {
            site.supply(grower, segments(8));
            site.supply(grower, new ItemStack(Items.COBBLESTONE_STAIRS, 8));
        }
        site.load(marker, tag -> tag.putInt("built", 99));
        site.load(other.above(), tag -> tag.putInt("ticks", Surveying.GROW_TICKS + 1));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        SurveyMenu menu = site.surveyMenu(marker, player);
        h.assertValueEqual(menu.survey(), Optional.empty(), "no plan to show");
        menu.clickMenuButton(player, PlanEdit.SHORTER.ordinal());
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        site.stop(marker.above(), player), Optional.of(key(Surveying.NO_PLAN)), "no plan"))
                .thenIdle(WAIT)
                .thenExecute(() -> {
                    h.assertValueEqual(site.built(marker), 99, "the marker's bytes stay");
                    h.assertValueEqual(site.saved(marker).getInt("length"), 3, "and no edit reached them");
                    h.assertValueEqual(
                            site.saved(other.above()).getInt("ticks"), Surveying.GROW_TICKS + 1, "the grower's stay");
                    untouched(h, site, marker, STAIRS, 1);
                    untouched(h, site, other, STAIRS, 1);
                    site.release();
                })
                .thenSucceed();
    }

    // ---- limits and chunks ----

    /**
     * The growers of a dimension share {@value Surveying#STEPS_PER_TICK} steps a tick: of one more grower than that,
     * all due at once and ticked in one tick, one is left for the next tick. Alone in its batch.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "grower_step_limit",
            timeoutTicks = 100)
    public static void theGrowersOfADimensionShareTheirStepsEachTick(GameTestHelper h) {
        Site site = Site.of(h, 8);
        MinePlan cell = new MinePlan(PlanShape.ROUTE, 1, Turn.NONE, Turn.NONE, false);
        int growers = Surveying.STEPS_PER_TICK + 1;
        site.ground(0);
        List<BlockPos> markers = new ArrayList<>();
        for (int n = 0; n < growers; n++) {
            BlockPos marker = site.start.offset(2 * n - growers, 0, 0);
            site.rig(marker, cell, true, 0);
            site.carve(marker, cell, 1, 1);
            site.supply(marker.above(), segments(1));
            markers.add(marker);
        }
        h.startSequence()
                .thenExecute(() -> {
                    for (BlockPos marker : markers) {
                        site.load(marker.above(), tag -> tag.putInt("ticks", Surveying.GROW_TICKS));
                        tick(
                                site.level,
                                marker.above(),
                                site.entity(marker.above()).getType());
                    }
                    int built = 0;
                    for (BlockPos marker : markers) built += site.built(marker);
                    h.assertValueEqual(built, Surveying.STEPS_PER_TICK, "the limit, in one tick");
                })
                .thenWaitUntil(() -> {
                    for (BlockPos marker : markers)
                        h.assertValueEqual(site.built(marker), 1, "the last on a later tick");
                })
                .thenExecute(site::release)
                .thenSucceed();
    }

    /** Holds one chunk loaded and nothing around it: a radius-0 region ticket keeps only that chunk at full status. */
    private static final TicketType<ChunkPos> HOLD =
            TicketType.create("infestusfrontier_structure_grower_test", Comparator.comparingLong(ChunkPos::toLong));

    /**
     * The next step's neighbours are in a chunk that is not loaded: the grower names it, builds nothing and loads
     * nothing, and builds once the chunk is there. Only the grower's chunk is held, and nothing ticks it, so the test
     * ticks the grower itself as the server ticks one in a ticking chunk.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void unloadedGroundStopsTheGrowerUntilItLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        // z ends in 1 and the route runs north: its first cell is at the chunk's edge, its neighbours in the next chunk
        BlockPos marker = new BlockPos((origin.getX() & ~15) + 256008, TOP, (origin.getZ() & ~15) + 256001);
        ChunkPos home = new ChunkPos(marker);
        ChunkPos next = new ChunkPos(marker.north(2));
        level.getChunkSource().addRegionTicket(HOLD, home, 0, home);
        level.getChunkAt(marker);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (BlockPos cell : BlockPos.betweenClosed(marker.offset(-1, -1, -1), marker.offset(1, 3, 1)))
            level.setBlock(cell, Blocks.STONE.defaultBlockState(), quiet);
        for (int up = 1; up <= 3; up++) level.setBlock(marker.above(up), Blocks.AIR.defaultBlockState(), quiet);
        level.setBlock(marker.north(), Blocks.AIR.defaultBlockState(), quiet);
        level.setBlock(marker.north().above(), Blocks.AIR.defaultBlockState(), quiet);
        level.setBlock(marker.north().above(2), Blocks.AIR.defaultBlockState(), quiet);
        level.setBlock(
                marker,
                MiningModule.SURVEYED_TISSUE
                        .get()
                        .defaultBlockState()
                        .setValue(SurveyedTissueBlock.FACING, Direction.NORTH),
                quiet);
        level.setBlock(marker.above(), MiningModule.STRUCTURE_GROWER.get().defaultBlockState(), quiet);
        BlockEntity plan = level.getBlockEntity(marker);
        CompoundTag tag = plan.saveWithFullMetadata(level.registryAccess());
        tag.putInt("shape", PlanShape.ROUTE.ordinal());
        tag.putInt("length", 1);
        tag.putBoolean("approved", true);
        plan.loadWithComponents(tag, level.registryAccess());
        BlockEntity grower = level.getBlockEntity(marker.above());
        level.getCapability(Capabilities.ItemHandler.BLOCK, marker.above(), null)
                .insertItem(0, segments(2), false);
        CompoundTag due = grower.saveWithFullMetadata(level.registryAccess());
        due.putInt("ticks", Surveying.GROW_TICKS);
        grower.loadWithComponents(due, level.registryAccess());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        StructureGrowerMenu menu =
                (StructureGrowerMenu) ((MenuProvider) grower).createMenu(0, player.getInventory(), player);
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(level.hasChunk(next.x, next.z), "the next chunk is not loaded"))
                .thenExecute(() -> {
                    tick(level, marker.above(), grower.getType());
                    h.assertValueEqual(
                            menu.stopKey(), Optional.of(key(Surveying.UNLOADED)), "it names the unloaded ground");
                    h.assertFalse(level.hasChunk(next.x, next.z), "and loaded nothing");
                    h.assertTrue(level.getBlockState(marker.north()).isAir(), "and built nothing");
                    level.getChunkSource().addRegionTicket(HOLD, next, 0, next);
                    level.getChunkAt(marker.north(2));
                    for (int wait = 0;
                            wait <= Surveying.RETRY_TICKS
                                    && level.getBlockState(marker.north()).isAir();
                            wait++) tick(level, marker.above(), grower.getType());
                    h.assertTrue(
                            level.getBlockState(marker.north()).is(MiningModule.ROOT_CELL.get()), "loaded, it builds");
                    h.assertValueEqual(menu.built(), 1, "one step");
                    for (BlockPos cell : BlockPos.betweenClosed(marker.offset(-1, -1, -1), marker.offset(1, 3, 1))) {
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

    // ---- discovery ----

    /**
     * Surveyed Tissue and the Structure Grower are crafted from Overworld materials and an Organ Bud; the guide's entry
     * teaches both plan shapes and shows both recipes from recipe data; the discovery flags and the milestone exist.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void recipesMakeTheMarkerAndTheGrowerAndTheGuideTeachesBothShapes(GameTestHelper h) {
        ItemStack segment = segments(1);
        ItemStack plate = new ItemStack(ProcessingModule.BONE_PLATE.get());
        M1RouteTests.craft(
                h,
                List.of(
                        segment.copy(),
                        segment.copy(),
                        new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()),
                        new ItemStack(Items.REDSTONE)),
                MiningModule.SURVEYED_TISSUE_ITEM.get());
        M1RouteTests.craft(
                h,
                List.of(
                        new ItemStack(OrganModule.ORGAN_BUD_ITEM.get()),
                        plate.copy(),
                        plate.copy(),
                        segment.copy(),
                        segment.copy()),
                MiningModule.STRUCTURE_GROWER_ITEM.get());
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String item : List.of("surveyed_tissue", "structure_grower"))
            for (String path : List.of("acquired/" + item, "recipes/" + item))
                h.assertTrue(advancements.get(id(path)) != null, path + " exists");
        h.assertTrue(advancements.get(id("contact/surveyed_step")) != null, "the milestone exists");
        var entry = BookDataManager.get()
                .getBook(id("the_waking_genome"))
                .getCategory(id("circulation"))
                .getEntries()
                .get(id("circulation/surveyed_mine"));
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
                List.of(id("surveyed_tissue"), id("structure_grower")),
                "the guide renders both recipes from recipe data");
        var lang = StarterAdvancementTests.lang();
        for (String page : List.of("staircase", "route"))
            h.assertTrue(
                    lang.has("entry.infestusfrontier.circulation.surveyed_mine." + page + "_text"),
                    "the guide teaches the " + page);
        h.assertTrue(
                Items.OAK_STAIRS.getDefaultInstance().is(MiningModule.MINE_TREADS)
                        && Items.COBBLESTONE_STAIRS.getDefaultInstance().is(MiningModule.MINE_TREADS),
                "vanilla stairs are treads");
        h.succeed();
    }

    // ---- the demo ----

    /**
     * The demo's exhibit: a supplied staircase and a lit gallery with two turns, each partly built with the rest
     * cleared, and a draft over hidden ore. Both growers finish their plans on their own; the draft stays a draft.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1200)
    public static void theDemoExhibitGrowsAStaircaseAndAGallery(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(new BlockPos(152, -62, -63), new BlockPos(174, -40, -48), 64);
        yard.force(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        BlockPos stairs = yard.at(DEMO_STAIRS);
        BlockPos gallery = yard.at(DEMO_GALLERY);
        BlockPos draft = yard.at(DEMO_DRAFT);
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> yard.build(h))
                .thenIdle(2)
                .thenExecute(() -> {
                    for (BlockPos marker : List.of(stairs, gallery, draft))
                        h.assertTrue(
                                h.getLevel().getBlockState(marker).is(MiningModule.SURVEYED_TISSUE.get()),
                                "a marker at " + marker);
                    for (BlockPos marker : List.of(stairs, gallery))
                        h.assertTrue(
                                h.getLevel().getBlockState(marker.above()).is(MiningModule.STRUCTURE_GROWER.get()),
                                "a grower on it");
                })
                .thenWaitUntil(() -> {
                    for (BlockPos marker : List.of(stairs, gallery)) {
                        StructureGrowerMenu menu = (StructureGrowerMenu)
                                ((MenuProvider) h.getLevel().getBlockEntity(marker.above()))
                                        .createMenu(0, player.getInventory(), player);
                        h.assertValueEqual(menu.stopKey(), Optional.of(key(Surveying.FINISHED)), "it finishes");
                    }
                })
                .thenExecute(() -> {
                    SurveyMenu menu = (SurveyMenu) ((MenuProvider) h.getLevel().getBlockEntity(draft))
                            .createMenu(0, player.getInventory(), player);
                    h.assertFalse(menu.survey().orElseThrow().approved(), "the draft stays a draft");
                    h.assertValueEqual(
                            h.getLevel()
                                    .getEntitiesOfClass(ItemEntity.class, new AABB(stairs).inflate(12))
                                    .size(),
                            0,
                            "nothing loose");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    /** Where the demo's markers are, in demo coordinates. */
    static final BlockPos DEMO_STAIRS = new BlockPos(156, -50, -50);

    static final BlockPos DEMO_GALLERY = new BlockPos(164, -61, -50);
    static final BlockPos DEMO_DRAFT = new BlockPos(171, -50, -50);
}
