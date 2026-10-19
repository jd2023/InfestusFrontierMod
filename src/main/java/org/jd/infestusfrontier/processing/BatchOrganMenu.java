package org.jd.infestusfrontier.processing;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/**
 * The server half of every batch organ's screen: the kind's ingredient slots and the output slot backed by the block
 * entity's own handlers, five synced ints (water, biomass, biomass capacity, progress percent, state) and the Start
 * button. Nothing here decides a batch; the block entity does.
 */
public final class BatchOrganMenu extends OrganMenu {
    public static final int START_BUTTON = 0;
    public static final int IDLE = 0;
    public static final int WORKING = 1;
    public static final int PAUSED = 2;
    private static final int WATER = 0;
    private static final int STORED = 1;
    private static final int CAPACITY = 2;
    private static final int PERCENT = 3;
    private static final int STATE = 4;
    private static final int SYNCED = 5;

    private final BatchOrganKind kind;
    private final Optional<BatchOrganBlockEntity> organ;

    /** Server side. */
    BatchOrganMenu(MenuType<BatchOrganMenu> type, int id, Inventory inventory, BatchOrganBlockEntity organ) {
        this(
                type,
                organ.kind(),
                id,
                inventory,
                organ.ingredients(),
                organ.output(),
                reading(
                        () -> organ.water().getFluidAmount(),
                        () -> organ.tank().stored().units(),
                        () -> organ.tank().capacity().units(),
                        organ::percent,
                        organ::state),
                at(organ),
                Optional.of(organ));
    }

    /** Client side. The position arrives as extra data; the kind comes with the menu type. */
    BatchOrganMenu(
            MenuType<BatchOrganMenu> type,
            BatchOrganKind kind,
            int id,
            Inventory inventory,
            RegistryFriendlyByteBuf extra) {
        this(
                type,
                kind,
                id,
                inventory,
                new ItemStackHandler(kind.slots()),
                new ItemStackHandler(1),
                new SimpleContainerData(SYNCED),
                at(inventory, extra),
                Optional.empty());
    }

    private BatchOrganMenu(
            MenuType<BatchOrganMenu> type,
            BatchOrganKind kind,
            int id,
            Inventory inventory,
            IItemHandler ingredients,
            IItemHandler output,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<BatchOrganBlockEntity> organ) {
        super(type, id, access, kind.block().get(), data);
        this.kind = kind;
        this.organ = organ;
        for (int i = 0; i < kind.slots(); i++) {
            Bounds cell = ingredientWell(kind, i);
            addSlot(new SlotItemHandler(ingredients, i, cell.x() + 1, cell.y() + 1));
        }
        Bounds out = outputWell(kind);
        addSlot(new SlotItemHandler(output, 0, out.x() + 1, out.y() + 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayer(inventory, kind.strips());
    }

    /** Where the kind's ingredient well {@code index} sits on the panel: a 3x3 grid for nine slots, else the parts row. */
    static Bounds ingredientWell(BatchOrganKind kind, int index) {
        if (kind.slots() > 3) return SlotGrid.standard3x3().cell(index);
        return new Bounds(
                Sheet.FIRST_PART_X + index * (Sheet.SLOT.width() + Sheet.GAP),
                Sheet.PARTS_Y,
                Sheet.SLOT.width(),
                Sheet.SLOT.height());
    }

    /** Where the kind's output well sits: under the vein on a grid panel, after the vein on a row panel. */
    static Bounds outputWell(BatchOrganKind kind) {
        if (kind.slots() > 3)
            return new Bounds(Sheet.SLOT_X + 57, Sheet.ROW2_Y, Sheet.SLOT.width(), Sheet.SLOT.height());
        int afterWells = Sheet.FIRST_PART_X + kind.slots() * (Sheet.SLOT.width() + Sheet.GAP);
        return new Bounds(
                afterWells + Sheet.VEIN_EMPTY.width() + Sheet.GAP,
                Sheet.PARTS_Y,
                Sheet.SLOT.width(),
                Sheet.SLOT.height());
    }

    public int ingredientSlots() {
        return kind.slots();
    }

    /** The slot index of the output well. */
    public int outputSlot() {
        return kind.slots();
    }

    public int waterCapacity() {
        return kind.waterMb();
    }

    public boolean holdsWater() {
        return kind.holdsWater();
    }

    public boolean holdsBiomass() {
        return kind.holdsBiomass();
    }

    public int water() {
        return synced(WATER);
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    public int percent() {
        return Math.clamp(synced(PERCENT), 0, 100);
    }

    /** One of {@link #IDLE}, {@link #WORKING}, {@link #PAUSED}. */
    public int state() {
        return Math.clamp(synced(STATE), IDLE, PAUSED);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != START_BUTTON) return false;
        organ.ifPresent(entity -> entity.start(player).ifPresent(refusal -> Refusals.show(player, refusal)));
        return true;
    }
}
