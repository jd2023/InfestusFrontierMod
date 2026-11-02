package org.jd.infestusfrontier.processing;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.processing.PalmSupply.Reached;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.processing.api.PalmRote;
import org.jd.infestusfrontier.storage.api.ItemStores;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/**
 * The Shaping Palm's crafting screen, the server half. Slot 0 is the vanilla {@link ResultSlot}, slots 1 to 9 are the
 * grid's cells over the block entity's own list, so closing the screen leaves them where they are. The result is the
 * crafting table's: the recipe lookup is {@link PalmCrafting#update}, taking it is {@code ResultSlot.onTake}. This menu
 * adds three things the table does not do: it looks the result up again before a click on it, because another viewer
 * may have used the grid since this menu last did; it refuses a craft whose result or remainder has no room, which the
 * table would drop on the ground; and it never clears the grid when it closes. Its first three buttons are the supply
 * link's actions (Restock, Return, Store result), each one request the server decides on the live grid and the live
 * store. A Palm with Rote has a fourth, Select, and four tray slots a player may take from; the client lays them out
 * from the block state, and the server judges every click. The client's copy is only a view.
 */
public final class ShapingPalmMenu extends OrganMenu {
    /** The vanilla result slot; the engine's result lookup writes slot 0. */
    public static final int RESULT = 0;

    public static final int FIRST_CELL = 1;
    /** The button ids: top every filled cell up from the store, move the grid to it, craft once into it. */
    public static final int RESTOCK = 0;

    public static final int RETURN = 1;
    public static final int STORE_RESULT = 2;
    /** Rote's button: record the grid as the pattern. */
    public static final int SELECT = 3;
    /** The first tray slot: the result; the three after it hold remainders. */
    public static final int FIRST_TRAY = FIRST_CELL + PalmContents.CELLS;
    /**
     * The synced ints: 0 no Reach, 1 Reach without a link, 2 linked; then the link's offset from the Palm; then the
     * status ordinal, the cycle's percent and the biomass held.
     */
    private static final int STATE = 0;

    private static final int OFFSET = 1;
    private static final int STATUS = 4;
    private static final int PERCENT = 5;
    private static final int BIOMASS = 6;
    private static final int SYNCED = 7;
    private static final int STRIPS = 2;
    /** A Palm with Rote has a third row for Select and the tray. */
    private static final int ROTE_STRIPS = 3;
    /** The tray's wells sit on that third row, right of the Select button. */
    private static final int TRAY_X = Sheet.LEFT_X + Sheet.BUTTON.width() + 6;

    private static final int TRAY_PITCH = Sheet.SLOT.width() + 4;
    private static final SlotGrid GRID = SlotGrid.standard3x3();
    /** How far right of the grid the result well sits. */
    private static final int RESULT_GAP = 24;
    /** The same gap on a Palm with Rote. */
    private static final int ROTE_RESULT_GAP = 8;

    private final Optional<ShapingPalmBlockEntity> palm;
    private final boolean rote;
    private final Grid grid;
    private final ResultContainer results = new ResultContainer();
    private final Player player;
    /** The version of the grid the result was last looked up for; a menu on the client never looks. */
    private int seen = -1;

    /** Server side. */
    ShapingPalmMenu(int id, Inventory inventory, ShapingPalmBlockEntity palm) {
        this(
                id,
                inventory,
                new Parts(palm.grid(), palm.tray()),
                Optional.of(palm),
                palm.getBlockState().getOptionalValue(ShapingPalmBlock.ROTE).orElse(false),
                at(palm),
                reading(reads(palm)));
    }

    /** Client side. The position arrives as extra data; whether the Palm has Rote arrives with its block state. */
    public ShapingPalmMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, extra.readBlockPos());
    }

    private ShapingPalmMenu(int id, Inventory inventory, BlockPos pos) {
        this(
                id,
                inventory,
                new Parts(
                        NonNullList.withSize(PalmContents.CELLS, ItemStack.EMPTY),
                        NonNullList.withSize(PalmRote.TRAY, ItemStack.EMPTY)),
                Optional.empty(),
                inventory
                        .player
                        .level()
                        .getBlockState(pos)
                        .getOptionalValue(ShapingPalmBlock.ROTE)
                        .orElse(false),
                ContainerLevelAccess.create(inventory.player.level(), pos),
                new SimpleContainerData(SYNCED));
    }

    /** The cells of the grid and the tray the menu shows: the organ's own lists on the server, scratch ones on the client. */
    private record Parts(NonNullList<ItemStack> cells, NonNullList<ItemStack> tray) {}

    private ShapingPalmMenu(
            int id,
            Inventory inventory,
            Parts parts,
            Optional<ShapingPalmBlockEntity> palm,
            boolean rote,
            ContainerLevelAccess access,
            ContainerData data) {
        super(ProcessingModule.SHAPING_PALM_MENU.get(), id, access, ProcessingModule.SHAPING_PALM.get(), data);
        this.palm = palm;
        this.rote = rote;
        this.player = inventory.player;
        this.grid = new Grid(parts.cells());
        Bounds out = resultWell(rote);
        addSlot(new ResultSlot(player, grid, results, RESULT, out.x() + 1, out.y() + 1));
        for (int cell = 0; cell < PalmContents.CELLS; cell++) {
            Bounds well = GRID.cell(cell);
            addSlot(new Cell(grid, cell, well.x() + 1, well.y() + 1, palm));
        }
        Tray tray = new Tray(parts.tray());
        for (int slot = 0; slot < PalmRote.TRAY; slot++) {
            Bounds well = trayWell(slot);
            addSlot(new TraySlot(tray, slot, well.x() + 1, well.y() + 1, palm, rote));
        }
        addPlayer(inventory, strips());
    }

    private static IntSupplier[] reads(ShapingPalmBlockEntity palm) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[STATE] = () -> palm.link().isPresent() ? 2 : palm.hasReach() ? 1 : 0;
        reads[STATUS] = () -> palm.status().ordinal();
        reads[PERCENT] = palm::percent;
        reads[BIOMASS] = palm::biomassUnits;
        for (int axis = 0; axis < 3; axis++) {
            int along = axis;
            reads[OFFSET + axis] = () ->
                    palm.link().map(to -> offset(palm.getBlockPos(), to, along)).orElse(0);
        }
        return reads;
    }

    private static int offset(BlockPos from, BlockPos to, int axis) {
        return switch (axis) {
            case 0 -> to.getX() - from.getX();
            case 1 -> to.getY() - from.getY();
            default -> to.getZ() - from.getZ();
        };
    }

    /** 0 when the Palm has no Reach, 1 when it has Reach and no link, 2 when it is linked. */
    public int reachState() {
        return synced(STATE);
    }

    /** The linked store's offset from the Palm along x (0), y (1) or z (2); 0 while there is no link. */
    public int linkOffset(int axis) {
        return synced(OFFSET + axis);
    }

    /**
     * Where the result well sits on the panel: level with the grid's middle row, right of the grid. With Rote it sits
     * close to the grid, so the biomass tank's amount, which runs left under it, clears the well.
     */
    static Bounds resultWell(boolean rote) {
        Bounds middle = GRID.cell(PalmContents.CELLS / 2);
        int gap = rote ? ROTE_RESULT_GAP : RESULT_GAP;
        return new Bounds(GRID.bounds().right() + gap, middle.y(), Sheet.SLOT.width(), Sheet.SLOT.height());
    }

    /** Where tray well {@code slot} sits: on the third row, right of the Select button. */
    static Bounds trayWell(int slot) {
        return new Bounds(TRAY_X + slot * TRAY_PITCH, trayY(), Sheet.SLOT.width(), Sheet.SLOT.height());
    }

    /** Top of the third row's wells: under the supply buttons, which end 3 below the grid. */
    static int trayY() {
        return GRID.bounds().bottom() + 3 + Sheet.BUTTON.height() + 2;
    }

    /** Whether the Palm has Rote, from its block state: the panel's height and the slots' places depend on it. */
    public boolean hasRote() {
        return rote;
    }

    int strips() {
        return rote ? ROTE_STRIPS : STRIPS;
    }

    /** What the Palm is doing and why, as it last judged. */
    public PalmStatus status() {
        return PalmStatus.of(synced(STATUS));
    }

    /** Percent of the cycle done. */
    public int percent() {
        return synced(PERCENT);
    }

    @Override
    public int stored() {
        return synced(BIOMASS);
    }

    @Override
    public int capacity() {
        return PalmRote.CAPACITY.units();
    }

    /** Looks the result up for the grid as it is now. */
    private void refresh() {
        palm.ifPresent(organ -> {
            access().execute((level, pos) -> PalmCrafting.update(this, level, player, grid, results));
            seen = organ.version();
        });
    }

    @Override
    public void slotsChanged(Container container) {
        refresh();
    }

    @Override
    public void broadcastChanges() {
        if (palm.isPresent() && palm.get().version() != seen) refresh();
        super.broadcastChanges();
    }

    /** True when taking the current result would leave something with nowhere to go; the player is told why. */
    private boolean refuses(Player taker, boolean resultToInventory) {
        if (palm.isEmpty()) return false;
        ItemStack result = results.getItem(0);
        if (result.isEmpty()) return false;
        Optional<Refusal> why =
                palm.get().refusal().or(() -> PalmCrafting.refusal(taker, grid, result, resultToInventory));
        why.ifPresent(refusal -> Refusals.show(taker, refusal));
        return why.isPresent();
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player clicker) {
        if (palm.isPresent() && slotId == RESULT) {
            refresh();
            boolean takes = type == ClickType.PICKUP || type == ClickType.SWAP || type == ClickType.THROW;
            ItemStack carried = getCarried();
            boolean fits = type != ClickType.PICKUP
                    || carried.isEmpty()
                    || ItemStack.isSameItemSameComponents(carried, results.getItem(0));
            if (takes && fits && refuses(clicker, type == ClickType.SWAP)) return;
        } else if (palm.isPresent() && palm.get().version() != seen) refresh();
        super.clicked(slotId, button, type, clicker);
    }

    /**
     * A shift-click on the result is the crafting table's: the result goes to the inventory and the craft is taken;
     * the engine repeats it while the grid still makes the same result. Each round is refused whole when the result or
     * a remainder has no room. Any other slot moves as in every organ.
     */
    @Override
    public ItemStack quickMoveStack(Player taker, int index) {
        if (index != RESULT) return super.quickMoveStack(taker, index);
        if (!slots.get(RESULT).hasItem() || refuses(taker, true)) return ItemStack.EMPTY;
        return craft(taker, stack -> {
            moveItemStackTo(stack, FIRST_CELL + PalmContents.CELLS, slots.size(), true);
            return stack;
        });
    }

    /**
     * One craft whose result goes where {@code sink} puts it, with the table's own bookkeeping: the item's crafted
     * hook, the statistics and the recipe unlock of {@code onTake}. {@code sink} returns what it could not place; a
     * craft that placed nothing returns empty and changes nothing, and a part left over is thrown at the player's
     * feet, as the table does. Returns the result as it was.
     */
    private ItemStack craft(Player taker, UnaryOperator<ItemStack> sink) {
        Slot slot = slots.get(RESULT);
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        stack.getItem().onCraftedBy(stack, taker.level(), taker);
        stack.setCount(sink.apply(stack).getCount());
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onQuickCraft(stack, before);
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(taker, stack);
        if (!stack.isEmpty()) taker.drop(stack, false);
        return before;
    }

    /** The supply link's actions, sent by id; the server decides on the live grid and the live store. */
    @Override
    public boolean clickMenuButton(Player taker, int id) {
        if (id < RESTOCK || id > SELECT) return false;
        if (palm.isEmpty()) return true;
        if (taker.containerMenu != this || !stillValid(taker)) return false;
        ShapingPalmBlockEntity organ = palm.get();
        Optional<Refusal> why = organ.refusal();
        if (why.isPresent()) return refuse(taker, why.get());
        if (id == SELECT) return select(taker, organ);
        return switch (organ.store()) {
            case Reached.Refused refused -> refuse(taker, refused.why());
            case Reached.Store reached -> act(taker, organ, reached.handler(), id);
        };
    }

    /** Select: the grid becomes the Palm's pattern, or the reason it cannot; the Palm changes nothing on a refusal. */
    private boolean select(Player taker, ShapingPalmBlockEntity organ) {
        Optional<Refusal> why = organ.select();
        if (why.isPresent()) return refuse(taker, why.get());
        taker.displayClientMessage(
                Component.translatable(
                        "infestusfrontier.status.palm.selected",
                        organ.pattern().get().result().getHoverName()),
                true);
        return true;
    }

    private boolean refuse(Player taker, Refusal why) {
        Refusals.show(taker, why);
        return false;
    }

    private boolean act(Player taker, ShapingPalmBlockEntity organ, IItemHandler store, int id) {
        refresh();
        return switch (id) {
            case RESTOCK -> {
                int moved = PalmSupply.restock(organ.grid(), store, back(taker));
                if (moved > 0) organ.touch();
                yield moved == 0
                        ? refuse(taker, PalmSupply.restockRefusal(organ.grid()))
                        : said(taker, "restocked", moved);
            }
            case RETURN -> {
                if (organ.grid().stream().allMatch(ItemStack::isEmpty)) yield refuse(taker, PalmSupply.GRID_EMPTY);
                int moved = PalmSupply.giveAll(organ.grid(), store);
                if (moved > 0) organ.touch();
                yield moved == 0 ? refuse(taker, PalmSupply.STORE_FULL) : said(taker, "returned", moved);
            }
            default -> storeResult(taker, store);
        };
    }

    private boolean storeResult(Player taker, IItemHandler store) {
        ItemStack result = results.getItem(0);
        if (result.isEmpty()) return refuse(taker, PalmSupply.NO_RESULT);
        if (!ItemStores.insert(store, result, true).isEmpty()) return refuse(taker, PalmSupply.STORE_FULL);
        Optional<Refusal> room = PalmCrafting.refusal(taker, grid, result, false);
        if (room.isPresent()) return refuse(taker, room.get());
        ItemStack made = craft(taker, stack -> ItemStores.insert(store, stack, false));
        if (made.isEmpty()) return refuse(taker, PalmSupply.STORE_FULL);
        return said(taker, "stored", made.getCount());
    }

    /** What did not go back to the store goes to the player, never to the floor of the store's chunk. */
    private static Consumer<ItemStack> back(Player taker) {
        return stack -> taker.getInventory().placeItemBackInInventory(stack);
    }

    private boolean said(Player taker, String what, int count) {
        refresh();
        taker.displayClientMessage(Component.translatable("infestusfrontier.status.palm." + what, count), true);
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != results && super.canTakeItemForPickAll(stack, slot);
    }

    /**
     * One tray slot. The player takes from it and puts nothing in; a Palm without Rote, or one that could not read its
     * save, gives nothing.
     */
    private static final class TraySlot extends Slot {
        private final Optional<ShapingPalmBlockEntity> palm;
        private final boolean rote;

        TraySlot(Container container, int index, int x, int y, Optional<ShapingPalmBlockEntity> palm, boolean rote) {
            super(container, index, x, y);
            this.palm = palm;
            this.rote = rote;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player taker) {
            return rote && palm.map(ShapingPalmBlockEntity::readable).orElse(true);
        }

        @Override
        public boolean isActive() {
            return rote;
        }
    }

    /** The organ's tray as a container, so a click saves the organ and wakes it to try its next cycle. */
    private final class Tray implements Container {
        private final NonNullList<ItemStack> stacks;

        Tray(NonNullList<ItemStack> stacks) {
            this.stacks = stacks;
        }

        @Override
        public int getContainerSize() {
            return stacks.size();
        }

        @Override
        public boolean isEmpty() {
            return stacks.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return stacks.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = ContainerHelper.removeItem(stacks, slot, amount);
            if (!taken.isEmpty()) setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = ContainerHelper.takeItem(stacks, slot);
            setChanged();
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            stacks.set(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            palm.ifPresent(ShapingPalmBlockEntity::touchTray);
        }

        @Override
        public boolean stillValid(Player taker) {
            return true;
        }

        @Override
        public void clearContent() {
            stacks.replaceAll(stack -> ItemStack.EMPTY);
            setChanged();
        }
    }

    /** One cell of the grid. An organ that could not read its save takes nothing in and gives nothing out. */
    private static final class Cell extends Slot {
        private final Optional<ShapingPalmBlockEntity> palm;

        Cell(Container container, int index, int x, int y, Optional<ShapingPalmBlockEntity> palm) {
            super(container, index, x, y);
            this.palm = palm;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return palm.map(ShapingPalmBlockEntity::readable).orElse(true);
        }

        @Override
        public boolean mayPickup(Player taker) {
            return palm.map(ShapingPalmBlockEntity::readable).orElse(true);
        }
    }

    /**
     * This menu's view of the organ's nine cells as the crafting grid the engine's result slot works on. A change
     * saves the organ, bumps its version for the other viewers and looks this menu's result up again, as a crafting
     * table's own grid does.
     */
    private final class Grid implements CraftingContainer {
        private final NonNullList<ItemStack> cells;

        Grid(NonNullList<ItemStack> cells) {
            this.cells = cells;
        }

        private void changed() {
            palm.ifPresent(ShapingPalmBlockEntity::touch);
            slotsChanged(this);
        }

        @Override
        public int getContainerSize() {
            return cells.size();
        }

        @Override
        public boolean isEmpty() {
            return cells.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= getContainerSize() ? ItemStack.EMPTY : cells.get(slot);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            palm.ifPresent(ShapingPalmBlockEntity::touch);
            return ContainerHelper.takeItem(cells, slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = ContainerHelper.removeItem(cells, slot, amount);
            if (!taken.isEmpty()) changed();
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            cells.set(slot, stack);
            changed();
        }

        @Override
        public void setChanged() {
            palm.ifPresent(ShapingPalmBlockEntity::touch);
        }

        @Override
        public boolean stillValid(Player taker) {
            return true;
        }

        @Override
        public void clearContent() {
            cells.clear();
            changed();
        }

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return cells;
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            for (ItemStack stack : cells) contents.accountSimpleStack(stack);
        }
    }
}
