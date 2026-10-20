package org.jd.infestusfrontier.cultivation;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/** The pouch screen's server half: the four stock slots and the reserve as a synced int. The stick sets the reserve. */
public final class SeedPouchMenu extends OrganMenu {
    private static final int RESERVE = 0;

    /** Server side. */
    SeedPouchMenu(int id, Inventory inventory, SeedPouchBlockEntity pouch) {
        this(id, inventory, pouch.stock(), reading(pouch::reserve), at(pouch), Optional.of(pouch));
    }

    /** Client side. The position arrives as extra data. */
    public SeedPouchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(SeedPouchBlockEntity.SLOTS),
                new SimpleContainerData(1),
                at(inventory, extra),
                Optional.empty());
    }

    private SeedPouchMenu(
            int id,
            Inventory inventory,
            IItemHandler stock,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<SeedPouchBlockEntity> pouch) {
        super(CultivationModule.SEED_POUCH_MENU.get(), id, access, CultivationModule.SEED_POUCH.get(), data);
        for (int slot = 0; slot < SeedPouchBlockEntity.SLOTS; slot++)
            addSlot(new SlotItemHandler(
                    stock, slot, Sheet.SLOT_X + slot * (Sheet.SLOT.width() + Sheet.GAP), Sheet.SLOT_Y));
        addPlayer(inventory, 0);
    }

    @Override
    public int stored() {
        return 0;
    }

    @Override
    public int capacity() {
        return 1;
    }

    public int reserve() {
        return synced(RESERVE);
    }
}
