package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.ShapingPalmTests.put;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.stack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.processing.api.PalmRote;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.ItemStore;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;

/**
 * The two Palm supply exhibits of the default demo ({@code captures/demo.json}, south of row C at x 40 to 80): a Capsule
 * Tree of bulk, Craw and Larder pods, and a vanilla chest, each joined by Item Veins to the Mouth under a Rote Palm that
 * makes iron bars into a chest. Every test builds them from the demo's own commands, moved to an unloaded corner of
 * the world, so the exhibit a player walks to and the line the tests run are one thing. Nothing here is a command,
 * a probe pairing or a preset link.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class PalmExhibitTests {
    private static final int MIN_X = 40;
    private static final int MAX_X = 80;
    private static final int MIN_Y = -62;
    private static final int MAX_Y = -56;
    private static final int MIN_Z = -22;
    private static final int MAX_Z = -14;
    private static final int SHIFT_X = 244000;
    private static final int SHIFT_Y = 160;
    private static final int SHIFT_Z = 244000;

    private static final int CRAFT_TICKS = PalmRote.TICKS;
    private static final int BARS_A_CRAFT = 16;
    private static final int INGOTS_A_CRAFT = 6;

    private static final Pattern SET = Pattern.compile("^setblock (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    private static final Pattern FILL =
            Pattern.compile("^fill (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    private static final Pattern ROOT = Pattern.compile("root:\\[I;(-?\\d+),(-?\\d+),(-?\\d+)]");

    // the tree exhibit and the chest exhibit, in demo coordinates
    private static final BlockPos TREE_ROOT = new BlockPos(42, -60, -16);
    private static final BlockPos TREE_PALM = new BlockPos(55, -60, -16);
    private static final BlockPos TREE_BARS = new BlockPos(56, -60, -16);
    private static final BlockPos CHEST_STORE = new BlockPos(64, -60, -16);
    private static final BlockPos CHEST_PALM = new BlockPos(77, -60, -16);
    private static final BlockPos CHEST_BARS = new BlockPos(78, -60, -16);
    private static final BlockPos CHEST_VEIN = new BlockPos(70, -61, -16);

    private PalmExhibitTests() {}

    private static List<String> demoExhibits() {
        List<String> found = new ArrayList<>();
        try (InputStream in = PalmExhibitTests.class.getResourceAsStream("/captures/demo.json")) {
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
        if (set.matches()) {
            String body = set.group(4);
            Matcher root = ROOT.matcher(body);
            if (root.find())
                body = root.replaceFirst(Matcher.quoteReplacement("root:[I;%d,%d,%d]"
                        .formatted(
                                Integer.parseInt(root.group(1)) + SHIFT_X,
                                Integer.parseInt(root.group(2)) + SHIFT_Y,
                                Integer.parseInt(root.group(3)) + shiftZ)));
            return "setblock %d %d %d %s"
                    .formatted(
                            Integer.parseInt(set.group(1)) + SHIFT_X,
                            Integer.parseInt(set.group(2)) + SHIFT_Y,
                            Integer.parseInt(set.group(3)) + shiftZ,
                            body);
        }
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

    /** One copy of both exhibits in the test world. */
    private static final class Yard {
        final GameTestHelper h;
        final ServerLevel level;
        final List<ChunkPos> chunks = new ArrayList<>();
        final int shiftZ;

        Yard(GameTestHelper h, int slot) {
            this.h = h;
            this.level = h.getLevel();
            this.shiftZ = SHIFT_Z + 64 * slot;
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
            for (String command : demoExhibits()) run(shifted(command, shiftZ));
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

        BlockPos at(BlockPos demo) {
            return new BlockPos(demo.getX() + SHIFT_X, demo.getY() + SHIFT_Y, demo.getZ() + shiftZ);
        }

        ChestBlockEntity chest(BlockPos demo) {
            return (ChestBlockEntity) level.getBlockEntity(at(demo));
        }

        ItemStore tree() {
            return (ItemStore) level.getCapability(Capabilities.ItemHandler.BLOCK, at(TREE_ROOT), Direction.UP);
        }

        PalmRote rote(BlockPos palm) {
            return level.getBlockEntity(at(palm)).collectComponents().get(ProcessingModule.PALM_ROTE.get());
        }

        MenuPlayer open(BlockPos palm, String name) {
            MenuPlayer player = new MenuPlayer(level, name);
            BlockPos at = at(palm);
            player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
            player.openMenu((MenuProvider) level.getBlockEntity(at), at);
            player.containerMenu.broadcastChanges();
            return player;
        }

        void forget() {
            for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }

    /** The iron ingots in the grid of the exhibit Palm at {@code palm}. */
    private static int gridIron(Yard yard, BlockPos palm) {
        int total = 0;
        for (ItemStack stack : yard.level
                .getBlockEntity(yard.at(palm))
                .collectComponents()
                .getOrDefault(ProcessingModule.PALM_CONTENTS.get(), PalmContents.EMPTY)
                .grid()) if (stack.is(Items.IRON_INGOT)) total += stack.getCount();
        return total;
    }

    // ---- the exhibits as the demo builds them ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void noExhibitDependsOnAHiddenLinkOrACommand(GameTestHelper h) {
        List<String> exhibits = demoExhibits();
        h.assertTrue(exhibits.size() > 30, "the demo holds the two exhibits: " + exhibits.size() + " commands");
        for (String command : exhibits) {
            h.assertFalse(command.contains("link:"), "no preset link in " + command);
            h.assertFalse(command.contains("palm_source"), "no probe pairing in " + command);
        }
        try (InputStream in = PalmExhibitTests.class.getResourceAsStream("/captures/demo.json")) {
            String all = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            h.assertFalse(all.contains(",link:[I;"), "no Palm in the whole demo carries a direct link");
            h.assertFalse(all.contains("reach:1b,link"), "none does");
        } catch (IOException e) {
            h.fail(e.getMessage());
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1200)
    public static void theTreeExhibitMakesBarsFromTheTreesFreeIronAndTheLarderKeepsItsReserve(GameTestHelper h) {
        Yard yard = new Yard(h, 0);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        yard.chest(TREE_BARS).countItem(Items.IRON_BARS),
                        4 * BARS_A_CRAFT,
                        "four crafts of bars reached the output chest"))
                .thenExecute(() -> {
                    h.assertValueEqual(
                            yard.tree().count(stack(Items.IRON_INGOT, 1)),
                            16,
                            "the Larder's 16 protected ingots stayed in the tree: 40 - 24");
                    h.assertValueEqual(
                            yard.tree().count(stack(Items.GOLD_INGOT, 1)),
                            16,
                            "the gold, which no filter lets out, is untouched");
                    h.assertValueEqual(yard.tree().count(stack(Items.COAL, 1)), 40, "and so is the coal");
                    h.assertValueEqual(yard.tree().count(stack(Items.REDSTONE, 1)), 8, "and the redstone in the Craw");
                    PalmRote rote = yard.rote(TREE_PALM);
                    h.assertValueEqual(rote.cycles(), 4, "the Palm made four crafts");
                    h.assertValueEqual(rote.biomass().stored().units(), 300 - 4 * 10, "for 10 BU each");
                    yard.forget();
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1200)
    public static void theChestExhibitMakesBarsFromItsChestAndLeavesWhatTheFilterHolds(GameTestHelper h) {
        Yard yard = new Yard(h, 1);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        yard.chest(CHEST_BARS).countItem(Items.IRON_BARS),
                        5 * BARS_A_CRAFT,
                        "five crafts of bars reached the output chest"))
                .thenExecute(() -> {
                    ChestBlockEntity chest = yard.chest(CHEST_STORE);
                    h.assertValueEqual(chest.countItem(Items.IRON_INGOT), 0, "all 30 ingots were used");
                    h.assertValueEqual(chest.countItem(Items.OAK_PLANKS), 32, "the planks stay");
                    h.assertValueEqual(chest.countItem(Items.APPLE), 12, "and the apples");
                    h.assertValueEqual(yard.rote(CHEST_PALM).cycles(), 5, "five crafts");
                    yard.forget();
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1500)
    public static void aBrokenVeinStopsTheStoreAndAMendedOneRestoresItWhileHeldIngredientsFinish(GameTestHelper h) {
        Yard yard = new Yard(h, 2);
        BlockPos vein = yard.at(CHEST_VEIN);
        int[] held = new int[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        yard.rote(CHEST_PALM).cycles() >= 1
                                || yard.rote(CHEST_PALM).cycle().isPresent(),
                        "the Palm has started"))
                .thenExecute(() -> yard.level.setBlock(vein, Blocks.AIR.defaultBlockState(), 3))
                .thenIdle(CRAFT_TICKS + 20)
                .thenExecute(() -> {
                    held[0] = yard.chest(CHEST_STORE).countItem(Items.IRON_INGOT);
                    h.assertTrue(held[0] > 0, "a store with the vein broken keeps its remaining ingots");
                    int accounted = held[0]
                            + gridIron(yard, CHEST_PALM)
                            + INGOTS_A_CRAFT
                                    * (yard.rote(CHEST_PALM).cycles()
                                            + (yard.rote(CHEST_PALM).cycle().isPresent() ? 1 : 0));
                    h.assertTrue(
                            accounted <= 30 && accounted >= 30 - 8,
                            "the store, the Palm's grid and its crafts hold the 30 ingots but the 8 a Mouth may carry: "
                                    + accounted);
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    h.assertValueEqual(
                            yard.chest(CHEST_STORE).countItem(Items.IRON_INGOT),
                            held[0],
                            "no more ingots leave while the vein is broken");
                    yard.level.setBlock(vein, LogisticsModule.ITEM_VEIN.get().defaultBlockState(), 3);
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        yard.chest(CHEST_BARS).countItem(Items.IRON_BARS),
                        5 * BARS_A_CRAFT,
                        "mended, the exhibit goes on to its five crafts"))
                .thenExecute(yard::forget)
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1200)
    public static void aPalmSavedAndReloadedInTheMiddleOfAnExhibitCycleFinishesItOnceAndTheTotalHolds(
            GameTestHelper h) {
        Yard yard = new Yard(h, 3);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(yard.rote(TREE_PALM).cycle().isPresent(), "a cycle is under way"))
                .thenExecute(() -> {
                    BlockPos palm = yard.at(TREE_PALM);
                    var registries = yard.level.registryAccess();
                    PalmRote before = yard.rote(TREE_PALM);
                    CompoundTag saved = yard.level.getBlockEntity(palm).saveWithFullMetadata(registries);
                    saved.put("link", net.minecraft.nbt.NbtUtils.writeBlockPos(yard.at(TREE_ROOT)));
                    yard.level.getBlockEntity(palm).loadWithComponents(saved, registries);
                    PalmRote after = yard.rote(TREE_PALM);
                    h.assertTrue(after.equals(before), "the cycle, tray and biomass survived the reload");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        yard.chest(TREE_BARS).countItem(Items.IRON_BARS), 4 * BARS_A_CRAFT, "every craft arrived once"))
                .thenExecute(() -> {
                    h.assertValueEqual(yard.rote(TREE_PALM).cycles(), 4, "four crafts in all");
                    yard.forget();
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void theExhibitPalmsRestockReturnAndStoreButtonsWorkOnTheTree(GameTestHelper h) {
        Yard yard = new Yard(h, 4);
        // no biomass, so the Palm never starts a cycle of its own and the grid stays where the buttons leave it
        h.assertTrue(
                yard.level
                        .getCapability(BiomassCapabilities.BLOCK, yard.at(TREE_PALM), null)
                        .extract(Biomass.of(300), false)
                        .isDone(),
                "the Palm gave up its 300 BU");
        MenuPlayer[] player = new MenuPlayer[1];
        int[] grid = new int[1];
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertValueEqual(
                            yard.tree().count(stack(Items.IRON_INGOT, 1)), 16, "only the Larder's 16 are left");
                    h.assertValueEqual(gridIron(yard, TREE_PALM), 24, "the Mouth fed the Palm the 24 free ingots");
                })
                .thenExecute(() -> {
                    player[0] = yard.open(TREE_PALM, "exhibit_buttons");
                    grid[0] = gridIron(yard, TREE_PALM);
                    h.assertTrue(
                            player[0].containerMenu.clickMenuButton(player[0], ShapingPalmMenu.RETURN), "Return goes");
                    h.assertValueEqual(gridIron(yard, TREE_PALM), 0, "the grid is empty");
                    h.assertValueEqual(
                            yard.tree().count(stack(Items.IRON_INGOT, 1)),
                            16 + grid[0],
                            "every ingot went back into the tree, none lost or made");
                    put(player[0], 0, stack(Items.IRON_INGOT, 1));
                    h.assertTrue(
                            player[0].containerMenu.clickMenuButton(player[0], ShapingPalmMenu.RESTOCK),
                            "Restock goes");
                    h.assertValueEqual(
                            gridIron(yard, TREE_PALM), 1 + grid[0], "the cell took exactly the tree's free ingots");
                    h.assertValueEqual(
                            yard.tree().count(stack(Items.IRON_INGOT, 1)), 16, "and the Larder's reserve stayed");
                    h.assertTrue(
                            player[0].containerMenu.clickMenuButton(player[0], ShapingPalmMenu.RETURN), "Return again");
                    for (int cell = 0; cell < 6; cell++) put(player[0], cell, stack(Items.IRON_INGOT, 1));
                    h.assertTrue(
                            player[0].containerMenu.clickMenuButton(player[0], ShapingPalmMenu.STORE_RESULT),
                            "Store result goes");
                    h.assertValueEqual(
                            yard.chest(TREE_BARS).countItem(Items.IRON_BARS),
                            BARS_A_CRAFT,
                            "16 bars reached the output chest through the network");
                    h.assertValueEqual(player[0].crafted(Items.IRON_BARS), BARS_A_CRAFT, "as the player's craft");
                    h.assertValueEqual(gridIron(yard, TREE_PALM), 0, "the six ingots were spent");
                    yard.forget();
                })
                .thenSucceed();
    }
}
