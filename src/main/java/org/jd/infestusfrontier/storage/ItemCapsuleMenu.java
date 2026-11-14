package org.jd.infestusfrontier.storage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jd.infestusfrontier.core.storage.Ledger;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.organ.api.WorkshopActionEvent;
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
    /** The synced ints after the row: how many of each view cell's kind a Larder's reserves keep. */
    public static final int RESERVED_DATA = 6;

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

    /**
     * What one update reads of the tree: the store, its counts, the kinds with the reserves kept of each shown cell
     * and the root's growth cost, all read once together. The kinds are the store's shared list: copy before editing.
     */
    private record Frame(
            Optional<CapsuleTree> store,
            int slots,
            int used,
            int fed,
            int cost,
            List<ItemStack> kinds,
            int row,
            int[] reserved) {
        static Frame of(Tree tree, int wanted) {
            Optional<CapsuleTree> store = tree.usable();
            List<ItemStack> kinds = store.map(CapsuleTree::kinds).orElse(List.of());
            int row = Ledger.clampRow(wanted, kinds.size());
            int[] reserved = new int[VIEW];
            if (store.isPresent())
                for (int cell = 0; cell < VIEW; cell++)
                    reserved[cell] = store.get().kindReserved(Ledger.entry(row, cell));
            Optional<CapsuleStemBlockEntity> root = tree.root();
            int cost = root.map(stem -> stem.nextCost(stem.getLevel())).orElse(0);
            return new Frame(
                    store,
                    store.map(CapsuleTree::slots).orElse(0),
                    store.map(CapsuleTree::used).orElse(0),
                    root.map(stem -> Math.min(stem.fed(), cost)).orElse(0),
                    cost,
                    kinds,
                    row,
                    reserved);
        }

        ItemStack kind(int cell) {
            int entry = Ledger.entry(row, cell);
            return entry >= 0 && entry < kinds.size() ? kinds.get(entry).copy() : ItemStack.EMPTY;
        }
    }

    /**
     * The first row the view starts at, shared with the synced ints, and the frame of the update in progress. A
     * broadcast reads one frame for every int and cell it sends; anything outside a broadcast, a click above all,
     * reads the tree as it is then.
     */
    private static final class View {
        private final Tree tree;
        int row;
        Frame frame;

        View(Tree tree) {
            this.tree = tree;
        }

        /** The update's frame, or a fresh one when none is in progress. */
        Frame frame() {
            return frame != null ? frame : Frame.of(tree, row);
        }

        void update(Runnable broadcast) {
            if (frame != null || tree.organ().isEmpty()) {
                broadcast.run();
                return;
            }
            frame = Frame.of(tree, row);
            try {
                broadcast.run();
            } finally {
                frame = null;
            }
        }
    }

    private final Tree tree;
    private final View view;
    /** True while a click has put something into the tree that no one has been credited for yet. */
    private boolean stored;
    /** The kind each cell showed the client at the last broadcast; a click on another kind there is refused. */
    private final ItemStack[] shown = new ItemStack[VIEW];

    ItemCapsuleMenu(int id, Inventory inventory, BlockEntity organ, Supplier<Optional<CapsuleTree>> store) {
        this(id, inventory, new View(new Tree(Optional.of(organ), store)), at(organ));
    }

    public ItemCapsuleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new View(new Tree(Optional.empty(), Optional::empty)), at(inventory, extra));
    }

    private ItemCapsuleMenu(int id, Inventory inventory, View view, ContainerLevelAccess access) {
        super(
                StorageModule.ITEM_CAPSULE_MENU.get(),
                id,
                access,
                view.tree
                        .organ()
                        .map(organ -> organ.getBlockState().getBlock())
                        .orElse(StorageModule.ITEM_CAPSULE.get()),
                data(view));
        this.tree = view.tree;
        this.view = view;
        ItemStack[] copy = new ItemStack[VIEW];
        Arrays.fill(copy, ItemStack.EMPTY);
        KindSlot.Source source = new KindSlot.Source() {
            private int entry(int cell) {
                return Ledger.entry(shownRow(), cell);
            }

            @Override
            public ItemStack get(int cell) {
                if (tree.organ().isEmpty()) return copy[cell];
                return view.frame != null
                        ? view.frame.kind(cell)
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
                    if (held.isEmpty()) planted(stack, store.insert(stack.copy(), false));
                    else if (ItemStack.isSameItemSameComponents(held, stack)) {
                        int delta = stack.getCount() - held.getCount();
                        if (delta > 0) {
                            ItemStack added = held.copyWithCount(delta);
                            planted(added, store.insert(added.copy(), false));
                        } else if (delta < 0) store.take(held, -delta);
                    }
                });
            }

            /** Vanilla splits what it puts by {@link #room} first, so nothing is ever left over. */
            private void planted(ItemStack offered, ItemStack rest) {
                if (!rest.isEmpty()) StorageModule.LOGGER.warn("Capsule Stem ledger refused {} it had room for", rest);
                if (rest.getCount() < offered.getCount()) stored = true;
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

    private static ContainerData data(View view) {
        if (view.tree.organ().isEmpty()) return new SimpleContainerData(RESERVED_DATA + VIEW);
        IntSupplier[] reads = new IntSupplier[RESERVED_DATA + VIEW];
        reads[0] = () -> view.frame().slots();
        reads[1] = () -> view.frame().used();
        reads[2] = () -> view.frame().fed();
        reads[3] = () -> view.frame().cost();
        reads[KINDS_DATA] = () -> view.frame().kinds().size();
        reads[ROW_DATA] = () -> view.frame().row();
        for (int cell = 0; cell < VIEW; cell++) {
            int which = cell;
            reads[RESERVED_DATA + cell] = () -> view.frame().reserved()[which];
        }
        return reading(reads);
    }

    /** How many of view cell {@code cell}'s kind the Larders' reserves keep: the tree's now, the client's as sent. */
    public int reserved(int cell) {
        return tree.organ().isEmpty()
                ? synced(RESERVED_DATA + cell)
                : tree.usable()
                        .map(store -> store.kindReserved(Ledger.entry(shownRow(), cell)))
                        .orElse(0);
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
        view.update(() -> {
            super.broadcastChanges();
            remember();
        });
    }

    @Override
    public void broadcastFullState() {
        view.update(() -> {
            super.broadcastFullState();
            remember();
        });
    }

    @Override
    public void sendAllDataToRemote() {
        view.update(super::sendAllDataToRemote);
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
        credit(player);
    }

    /** Tells the world a player stored something in a standing tree, once for the click that did it. */
    private void credit(Player player) {
        if (!stored) return;
        stored = false;
        if (tree.root().isPresent() && player instanceof ServerPlayer server && !(server instanceof FakePlayer))
            NeoForge.EVENT_BUS.post(new WorkshopActionEvent(server, WorkshopActionEvent.Action.TREE_STORED));
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof KindSlot);
    }

    /**
     * A cell gives a stack of its kind at a time, never the part a Larder's reserve keeps; inventory items go where the tree's rules put them. Vanilla repeats
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
            int free = held.getCount() - reserved(index);
            if (free <= 0) return ItemStack.EMPTY; // the reserve is not for the inventory to copy
            ItemStack moving = held.copyWithCount(Math.min(free, held.getMaxStackSize()));
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
        if (rest.getCount() != stack.getCount()) {
            slot.set(rest);
            stored = true;
            credit(player);
        }
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
