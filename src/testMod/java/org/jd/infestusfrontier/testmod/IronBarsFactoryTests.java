package org.jd.infestusfrontier.testmod;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.mineral.Metal;
import org.jd.infestusfrontier.core.mineral.Parcel;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.mineral.MineralModule;
import org.jd.infestusfrontier.processing.BatchOrganMenu;
import org.jd.infestusfrontier.processing.PalmStatus;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.processing.api.PalmRote;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jd.infestusfrontier.storage.api.ItemStore;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;
import org.slf4j.Logger;

/**
 * The raw-iron-to-iron-bars factory of the default demo ({@code captures/demo.json}, south of row C). Every test
 * builds it from the demo's own commands, moved to an unloaded corner of the world, so the exhibit a player walks to
 * and the line the tests run are one thing. The line is a Larder of raw iron, a Gizzard, a Kidney with a water
 * Junction, a waste chest, a Bio-Furnace, a Palm with Rote and a chest of bars, joined by Mouths, Lobes and veins, with
 * one Reservoir and a biomass trunk under the organs.
 *
 * <p>The ledger of a whole run, by hand: 15 raw iron past the Larder's reserve of 3 make 60 dust, and the Kidney
 * washes the 15 Tailings for 15 more. 75 dust make 18 ingots and leave 3. 18 ingots are three crafts of 6, 48 bars.
 * Biomass: 15 x 20 + 15 x 30 + 3 x 20 + 18 x 40 + 3 x 10 = 1560 BU. Water: 15 x 250 = 3750 mB, of which three settles
 * clear 3000 and 750 stay dirty in the Kidney.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class IronBarsFactoryTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** The factory's box in the demo, in demo coordinates: x 0 to 31, z -22 to -15, y -62 to -59. */
    private static final int MIN_X = 0;

    private static final int MAX_X = 31;
    private static final int MIN_Y = -62;
    private static final int MAX_Y = -59;
    private static final int MIN_Z = -22;
    private static final int MAX_Z = -15;
    /** Where the copy stands: whole chunks away, and above any terrain. */
    private static final int SHIFT_X = 240000;

    private static final int SHIFT_Y = 160;
    private static final int SHIFT_Z = 240000;

    private static final int RAW = 15;
    private static final int RESERVE = 3;
    private static final int CRAFTS = 3;
    private static final int BARS = 16;
    private static final int INGOTS_A_CRAFT = 6;
    private static final int BIOMASS_START = 4000;

    private static final int GRIND_BU = 20;
    private static final int WASH_BU = 30;
    private static final int SETTLE_BU = 20;
    private static final int SMELT_BU = 40;
    private static final int CRAFT_BU = 10;
    private static final int WASH_MB = 250;

    private static final int DUST = 0;
    private static final int TAILINGS = 1;
    private static final int RESIDUE = 2;

    private static final Pattern SET = Pattern.compile("^setblock (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    private static final Pattern FILL =
            Pattern.compile("^fill (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    private static final Pattern BODY = Pattern.compile("body:\\[([-\\d, ]+)]");
    private static final Pattern BLOCK_ID = Pattern.compile("^[a-z0-9_:]+");

    private IronBarsFactoryTests() {}

    // ---- the demo's factory, replayed ----

    private record Placed(BlockPos pos, String block) {
        String id() {
            Matcher matcher = BLOCK_ID.matcher(block);
            if (!matcher.find()) throw new IllegalStateException(block);
            String id = matcher.group();
            return id.contains(":") ? id : "minecraft:" + id;
        }
    }

    /** The demo's commands that stand inside the factory's box, in order. */
    private static List<String> demoFactory() {
        List<String> found = new ArrayList<>();
        try (InputStream in = IronBarsFactoryTests.class.getResourceAsStream("/captures/demo.json")) {
            if (in == null) throw new IllegalStateException("demo.json is not on the test classpath");
            for (JsonElement command : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonArray("commands")) {
                String text = command.getAsString();
                Matcher set = SET.matcher(text);
                Matcher fill = FILL.matcher(text);
                if (set.matches() && inBox(set, 1)) found.add(text);
                else if (fill.matches() && inBox(fill, 1) && inBox(fill, 4)) found.add(text);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return found;
    }

    private static boolean inBox(Matcher matcher, int first) {
        int x = Integer.parseInt(matcher.group(first));
        int y = Integer.parseInt(matcher.group(first + 1));
        int z = Integer.parseInt(matcher.group(first + 2));
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y && z >= MIN_Z && z <= MAX_Z;
    }

    private static String shifted(String command, int shiftZ) {
        Matcher set = SET.matcher(command);
        if (set.matches())
            return "setblock %d %d %d %s"
                    .formatted(
                            Integer.parseInt(set.group(1)) + SHIFT_X,
                            Integer.parseInt(set.group(2)) + SHIFT_Y,
                            Integer.parseInt(set.group(3)) + shiftZ,
                            shiftedBody(set.group(4), shiftZ));
        Matcher fill = FILL.matcher(command);
        if (!fill.matches()) throw new IllegalStateException(command);
        return "fill %d %d %d %d %d %d %s"
                .formatted(
                        Integer.parseInt(fill.group(1)) + SHIFT_X,
                        Integer.parseInt(fill.group(2)) + SHIFT_Y,
                        Integer.parseInt(fill.group(3)) + shiftZ,
                        Integer.parseInt(fill.group(4)) + SHIFT_X,
                        Integer.parseInt(fill.group(5)) + SHIFT_Y,
                        Integer.parseInt(fill.group(6)) + shiftZ,
                        fill.group(7));
    }

    /** A Reservoir cell names its body in absolute coordinates; the copy names its own. */
    private static String shiftedBody(String block, int shiftZ) {
        Matcher body = BODY.matcher(block);
        if (!body.find()) return block;
        String[] numbers = body.group(1).split(",");
        StringBuilder moved = new StringBuilder();
        for (int i = 0; i < numbers.length; i++) {
            int value = Integer.parseInt(numbers[i].trim());
            moved.append(i == 0 ? "" : ",").append(value + new int[] {SHIFT_X, SHIFT_Y, shiftZ}[i % 3]);
        }
        return body.replaceFirst(Matcher.quoteReplacement("body:[" + moved + "]"));
    }

    /** One copy of the factory in the test world, and the places the tests look at. */
    private static final class Line {
        final GameTestHelper h;
        final ServerLevel level;
        final List<ChunkPos> chunks = new ArrayList<>();
        final List<Placed> placed = new ArrayList<>();
        final BlockPos larder;
        final BlockPos gizzard;
        final BlockPos kidney;
        final BlockPos furnace;
        final BlockPos palm;
        final BlockPos reservoir;
        final BlockPos waste;
        final BlockPos bars;
        final BlockPos biomassJunction;
        final BlockPos waterJunction;
        final int commands;

        /** Builds the copy number {@code slot}; copies never share a chunk. */
        Line(GameTestHelper h, int slot) {
            this.h = h;
            this.level = h.getLevel();
            int shiftZ = SHIFT_Z + 64 * slot;
            for (int x = (MIN_X + SHIFT_X) >> 4; x <= (MAX_X + SHIFT_X) >> 4; x++)
                for (int z = (MIN_Z + shiftZ) >> 4; z <= (MAX_Z + shiftZ) >> 4; z++) chunks.add(new ChunkPos(x, z));
            for (ChunkPos chunk : chunks) TestChunks.force(level, chunk);
            for (BlockPos at : BlockPos.betweenClosed(
                    MIN_X + SHIFT_X - 1,
                    MIN_Y + SHIFT_Y - 1,
                    MIN_Z + shiftZ - 1,
                    MAX_X + SHIFT_X + 1,
                    MAX_Y + SHIFT_Y + 2,
                    MAX_Z + shiftZ + 1)) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            List<String> factory = demoFactory();
            commands = factory.size();
            for (String command : factory) {
                String moved = shifted(command, shiftZ);
                run(moved);
                Matcher set = SET.matcher(moved);
                if (set.matches())
                    placed.add(new Placed(
                            new BlockPos(
                                    Integer.parseInt(set.group(1)),
                                    Integer.parseInt(set.group(2)),
                                    Integer.parseInt(set.group(3))),
                            set.group(4)));
            }
            larder = only("infestusfrontier:larder");
            gizzard = only("infestusfrontier:mineral_gizzard");
            kidney = only("infestusfrontier:washing_kidney");
            furnace = only("infestusfrontier:bio_furnace");
            palm = only("infestusfrontier:shaping_palm");
            reservoir = only("infestusfrontier:reservoir_cell");
            List<BlockPos> chests = all("minecraft:chest").stream()
                    .sorted(Comparator.comparingInt(BlockPos::getX))
                    .toList();
            h.assertValueEqual(chests.size(), 2, "a waste chest and a chest of bars");
            waste = chests.get(0);
            bars = chests.get(1);
            biomassJunction = junction("west=intake");
            waterJunction = junction("north=intake");
        }

        private void run(String command) {
            CommandSourceStack source =
                    level.getServer().createCommandSourceStack().withLevel(level);
            try {
                level.getServer().getCommands().getDispatcher().execute(command, source);
            } catch (CommandSyntaxException e) {
                h.fail("the demo's command failed: " + command + ": " + e.getMessage());
            }
        }

        List<BlockPos> all(String id) {
            return placed.stream()
                    .filter(p -> p.id().equals(id))
                    .map(Placed::pos)
                    .toList();
        }

        BlockPos only(String id) {
            List<BlockPos> found = all(id);
            h.assertValueEqual(found.size(), 1, "one " + id + " in the demo's factory");
            return found.get(0);
        }

        private BlockPos junction(String face) {
            return placed.stream()
                    .filter(p -> p.id().equals("infestusfrontier:vascular_junction")
                            && p.block().contains(face))
                    .map(Placed::pos)
                    .findFirst()
                    .orElseThrow();
        }

        BatchOrganContents contents(BlockPos organ) {
            return level.getBlockEntity(organ).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        }

        /** Replaces what the organ holds, as a chunk saved in that state would. */
        void preset(BlockPos organ, List<ItemStack> ingredients, List<ItemStack> outputs) {
            BatchOrganContents like = contents(organ);
            level.getBlockEntity(organ)
                    .applyComponents(
                            DataComponentMap.builder()
                                    .set(
                                            ProcessingModule.BATCH_CONTENTS.get(),
                                            new BatchOrganContents(
                                                    ingredients,
                                                    outputs,
                                                    like.waterMb(),
                                                    like.biomass(),
                                                    like.history(),
                                                    Optional.empty(),
                                                    Optional.empty(),
                                                    like.experience(),
                                                    like.dirtyMb()))
                                    .build(),
                            DataComponentPatch.EMPTY);
        }

        PalmRote rote(BlockPos at) {
            return level.getBlockEntity(at).collectComponents().get(ProcessingModule.PALM_ROTE.get());
        }

        /** A player at the block's screen, as a click on it opens one. */
        MenuPlayer open(BlockPos at, String name) {
            MenuPlayer player = new MenuPlayer(level, name);
            player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
            player.openMenu((MenuProvider) level.getBlockEntity(at), at);
            player.containerMenu.broadcastChanges();
            return player;
        }

        PalmStatus palmStatus() {
            MenuPlayer player = open(palm, "iron_bars_status");
            PalmStatus status = ((ShapingPalmMenu) player.containerMenu).status();
            player.closeContainer();
            return status;
        }

        /** The screen's mask of output trays that hold a batch back, bit n for output n. */
        int blocked(BlockPos organ) {
            MenuPlayer player = open(organ, "iron_bars_blocked");
            int mask = ((BatchOrganMenu) player.containerMenu).blocked();
            player.closeContainer();
            return mask;
        }

        PalmContents grid() {
            return level.getBlockEntity(palm)
                    .collectComponents()
                    .getOrDefault(ProcessingModule.PALM_CONTENTS.get(), PalmContents.EMPTY);
        }

        ChestBlockEntity chest(BlockPos at) {
            return (ChestBlockEntity) level.getBlockEntity(at);
        }

        ItemStore larderStore() {
            return (ItemStore) level.getCapability(Capabilities.ItemHandler.BLOCK, larder, Direction.UP);
        }

        IItemHandler palmPorts() {
            return level.getCapability(Capabilities.ItemHandler.BLOCK, palm, null);
        }

        /** The probe on one face of a Junction, as a player cycles it. */
        void probe(BlockPos junction, Direction face, int clicks) {
            MenuPlayer player = new MenuPlayer(level, "iron_bars_probe");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(InteractionModule.SYNAPTIC_PROBE.get()));
            player.moveTo(junction.getX() + 0.5, junction.getY() + 1, junction.getZ() - 1.5);
            Vec3 hit = Vec3.atCenterOf(junction)
                    .add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
            for (int i = 0; i < clicks; i++)
                player.gameMode.useItemOn(
                        player,
                        level,
                        player.getMainHandItem(),
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(hit, face, junction, false));
        }

        /** Biomass held in a fluid block's own tanks, in BU. */
        int stored(BlockPos at) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, at, null);
            if (handler == null) handler = level.getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.UP);
            if (handler == null) return 0;
            int held = 0;
            for (int tank = 0; tank < handler.getTanks(); tank++)
                if (BiomassFluids.isBiomass(handler.getFluidInTank(tank)))
                    held += handler.getFluidInTank(tank).getAmount();
            return held;
        }

        /** All the biomass anywhere on the line: Reservoir, Junctions, the three batch organs and the Palm. */
        int biomass() {
            return stored(reservoir)
                    + stored(biomassJunction)
                    + stored(waterJunction)
                    + contents(gizzard).biomass().stored().units()
                    + contents(kidney).biomass().stored().units()
                    + contents(furnace).biomass().stored().units()
                    + rote(palm).biomass().stored().units();
        }

        /** Tailings of three units, the Kidney's, in a chest. */
        int washedIn(BlockPos chest) {
            int total = 0;
            for (int slot = 0; slot < chest(chest).getContainerSize(); slot++) {
                ItemStack stack = chest(chest).getItem(slot);
                if (stack.is(MineralModule.MINERAL_TAILINGS.get())
                        && new Parcel(Metal.IRON, 3).equals(stack.get(MineralModule.PARCEL.get())))
                    total += stack.getCount();
            }
            return total;
        }

        /** True when the organ holds no finished product. */
        boolean outputsEmpty(BlockPos organ) {
            return contents(organ).outputs().stream().allMatch(ItemStack::isEmpty);
        }

        /** Items of one kind in a chest. */
        int in(BlockPos chest, net.minecraft.world.item.Item item) {
            return chest(chest).countItem(item);
        }

        void forget() {
            for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
        }

        void remember() {
            for (ChunkPos chunk : chunks) TestChunks.force(level, chunk);
        }

        boolean unloaded() {
            for (BlockPos at : List.of(larder, gizzard, kidney, furnace, palm, bars, waste, reservoir))
                if (level.hasChunkAt(at)) return false;
            return true;
        }

        boolean loaded() {
            for (BlockPos at : List.of(larder, gizzard, kidney, furnace, palm, bars, waste, reservoir))
                if (!level.hasChunkAt(at)) return false;
            return true;
        }

        /** Everything the line holds of one item: organs' trays and slots, Palm, chests and store. */
        int count(net.minecraft.world.item.Item item) {
            int total = in(waste, item) + in(bars, item);
            for (BlockPos organ : List.of(gizzard, kidney, furnace)) {
                BatchOrganContents held = contents(organ);
                for (ItemStack stack : held.ingredients()) if (stack.is(item)) total += stack.getCount();
                for (ItemStack stack : held.outputs()) if (stack.is(item)) total += stack.getCount();
            }
            PalmRote rote = rote(palm);
            for (ItemStack stack : rote.tray()) if (stack.is(item)) total += stack.getCount();
            for (ItemStack stack : grid().grid()) if (stack.is(item)) total += stack.getCount();
            return total;
        }
    }

    /** What one finished run adds up to, read off the line. */
    private record Ledger(
            int gizzardGrinds,
            int kidneyBatches,
            int smelts,
            int crafts,
            int bars,
            int ingotsLeft,
            int dustLeft,
            int washedTailings,
            int residue,
            int dirtyMb,
            int biomassLeft,
            int rawLeft) {}

    private static Ledger ledger(Line line) {
        BatchOrganContents kidney = line.contents(line.kidney);
        return new Ledger(
                line.contents(line.gizzard).history().completedBatches(),
                kidney.history().completedBatches(),
                line.contents(line.furnace).history().completedBatches(),
                line.rote(line.palm).cycles(),
                line.count(Items.IRON_BARS),
                line.count(Items.IRON_INGOT),
                line.count(MineralModule.IRON_DUST.get()),
                line.count(MineralModule.MINERAL_TAILINGS.get()),
                line.count(MineralModule.ROCK_RESIDUE.get()),
                kidney.dirtyMb(),
                line.biomass(),
                line.larderStore().count(new ItemStack(Items.RAW_IRON)));
    }

    private static void assertWholeRun(GameTestHelper h, Line line, Ledger end, int extraResidue) {
        int washes = RAW;
        int settles = washes / 4;
        h.assertValueEqual(end.gizzardGrinds(), RAW, "15 raw iron ground, one batch each");
        h.assertValueEqual(end.kidneyBatches(), washes + settles, "15 washes and 3 settles");
        h.assertValueEqual(end.smelts(), 18, "18 ingots smelted");
        h.assertValueEqual(end.crafts(), CRAFTS, "three iron-bars crafts");
        h.assertValueEqual(end.bars(), CRAFTS * BARS, "48 bars, all in the output chest");
        h.assertValueEqual(end.ingotsLeft(), 0, "every ingot became bars: 18 = 3 x 6");
        h.assertValueEqual(end.dustLeft(), 75 - 72, "75 dust made, 72 smelted, 3 left in the furnace");
        h.assertValueEqual(end.washedTailings(), RAW, "15 washed Tailings in the waste chest");
        h.assertValueEqual(end.residue(), RAW + settles + extraResidue, "Rock Residue: 15 ground and 3 settled");
        h.assertValueEqual(end.dirtyMb(), washes * WASH_MB - settles * 1000, "750 mB stay dirty in the Kidney");
        h.assertValueEqual(end.rawLeft(), RESERVE, "the Larder still holds its 3 reserved raw iron");
        h.assertValueEqual(
                end.biomassLeft(),
                BIOMASS_START
                        - (RAW * GRIND_BU + washes * WASH_BU + settles * SETTLE_BU + 18 * SMELT_BU + CRAFTS * CRAFT_BU),
                "biomass: 4000 less 1560 spent, wherever it sits");
    }

    // ---- the tests ----

    /**
     * The whole factory with no hand: water and biomass are cut by the probe first, the line waits and says so, each
     * supply is restored by the probe, the chunks holding the line unload and load again in the middle of the washing,
     * and 15 raw iron past the Larder's reserve end as 48 bars in the output chest. The ledger is exact.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "iron_bars_factory",
            timeoutTicks = 24000)
    public static void aLarderOfRawIronBecomesThreeCraftsOfIronBarsWithNoHandAndTheLedgerIsExact(GameTestHelper h) {
        Line line = new Line(h, 0);
        long[] marks = new long[4];
        Ledger[] mid = new Ledger[1];
        BatchOrganContents[] before = new BatchOrganContents[1];
        PalmRote[] palmBefore = new PalmRote[1];
        h.assertTrue(line.commands > 40, "the demo holds the factory: " + line.commands + " commands");
        h.assertValueEqual(line.larderStore().count(new ItemStack(Items.RAW_IRON)), 18, "18 raw iron in the Larder");
        h.assertValueEqual(line.larderStore().automatable(new ItemStack(Items.RAW_IRON)), RAW, "15 past the reserve");
        h.assertValueEqual(line.stored(line.reservoir), BIOMASS_START, "a full Reservoir");
        // the player cuts both supplies before anything has moved: two clicks close an intake, one opens it again
        line.probe(line.biomassJunction, Direction.WEST, 2);
        line.probe(line.waterJunction, Direction.NORTH, 2);
        h.startSequence()
                .thenExecute(() -> marks[0] = h.getLevel().getGameTime())
                .thenIdle(300)
                .thenExecute(() -> {
                    BatchOrganContents gizzard = line.contents(line.gizzard);
                    h.assertTrue(gizzard.batch().isEmpty(), "no biomass reaches the Gizzard, so it grinds nothing");
                    h.assertValueEqual(gizzard.biomass().stored().units(), 0, "its biomass tank reads 0");
                    h.assertValueEqual(
                            gizzard.ingredients().get(0).getCount(),
                            RAW,
                            "the raw iron waits in the Gizzard, all 15 past the reserve");
                    h.assertValueEqual(line.stored(line.reservoir), BIOMASS_START, "the Reservoir is untouched");
                    h.assertValueEqual(
                            line.larderStore().count(new ItemStack(Items.RAW_IRON)),
                            RESERVE,
                            "only the 3 reserved raw iron stay in the Larder");
                    h.assertValueEqual(
                            line.larderStore().automatable(new ItemStack(Items.RAW_IRON)),
                            0,
                            "and the line cannot take them");
                    marks[1] = h.getLevel().getGameTime();
                    line.probe(line.biomassJunction, Direction.WEST, 1);
                })
                // biomass is back; water is still cut: the Kidney waits and the rest of the line works
                .thenWaitUntil(() -> {
                    BatchOrganContents kidney = line.contents(line.kidney);
                    h.assertTrue(
                            line.contents(line.gizzard).history().completedBatches() == RAW
                                    && kidney.ingredients().get(0).getCount() == RAW
                                    && line.rote(line.palm).cycles() == 2,
                            "the unwashed line made 60 dust, 15 ingots and two crafts");
                })
                .thenWaitUntil(() -> h.assertTrue(
                        line.rote(line.palm).cycle().isEmpty()
                                && line.in(line.bars, Items.IRON_BARS) == 2 * BARS
                                && line.contents(line.furnace).batch().isEmpty()
                                && line.contents(line.furnace).ingredients().stream()
                                                .mapToInt(ItemStack::getCount)
                                                .sum()
                                        == 0,
                        "everything that can be made without water is made"))
                .thenExecute(() -> {
                    BatchOrganContents kidney = line.contents(line.kidney);
                    h.assertTrue(kidney.batch().isEmpty(), "no water, so the Kidney washes nothing");
                    h.assertValueEqual(kidney.waterMb(), 0, "its clean water tank reads 0");
                    h.assertValueEqual(kidney.ingredients().get(0).getCount(), RAW, "the 15 Tailings wait in it");
                    h.assertValueEqual(line.in(line.bars, Items.IRON_BARS), 2 * BARS, "two crafts of bars so far");
                    h.assertValueEqual(line.count(Items.IRON_INGOT), 3, "three ingots wait in the Palm");
                    h.assertValueEqual(line.palmStatus(), PalmStatus.MISSING, "and the Palm says it lacks items");
                    mid[0] = ledger(line);
                    marks[2] = h.getLevel().getGameTime();
                    line.probe(line.waterJunction, Direction.NORTH, 1);
                })
                // water is back: the washing begins; unload and load the whole line in the middle of it
                .thenWaitUntil(() -> {
                    BatchOrganContents kidney = line.contents(line.kidney);
                    h.assertTrue(
                            kidney.history().completedBatches() >= 2
                                    && kidney.batch().isPresent()
                                    && kidney.batch().get().batch().ticksDone() >= 40,
                            "the Kidney is washing");
                })
                .thenExecute(() -> {
                    before[0] = line.contents(line.kidney);
                    palmBefore[0] = line.rote(line.palm);
                    line.forget();
                })
                .thenWaitUntil(() -> h.assertTrue(line.unloaded(), "the line's chunks unloaded"))
                .thenExecute(line::remember)
                .thenWaitUntil(() -> h.assertTrue(line.loaded(), "and came back"))
                .thenExecute(() -> {
                    BatchOrganContents back = line.contents(line.kidney);
                    h.assertTrue(
                            back.batch().isPresent(), "the wash in the middle of the save is still the same batch");
                    h.assertValueEqual(
                            back.batch().get().batch().recipe(),
                            before[0].batch().get().batch().recipe(),
                            "the same recipe");
                    h.assertTrue(
                            back.batch().get().batch().ticksDone()
                                    >= before[0].batch().get().batch().ticksDone(),
                            "its progress was kept");
                    h.assertTrue(back.dirtyMb() >= before[0].dirtyMb(), "and its dirty water");
                    h.assertValueEqual(
                            back.history().completedBatches()
                                    >= before[0].history().completedBatches(),
                            true,
                            "and its history");
                    h.assertValueEqual(line.rote(line.palm).cycles(), palmBefore[0].cycles(), "the Palm's crafts too");
                })
                .thenWaitUntil(() -> h.assertTrue(finished(line, 0), "the stock has become bars"))
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(finished(line, 0), "and nothing moves after that");
                    marks[3] = h.getLevel().getGameTime();
                    Ledger end = ledger(line);
                    assertWholeRun(h, line, end, 0);
                    h.assertValueEqual(
                            line.in(line.bars, Items.IRON_BARS), line.count(Items.IRON_BARS), "no stray bars");
                    LOGGER.info(
                            "IRON BARS FACTORY LEDGER, 15 raw iron past a reserve of 3, supplies cut and restored: "
                                    + "{} ticks in all; biomass restored at tick {}, with {} bars made by tick {} when "
                                    + "water came back; {} grinds, {} washes and settles, {} smelts, {} crafts; {} bars, "
                                    + "{} dust left, {} washed Tailings, {} Rock Residue, {} mB dirty; biomass {} of {} "
                                    + "left; {} raw iron reserved.",
                            marks[3] - marks[0],
                            marks[1] - marks[0],
                            mid[0].bars(),
                            marks[2] - marks[0],
                            end.gizzardGrinds(),
                            end.kidneyBatches(),
                            end.smelts(),
                            end.crafts(),
                            end.bars(),
                            end.dustLeft(),
                            end.washedTailings(),
                            end.residue(),
                            end.dirtyMb(),
                            end.biomassLeft(),
                            BIOMASS_START,
                            end.rawLeft());
                })
                .thenSucceed();
    }

    /**
     * The waste chest is full and the Gizzard's Rock Residue tray holds 62: the third grind finds no room, the
     * Gizzard says its residue tray is full, spends nothing and keeps its raw iron. Emptying the chest lets the line
     * run to the same exact end, with the 62 extra residue accounted for.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "iron_bars_factory",
            timeoutTicks = 24000)
    public static void aFullWasteChestPausesTheGrindWithItsReasonAndEmptyingItRunsTheLineOn(GameTestHelper h) {
        Line line = new Line(h, 1);
        ChestBlockEntity waste = line.chest(line.waste);
        for (int slot = 0; slot < waste.getContainerSize(); slot++)
            waste.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        BatchOrganContents gizzard = line.contents(line.gizzard);
        List<ItemStack> trays = new ArrayList<>(gizzard.outputs());
        trays.set(RESIDUE, new ItemStack(MineralModule.ROCK_RESIDUE.get(), 62));
        line.preset(line.gizzard, gizzard.ingredients(), trays);
        int[] spent = new int[1];
        h.startSequence()
                .thenWaitUntil(() -> {
                    BatchOrganContents now = line.contents(line.gizzard);
                    h.assertTrue(
                            now.history().completedBatches() == 2 && now.batch().isEmpty(),
                            "two grinds are done and the third waits");
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    BatchOrganContents now = line.contents(line.gizzard);
                    h.assertTrue(now.batch().isEmpty(), "the Gizzard stays paused");
                    h.assertValueEqual(now.history().completedBatches(), 2, "no third grind");
                    h.assertValueEqual(now.outputs().get(RESIDUE).getCount(), 64, "its residue tray is full");
                    h.assertValueEqual(line.blocked(line.gizzard), 1 << RESIDUE, "and the screen names that tray");
                    h.assertValueEqual(now.ingredients().get(0).getCount(), RAW - 2, "13 raw iron wait in it");
                    spent[0] = now.biomass().stored().units();
                    h.assertValueEqual(line.in(line.waste, Items.COBBLESTONE), 27 * 64, "the waste chest is unchanged");
                    h.assertValueEqual(
                            line.in(line.waste, MineralModule.ROCK_RESIDUE.get()), 0, "and holds no residue");
                    waste.clearContent();
                })
                .thenWaitUntil(() -> h.assertTrue(
                        line.contents(line.gizzard).history().completedBatches() >= 3,
                        "emptying the chest lets the residue go and the grind resume"))
                .thenWaitUntil(() -> h.assertTrue(finished(line, 62), "the stock has become bars"))
                .thenIdle(200)
                .thenExecute(() -> {
                    h.assertTrue(finished(line, 62), "and nothing moves after that");
                    assertWholeRun(h, line, ledger(line), 62);
                })
                .thenSucceed();
    }

    /**
     * The chest of bars is full and the Palm's tray holds 40 bars: the first craft fits (56), the second finds no
     * room and the Palm says its tray is full. Emptying the chest sends the bars out and the second craft runs.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "iron_bars_factory",
            timeoutTicks = 6000)
    public static void aFullChestOfBarsHoldsTheNextCraftWithItsReasonAndEmptyingItResumes(GameTestHelper h) {
        Line line = new Line(h, 2);
        h.getLevel().setBlockAndUpdate(line.larder, Blocks.AIR.defaultBlockState());
        ChestBlockEntity bars = line.chest(line.bars);
        for (int slot = 0; slot < bars.getContainerSize(); slot++)
            bars.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        PalmRote rote = line.rote(line.palm);
        List<ItemStack> tray = new ArrayList<>(rote.tray());
        tray.set(0, new ItemStack(Items.IRON_BARS, 40));
        line.level
                .getBlockEntity(line.palm)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(
                                        ProcessingModule.PALM_ROTE.get(),
                                        new PalmRote(
                                                rote.pattern(),
                                                rote.cycle(),
                                                tray,
                                                rote.biomass(),
                                                rote.cycles(),
                                                rote.restockOwed()))
                                .build(),
                        DataComponentPatch.EMPTY);
        IItemHandler ports = line.palmPorts();
        for (int round = 0; round < 2; round++)
            for (int cell = 0; cell < INGOTS_A_CRAFT; cell++)
                h.assertTrue(
                        ports.insertItem(cell, new ItemStack(Items.IRON_INGOT), false)
                                .isEmpty(),
                        "the Palm takes an ingot into each of its six cells");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(line.rote(line.palm).cycles() == 1, "the first craft is done"))
                .thenIdle(200)
                .thenExecute(() -> {
                    PalmRote now = line.rote(line.palm);
                    h.assertValueEqual(now.cycles(), 1, "no second craft");
                    h.assertValueEqual(now.tray().get(0).getCount(), 56, "the tray holds 40 and 16 bars");
                    h.assertValueEqual(line.palmStatus(), PalmStatus.TRAY_FULL, "the Palm says its tray is full");
                    h.assertValueEqual(line.count(Items.IRON_INGOT), 6, "its six ingots wait in the grid");
                    h.assertValueEqual(line.in(line.bars, Items.IRON_BARS), 0, "no bars in the chest");
                    h.assertValueEqual(line.in(line.bars, Items.COBBLESTONE), 27 * 64, "and nothing was pushed out");
                    bars.clearContent();
                })
                .thenWaitUntil(() -> h.assertTrue(
                        line.rote(line.palm).cycles() == 2
                                && line.rote(line.palm).cycle().isEmpty()
                                && line.in(line.bars, Items.IRON_BARS) == 72,
                        "emptying the chest sends the bars out and the second craft runs"))
                .thenExecute(() -> {
                    h.assertValueEqual(line.count(Items.IRON_BARS), 72, "40 and two crafts of 16, once each");
                    h.assertValueEqual(line.count(Items.IRON_INGOT), 0, "all twelve ingots became bars");
                    h.assertValueEqual(line.palmStatus(), PalmStatus.MISSING, "the Palm waits for ingots");
                })
                .thenSucceed();
    }

    /**
     * No supply is cut and no output is full: the stock becomes bars as fast as the furnace smelts. The furnace takes
     * 18 ingots at 16 s each, so the run cannot end before 5760 ticks and takes little more.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "iron_bars_factory",
            timeoutTicks = 12000)
    public static void anUninterruptedRunEndsWhenTheFurnaceHasSmeltedItsEighteenIngots(GameTestHelper h) {
        Line line = new Line(h, 4);
        long[] start = new long[1];
        h.startSequence()
                .thenExecute(() -> start[0] = h.getLevel().getGameTime())
                .thenWaitUntil(() -> h.assertTrue(finished(line, 0), "the stock has become bars"))
                .thenExecute(() -> {
                    long ticks = h.getLevel().getGameTime() - start[0];
                    assertWholeRun(h, line, ledger(line), 0);
                    h.assertTrue(
                            ticks >= 18 * 320 && ticks <= 18 * 320 + 800,
                            "the furnace sets the pace: " + ticks + " ticks for 18 ingots at 320");
                    LOGGER.info(
                            "IRON BARS FACTORY RUN, uninterrupted: {} ticks ({} s) from a full Larder to 48 bars.",
                            ticks,
                            ticks / 20);
                })
                .thenSucceed();
    }

    /**
     * Every recipe the factory depends on ships, the Palm's pattern in the demo is what Select makes from six ingots,
     * and the line's parts can be made from obtainable items by shipped recipes.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "iron_bars_factory")
    public static void everyRecipeTheFactoryNeedsShipsAndTheDemoPatternIsWhatSelectMakes(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        for (String recipe : List.of(
                "minecraft:iron_bars",
                "infestusfrontier:grinding/iron",
                "infestusfrontier:washing/iron",
                "infestusfrontier:settling",
                "infestusfrontier:dust_smelting/iron",
                "infestusfrontier:mineral_gizzard",
                "infestusfrontier:washing_kidney",
                "infestusfrontier:bio_furnace",
                "infestusfrontier:shaping_palm",
                "infestusfrontier:mouth",
                "infestusfrontier:yield_lobe",
                "infestusfrontier:feeding_lobe",
                "infestusfrontier:vascular_junction",
                "infestusfrontier:reservoir_cell",
                "infestusfrontier:growth/biomass_bladder",
                "infestusfrontier:capsule_stem",
                "infestusfrontier:activating/synaptic_gel",
                "infestusfrontier:activating/contractile_fiber",
                "infestusfrontier:activating/capillary_gel",
                "infestusfrontier:activating/sealing_resin",
                "infestusfrontier:activating/elastic_gel",
                "infestusfrontier:preparing/bone_plate",
                "infestusfrontier:preparing/membrane_sheet",
                "infestusfrontier:culturing/organ_bud")) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(recipe);
            boolean shipped = recipes.byKey(id).isPresent();
            h.assertTrue(shipped, "recipe " + recipe + " ships");
        }
        Optional<? extends RecipeHolder<?>> bars =
                recipes.byKey(net.minecraft.resources.ResourceLocation.parse("minecraft:iron_bars"));
        h.assertTrue(bars.isPresent(), "the vanilla iron-bars recipe is there");
        Line line = new Line(h, 3);
        PalmRote shown = line.rote(line.palm);
        h.assertTrue(shown.pattern().isPresent(), "the demo's Palm has a pattern");
        h.assertValueEqual(
                shown.pattern().get().recipe().toString(), "minecraft:iron_bars", "the vanilla iron-bars recipe");
        h.assertTrue(
                ItemStack.matches(shown.pattern().get().result(), new ItemStack(Items.IRON_BARS, BARS)), "16 bars");
        // a fresh Palm given Reach and Rote the way a player gives them, six ingots in the top two rows, then Select
        BlockPos fresh = line.palm.east(4);
        line.level.setBlockAndUpdate(fresh.below(), EcologyTests.mature());
        line.level.setBlockAndUpdate(fresh, ProcessingModule.SHAPING_PALM.get().defaultBlockState());
        MenuPlayer hand = new MenuPlayer(h.getLevel(), "iron_bars_select");
        giveReachAndRote(h, hand, fresh);
        MenuPlayer player = line.open(fresh, "iron_bars_select_open");
        for (int cell = 0; cell < INGOTS_A_CRAFT; cell++)
            ShapingPalmTests.put(player, cell, new ItemStack(Items.IRON_INGOT));
        player.containerMenu.clickMenuButton(player, ShapingPalmMenu.SELECT);
        player.closeContainer();
        PalmRote selected = line.rote(fresh);
        h.assertTrue(selected.pattern().isPresent(), "Select recorded the iron-bars pattern");
        h.assertValueEqual(selected.pattern().get(), shown.pattern().get(), "it is the pattern the demo's Palm holds");
        h.succeed();
    }

    private static void giveReachAndRote(GameTestHelper h, MenuPlayer player, BlockPos at) {
        for (ItemStack tool : List.of(
                new ItemStack(LogisticsModule.CONTRACTILE_FIBER.get()),
                new ItemStack(ProcessingModule.SYNAPTIC_GEL.get()))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
            player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() - 1.5);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        }
    }

    /** True when the whole stock has become bars and nothing is moving any more. */
    private static boolean finished(Line line, int extraResidue) {
        return line.rote(line.palm).cycles() == CRAFTS
                && line.rote(line.palm).cycle().isEmpty()
                && line.in(line.bars, Items.IRON_BARS) == CRAFTS * BARS
                && line.contents(line.kidney).batch().isEmpty()
                && line.contents(line.gizzard).batch().isEmpty()
                && line.contents(line.furnace).batch().isEmpty()
                && line.outputsEmpty(line.gizzard)
                && line.outputsEmpty(line.kidney)
                && line.outputsEmpty(line.furnace)
                && line.washedIn(line.waste) == RAW
                && line.count(MineralModule.ROCK_RESIDUE.get()) == RAW + 3 + extraResidue;
    }
}
