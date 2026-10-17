package org.jd.infestusfrontier.processing;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.foundation.screen.BiomassView;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/**
 * The Bowl screen's server half: nine ingredient slots and the output slot backed by the block entity's own handlers,
 * five synced ints (water, biomass, biomass capacity, progress percent, state) and the Start button. Nothing here
 * decides a batch; the block entity does.
 */
public final class CultureBowlMenu extends AbstractContainerMenu implements BiomassView {
    public static final int START_BUTTON = 0;
    public static final int IDLE = 0;
    public static final int WORKING = 1;
    public static final int PAUSED = 2;
    static final int INGREDIENTS = CultureBowlBlockEntity.INGREDIENT_SLOTS;
    static final int OUTPUT_SLOT = INGREDIENTS;
    private static final int WATER = 0;
    private static final int STORED = 1;
    private static final int CAPACITY = 2;
    private static final int PERCENT = 3;
    private static final int STATE = 4;
    private static final int OUTPUT_X = Sheet.SLOT_X + 58;
    private static final int INVENTORY_START = OUTPUT_SLOT + 1;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int SLOT_END = HOTBAR_START + 9;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Optional<CultureBowlBlockEntity> bowl;

    /** Server side. */
    CultureBowlMenu(int id, Inventory inventory, CultureBowlBlockEntity bowl) {
        this(
                id,
                inventory,
                bowl.ingredients(),
                bowl.output(),
                new BowlData(bowl),
                ContainerLevelAccess.create(bowl.getLevel(), bowl.getBlockPos()),
                Optional.of(bowl));
    }

    /** Client side. The position arrives as extra data. */
    public CultureBowlMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(INGREDIENTS),
                new ItemStackHandler(1),
                new SimpleContainerData(5),
                ContainerLevelAccess.create(inventory.player.level(), extra.readBlockPos()),
                Optional.empty());
    }

    private CultureBowlMenu(
            int id,
            Inventory inventory,
            IItemHandler ingredients,
            IItemHandler output,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<CultureBowlBlockEntity> bowl) {
        super(ProcessingModule.CULTURE_BOWL_MENU.get(), id);
        this.data = data;
        this.access = access;
        this.bowl = bowl;
        SlotGrid grid = SlotGrid.standard3x3();
        for (int i = 0; i < INGREDIENTS; i++) {
            Bounds cell = grid.cell(i);
            addSlot(new SlotItemHandler(ingredients, i, cell.x() + 1, cell.y() + 1));
        }
        addSlot(new SlotItemHandler(output, 0, OUTPUT_X, Sheet.SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, Sheet.inventoryY(3) + 18 * row));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, Sheet.hotbarY(3)));
        addDataSlots(data);
    }

    public int water() {
        return data.get(WATER);
    }

    @Override
    public int stored() {
        return data.get(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, data.get(CAPACITY));
    }

    public int percent() {
        return Math.clamp(data.get(PERCENT), 0, 100);
    }

    /** One of {@link #IDLE}, {@link #WORKING}, {@link #PAUSED}. */
    public int state() {
        return Math.clamp(data.get(STATE), IDLE, PAUSED);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != START_BUTTON) return false;
        bowl.ifPresent(entity -> entity.start(player).ifPresent(refusal -> Refusals.show(player, refusal)));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved;
        if (index < INVENTORY_START) moved = moveItemStackTo(stack, INVENTORY_START, SLOT_END, true);
        else if (slots.get(0).mayPlace(stack)) moved = moveItemStackTo(stack, 0, INGREDIENTS, false);
        else if (index < HOTBAR_START) moved = moveItemStackTo(stack, HOTBAR_START, SLOT_END, false);
        else moved = moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ProcessingModule.CULTURE_BOWL.get());
    }

    /** Reads the live bowl on the server; vanilla syncs changed ints to the client. */
    private static final class BowlData implements ContainerData {
        private final CultureBowlBlockEntity bowl;

        BowlData(CultureBowlBlockEntity bowl) {
            this.bowl = bowl;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case WATER -> bowl.water().getFluidAmount();
                case STORED -> bowl.tank().stored().units();
                case CAPACITY -> bowl.tank().capacity().units();
                case PERCENT -> bowl.percent();
                default -> bowl.state();
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 5;
        }
    }
}
