package org.jd.infestusfrontier.testmod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.mining.Turn;
import org.jd.infestusfrontier.testmod.GalleryRig.Cutter;
import org.jd.infestusfrontier.testmod.GalleryRig.Face;

/**
 * A real server restart in the middle of a gallery job. These tests run only in the two restart runs of the gate,
 * which share one world directory: {@value #PHASE} is {@code before} in the first and {@code after} in the second; in
 * each, the other phase's test passes at once. Before: a wide three-jaw face and a narrow face start from one landing
 * at a fixed yard in forced chunks; once both are part-built the test writes what it counted and passes, and the server
 * stops and saves. After: the new server loads that world; the minehead, plans, organs and histories are read from
 * disk, and both faces finish from where they were with every block cut once and in storage once.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class GalleryRestartTests {
    /** The system property naming the run: {@code before} or {@code after} the restart. */
    static final String PHASE = "infestusfrontier.restart.phase";

    private static final String NAMESPACE = "infestusfrontier_restart";
    private static final BlockPos YARD = new BlockPos(330008, 100, 470408);
    private static final String RECORD = "gallery-restart.properties";
    private static final List<Cutter> CUTTERS =
            List.of(Cutter.WEST_LEFT, Cutter.WEST_RIGHT, Cutter.WEST_BACK, Cutter.NORTH_RIGHT);
    private static final int WIDE_STEPS = 3;
    private static final int NARROW_STEPS = 4;

    private static Path record(GameTestHelper h) {
        return h.getLevel().getServer().getServerDirectory().resolve(RECORD);
    }

    /**
     * Before the restart: the mine starts; once each face has built a step and neither is finished, the counts are
     * written down and the test passes with the mine still at work in its forced chunks.
     */
    @GameTest(templateNamespace = NAMESPACE, template = "empty", timeoutTicks = 40 * GalleryTests.BLOCK_TICKS)
    public static void beforeTheRestartTheGalleriesAreLeftMidJob(GameTestHelper h) {
        if (!"before".equals(System.getProperty(PHASE))) {
            h.succeed();
            return;
        }
        GalleryRig rig = GalleryRig.at(h, YARD);
        rig.minehead(4000, "stairs", "wider_section");
        rig.face(Face.WEST, WIDE_STEPS, Turn.NONE, Turn.NONE, true, 16);
        rig.face(Face.NORTH, NARROW_STEPS, Turn.NONE, Turn.NONE, false, 8);
        for (Cutter cutter : CUTTERS) rig.cutter(cutter);
        int stone = rig.count(Face.WEST, Blocks.STONE) + rig.count(Face.NORTH, Blocks.STONE);
        h.startSequence()
                .thenExecute(() -> h.assertValueEqual(stone, 12 * WIDE_STEPS + 3 * NARROW_STEPS, "all of it to cut"))
                .thenWaitUntil(() -> h.assertTrue(
                        rig.built(Face.WEST) >= 1 && rig.built(Face.NORTH) >= 1, "both faces have built a step"))
                .thenExecute(() -> {
                    h.assertTrue(rig.built(Face.WEST) < WIDE_STEPS, "the wide face is mid-job");
                    h.assertTrue(rig.built(Face.NORTH) < NARROW_STEPS, "the narrow face is mid-job");
                    Properties counted = new Properties();
                    counted.setProperty("stone", Integer.toString(stone));
                    counted.setProperty("west", Integer.toString(rig.built(Face.WEST)));
                    counted.setProperty("north", Integer.toString(rig.built(Face.NORTH)));
                    counted.setProperty("stored", Integer.toString(rig.stored(Items.COBBLESTONE)));
                    for (Cutter cutter : CUTTERS)
                        counted.setProperty(cutter.name(), Integer.toString(rig.cuts(cutter)));
                    try (Writer out = Files.newBufferedWriter(record(h))) {
                        counted.store(out, "MINE-4 restart: counted before the server stopped");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .thenSucceed();
    }

    /**
     * After the restart: what was counted before is still there, not reset, and the job was not done; then both faces
     * finish, each block cut once and in storage once, with nothing loose and the beds empty.
     */
    @GameTest(templateNamespace = NAMESPACE, template = "empty", timeoutTicks = 80 * GalleryTests.BLOCK_TICKS)
    public static void afterTheRestartTheGalleriesFinishWithNothingLostOrRepeated(GameTestHelper h) {
        if (!"after".equals(System.getProperty(PHASE))) {
            h.succeed();
            return;
        }
        Properties counted = new Properties();
        try (Reader in = Files.newBufferedReader(record(h))) {
            counted.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("the run before the restart wrote no record", e);
        }
        GalleryRig rig = GalleryRig.at(h, YARD);
        int stone = Integer.parseInt(counted.getProperty("stone"));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(rig.entity(Cutter.NORTH_RIGHT.cilia) != null, "the mine is loaded"))
                .thenExecute(() -> {
                    CompoundTag head = rig.saved(GalleryRig.HEAD);
                    h.assertValueEqual(
                            head.getList("mutations", 8).getString(1), "wider_section", "the mutation is kept");
                    h.assertTrue(
                            Integer.parseInt(counted.getProperty("west")) < WIDE_STEPS
                                    && Integer.parseInt(counted.getProperty("north")) < NARROW_STEPS,
                            "the server stopped mid-job");
                    h.assertTrue(
                            rig.built(Face.WEST) >= Integer.parseInt(counted.getProperty("west")),
                            "the wide plan's built steps are not reset");
                    h.assertTrue(
                            rig.built(Face.NORTH) >= Integer.parseInt(counted.getProperty("north")),
                            "nor the narrow one's");
                    h.assertTrue(
                            rig.stored(Items.COBBLESTONE) >= Integer.parseInt(counted.getProperty("stored")),
                            "the storage is kept");
                    for (Cutter cutter : CUTTERS)
                        h.assertTrue(
                                rig.cuts(cutter) >= Integer.parseInt(counted.getProperty(cutter.name())),
                                cutter + ": its history is kept");
                })
                .thenWaitUntil(() -> {
                    h.assertValueEqual(rig.built(Face.WEST), WIDE_STEPS, "the wide face finishes");
                    h.assertValueEqual(rig.built(Face.NORTH), NARROW_STEPS, "the narrow face finishes");
                })
                .thenWaitUntil(() -> h.assertValueEqual(rig.stored(Items.COBBLESTONE), stone, "every cut in storage"))
                .thenExecute(() -> {
                    int cuts = 0;
                    int beds = 0;
                    for (Cutter cutter : CUTTERS) {
                        cuts += rig.cuts(cutter);
                        beds += rig.bed(cutter);
                    }
                    h.assertValueEqual(cuts, stone, "each block cut once across the restart");
                    h.assertValueEqual(beds, 0, "the beds are empty");
                    h.assertValueEqual(rig.loose(), 0, "nothing fell loose");
                    h.assertValueEqual(
                            rig.saved(Face.WEST.grower())
                                    .getCompound("segments")
                                    .getInt("count"),
                            16 - 3 * WIDE_STEPS,
                            "three segments paid once for each wide step");
                    h.assertValueEqual(
                            rig.saved(Face.NORTH.grower())
                                    .getCompound("segments")
                                    .getInt("count"),
                            8 - NARROW_STEPS,
                            "one for each narrow step");
                    rig.release();
                })
                .thenSucceed();
    }
}
