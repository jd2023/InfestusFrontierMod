package org.jd.infestusfrontier.storage;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jd.infestusfrontier.core.storage.Ledger;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/**
 * The ledger of one tree's store, opened from any of its stem cells: every kind the tree holds, once, with its whole
 * count over its pods, sorted by name, in {@value Ledger#ROWS} rows of {@value Ledger#COLUMNS} that scroll, with one
 * open entry after the last kind for a new kind; and the root's bone meal toward its next growth. A pod opens
 * {@link ItemCapsulePodMenu}.
 *
 * <p>A scroll is a button click carrying the row asked for; the server keeps the row and sends it back with the
 * entries. The client sends a click on a cell only once it shows the row it asked for, and the server refuses a click
 * on a cell that changed kind since its last broadcast; what changed in the one round trip between a broadcast and a
 * click, a hopper or another viewer, is the window vanilla containers have too.
 */
public final class ItemCapsuleMenu extends OrganMenu {
    public static final int VIEW = Ledger.VIEW;
    /** The synced ints after the four every tree screen shows: how many kinds the tree holds, the row it shows. */
    public static final int KINDS_DATA = 4;

    public static final int ROW_DATA = 5;
    static final int GRID_Y = 36;
    /** The view: three rows of eight wells at the inventory's pitch, from the panel's left margin. */
    static final SlotGrid GRID = new SlotGrid(
            new Bounds(Sheet.LEFT_X, GRID_Y, Sheet.SLOT.width(), Sheet.SLOT.height()), Ledger.COLUMNS, Ledger.ROWS, 18);
    /** The pods line and the growth line under the view; three body strips make room for them. */
    static final int BODY_Y = 96;

    static final int GROWTH_Y = 108;
    static final int STRIPS = 3;

    /** The organ the ledger was opened from and the way to its store; neither on the client. */
    private record Tree(Optional<BlockEntity> organ, Supplier<Optional<CapsuleTree>> store) {
        Optional<CapsuleTree> usable() {
            return store.get().filter(CapsuleTree::isUsable);
        }

        int kinds() {
            return usable().map(tree -> tree.kinds().size()).orElse(0);
        }

        /** The root of the tree, while the organ is a standing stem cell. */
        Optional<CapsuleStemBlockEntity> root() {
            return organ.filter(CapsuleStemBlockEntity.class::isInstance)
                    .map(CapsuleStemBlockEntity.class::cast)
                    .filter(cell -> cell.getLevel() != null)
                    .flatMap(CapsuleStemBlockEntity::root);
        }
    }

    /** The first row the view starts at, shared with the synced ints. */
    private static final class View {
        int row;
    }

    private final Tree tree;
    private final View view;
    /** The kind each cell showed the client at the last broadcast; a click on another kind there is refused. */
    private final ItemStack[] shown = new ItemStack[VIEW];

    ItemCapsuleMenu(int id, Inventory inventory, BlockEntity organ, Supplier<Optional<CapsuleTree>> store) {
        this(id, inventory, new Tree(Optional.of(organ), store), at(organ), new View());
    }

    public ItemCapsuleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new Tree(Optional.empty(), Optional::empty), at(inventory, extra), new View());
    }

    private ItemCapsuleMenu(int id, Inventory inventory, Tree tree, ContainerLevelAccess access, View view) {
        super(
                StorageModule.ITEM_CAPSULE_MENU.get(),
                id,
                access,
                tree.organ().map(organ -> organ.getBlockState().getBlock()).orElse(StorageModule.ITEM_CAPSULE.get()),
                data(tree, view));
        this.tree = tree;
        this.view = view;
        ItemStack[] copy = new ItemStack[VIEW];
        Arrays.fill(copy, ItemStack.EMPTY);
        KindSlot.Source source = new KindSlot.Source() {
            private int entry(int cell) {
                return Ledger.entry(shownRow(), cell);
            }

            @Override
            public ItemStack get(int cell) {
                return tree.organ().isEmpty()
                        ? copy[cell]
                        : tree.usable().map(store -> store.kind(entry(cell))).orElse(ItemStack.EMPTY);
            }

            @Override
            public void set(int cell, ItemStack stack) {
                if (tree.organ().isEmpty()) {
                    copy[cell] = stack.copy();
                    return;
                }
                tree.usable().ifPresent(store -> {
                    ItemStack held = store.kind(entry(cell));
                    if (stack.isEmpty()) return; // a kind leaves only through take; an empty cell is empty already
                    if (held.isEmpty()) planted(store.insert(stack.copy(), false));
                    else if (ItemStack.isSameItemSameComponents(held, stack)) {
                        int delta = stack.getCount() - held.getCount();
                        if (delta > 0) planted(store.insert(held.copyWithCount(delta), false));
                        else if (delta < 0) store.take(held, -delta);
                    }
                });
            }

            /** Vanilla splits what it puts by {@link #room} first, so nothing is ever left over. */
            private void planted(ItemStack rest) {
                if (!rest.isEmpty()) StorageModule.LOGGER.warn("Capsule Stem ledger refused {} it had room for", rest);
            }

            @Override
            public ItemStack take(int cell, int amount) {
                if (tree.organ().isEmpty()) {
                    ItemStack held = copy[cell];
                    int taken = Math.min(amount, held.getCount());
                    copy[cell] = held.copyWithCount(held.getCount() - taken);
                    return held.copyWithCount(taken);
                }
                return tree.usable()
                        .map(store -> store.take(store.kind(entry(cell)), amount))
                        .orElse(ItemStack.EMPTY);
            }

            /** The client predicts that the tree takes it all; the server answers with what fitted. */
            @Override
            public int room(int cell, ItemStack stack) {
                return tree.organ().isEmpty()
                        ? Integer.MAX_VALUE / 2
                        : tree.usable().map(store -> store.room(stack)).orElse(0);
            }

            @Override
            public boolean active(int cell) {
                return tree.organ().isEmpty() || tree.usable().isPresent();
            }
        };
        SimpleContainer shared = new SimpleContainer(VIEW);
        for (int cell = 0; cell < VIEW; cell++) {
            Bounds well = GRID.cell(cell);
            addSlot(new KindSlot(source, shared, cell, well.x() + 1, well.y() + 1));
        }
        addPlayer(inventory, STRIPS);
        Arrays.fill(shown, ItemStack.EMPTY);
        remember();
    }

    private static ContainerData data(Tree tree, View view) {
        if (tree.organ().isEmpty()) return new SimpleContainerData(ROW_DATA + 1);
        return reading(
                () -> tree.usable().map(CapsuleTree::slots).orElse(0),
                () -> tree.usable().map(CapsuleTree::used).orElse(0),
                () -> tree.root()
                        .map(root -> Math.min(root.fed(), root.nextCost(root.getLevel())))
                        .orElse(0),
                () -> tree.root().map(root -> root.nextCost(root.getLevel())).orElse(0),
                tree::kinds,
                () -> Ledger.clampRow(view.row, tree.kinds()));
    }

    /** Keeps the kind each cell shows after the client was sent it. */
    private void remember() {
        for (int cell = 0; cell < VIEW; cell++) {
            ItemStack now = slots.get(cell).getItem();
            if (!ItemStack.isSameItemSameComponents(shown[cell], now)) shown[cell] = now.copyWithCount(1);
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        remember();
    }

    @Override
    public void broadcastFullState() {
        super.broadcastFullState();
        remember();
    }

    /** How many kinds the tree holds: its own on the server, as last sent on the client. */
    public int kinds() {
        return tree.organ().isEmpty() ? synced(KINDS_DATA) : tree.kinds();
    }

    /** The first row the view shows, kept to the kinds there are: on the client, the row asked for. */
    public int shownRow() {
        return Ledger.clampRow(view.row, kinds());
    }

    public int lastRow() {
        return Ledger.lastRow(kinds());
    }

    /** Client: asks for a view from {@code row}; true when that changes the row. The screen sends it as a button. */
    public boolean scrollTo(int row) {
        int next = Ledger.clampRow(row, kinds());
        if (next == shownRow()) return false;
        view.row = next;
        return true;
    }

    /** Client: true until the server shows the row asked for; clicks on entries wait for it. */
    public boolean pending() {
        return tree.organ().isEmpty() && shownRow() != synced(ROW_DATA);
    }

    /** The server moves the view to row {@code button}, kept to the kinds it has; true when the view moved. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer) || player.containerMenu != this || !stillValid(player)) return false;
        int next = Ledger.clampRow(button, kinds());
        if (next == shownRow()) return false;
        view.row = next;
        return true;
    }

    /**
     * A click on a cell waits on the client until the server shows the row asked for (the screen holds the packet
     * too), and is refused on the server while the cell shows another kind than its last broadcast sent; hotbar swaps
     * never touch a cell.
     */
    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (tree.organ().isPresent() && (player.containerMenu != this || !stillValid(player))) return;
        if (slot >= 0 && slot < VIEW) {
            Slot cell = getSlot(slot);
            if (type == ClickType.SWAP || !cell.isActive()) return;
            if (tree.organ().isEmpty() ? pending() : !ItemStack.isSameItemSameComponents(shown[slot], cell.getItem()))
                return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof KindSlot);
    }

    /**
     * A cell gives a stack of its kind at a time; inventory items go where the tree's rules put them. Vanilla repeats
     * a shift-click while the cell shows the same item, so a cell whose kind is gone, and now shows the next kind of
     * that item, answers empty: a kind is never taken for another.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || player.containerMenu != this || !stillValid(player))
            return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem()) return ItemStack.EMPTY;
        if (index < VIEW) {
            ItemStack held = slot.getItem();
            ItemStack moving = held.copyWithCount(Math.min(held.getCount(), held.getMaxStackSize()));
            int before = moving.getCount();
            moveItemStackTo(moving, VIEW, slots.size(), true);
            int moved = before - moving.getCount();
            if (moved == 0) return ItemStack.EMPTY;
            slot.remove(moved);
            return ItemStack.isSameItemSameComponents(slot.getItem(), held)
                    ? held.copyWithCount(moved)
                    : ItemStack.EMPTY;
        }
        Optional<CapsuleTree> store = tree.usable();
        if (store.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack rest = store.get().insert(stack, false);
        if (rest.getCount() != stack.getCount()) slot.set(rest);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player)
                && (tree.organ().isEmpty()
                        || tree.organ().filter(organ -> !organ.isRemoved()).isPresent()
                                && tree.usable().isPresent());
    }

    @Override
    public int stored() {
        return 0;
    }

    @Override
    public int capacity() {
        return 1;
    }

    /** Compartments of the store: one a pod. */
    int compartments() {
        return synced(0);
    }

    public int used() {
        return synced(1);
    }

    /** Bone meal the root holds toward its next growth. */
    int fed() {
        return synced(2);
    }

    /** What the next growth costs; 0 when nothing is missing and nothing more fits. */
    int cost() {
        return synced(3);
    }
}
