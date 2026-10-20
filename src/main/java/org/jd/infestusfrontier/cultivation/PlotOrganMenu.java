package org.jd.infestusfrontier.cultivation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The server half of a plot organ's screen: its item slots on the parts row, take-only for the harvester, and the
 * tank as two synced ints.
 */
public final class PlotOrganMenu extends OrganMenu {
    private static final int STORED = 0;
    private static final int CAPACITY = 1;

    /** Server side. */
    PlotOrganMenu(
            MenuType<PlotOrganMenu> type, int id, Inventory inventory, PlotOrganBlockEntity organ, boolean takeOnly) {
        this(
                type,
                id,
                inventory,
                organ.items(),
                takeOnly,
                reading(
                        () -> organ.tank().stored().units(),
                        () -> organ.tank().capacity().units()),
                at(organ),
                organ.getBlockState().getBlock());
    }

    /** Client side: the planter's one slot or the harvester's four, by menu type. */
    public static PlotOrganMenu planter(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        return new PlotOrganMenu(
                CultivationModule.PLANTING_PROBOSCIS_MENU.get(),
                id,
                inventory,
                new ItemStackHandler(1),
                false,
                new SimpleContainerData(2),
                at(inventory, extra),
                CultivationModule.PLANTING_PROBOSCIS.get());
    }

    public static PlotOrganMenu harvester(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        return new PlotOrganMenu(
                CultivationModule.HARVEST_COROLLA_MENU.get(),
                id,
                inventory,
                new ItemStackHandler(HarvestCorollaBlockEntity.OUTPUT_SLOTS),
                true,
                new SimpleContainerData(2),
                at(inventory, extra),
                CultivationModule.HARVEST_COROLLA.get());
    }

    private PlotOrganMenu(
            MenuType<PlotOrganMenu> type,
            int id,
            Inventory inventory,
            IItemHandler items,
            boolean takeOnly,
            ContainerData data,
            ContainerLevelAccess access,
            Block block) {
        super(type, id, access, block, data);
        for (int slot = 0; slot < items.getSlots(); slot++)
            addSlot(
                    new SlotItemHandler(
                            items, slot, Sheet.SLOT_X + slot * (Sheet.SLOT.width() + Sheet.GAP), Sheet.SLOT_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return !takeOnly && super.mayPlace(stack);
                        }
                    });
        addPlayer(inventory, 0);
    }

    public int slotCount() {
        return slots.size() - 36;
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }
}
