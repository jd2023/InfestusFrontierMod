package org.jd.infestusfrontier.testmod;

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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * One exhibit of {@code captures/demo.json}, replayed far from the test structures. The exhibit is the box of demo
 * coordinates the test names; its {@code setblock} and {@code fill} commands are shifted into a yard that is
 * {@value #SLOT_Z} blocks further along Z for each slot, so tests in one world never share a yard.
 */
final class DemoExhibit {
    /** Ticks the entities of a forced chunk take to load from a saved world. */
    static final int ENTITIES_LOAD_TICKS = 10;

    private static final Pattern SET = Pattern.compile("^setblock (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    private static final Pattern FILL =
            Pattern.compile("^fill (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    private static final int SHIFT_X = 246000;
    private static final int SHIFT_Y = 160;
    private static final int SHIFT_Z = 246000;
    private static final int SLOT_Z = 64;

    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    private final int shiftZ;

    /** The exhibit inside the demo box {@code min}..{@code max}, replayed in yard {@code slot}. */
    DemoExhibit(BlockPos min, BlockPos max, int slot) {
        this.minX = min.getX();
        this.minY = min.getY();
        this.minZ = min.getZ();
        this.maxX = max.getX();
        this.maxY = max.getY();
        this.maxZ = max.getZ();
        this.shiftZ = SHIFT_Z + slot * SLOT_Z;
    }

    /** The demo's {@code setblock} and {@code fill} commands that lie wholly inside the box, as written in the demo. */
    List<String> commands() {
        List<String> found = new ArrayList<>();
        try (InputStream in = DemoExhibit.class.getResourceAsStream("/captures/demo.json")) {
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

    /** Where the demo position {@code demo} lies in this exhibit's yard. */
    BlockPos at(BlockPos demo) {
        return new BlockPos(demo.getX() + SHIFT_X, demo.getY() + SHIFT_Y, demo.getZ() + shiftZ);
    }

    /** Forces the yard's chunks and loads their entities. The saved test world may hold an earlier run's items there. */
    void force(GameTestHelper h) {
        for (ChunkPos chunk : chunks()) TestChunks.force(h.getLevel(), chunk);
    }

    /** Releases the chunks {@link #force} held. */
    void forget(GameTestHelper h) {
        for (ChunkPos chunk : chunks()) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
    }

    /** Empties the yard's blocks and block entities and discards the items around it, dropping nothing. */
    void tidy(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (BlockPos pos : BlockPos.betweenClosed(
                minX + SHIFT_X - 1,
                minY + SHIFT_Y - 1,
                minZ + shiftZ - 1,
                maxX + SHIFT_X + 1,
                maxY + SHIFT_Y + 3,
                maxZ + shiftZ + 1)) {
            level.removeBlockEntity(pos); // a saved world still holds an earlier run's chest
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        AABB yard = new AABB(
                        minX + SHIFT_X,
                        minY + SHIFT_Y,
                        minZ + shiftZ,
                        maxX + SHIFT_X + 1,
                        maxY + SHIFT_Y + 3,
                        maxZ + shiftZ + 1)
                .inflate(16);
        level.getEntitiesOfClass(ItemEntity.class, yard).forEach(ItemEntity::discard);
    }

    /** Tidies the yard, then builds the exhibit from its own demo commands; a command that fails fails the test. */
    void build(GameTestHelper h) {
        tidy(h);
        ServerLevel level = h.getLevel();
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level);
        for (String command : commands()) {
            try {
                level.getServer().getCommands().getDispatcher().execute(shifted(command), source);
            } catch (CommandSyntaxException e) {
                h.fail("the demo's command failed: " + command + ": " + e.getMessage());
            }
        }
    }

    private List<ChunkPos> chunks() {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = (minX + SHIFT_X) >> 4; x <= (maxX + SHIFT_X) >> 4; x++)
            for (int z = (minZ + shiftZ) >> 4; z <= (maxZ + shiftZ) >> 4; z++) chunks.add(new ChunkPos(x, z));
        return chunks;
    }

    private boolean inBox(Matcher matcher, int first) {
        int x = Integer.parseInt(matcher.group(first));
        int y = Integer.parseInt(matcher.group(first + 1));
        int z = Integer.parseInt(matcher.group(first + 2));
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private String shifted(String command) {
        Matcher set = SET.matcher(command);
        if (set.matches())
            return "setblock %d %d %d %s"
                    .formatted(
                            Integer.parseInt(set.group(1)) + SHIFT_X,
                            Integer.parseInt(set.group(2)) + SHIFT_Y,
                            Integer.parseInt(set.group(3)) + shiftZ,
                            set.group(4));
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
}
