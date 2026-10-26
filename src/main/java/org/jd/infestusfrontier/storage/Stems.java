package org.jd.infestusfrontier.storage;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jd.infestusfrontier.core.storage.StemPlan;
import org.jd.infestusfrontier.storage.api.CapsuleStock;

/**
 * Capsule Stems and their pods. Each pod keeps its own compartment; the tree's store ({@link CapsuleTree}) is made by
 * the root when first asked and shared by every pod the root reaches until the tree changes. A pod whose root is gone
 * or does not name it stands alone with a store of its own.
 */
final class Stems {
    private Stems() {}

    /** The store of {@code pod}'s tree, or of the pod alone; empty while the pod is unreadable or out of reach. */
    static Optional<CapsuleTree> of(ItemCapsuleBlockEntity pod) {
        Level level = pod.getLevel();
        if (level == null || !pod.isReadable()) return Optional.empty();
        CapsuleTree store = pod.store();
        if (store != null && store.isUsable() && store.whole()) return Optional.of(store);
        Optional<CapsuleStemBlockEntity> root = pod.root().flatMap(pos -> rootAt(level, pos));
        if (root.isPresent()) {
            Optional<CapsuleTree> tree = root.get().tree();
            if (tree.isPresent() && pod.store() == tree.get()) return tree;
            pod.hangOn(Optional.empty());
        }
        return Optional.of(new CapsuleTree(level, List.of(pod), List.of(pod.getBlockPos())));
    }

    /** The store of the tree {@code cell} belongs to; empty while its root is gone or part of the tree out of reach. */
    static Optional<CapsuleTree> of(CapsuleStemBlockEntity cell) {
        return cell.root().flatMap(CapsuleStemBlockEntity::tree);
    }

    /** The root standing at {@code pos}: a loaded stem cell that holds a plan. */
    static Optional<CapsuleStemBlockEntity> rootAt(LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        return level.getBlockEntity(pos) instanceof CapsuleStemBlockEntity cell && cell.isRoot()
                ? Optional.of(cell)
                : Optional.empty();
    }

    /** The stem cell standing at {@code pos}, whatever tree it belongs to. */
    static Optional<CapsuleStemBlockEntity> cellAt(LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        return level.getBlockEntity(pos) instanceof CapsuleStemBlockEntity cell ? Optional.of(cell) : Optional.empty();
    }

    /**
     * Whether a capsule takes {@code stack}: anything but an item that carries a capsule's compartments, so stores
     * never nest. Capsule items fit in no container item either, as shulker boxes do not fit in each other.
     */
    static boolean takes(ItemStack stack) {
        CapsuleStock carried = stack.get(StorageModule.CAPSULE_STOCK.get());
        return !stack.isEmpty() && (carried == null || carried.isEmpty());
    }

    /** The direction of a plan side: north, east, south, west. */
    static Direction direction(int side) {
        return Direction.fromDelta(StemPlan.DX[side], 0, StemPlan.DZ[side]);
    }

    /** The plan side of a horizontal direction. */
    static int side(Direction direction) {
        for (int side = 0; side < StemPlan.SIDES; side++) if (direction(side) == direction) return side;
        throw new IllegalArgumentException(direction + " is no side");
    }
}
