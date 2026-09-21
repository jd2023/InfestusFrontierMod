package org.jd.infestusfrontier.processing.menu;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Narrow processing-owned port used by the menu adapter. */
public interface BowlMenuTarget {
    UUID menuOwner();
    BlockPos menuPosition();
    boolean isPresentIn(ServerLevel level);
    CultureBowlMenuSnapshot menuSnapshot(BowlRefusal refusal);
    ApplyResult applyMenuIntent(ServerPlayer player, BowlIntentPayload payload);

    record ApplyResult(BowlRefusal refusal, ItemStack extracted) {
        public ApplyResult {
            if (refusal == null || extracted == null) throw new IllegalArgumentException("Menu result fields are required");
            if (refusal != BowlRefusal.NONE && !extracted.isEmpty()) throw new IllegalArgumentException("Refusal cannot extract an item");
        }
        public static ApplyResult refused(BowlRefusal refusal) { return new ApplyResult(refusal, ItemStack.EMPTY); }
        public static ApplyResult accepted() { return new ApplyResult(BowlRefusal.NONE, ItemStack.EMPTY); }
        public static ApplyResult extracted(ItemStack stack) { return new ApplyResult(BowlRefusal.NONE, stack); }
    }
}
