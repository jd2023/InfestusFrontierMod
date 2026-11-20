package org.jd.infestusfrontier.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.StairBlock;
import org.jd.infestusfrontier.core.mining.GrowerProgress;
import org.jd.infestusfrontier.core.mining.Surveying;

/**
 * Everything a Structure Grower owns: its Root Segments, treads, lights and ribs, and its progress, whose steps are its
 * history. Where it stands in a plan is the marker's, not the grower's. Saved on the block entity and carried on the
 * item when the grower is broken; a grower saved before it took ribs reads with none. The stacks are copies.
 */
record StructureGrowerContents(
        ItemStack segments, ItemStack treads, ItemStack lights, ItemStack ribs, GrowerProgress progress) {
    static final StructureGrowerContents EMPTY = new StructureGrowerContents(
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, GrowerProgress.START);

    StructureGrowerContents {
        segments = segments.copy();
        treads = treads.copy();
        lights = lights.copy();
        ribs = ribs.copy();
    }

    /** The saved numbers before they are judged; impossible ones are a decode error, never an exception. */
    private record Raw(ItemStack segments, ItemStack treads, ItemStack lights, ItemStack ribs, int steps, int ticks) {}

    static final MapCodec<StructureGrowerContents> CODEC = RecordCodecBuilder.<Raw>mapCodec(instance -> instance.group(
                            ItemStack.OPTIONAL_CODEC.fieldOf("segments").forGetter(Raw::segments),
                            ItemStack.OPTIONAL_CODEC.fieldOf("treads").forGetter(Raw::treads),
                            ItemStack.OPTIONAL_CODEC.fieldOf("lights").forGetter(Raw::lights),
                            ItemStack.OPTIONAL_CODEC
                                    .optionalFieldOf("ribs", ItemStack.EMPTY)
                                    .forGetter(Raw::ribs),
                            Codec.INT.fieldOf("steps").forGetter(Raw::steps),
                            Codec.INT.fieldOf("ticks").forGetter(Raw::ticks))
                    .apply(instance, Raw::new))
            .flatXmap(StructureGrowerContents::read, contents -> DataResult.success(contents.raw()));

    /** The saved keys, for taking them off a tag that a carried grower's item also holds. */
    static final String[] KEYS = {"segments", "treads", "lights", "ribs", "steps", "ticks"};

    private Raw raw() {
        return new Raw(segments, treads, lights, ribs, progress.steps(), progress.ticks());
    }

    private static DataResult<StructureGrowerContents> read(Raw raw) {
        if (raw.steps() < 0) return DataResult.error(() -> "steps " + raw.steps());
        if (raw.ticks() < 0 || raw.ticks() > Surveying.GROW_TICKS)
            return DataResult.error(() -> "ticks " + raw.ticks());
        StructureGrowerContents contents = new StructureGrowerContents(
                raw.segments(), raw.treads(), raw.lights(), raw.ribs(), new GrowerProgress(raw.steps(), raw.ticks()));
        Optional<String> problem = contents.problem();
        return problem.isPresent() ? DataResult.error(problem::get) : DataResult.success(contents);
    }

    /** Why these contents cannot be a grower's, or empty when they can. */
    Optional<String> problem() {
        if (!segments.isEmpty() && (!segments.is(MiningModule.ROOT_SEGMENT.get()) || over(segments)))
            return Optional.of("segments " + segments);
        if (!treads.isEmpty() && (!tread(treads) || over(treads))) return Optional.of("treads " + treads);
        if (!lights.isEmpty() && (!light(lights) || over(lights))) return Optional.of("lights " + lights);
        if (!ribs.isEmpty() && (!rib(ribs) || over(ribs))) return Optional.of("ribs " + ribs);
        return Optional.empty();
    }

    /** True for stairs a grower lays as treads: tagged, a plain stair block with no block entity. */
    static boolean tread(ItemStack stack) {
        return stack.is(MiningModule.MINE_TREADS)
                && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof StairBlock
                && !item.getBlock().defaultBlockState().hasBlockEntity();
    }

    /** True for what a grower lights a step with: a torch, or what it grows a Lumen Node from. */
    static boolean light(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(MiningModule.LIVING_LIGHTS);
    }

    /** True for what a grower frames a step with, one a column: Bone Plates. */
    static boolean rib(ItemStack stack) {
        return stack.is(MiningModule.MINE_RIBS);
    }

    /** True for what the grower takes from a hand. */
    static boolean loads(ItemStack stack) {
        return stack.is(MiningModule.ROOT_SEGMENT.get()) || tread(stack) || light(stack) || rib(stack);
    }

    private static boolean over(ItemStack stack) {
        return stack.getCount() > Math.min(Surveying.MAX_STOCK, stack.getMaxStackSize());
    }
}
