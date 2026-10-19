package org.jd.infestusfrontier.processing;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Sac screen's server half: the feed slot, backed by the block entity's own slot so items stay with the block,
 * and three synced ints: percent done, biomass stored and capacity. Nothing here starts a batch; the block
 * entity takes from the slot on its own tick.
 */
public final class DigestiveSacMenu extends OrganMenu {
    static final int FEED_SLOT = 0;
    private static final int PERCENT = 0;
    private static final int STORED = 1;
    private static final int CAPACITY = 2;

    /** Server side. */
    DigestiveSacMenu(int id, Inventory inventory, DigestiveSacBlockEntity sac) {
        this(
                id,
                inventory,
                sac.feed(),
                reading(
                        sac::percent,
                        () -> sac.tank().stored().units(),
                        () -> sac.tank().capacity().units()),
                at(sac));
    }

    /** Client side. The position arrives as extra data. */
    public DigestiveSacMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new ItemStackHandler(1), new SimpleContainerData(3), at(inventory, extra));
    }

    private DigestiveSacMenu(
            int id, Inventory inventory, IItemHandler feed, ContainerData data, ContainerLevelAccess access) {
        super(ProcessingModule.DIGESTIVE_SAC_MENU.get(), id, access, ProcessingModule.DIGESTIVE_SAC.get(), data);
        addSlot(new SlotItemHandler(feed, 0, Sheet.SLOT_X, Sheet.SLOT_Y));
        addPlayer(inventory, 0);
    }

    public int percent() {
        return Math.clamp(synced(PERCENT), 0, 100);
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    static boolean isSac(BlockEntity entity) {
        return entity instanceof DigestiveSacBlockEntity;
    }
}
