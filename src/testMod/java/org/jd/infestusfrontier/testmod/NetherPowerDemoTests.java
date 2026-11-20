package org.jd.infestusfrontier.testmod;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.power.PowerModule;

/** The exact workshop installed by demo.sh must produce from assembled parts, starting with empty energy stores. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class NetherPowerDemoTests {
    private static final String BUILD =
            "execute in minecraft:the_nether run function infestusfrontier_tests:nether_power_demo";
    private static final BlockPos HEAT = new BlockPos(22, 80, 16);
    private static final BlockPos STEAM = new BlockPos(31, 80, 16);
    private static final BlockPos ELECTRIC = new BlockPos(40, 80, 16);

    @GameTest(template = "empty", batch = "nether_power_demo", timeoutTicks = 260)
    public static void playableDemoProducesOnlyWithItsConnectedMultiblockParts(GameTestHelper h) throws Exception {
        try (var in = NetherPowerDemoTests.class.getResourceAsStream("/captures/demo.json")) {
            h.assertTrue(in != null, "playable demo exists");
            var commands = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonArray("commands");
            h.assertTrue(
                    java.util.stream.StreamSupport.stream(commands.spliterator(), false)
                            .anyMatch(command -> command.getAsString().equals(BUILD)),
                    "demo.sh installs this exact workshop");
        }
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "Nether available");
        for (int x = 1; x <= 2; x++)
            for (int z = 0; z <= 1; z++) {
                nether.setChunkForced(x, z, true);
                nether.getChunk(x, z);
            }
        for (int x = 13; x <= 14; x++) {
            h.getLevel().setChunkForced(x, 1, true);
            h.getLevel().getChunk(x, 1);
        }
        h.getLevel()
                .getServer()
                .getCommands()
                .performPrefixedCommand(h.getLevel().getServer().createCommandSourceStack(), BUILD);
        h.assertTrue(nether.getBlockState(new BlockPos(27, 80, 4)).is(Blocks.NETHER_PORTAL), "return portal exists");
        h.assertTrue(
                h.getLevel().getBlockState(new BlockPos(223, -60, 24)).is(Blocks.NETHER_PORTAL),
                "workshop portal exists in Overworld");
        PowerRig heat = new PowerRig(nether.getBlockEntity(HEAT));
        PowerRig steam = new PowerRig(nether.getBlockEntity(STEAM));
        PowerRig electric = new PowerRig(nether.getBlockEntity(ELECTRIC));
        for (PowerRig core : List.of(heat, steam, electric)) {
            h.assertValueEqual(core.store().heat(), 0, "no prefilled heat");
            h.assertValueEqual(core.store().steam(), 0, "no prefilled steam");
            h.assertValueEqual(core.store().charge(), 0, "no prefilled electricity");
        }
        int[] before = new int[3];
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertTrue(heat.store().heat() > 0, "two-block heat body produces");
                    h.assertTrue(steam.store().water() < 16000, "boiler uses water");
                    h.assertTrue(electric.store().charge() > 0, "added generator produces electricity");
                    var bed = nether.getBlockEntity(new BlockPos(30, 80, 22));
                    h.assertTrue(bed != null, "mining output exists");
                    var slots = bed.saveWithoutMetadata(nether.registryAccess()).getList("slots", 10);
                    boolean diamond = false;
                    for (int i = 0; i < slots.size(); i++)
                        diamond |= ItemStack.parseOptional(nether.registryAccess(), slots.getCompound(i))
                                .is(Items.DIAMOND);
                    h.assertTrue(diamond, "real steam route lets existing jaw harvest diamond into Work Bed");
                })
                .thenExecute(() -> {
                    before[0] = heat.store().heat();
                    before[1] = steam.store().water();
                    before[2] = electric.store().charge();
                    nether.setBlockAndUpdate(
                            HEAT.east(),
                            PowerModule.COLLECTOR
                                    .get()
                                    .defaultBlockState()
                                    .setValue(DirectionalBlock.FACING, Direction.EAST));
                    nether.removeBlock(STEAM.west(), false);
                    nether.removeBlock(ELECTRIC.north(), false);
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    h.assertValueEqual(heat.store().heat(), before[0], "collector facing away cannot heat core");
                    h.assertValueEqual(steam.store().water(), before[1], "missing boiler cannot boil");
                    h.assertValueEqual(electric.store().charge(), before[2], "missing generator cannot generate");
                    nether.setBlockAndUpdate(
                            HEAT.east(),
                            PowerModule.COLLECTOR
                                    .get()
                                    .defaultBlockState()
                                    .setValue(DirectionalBlock.FACING, Direction.WEST));
                    nether.setBlockAndUpdate(
                            STEAM.west(),
                            PowerModule.BOILER
                                    .get()
                                    .defaultBlockState()
                                    .setValue(DirectionalBlock.FACING, Direction.EAST));
                    nether.setBlockAndUpdate(
                            ELECTRIC.north(),
                            PowerModule.GENERATOR
                                    .get()
                                    .defaultBlockState()
                                    .setValue(DirectionalBlock.FACING, Direction.SOUTH));
                })
                .thenWaitUntil(() -> {
                    h.assertTrue(heat.store().heat() > before[0], "same heat body resumes");
                    h.assertTrue(steam.store().water() < before[1], "same boiler body resumes");
                    h.assertTrue(electric.store().charge() > before[2], "same electrical body resumes");
                    for (PowerRig core : List.of(heat, steam, electric))
                        h.assertTrue(
                                nether.getBlockEntity(core.getBlockPos()) == core.entity(), "original core retained");
                })
                .thenExecute(() -> {
                    for (int x = 1; x <= 2; x++) for (int z = 0; z <= 1; z++) nether.setChunkForced(x, z, false);
                    for (int x = 13; x <= 14; x++) h.getLevel().setChunkForced(x, 1, false);
                })
                .thenSucceed();
    }
}
