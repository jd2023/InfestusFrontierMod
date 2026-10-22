package org.jd.infestusfrontier.storage;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.Box;
import org.jd.infestusfrontier.storage.api.CapsuleStock;

/**
 * Item Capsule bodies. Each cell keeps its own compartments; the body's store ({@link CapsuleBody}) is made when the
 * body forms, or when a cell is first asked for it, and is shared by every cell until the body changes. Asking a cell
 * whose body is out of reach reads at most its box, {@value org.jd.infestusfrontier.core.storage.Body#MAX_SIDE}³
 * cells, and finds no store.
 */
final class Capsules {
    /** A body formed or looked at again gets a fresh store; every cell shows where the body ends. */
    static final Bodies.Kind<ItemCapsuleBlockEntity> KIND = new Bodies.Kind<>() {
        @Override
        public Class<ItemCapsuleBlockEntity> type() {
            return ItemCapsuleBlockEntity.class;
        }

        @Override
        public void formed(List<ItemCapsuleBlockEntity> members, Box body) {
            attach(members.get(0).getLevel(), members);
            for (ItemCapsuleBlockEntity member : members) show(member, body);
        }

        @Override
        public void alone(ItemCapsuleBlockEntity cell) {
            show(cell, cell.body());
        }
    };

    private Capsules() {}

    /** The store of {@code cell}'s body, or empty while the cell is unreadable or part of its body is out of reach. */
    static Optional<CapsuleBody> of(ItemCapsuleBlockEntity cell) {
        Level level = cell.getLevel();
        if (level == null || !cell.isReadable()) return Optional.empty();
        CapsuleBody store = cell.store();
        if (store != null && store.isUsable()) return Optional.of(store);
        return Bodies.members(cell, KIND).map(members -> attach(level, members));
    }

    private static CapsuleBody attach(Level level, List<ItemCapsuleBlockEntity> members) {
        CapsuleBody store = new CapsuleBody(level, members);
        for (ItemCapsuleBlockEntity member : members) member.attach(store);
        return store;
    }

    /**
     * Whether a capsule takes {@code stack}: anything but an item that carries a capsule's compartments, so stores
     * never nest. Capsule items fit in no container item either, as shulker boxes do not fit in each other.
     */
    static boolean takes(ItemStack stack) {
        CapsuleStock carried = stack.get(StorageModule.CAPSULE_STOCK.get());
        return !stack.isEmpty() && (carried == null || carried.isEmpty());
    }

    private static void show(ItemCapsuleBlockEntity cell, Box body) {
        Level level = cell.getLevel();
        if (level == null) return;
        BlockState shown = ItemCapsuleBlock.shaped(cell.getBlockState(), cell.getBlockPos(), body);
        if (shown != cell.getBlockState()) level.setBlock(cell.getBlockPos(), shown, Block.UPDATE_CLIENTS);
    }
}
