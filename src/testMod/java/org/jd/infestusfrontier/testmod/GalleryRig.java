package org.jd.infestusfrontier.testmod;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Turn;
import org.jd.infestusfrontier.mining.TunnelRootMenu;

/**
 * A minehead whose two-deep staircase is finished, and the gallery faces at its landing, in a yard of stone far from
 * the test structures. The Rootstock faces north, so the landing junction joins routes west, north and east. Every
 * face's jaws stand on Biomass Veins of one line fed by one bladder through a Vascular Junction; every face's cilia stand
 * on Mouths of one Item Vein network whose one filtered Intake Mouth fills one storage chest with cobblestone and raw
 * iron. The landing's root cell is part of the biomass line, so a jaw can stand behind a face's grower on it. The
 * growers are stocked by hand.
 *
 * <p>Positions are relative to the head; x is east, z is south.
 */
final class GalleryRig {
    static final BlockPos HEAD = BlockPos.ZERO;
    static final BlockPos STAIRS = new BlockPos(0, -1, -1);
    static final int DEPTH = 2;
    static final BlockPos LANDING = new BlockPos(0, -3, -4);
    static final BlockPos CHEST = new BlockPos(5, -6, -4);
    static final BlockPos BLADDER = new BlockPos(-1, -5, -7);
    private static final int REACH = 12;
    private static final String SUBSTRATE = "infestusfrontier:living_substrate[stage=mature]";

    /** A route marker beside the landing, facing away from it, and the grower that stands on it. */
    enum Face {
        WEST(new BlockPos(-1, -3, -4), Direction.WEST),
        NORTH(new BlockPos(0, -3, -5), Direction.NORTH),
        EAST(new BlockPos(1, -3, -4), Direction.EAST);

        final BlockPos marker;
        final Direction facing;

        Face(BlockPos marker, Direction facing) {
            this.marker = marker;
            this.facing = facing;
        }

        BlockPos grower() {
            return marker.above();
        }
    }

    /** A jaw facing a face's grower, its bed behind it, and cilia on a Mouth facing that bed. */
    enum Cutter {
        WEST_LEFT(Face.WEST, new BlockPos(-1, -2, -3), Direction.NORTH, new BlockPos(-2, -2, -2), Direction.EAST),
        WEST_RIGHT(Face.WEST, new BlockPos(-1, -2, -5), Direction.SOUTH, new BlockPos(-2, -2, -6), Direction.EAST),
        WEST_BACK(Face.WEST, new BlockPos(0, -2, -4), Direction.WEST, new BlockPos(2, -2, -4), Direction.WEST),
        NORTH_RIGHT(Face.NORTH, new BlockPos(1, -2, -5), Direction.WEST, new BlockPos(3, -2, -5), Direction.WEST);

        final Face face;
        final BlockPos jaw;
        final Direction facing;
        final BlockPos cilia;
        final Direction ciliaFacing;

        Cutter(Face face, BlockPos jaw, Direction facing, BlockPos cilia, Direction ciliaFacing) {
            this.face = face;
            this.jaw = jaw;
            this.facing = facing;
            this.cilia = cilia;
            this.ciliaFacing = ciliaFacing;
        }

        BlockPos bed() {
            return jaw.relative(facing.getOpposite());
        }

        BlockPos mouth() {
            return cilia.below();
        }
    }

    final GameTestHelper h;
    final ServerLevel level;
    final BlockPos start;
    /** Chunks forced beyond the yard for a route that runs out of it. */
    private final List<ChunkPos> beyond = new ArrayList<>();

    private GalleryRig(GameTestHelper h, BlockPos start) {
        this.h = h;
        this.level = h.getLevel();
        this.start = start;
    }

    /** A yard in its own forced chunks: tests that share a batch take different {@code slot}s. */
    static GalleryRig of(GameTestHelper h, int slot) {
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        return at(h, new BlockPos((origin.getX() & ~15) + 330008, 100, (origin.getZ() & ~15) + 350408 + slot * 64));
    }

    /** A yard at a fixed place, for a run that must find it again after the server restarts. */
    static GalleryRig at(GameTestHelper h, BlockPos start) {
        GalleryRig rig = new GalleryRig(h, start);
        for (ChunkPos chunk : rig.chunks()) TestChunks.force(rig.level, chunk);
        return rig;
    }

    List<ChunkPos> chunks() {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -REACH; x <= REACH + 15; x += 16)
            for (int z = -REACH; z <= REACH + 15; z += 16) {
                ChunkPos chunk = new ChunkPos(start.offset(Math.min(x, REACH), 0, Math.min(z, REACH)));
                if (!chunks.contains(chunk)) chunks.add(chunk);
            }
        return chunks;
    }

    BlockPos low() {
        return start.offset(-REACH, -9, -REACH);
    }

    BlockPos high() {
        return start.offset(REACH, 4, REACH);
    }

    AABB yard() {
        return AABB.encapsulatingFullBlocks(low(), high());
    }

    /** Clears the yard's blocks and any entity but players. */
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
        for (ChunkPos chunk : beyond) level.setChunkForced(chunk.x, chunk.z, false);
    }

    /** Forces the chunk of {@code offset} too, for a route that runs out of the yard; {@link #release} lets it go. */
    void cover(BlockPos offset) {
        ChunkPos chunk = new ChunkPos(at(offset));
        if (chunks().contains(chunk) || beyond.contains(chunk)) return;
        beyond.add(chunk);
        TestChunks.force(level, chunk);
    }

    BlockPos at(BlockPos offset) {
        return start.offset(offset);
    }

    void run(String command) {
        CommandSourceStack source =
                level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput();
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
        run("fill %d %d %d %d %d %d %s".formatted(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), block));
    }

    // ---- building ----

    /**
     * Stone all round, the Rootstock with {@code mutations} on its foundation before its finished staircase, the
     * landing as part of the biomass line, the bladder with {@code bladder} BU and its Vascular Junction, and the
     * storage chest behind its filtered Intake Mouth.
     */
    void minehead(int bladder, String... mutations) {
        clear();
        fill(new BlockPos(-REACH, -9, -REACH), new BlockPos(REACH, 2, 3), "stone");
        fill(new BlockPos(-1, 0, -1), new BlockPos(1, 3, 3), "air");
        set(new BlockPos(0, -1, 0), SUBSTRATE);
        List<String> named = new ArrayList<>();
        for (String mutation : mutations) named.add("\"" + mutation + "\"");
        set(
                HEAD,
                ("infestusfrontier:tunnel_root[facing=north]{segments:{},output:[{},{},{},{}],"
                                + "tank:{stored:0,capacity:2000},cells:0,ticks:0,steer:0,mutations:[%s]}")
                        .formatted(String.join(",", named)));
        set(
                STAIRS,
                ("infestusfrontier:surveyed_tissue[facing=north]{shape:0,length:%d,first_way:0,first_at:0,"
                                + "second_way:0,second_at:0,lit:0b,approved:1b,built:%d}")
                        .formatted(DEPTH, DEPTH + 1));
        MinePlan stairs = new MinePlan(PlanShape.STAIRCASE, DEPTH, Turn.NONE, Turn.NONE, false);
        for (PlanStep step : stairs.steps()) {
            for (Offset cell : step.passage()) set(stairs(cell), "air");
            set(stairs(step.root()), "infestusfrontier:root_cell[facing=north,bend=false]");
            step.tread().ifPresent(tread -> set(stairs(tread), "cobblestone_stairs[facing=south,half=bottom]"));
        }
        // the shared biomass line: the landing and a spine under it, fed through a junction from the bladder
        set(LANDING, "infestusfrontier:biomass_vein");
        fill(new BlockPos(-1, -5, -5), new BlockPos(1, -5, -3), "infestusfrontier:biomass_vein");
        fill(new BlockPos(0, -4, -4), new BlockPos(0, -4, -4), "infestusfrontier:biomass_vein");
        set(new BlockPos(-1, -6, -6), SUBSTRATE);
        set(new BlockPos(-1, -6, -7), SUBSTRATE);
        set(new BlockPos(-1, -5, -6), "infestusfrontier:vascular_junction[north=intake,south=outlet]");
        set(BLADDER, "infestusfrontier:biomass_bladder{tank:{stored:%d,reserved:0,capacity:4000}}".formatted(bladder));
        // the shared export: a slab of Item Veins under everything, one filtered Intake Mouth and its chest
        fill(new BlockPos(-2, -7, -7), new BlockPos(4, -7, -1), "infestusfrontier:item_vein");
        set(
                new BlockPos(5, -7, -4),
                "infestusfrontier:mouth[mode=intake]{filter:[{id:\"minecraft:cobblestone\",count:1},"
                        + "{id:\"minecraft:raw_iron\",count:1}]}");
        set(CHEST, "chest[facing=north]");
    }

    /** The marker of a route of {@code length} cells from the landing, and its grower holding {@code segments}. */
    void face(Face face, int length, Turn first, Turn second, boolean wide, int segments) {
        set(
                face.marker,
                ("infestusfrontier:surveyed_tissue[facing=%s]{shape:1,length:%d,first_way:%d,first_at:%d,"
                                + "second_way:%d,second_at:%d,lit:0b,approved:1b,built:0,wide:%s}")
                        .formatted(
                                face.facing.getSerializedName(),
                                length,
                                first.way().ordinal(),
                                first.at(),
                                second.way().ordinal(),
                                second.at(),
                                wide ? "1b" : "0b"));
        set(
                face.grower(),
                ("infestusfrontier:structure_grower{segments:%s,treads:{},lights:{},steps:0,ticks:0}")
                        .formatted(
                                segments > 0
                                        ? "{id:\"infestusfrontier:root_segment\",count:%d}".formatted(segments)
                                        : "{}"));
    }

    /** A jaw with an empty tank on the biomass line, its bed, and its cilia with a full tank on an export Mouth. */
    void cutter(Cutter cutter) {
        BlockPos foundation = cutter.jaw.below();
        if (!foundation.equals(LANDING)) {
            set(foundation, "infestusfrontier:biomass_vein");
            fill(foundation.below(), foundation.below(2), "infestusfrontier:biomass_vein");
        }
        set(
                cutter.jaw,
                "infestusfrontier:fracture_jaw[facing=%s]{tank:{stored:0,capacity:2000},cuts:0,ticks:0}"
                        .formatted(cutter.facing.getSerializedName()));
        set(cutter.bed().below(), SUBSTRATE);
        set(cutter.bed(), "infestusfrontier:work_bed");
        set(cutter.mouth(), "infestusfrontier:mouth[mode=output]");
        fill(cutter.mouth().below(), cutter.mouth().below(3), "infestusfrontier:item_vein");
        set(
                cutter.cilia,
                "infestusfrontier:collection_cilia[facing=%s]{tank:{stored:1000,capacity:1000},sent:0}"
                        .formatted(cutter.ciliaFacing.getSerializedName()));
    }

    // ---- reading ----

    /** The plan the marker of {@code face} holds now. */
    MinePlan plan(Face face) {
        CompoundTag saved = saved(face.marker);
        return new MinePlan(
                PlanShape.ROUTE,
                saved.getInt("length"),
                saved.getInt("first_way") == 0
                        ? Turn.NONE
                        : new Turn(
                                org.jd.infestusfrontier.core.mining.Steer.values()[saved.getInt("first_way")],
                                saved.getInt("first_at")),
                saved.getInt("second_way") == 0
                        ? Turn.NONE
                        : new Turn(
                                org.jd.infestusfrontier.core.mining.Steer.values()[saved.getInt("second_way")],
                                saved.getInt("second_at")),
                false,
                saved.getBoolean("wide"));
    }

    /** Where {@code offset} of {@code face}'s plan lies, relative to the head. */
    static BlockPos plan(Face face, Offset offset) {
        return face.marker
                .relative(face.facing, offset.forward())
                .relative(face.facing.getClockWise(), offset.right())
                .above(offset.up());
    }

    private static BlockPos stairs(Offset offset) {
        return STAIRS.relative(Direction.NORTH, offset.forward())
                .relative(Direction.EAST, offset.right())
                .above(offset.up());
    }

    /** How many blocks of {@code face}'s whole route are {@code block} now. */
    int count(Face face, Block block) {
        int found = 0;
        for (PlanStep step : plan(face).steps())
            for (Offset cell : step.cleared())
                if (level.getBlockState(at(plan(face, cell))).is(block)) found++;
        return found;
    }

    BlockEntity entity(BlockPos offset) {
        return level.getBlockEntity(at(offset));
    }

    CompoundTag saved(BlockPos offset) {
        return entity(offset).saveWithFullMetadata(level.registryAccess());
    }

    int built(Face face) {
        return saved(face.marker).getInt("built");
    }

    int cuts(Cutter cutter) {
        return saved(cutter.jaw).getInt("cuts");
    }

    /** Items the bed of {@code cutter} holds now. */
    int bed(Cutter cutter) {
        return saved(cutter.bed()).getList("slots", 10).stream()
                .mapToInt(slot -> ((CompoundTag) slot).getInt("count"))
                .sum();
    }

    /** How many {@code item} the storage chest holds. */
    int stored(Item item) {
        return entity(CHEST) instanceof Container container ? container.countItem(item) : -1;
    }

    int loose() {
        return level.getEntitiesOfClass(ItemEntity.class, yard()).size();
    }

    /** A path a zombie would walk from {@code from}, standing, to {@code to}. */
    Path walk(BlockPos from, BlockPos to) {
        Zombie walker = new Zombie(EntityType.ZOMBIE, level);
        walker.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(96);
        walker.moveTo(Vec3.atBottomCenterOf(from));
        walker.setOnGround(true);
        Path path = walker.getNavigation().createPath(to, 0);
        walker.discard();
        return path;
    }

    TunnelRootMenu menu(Player player) {
        h.assertTrue(entity(HEAD) instanceof MenuProvider, "a Rootstock at the minehead");
        return (TunnelRootMenu) ((MenuProvider) entity(HEAD)).createMenu(0, player.getInventory(), player);
    }

    /** What the Rootstock's screen shows of its faces, in turn order. */
    List<TunnelRootMenu.Face> faces(Player player) {
        return menu(player).faces();
    }

    /** The translation key of what holds up face {@code index} on the Rootstock's screen, or empty while it works. */
    Optional<String> faceStop(Player player, int index) {
        List<TunnelRootMenu.Face> faces = faces(player);
        h.assertTrue(index < faces.size(), "face " + index + " of " + faces.size());
        return faces.get(index).stop().map(stop -> "infestusfrontier.refusal." + stop.key());
    }
}
