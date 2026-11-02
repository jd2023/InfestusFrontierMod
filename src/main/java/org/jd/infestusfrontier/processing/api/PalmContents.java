package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The saved and carried form of a Shaping Palm: the nine stacks of its grid, in the crafting table's row-major order,
 * with empty stacks for empty cells. A value: the stacks are copied in and out, and two contents are equal when every
 * stack matches item for item and component for component.
 */
public record PalmContents(List<ItemStack> grid) {
    /** The cells of the grid: three by three, as at a crafting table. */
    public static final int CELLS = 9;

    public static final PalmContents EMPTY = new PalmContents(List.of());

    public PalmContents {
        if (grid.size() > CELLS) throw new IllegalArgumentException(grid.size() + " grid cells");
        List<ItemStack> padded = new ArrayList<>(CELLS);
        for (ItemStack stack : grid) padded.add(stack.copy());
        while (padded.size() < CELLS) padded.add(ItemStack.EMPTY);
        grid = List.copyOf(padded);
    }

    /** A fresh copy of the stack in cell {@code cell}. */
    public ItemStack cell(int cell) {
        return grid.get(cell).copy();
    }

    @Override
    public List<ItemStack> grid() {
        return grid.stream().map(ItemStack::copy).toList();
    }

    public boolean isEmpty() {
        return grid.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PalmContents that && ItemStack.listMatches(grid, that.grid);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(grid);
    }

    /** Saved as {@code grid}, at most nine stacks. */
    public static final Codec<PalmContents> CODEC = ItemStack.OPTIONAL_CODEC
            .listOf(0, CELLS)
            .fieldOf("grid")
            .xmap(PalmContents::new, PalmContents::grid)
            .codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, PalmContents> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
