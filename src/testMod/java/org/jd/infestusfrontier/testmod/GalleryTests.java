package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Collecting;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.Galleries;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Rootstocking;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.core.mining.Turn;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.mining.RootCellBlock;
import org.jd.infestusfrontier.mining.TunnelRootMenu;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.testmod.GalleryRig.Cutter;
import org.jd.infestusfrontier.testmod.GalleryRig.Face;

/**
 * MINE-4, galleries: level routes cut from the landing of a finished staircase, each by its own face, all fed by one
 * biomass line and exporting into one storage ({@link GalleryRig}). The tests prove two faces at work together and in
 * turn, a face starved and a face whose output is full pausing alone and resuming with every cut accounted for, the
 * Wider Section's 3x3 cut and what it needs, the junctions and the limits on routes and faces, the longest route with
 * two turns, and unloading the whole mine mid-job.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class GalleryTests {
    /** Ticks one block of a gallery may take: a jaw's cut, a sweep and the grower's look, with a margin. */
    static final int BLOCK_TICKS = Fracturing.CUT_TICKS + Collecting.SWEEP_TICKS + Surveying.GROW_TICKS;

    private static String key(Refusal stop) {
        return "infestusfrontier.refusal." + stop.key();
    }

    private static Player hand(GameTestHelper h) {
        return EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
    }

    // ---- two faces on one line ----

    /**
     * Two narrow faces, west and north, three cells each with an ore in each, fed by one bladder and exporting into
     * one chest, on a Rootstock without the Wider Section. Both work at once and take turns: neither is ever more than
     * a few cuts ahead of the other. Both routes are cut and built; every stone is one cobblestone and every ore one
     * raw iron in the chest, the beds are empty, nothing fell loose, each jaw cut each of its blocks once and each
     * grower paid one segment a step. The steward earns the gallery and the two faces at once.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 30 * BLOCK_TICKS)
    public static void twoFacesShareOneLineInTurnAndEveryCutReachesStorage(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 0);
        rig.minehead(4000, "stairs");
        rig.face(Face.WEST, 3, Turn.NONE, Turn.NONE, false, 8);
        rig.face(Face.NORTH, 3, Turn.NONE, Turn.NONE, false, 8);
        rig.cutter(Cutter.WEST_LEFT);
        rig.cutter(Cutter.NORTH_RIGHT);
        for (Face face : List.of(Face.WEST, Face.NORTH))
            rig.level.setBlockAndUpdate(
                    rig.at(GalleryRig.plan(
                            face, rig.plan(face).steps().get(1).passage().get(0))),
                    Blocks.IRON_ORE.defaultBlockState());
        int stone = rig.count(Face.WEST, Blocks.STONE) + rig.count(Face.NORTH, Blocks.STONE);
        ServerPlayer steward = player(h);
        Organs.placedBy(rig.level, rig.at(GalleryRig.HEAD), steward);
        PendingCredits credits = PendingCredits.get(rig.level);
        Player player = hand(h);
        int[] widest = new int[1];
        h.startSequence()
                .thenExecute(() -> h.assertValueEqual(stone, 16, "sixteen stones and two ores to cut"))
                .thenWaitUntil(() -> {
                    List<TunnelRootMenu.Face> faces = rig.faces(player);
                    h.assertValueEqual(faces.size(), 2, "the landing opens two faces");
                    h.assertTrue(faces.stream().allMatch(f -> f.stop().isEmpty()), "both at work: " + faces);
                })
                .thenWaitUntil(() -> {
                    int west = rig.cuts(Cutter.WEST_LEFT);
                    int north = rig.cuts(Cutter.NORTH_RIGHT);
                    widest[0] = Math.max(widest[0], Math.abs(west - north));
                    h.assertTrue(west >= 6 && north >= 6, "both faces cut together: " + west + ", " + north);
                })
                .thenWaitUntil(() -> {
                    h.assertValueEqual(rig.built(Face.WEST), 3, "the west route is built");
                    h.assertValueEqual(rig.built(Face.NORTH), 3, "the north route is built");
                })
                .thenWaitUntil(() -> {
                    h.assertValueEqual(rig.stored(Items.COBBLESTONE), stone, "every stone in storage");
                    h.assertValueEqual(rig.stored(Items.RAW_IRON), 2, "and every ore");
                })
                .thenWaitUntil(() -> {
                    h.assertValueEqual(rig.faces(player).size(), 0, "finished routes are no longer faces");
                    h.assertValueEqual(
                            rig.menu(player).stopKey(),
                            Optional.of(key(Rootstocking.FINISHED)),
                            "the minehead has nothing left to cut");
                })
                .thenExecute(() -> {
                    h.assertTrue(widest[0] <= 2, "in turn: one face was at most two cuts ahead, " + widest[0]);
                    h.assertValueEqual(rig.cuts(Cutter.WEST_LEFT), 9, "the west jaw cut each block once");
                    h.assertValueEqual(rig.cuts(Cutter.NORTH_RIGHT), 9, "the north jaw cut each block once");
                    h.assertValueEqual(rig.bed(Cutter.WEST_LEFT) + rig.bed(Cutter.NORTH_RIGHT), 0, "beds empty");
                    h.assertValueEqual(rig.loose(), 0, "nothing fell loose");
                    for (Face face : List.of(Face.WEST, Face.NORTH)) {
                        assertNarrow(h, rig, face);
                        CompoundTag grower = rig.saved(face.grower());
                        h.assertValueEqual(grower.getInt("steps"), 3, face + ": three steps built");
                        h.assertValueEqual(
                                grower.getCompound("segments").getInt("count"), 5, face + ": a segment a step");
                    }
                    Path path = rig.walk(rig.at(new BlockPos(-2, -2, -4)), rig.at(new BlockPos(-4, -2, -4)));
                    h.assertTrue(path != null && path.canReach(), "a zombie walks the west gallery");
                    credits.award(steward);
                    h.assertTrue(done(h, steward, "contact/mining_gallery"), "a gallery step on its own");
                    h.assertTrue(done(h, steward, "contact/shared_faces"), "and two faces at once");
                    rig.release();
                })
                .thenSucceed();
    }

    /** Every built cell of {@code face}'s narrow route: a root cell with two clear blocks over it. */
    static void assertNarrow(GameTestHelper h, GalleryRig rig, Face face) {
        for (PlanStep step : rig.plan(face).steps()) {
            BlockState root = rig.level.getBlockState(rig.at(GalleryRig.plan(face, step.root())));
            h.assertTrue(root.is(MiningModule.ROOT_CELL.get()), face + " step " + step.number() + ": a root cell");
            h.assertValueEqual(
                    root.getValue(RootCellBlock.BEND), step.bend(), face + " step " + step.number() + " bend");
            for (Offset cell : step.passage())
                h.assertTrue(
                        rig.level
                                .getBlockState(rig.at(GalleryRig.plan(face, cell)))
                                .isAir(),
                        face + " step " + step.number() + ": clear at " + cell);
        }
    }

    // ---- one face paused, the other at work ----

    /**
     * Two faces at work. The west face's jaw is cut off its supply and runs dry: the Rootstock's screen names that face
     * paused at its jaw for biomass while the north face keeps cutting. Its vein is put back and it cuts again. Then its
     * bed is filled with blocks no storage takes: the screen names the face paused at its cilia for full storage while
     * the north face still cuts. The blocks are taken out and it goes on. Both routes finish with every mined block in
     * storage exactly once and nothing loose.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 60 * BLOCK_TICKS)
    public static void aStarvedOrFullFacePausesAloneAndResumesWhenRestored(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 1);
        rig.minehead(4000, "stairs");
        rig.face(Face.WEST, 4, Turn.NONE, Turn.NONE, false, 8);
        rig.face(Face.NORTH, 6, Turn.NONE, Turn.NONE, false, 8);
        rig.cutter(Cutter.WEST_LEFT);
        rig.cutter(Cutter.NORTH_RIGHT);
        int stone = rig.count(Face.WEST, Blocks.STONE) + rig.count(Face.NORTH, Blocks.STONE);
        Player player = hand(h);
        BlockPos vein = Cutter.WEST_LEFT.jaw.below();
        int[] north = new int[1];
        int[] west = new int[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        rig.cuts(Cutter.WEST_LEFT) >= 1 && rig.cuts(Cutter.NORTH_RIGHT) >= 1, "both at work"))
                .thenExecute(() -> {
                    // the west jaw's supply is cut, and what its tank holds is poured back into the bladder
                    rig.set(vein, "infestusfrontier:living_substrate[stage=mature]");
                    CompoundTag jaw = rig.saved(Cutter.WEST_LEFT.jaw);
                    int held = jaw.getCompound("tank").getInt("stored");
                    jaw.getCompound("tank").putInt("stored", 0);
                    rig.entity(Cutter.WEST_LEFT.jaw).loadWithComponents(jaw, rig.level.registryAccess());
                    CompoundTag bladder = rig.saved(GalleryRig.BLADDER);
                    CompoundTag tank = bladder.getCompound("tank");
                    tank.putInt("stored", tank.getInt("stored") + held);
                    rig.entity(GalleryRig.BLADDER).loadWithComponents(bladder, rig.level.registryAccess());
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.faceStop(player, 0), Optional.of(key(Fracturing.NO_BIOMASS)), "west paused for biomass"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.faceStop(player, 1), Optional.empty(), "north still at work");
                    west[0] = rig.cuts(Cutter.WEST_LEFT);
                    north[0] = rig.cuts(Cutter.NORTH_RIGHT);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        rig.cuts(Cutter.NORTH_RIGHT) >= north[0] + 2, "the north face goes on cutting alone"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.cuts(Cutter.WEST_LEFT), west[0], "the starved face cut nothing");
                    rig.set(vein, "infestusfrontier:biomass_vein");
                })
                .thenWaitUntil(() ->
                        h.assertTrue(rig.cuts(Cutter.WEST_LEFT) > west[0], "supplied again, the west face resumes"))
                .thenWaitUntil(() -> h.assertValueEqual(rig.faceStop(player, 0), Optional.empty(), "and works"))
                .thenExecute(() -> {
                    // the west bed is filled with what no storage takes
                    CompoundTag bed = rig.saved(Cutter.WEST_LEFT.bed());
                    ListTag slots = new ListTag();
                    for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++)
                        slots.add(new ItemStack(Items.OBSIDIAN, 64).save(rig.level.registryAccess()));
                    bed.put("slots", slots);
                    rig.entity(Cutter.WEST_LEFT.bed()).loadWithComponents(bed, rig.level.registryAccess());
                    west[0] = rig.cuts(Cutter.WEST_LEFT);
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.faceStop(player, 0),
                        Optional.of(key(Collecting.STORAGE_FULL)),
                        "west paused: its output has nowhere to go"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.faceStop(player, 1), Optional.empty(), "north still at work");
                    north[0] = rig.cuts(Cutter.NORTH_RIGHT);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        rig.cuts(Cutter.NORTH_RIGHT) >= north[0] + 2, "the north face goes on cutting alone"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.cuts(Cutter.WEST_LEFT), west[0], "the full face cut nothing");
                    CompoundTag bed = rig.saved(Cutter.WEST_LEFT.bed());
                    ListTag slots = new ListTag();
                    for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++) slots.add(new CompoundTag());
                    bed.put("slots", slots);
                    rig.entity(Cutter.WEST_LEFT.bed()).loadWithComponents(bed, rig.level.registryAccess());
                })
                .thenWaitUntil(() -> h.assertTrue(rig.cuts(Cutter.WEST_LEFT) > west[0], "emptied, it resumes"))
                .thenWaitUntil(() -> {
                    h.assertValueEqual(rig.built(Face.WEST), 4, "the west route is built");
                    h.assertValueEqual(rig.built(Face.NORTH), 6, "the north route is built");
                })
                .thenWaitUntil(() -> h.assertValueEqual(rig.stored(Items.COBBLESTONE), stone, "every cut in storage"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.cuts(Cutter.WEST_LEFT), 12, "each west block cut once");
                    h.assertValueEqual(rig.cuts(Cutter.NORTH_RIGHT), 18, "each north block cut once");
                    h.assertValueEqual(rig.stored(Items.OBSIDIAN), 0, "nothing else reached storage");
                    h.assertValueEqual(rig.bed(Cutter.WEST_LEFT) + rig.bed(Cutter.NORTH_RIGHT), 0, "beds empty");
                    h.assertValueEqual(rig.loose(), 0, "nothing fell loose");
                    rig.release();
                })
                .thenSucceed();
    }

    // ---- the Wider Section ----

    /**
     * A wide west route of two steps and a narrow north route from one landing. On a Rootstock without the Wider
     * Section the wide face pauses naming the mutation and cuts nothing, while the narrow face works. With the mutation
     * and only two jaws it pauses naming the missing jaw. With the third jaw it cuts: every step is three root cells
     * across with three clear blocks over each, three segments a step, twelve cuts a step shared by the three jaws, and
     * all of it in storage. The narrow route still finishes beside it. The steward earns the wide gallery.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 60 * BLOCK_TICKS)
    public static void theWiderSectionCutsThreeByThreeOnlyWithItsMutationAndThreeJaws(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 2);
        rig.minehead(4000, "stairs");
        rig.face(Face.WEST, 2, Turn.NONE, Turn.NONE, true, 8);
        rig.face(Face.NORTH, 2, Turn.NONE, Turn.NONE, false, 4);
        rig.cutter(Cutter.WEST_LEFT);
        rig.cutter(Cutter.WEST_RIGHT);
        rig.cutter(Cutter.WEST_BACK);
        rig.cutter(Cutter.NORTH_RIGHT);
        BlockPos third = rig.at(Cutter.WEST_BACK.jaw);
        rig.level.setBlockAndUpdate(
                third, rig.level.getBlockState(third).setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        int wideStone = rig.count(Face.WEST, Blocks.STONE);
        int narrowStone = rig.count(Face.NORTH, Blocks.STONE);
        ServerPlayer steward = player(h);
        Organs.placedBy(rig.level, rig.at(GalleryRig.HEAD), steward);
        PendingCredits credits = PendingCredits.get(rig.level);
        Player player = hand(h);
        h.startSequence()
                .thenExecute(() -> {
                    h.assertValueEqual(wideStone, 24, "two wide steps: twelve blocks each");
                    h.assertValueEqual(narrowStone, 6, "two narrow steps: three blocks each");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.faceStop(player, 0),
                        Optional.of(key(Galleries.NEEDS_WIDER_SECTION)),
                        "a wide face needs the mutation"))
                .thenWaitUntil(() -> h.assertValueEqual(rig.built(Face.NORTH), 2, "the narrow face works"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.cuts(Cutter.WEST_LEFT) + rig.cuts(Cutter.WEST_RIGHT), 0, "nothing cut");
                    CompoundTag head = rig.saved(GalleryRig.HEAD);
                    ListTag mutations = new ListTag();
                    mutations.add(StringTag.valueOf("stairs"));
                    mutations.add(StringTag.valueOf("wider_section"));
                    head.put("mutations", mutations);
                    rig.entity(GalleryRig.HEAD).loadWithComponents(head, rig.level.registryAccess());
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.faceStop(player, 0), Optional.of(key(Galleries.NEEDS_JAWS)), "and three jaws"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.cuts(Cutter.WEST_LEFT) + rig.cuts(Cutter.WEST_RIGHT), 0, "still none");
                    rig.level.setBlockAndUpdate(
                            third,
                            rig.level.getBlockState(third).setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
                })
                .thenWaitUntil(() -> h.assertValueEqual(rig.built(Face.WEST), 2, "with three jaws it is cut and built"))
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.stored(Items.COBBLESTONE), wideStone + narrowStone, "every block in storage"))
                .thenExecute(() -> {
                    MinePlan wide = rig.plan(Face.WEST);
                    for (PlanStep step : wide.steps()) {
                        h.assertValueEqual(step.roots().size(), 3, "three root cells across");
                        for (Offset root : step.roots()) {
                            h.assertTrue(
                                    rig.level
                                            .getBlockState(rig.at(GalleryRig.plan(Face.WEST, root)))
                                            .is(MiningModule.ROOT_CELL.get()),
                                    "a root cell at " + root);
                            for (int up = 1; up <= Surveying.WIDE_HEADROOM; up++)
                                h.assertTrue(
                                        rig.level
                                                .getBlockState(rig.at(GalleryRig.plan(Face.WEST, root.above(up))))
                                                .isAir(),
                                        "clear " + up + " over " + root);
                        }
                    }
                    int jaws = rig.cuts(Cutter.WEST_LEFT) + rig.cuts(Cutter.WEST_RIGHT) + rig.cuts(Cutter.WEST_BACK);
                    h.assertValueEqual(jaws, 24, "twelve cuts a wide step, each block once");
                    h.assertTrue(
                            rig.cuts(Cutter.WEST_LEFT) > 0
                                    && rig.cuts(Cutter.WEST_RIGHT) > 0
                                    && rig.cuts(Cutter.WEST_BACK) > 0,
                            "all three jaws cut: " + rig.cuts(Cutter.WEST_LEFT) + ", " + rig.cuts(Cutter.WEST_RIGHT)
                                    + ", " + rig.cuts(Cutter.WEST_BACK));
                    h.assertValueEqual(
                            rig.saved(Face.WEST.grower())
                                    .getCompound("segments")
                                    .getInt("count"),
                            2,
                            "three segments a wide step");
                    assertNarrow(h, rig, Face.NORTH);
                    h.assertValueEqual(rig.loose(), 0, "nothing fell loose");
                    credits.award(steward);
                    h.assertTrue(done(h, steward, "contact/wide_gallery"), "a wide step on its own");
                    rig.release();
                })
                .thenSucceed();
    }

    // ---- junctions and limits ----

    /**
     * Route markers around the landing: one facing back toward the junction is no face, a draft is a face that names
     * its draft, and a finished north route makes its last cell a second junction whose three sides open faces too.
     * Five unfinished routes are more than one minehead works: every face pauses and the screen names the limit. One
     * marker taken out, the four faces are back. Nothing is cut while the limit holds.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void junctionsJoinThreeRoutesAndAMineheadWorksAtMostFourFaces(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 3);
        rig.minehead(0, "stairs");
        rig.face(Face.WEST, 4, Turn.NONE, Turn.NONE, false, 0);
        rig.face(Face.EAST, 4, Turn.NONE, Turn.NONE, false, 0);
        // a finished north route of two cells: its end is a junction
        rig.face(Face.NORTH, 2, Turn.NONE, Turn.NONE, false, 0);
        MinePlan north = rig.plan(Face.NORTH);
        for (PlanStep step : north.steps()) {
            for (Offset cell : step.passage()) rig.set(GalleryRig.plan(Face.NORTH, cell), "air");
            rig.set(GalleryRig.plan(Face.NORTH, step.root()), "infestusfrontier:root_cell[facing=north,bend=false]");
        }
        CompoundTag finished = rig.saved(Face.NORTH.marker);
        finished.putInt("built", 2);
        rig.entity(Face.NORTH.marker).loadWithComponents(finished, rig.level.registryAccess());
        BlockPos end = GalleryRig.plan(Face.NORTH, north.end());
        List<BlockPos> branches = new ArrayList<>();
        for (Direction side : List.of(Direction.WEST, Direction.NORTH, Direction.EAST)) {
            BlockPos at = end.relative(side);
            branches.add(at);
            rig.set(
                    at,
                    ("infestusfrontier:surveyed_tissue[facing=%s]{shape:1,length:3,first_way:0,first_at:0,"
                                    + "second_way:0,second_at:0,lit:0b,approved:%s,built:0}")
                            .formatted(side.getSerializedName(), side == Direction.NORTH ? "0b" : "1b"));
        }
        Player player = hand(h);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.menu(player).stopKey(),
                        Optional.of(key(Galleries.TOO_MANY_FACES)),
                        "five unfinished routes: more than a minehead works"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.faces(player).size(), 0, "no face is worked");
                    rig.set(branches.get(2), "stone");
                })
                .thenWaitUntil(() -> h.assertValueEqual(rig.faces(player).size(), 4, "four faces: the limit"))
                .thenExecute(() -> {
                    // the landing's west and east, then the second junction's west and (draft) north
                    List<TunnelRootMenu.Face> faces = rig.faces(player);
                    h.assertValueEqual(faces.get(0).stop(), Optional.of(Rootstocking.NO_JAW), "west: no jaw yet");
                    h.assertValueEqual(faces.get(1).stop(), Optional.of(Rootstocking.NO_JAW), "east: no jaw yet");
                    h.assertValueEqual(
                            faces.get(2).stop(),
                            Optional.of(Rootstocking.NO_GROWER),
                            "beyond the second junction: no grower yet");
                    h.assertValueEqual(
                            faces.get(3).stop(), Optional.of(Rootstocking.NOT_APPROVED), "and a draft names its draft");
                    h.assertValueEqual(rig.menu(player).stop(), Optional.empty(), "the minehead itself works");
                    // a marker beside the landing facing back toward it is no route
                    rig.set(
                            Face.EAST.marker,
                            "infestusfrontier:surveyed_tissue[facing=west]{shape:1,length:3,first_way:0,first_at:0,"
                                    + "second_way:0,second_at:0,lit:0b,approved:1b,built:0}");
                })
                .thenWaitUntil(() -> h.assertValueEqual(rig.faces(player).size(), 3, "a turned marker is no face"))
                .thenExecute(() -> {
                    h.assertValueEqual(Galleries.ROUTES_PER_JUNCTION, 3, "three sides a junction");
                    rig.release();
                })
                .thenSucceed();
    }

    /**
     * Finished one-cell routes branch from the landing and from the end of each: three from the landing, three from
     * each of their ends, then more from one end further on. With thirteen routes the minehead looks at them all and
     * has nothing to cut; a fourteenth is more than it looks at, and it says so.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void aMineheadLooksAtNoMoreThanThirteenRoutes(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 6);
        rig.minehead(0, "stairs");
        BlockPos landing = GalleryRig.LANDING;
        List<BlockPos> markers = new ArrayList<>();
        // Each marker: x and z from the landing, and the way it faces (0 south, 1 west, 2 north, 3 east). The
        // landing's three, three from each of their ends, one from the end of the west route's west branch, and the
        // fourteenth.
        int[][] routes = {
            {-1, 0, 1},
            {0, -1, 2},
            {1, 0, 3},
            {-2, 1, 0},
            {-3, 0, 1},
            {-2, -1, 2},
            {-1, -2, 1},
            {0, -3, 2},
            {1, -2, 3},
            {2, -1, 2},
            {3, 0, 3},
            {2, 1, 0},
            {-5, 0, 1},
            {-4, 1, 0}
        };
        for (int[] route : routes) {
            BlockPos at = landing.offset(route[0], 0, route[1]);
            markers.add(at);
            rig.set(
                    at,
                    ("infestusfrontier:surveyed_tissue[facing=%s]{shape:1,length:1,first_way:0,first_at:0,"
                                    + "second_way:0,second_at:0,lit:0b,approved:1b,built:1}")
                            .formatted(Direction.from2DDataValue(route[2]).getSerializedName()));
        }
        Player player = hand(h);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.menu(player).stopKey(),
                        Optional.of(key(Galleries.TOO_MANY_ROUTES)),
                        "fourteen routes: more than a minehead looks at"))
                .thenExecute(() -> rig.set(markers.get(markers.size() - 1), "stone"))
                .thenWaitUntil(() -> h.assertValueEqual(
                        rig.menu(player).stopKey(),
                        Optional.of(key(Rootstocking.FINISHED)),
                        "thirteen: every route is finished and there is nothing to cut"))
                .thenExecute(() -> {
                    h.assertValueEqual(Galleries.MAX_ROUTES, 13, "the limit");
                    rig.release();
                })
                .thenSucceed();
    }

    /**
     * The longest route, 64 cells with two turns, from the landing. Its first 61 steps stand built, as an earlier run
     * left them; the face cuts and builds the last three. Every cell is a root cell with two clear blocks over it, the
     * root bends at both turns, and a zombie walks the gallery from its first cell to its last.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 20 * BLOCK_TICKS)
    public static void theLongestRouteRunsSixtyFourCellsWithTwoTurns(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 4);
        rig.minehead(4000, "stairs");
        Turn left = new Turn(Steer.LEFT, 20);
        Turn right = new Turn(Steer.RIGHT, 40);
        rig.face(Face.NORTH, Surveying.MAX_ROUTE_LENGTH, left, right, false, 8);
        rig.cutter(Cutter.NORTH_RIGHT);
        MinePlan plan = rig.plan(Face.NORTH);
        int prebuilt = Surveying.MAX_ROUTE_LENGTH - 3;
        for (PlanStep step : plan.steps()) rig.cover(GalleryRig.plan(Face.NORTH, step.root()));
        for (PlanStep step : plan.steps()) {
            BlockPos root = GalleryRig.plan(Face.NORTH, step.root());
            rig.level.setBlockAndUpdate(rig.at(root.below()), Blocks.STONE.defaultBlockState());
            for (Offset cell : step.cleared())
                rig.level.setBlockAndUpdate(
                        rig.at(GalleryRig.plan(Face.NORTH, cell)), Blocks.STONE.defaultBlockState());
        }
        for (PlanStep step : plan.steps().subList(0, prebuilt)) {
            for (Offset cell : step.passage())
                rig.level.setBlockAndUpdate(rig.at(GalleryRig.plan(Face.NORTH, cell)), Blocks.AIR.defaultBlockState());
            Direction arrived = turned(Direction.NORTH, step.arrived());
            Direction leaving = turned(Direction.NORTH, step.leaving());
            rig.set(
                    GalleryRig.plan(Face.NORTH, step.root()),
                    "infestusfrontier:root_cell[facing=%s,bend=%s]"
                            .formatted(
                                    (step.bend() ? bendFacing(arrived, leaving) : leaving).getSerializedName(),
                                    step.bend()));
        }
        CompoundTag marker = rig.saved(Face.NORTH.marker);
        marker.putInt("built", prebuilt);
        rig.entity(Face.NORTH.marker).loadWithComponents(marker, rig.level.registryAccess());
        int stone = rig.count(Face.NORTH, Blocks.STONE);
        h.startSequence()
                .thenExecute(() -> h.assertValueEqual(stone, 9, "three steps left to cut"))
                .thenWaitUntil(
                        () -> h.assertValueEqual(rig.built(Face.NORTH), Surveying.MAX_ROUTE_LENGTH, "built to its end"))
                .thenWaitUntil(() -> h.assertValueEqual(rig.stored(Items.COBBLESTONE), stone, "the cut in storage"))
                .thenExecute(() -> {
                    List<PlanStep> steps = plan.steps();
                    h.assertValueEqual(steps.size(), 64, "64 cells");
                    h.assertValueEqual(steps.stream().filter(PlanStep::bend).count(), 2L, "two turns");
                    for (PlanStep step : steps) {
                        BlockState root = rig.level.getBlockState(rig.at(GalleryRig.plan(Face.NORTH, step.root())));
                        h.assertTrue(root.is(MiningModule.ROOT_CELL.get()), "cell " + step.number() + ": a root");
                        h.assertValueEqual(root.getValue(RootCellBlock.BEND), step.bend(), "bend at " + step.number());
                        for (Offset cell : step.passage())
                            h.assertTrue(
                                    rig.level
                                            .getBlockState(rig.at(GalleryRig.plan(Face.NORTH, cell)))
                                            .isAir(),
                                    "clear over cell " + step.number());
                    }
                    BlockPos first = rig.at(
                            GalleryRig.plan(Face.NORTH, steps.get(0).root().above(1)));
                    BlockPos last =
                            rig.at(GalleryRig.plan(Face.NORTH, plan.end().above(1)));
                    Path path = rig.walk(first, last);
                    h.assertTrue(path != null && path.canReach(), "a zombie walks all 64 cells");
                    h.assertValueEqual(path.getTarget(), last, "to the end");
                    for (PlanStep step : steps) {
                        BlockPos root = GalleryRig.plan(Face.NORTH, step.root());
                        for (int up = -1; up <= Surveying.ROUTE_HEADROOM; up++)
                            rig.level.setBlockAndUpdate(rig.at(root.above(up)), Blocks.AIR.defaultBlockState());
                    }
                    rig.release();
                })
                .thenSucceed();
    }

    /** The way {@code quarters} quarter turns clockwise of {@code forward}. */
    private static Direction turned(Direction forward, int quarters) {
        Direction way = forward;
        for (int n = 0; n < quarters; n++) way = way.getClockWise();
        return way;
    }

    /** The facing a bend cell from {@code arrived} to {@code leaving} is saved with, as the grower lays it. */
    private static Direction bendFacing(Direction arrived, Direction leaving) {
        Direction back = arrived.getOpposite();
        return leaving == back.getClockWise() ? back : leaving;
    }

    // ---- unloading ----

    /**
     * Two faces at work; the whole mine is written to disk, unloaded and read back as new block entities. While it is
     * gone nothing is cut. Back, nothing is reset: the plans, the built steps and every jaw's and grower's history are
     * at least what they were, and the faces finish with each block cut once and in storage once.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "gallery_unload",
            timeoutTicks = 60 * BLOCK_TICKS)
    public static void unloadingTheMineMidJobLosesAndRepeatsNothing(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 5);
        rig.minehead(4000, "stairs");
        rig.face(Face.WEST, 3, Turn.NONE, Turn.NONE, false, 8);
        rig.face(Face.NORTH, 3, Turn.NONE, Turn.NONE, false, 8);
        rig.cutter(Cutter.WEST_LEFT);
        rig.cutter(Cutter.NORTH_RIGHT);
        int stone = rig.count(Face.WEST, Blocks.STONE) + rig.count(Face.NORTH, Blocks.STONE);
        List<BlockPos> kept = List.of(
                GalleryRig.HEAD, Face.WEST.marker, Face.NORTH.marker, Cutter.WEST_LEFT.jaw, Cutter.NORTH_RIGHT.jaw);
        List<BlockEntity> before = new ArrayList<>();
        int[] counted = new int[4];
        Player player = hand(h);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(rig.built(Face.WEST) >= 1 && rig.built(Face.NORTH) >= 1, "mid-job"))
                .thenExecute(() -> {
                    for (BlockPos pos : kept) before.add(rig.entity(pos));
                    counted[0] = rig.built(Face.WEST);
                    counted[1] = rig.built(Face.NORTH);
                    counted[2] = rig.cuts(Cutter.WEST_LEFT);
                    counted[3] = rig.cuts(Cutter.NORTH_RIGHT);
                    rig.level.save(null, true, false);
                    for (ChunkPos chunk : rig.chunks()) rig.level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(rig.level.hasChunkAt(rig.start), "the mine is unloaded"))
                .thenExecute(() -> rig.level.save(null, true, false))
                .thenIdle(2 * Fracturing.CUT_TICKS)
                .thenExecute(() -> {
                    for (ChunkPos chunk : rig.chunks()) TestChunks.force(rig.level, chunk);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        rig.level.hasChunkAt(rig.start) && rig.entity(Cutter.NORTH_RIGHT.cilia) != null,
                        "the mine is back"))
                .thenExecute(() -> {
                    for (int n = 0; n < kept.size(); n++)
                        h.assertTrue(rig.entity(kept.get(n)) != before.get(n), kept.get(n) + ": read from disk");
                    h.assertTrue(rig.built(Face.WEST) >= counted[0], "the west plan is not reset");
                    h.assertTrue(rig.built(Face.NORTH) >= counted[1], "nor the north one");
                    h.assertTrue(rig.cuts(Cutter.WEST_LEFT) >= counted[2], "nor the west jaw's history");
                    h.assertTrue(rig.cuts(Cutter.NORTH_RIGHT) >= counted[3], "nor the north jaw's");
                })
                .thenWaitUntil(() -> h.assertValueEqual(rig.faces(player).size(), 2, "the minehead finds its faces"))
                .thenWaitUntil(() -> {
                    h.assertValueEqual(rig.built(Face.WEST), 3, "the west route finishes");
                    h.assertValueEqual(rig.built(Face.NORTH), 3, "the north route finishes");
                })
                .thenWaitUntil(() -> h.assertValueEqual(rig.stored(Items.COBBLESTONE), stone, "every cut once"))
                .thenExecute(() -> {
                    h.assertValueEqual(rig.cuts(Cutter.WEST_LEFT) + rig.cuts(Cutter.NORTH_RIGHT), stone, "cut once");
                    h.assertValueEqual(rig.bed(Cutter.WEST_LEFT) + rig.bed(Cutter.NORTH_RIGHT), 0, "beds empty");
                    h.assertValueEqual(rig.loose(), 0, "nothing fell loose");
                    rig.release();
                })
                .thenSucceed();
    }

    // ---- the guide ----

    /**
     * The guide's gallery entry loads, teaches every part of the shipped behaviour and renders the Wider Section
     * Construct from its recipe data; the three gallery advancements exist; every reason the Rootstock's screen can name
     * for a face is worded.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGuideTeachesGalleriesFromImplementedBehaviour(GameTestHelper h) {
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String path : List.of("contact/mining_gallery", "contact/shared_faces", "contact/wide_gallery"))
            h.assertTrue(advancements.get(StarterAdvancementTests.id(path)) != null, path + " exists");
        var entry = com.klikli_dev.modonomicon.data.BookDataManager.get()
                .getBook(StarterAdvancementTests.id("the_waking_genome"))
                .getCategory(StarterAdvancementTests.id("circulation"))
                .getEntries()
                .get(StarterAdvancementTests.id("circulation/mining_galleries"));
        h.assertTrue(entry != null, "the guide entry loaded");
        List<net.minecraft.resources.ResourceLocation> shown = new ArrayList<>();
        entry.getPages().stream()
                .filter(page -> page instanceof com.klikli_dev.modonomicon.book.page.BookRecipePage<?>)
                .map(page -> (com.klikli_dev.modonomicon.book.page.BookRecipePage<?>) page)
                .forEach(page -> shown.add(page.getRecipeId1()));
        h.assertValueEqual(
                shown,
                List.of(StarterAdvancementTests.id("construct/wider_section")),
                "the construct comes from recipe data");
        var lang = StarterAdvancementTests.lang();
        for (String page :
                List.of("use", "junction_text", "face_text", "share_text", "pause_text", "wide_text", "limits_text"))
            h.assertTrue(
                    lang.has("entry.infestusfrontier.circulation.mining_galleries." + page), "the guide has " + page);
        for (Refusal stop : TunnelRootMenu.STOPS)
            h.assertTrue(lang.has(key(stop)), "every reason a face can show is worded: " + stop.key());
        h.assertTrue(TunnelRootMenu.STOPS.containsAll(Galleries.STOPS), "the screen can name every gallery stop");
        h.succeed();
    }

    // ---- the demo ----

    private static final BlockPos DEMO_MIN = new BlockPos(37, -60, 8);
    private static final BlockPos DEMO_MAX = new BlockPos(53, -48, 23);
    private static final BlockPos DEMO_HEAD = new BlockPos(44, -51, 20);
    private static final BlockPos DEMO_CHEST = new BlockPos(49, -57, 16);
    private static final BlockPos DEMO_WIDE = new BlockPos(43, -54, 16);
    private static final BlockPos DEMO_NARROW = new BlockPos(44, -54, 15);

    /** Where {@code offset} of the plan of a marker at {@code marker} facing {@code facing} lies. */
    private static BlockPos cell(BlockPos marker, Direction facing, Offset offset) {
        return marker.relative(facing, offset.forward())
                .relative(facing.getClockWise(), offset.right())
                .above(offset.up());
    }

    /**
     * The demo's gallery exhibit, built from the demo's own commands: a minehead on a hill whose staircase is done,
     * with a wide three-jaw face and a narrow one-jaw face at its landing on one biomass line and one export line,
     * each gallery part-built as an earlier run left it. Both faces work at once; both galleries are finished, the
     * wide one three across and three high; everything still to cut arrives in the exhibit's chest and nothing is loose.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 60 * BLOCK_TICKS)
    public static void theDemoExhibitCutsBothGalleriesFromOneLine(GameTestHelper h) {
        DemoExhibit mine = new DemoExhibit(DEMO_MIN, DEMO_MAX, 72);
        mine.force(h);
        var level = h.getLevel();
        Player player = hand(h);
        MinePlan wide =
                new MinePlan(org.jd.infestusfrontier.core.mining.PlanShape.ROUTE, 4, Turn.NONE, Turn.NONE, false, true);
        MinePlan narrow =
                new MinePlan(org.jd.infestusfrontier.core.mining.PlanShape.ROUTE, 6, Turn.NONE, Turn.NONE, false);
        int[] stone = new int[1];
        int[] ore = new int[1];
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    mine.tidy(h);
                    mine.place(h);
                    List<BlockPos> left = new ArrayList<>();
                    for (PlanStep step : wide.steps().subList(2, 4))
                        for (Offset offset : step.cleared()) left.add(cell(DEMO_WIDE, Direction.WEST, offset));
                    for (PlanStep step : narrow.steps().subList(3, 6))
                        for (Offset offset : step.cleared()) left.add(cell(DEMO_NARROW, Direction.NORTH, offset));
                    for (BlockPos at : left) {
                        BlockState state = level.getBlockState(mine.at(at));
                        if (state.is(Blocks.STONE)) stone[0]++;
                        if (state.is(Blocks.IRON_ORE)) ore[0]++;
                    }
                    h.assertValueEqual(ore[0], 3, "three ores left to cut");
                    h.assertValueEqual(stone[0] + ore[0], 2 * 12 + 3 * 3, "two wide steps and three narrow ones");
                })
                .thenWaitUntil(() -> {
                    List<TunnelRootMenu.Face> faces = RootstockTests.menu(h, level, mine.at(DEMO_HEAD), player)
                            .faces();
                    h.assertValueEqual(faces.size(), 2, "the landing opens two faces");
                    h.assertTrue(faces.stream().allMatch(f -> f.stop().isEmpty()), "both at work: " + faces);
                    h.assertTrue(faces.get(0).wide() && !faces.get(1).wide(), "a wide face and a narrow one");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        RootstockTests.menu(h, level, mine.at(DEMO_HEAD), player)
                                .stopKey(),
                        Optional.of(key(Rootstocking.FINISHED)),
                        "both galleries finish"))
                .thenWaitUntil(() -> {
                    var chest = (net.minecraft.world.Container) level.getBlockEntity(mine.at(DEMO_CHEST));
                    h.assertValueEqual(chest.countItem(Items.COBBLESTONE), stone[0], "the spoil in the chest");
                    h.assertValueEqual(chest.countItem(Items.RAW_IRON), ore[0], "and the raw iron");
                })
                .thenExecute(() -> {
                    for (PlanStep step : wide.steps())
                        for (Offset root : step.roots()) {
                            h.assertTrue(
                                    level.getBlockState(mine.at(cell(DEMO_WIDE, Direction.WEST, root)))
                                            .is(MiningModule.ROOT_CELL.get()),
                                    "a wide root at " + root);
                            for (int up = 1; up <= Surveying.WIDE_HEADROOM; up++)
                                h.assertTrue(
                                        level.getBlockState(mine.at(cell(DEMO_WIDE, Direction.WEST, root.above(up))))
                                                .isAir(),
                                        "clear over " + root);
                        }
                    h.assertValueEqual(
                            level.getEntitiesOfClass(
                                            net.minecraft.world.entity.item.ItemEntity.class,
                                            net.minecraft
                                                    .world
                                                    .phys
                                                    .AABB
                                                    .encapsulatingFullBlocks(mine.at(DEMO_MIN), mine.at(DEMO_MAX))
                                                    .inflate(4))
                                    .size(),
                            0,
                            "nothing loose");
                    mine.tidy(h);
                    mine.forget(h);
                })
                .thenSucceed();
    }
}
