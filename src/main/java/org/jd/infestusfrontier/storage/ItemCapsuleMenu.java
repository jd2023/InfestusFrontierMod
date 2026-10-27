package org.jd.infestusfrontier.storage;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Sixteen compartments of one tree's store, opened from any of its stem cells, with the root's bone meal toward its
 * next growth. Each page has a fresh menu id and an immutable slot mapping. A pod opens {@link ItemCapsulePodMenu}.
 */
public final class ItemCapsuleMenu extends OrganMenu {
    public static final int PAGE_SIZE = 16;
    public static final int PREVIOUS = 0;
    public static final int NEXT = 1;
    static final int COLUMNS = 8;
    static final int PITCH = 20;
    static final int STRIPS = 3;
    static final int GRID_Y = 36;
    static final int BUTTON_Y = 82;
    static final int GROWTH_Y = BUTTON_Y + 22;
    private final Page page;
    private final long pageAfter;

    /** The organ the page was opened from and the way to its store; neither on the client. */
    private record Page(Optional<BlockEntity> organ, Supplier<Optional<CapsuleTree>> store, int number) {
        Optional<CapsuleTree> usable() {
            return store.get().filter(CapsuleTree::isUsable);
        }
    }

    ItemCapsuleMenu(int id, Inventory inventory, BlockEntity organ, Supplier<Optional<CapsuleTree>> store) {
        this(id, inventory, new Page(Optional.of(organ), store, 0), at(organ));
    }

    public ItemCapsuleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, at(inventory, extra), extra.readableBytes() > 0 ? extra.readVarInt() : 0);
    }

    private ItemCapsuleMenu(int id, Inventory inventory, ContainerLevelAccess access, int number) {
        this(id, inventory, new Page(Optional.empty(), Optional::empty, number), access);
    }

    private ItemCapsuleMenu(int id, Inventory inventory, Page page, ContainerLevelAccess access) {
        super(
                StorageModule.ITEM_CAPSULE_MENU.get(),
                id,
                access,
                page.organ().map(organ -> organ.getBlockState().getBlock()).orElse(StorageModule.ITEM_CAPSULE.get()),
                page.organ().isEmpty()
                        ? new SimpleContainerData(4)
                        : reading(
                                () -> page.usable().map(CapsuleTree::slots).orElse(0),
                                () -> page.usable().map(CapsuleTree::used).orElse(0),
                                () -> rootOf(page)
                                        .map(CapsuleStemBlockEntity::fed)
                                        .orElse(0),
                                () -> rootOf(page)
                                        .map(root -> root.nextCost(root.getLevel()))
                                        .orElse(0)));
        this.page = page;
        pageAfter = inventory.player.level().getGameTime() + 10;
        ItemStack[] copy = new ItemStack[PAGE_SIZE];
        Arrays.fill(copy, ItemStack.EMPTY);
        CompartmentSlot.Source source = new CompartmentSlot.Source() {
            private int treeSlot(int slot) {
                return page.number() * PAGE_SIZE + slot;
            }

            @Override
            public ItemStack get(int slot) {
                return page.organ().isEmpty()
                        ? copy[slot]
                        : page.usable().map(tree -> tree.stack(treeSlot(slot))).orElse(ItemStack.EMPTY);
            }

            @Override
            public void set(int slot, ItemStack stack) {
                if (page.organ().isEmpty()) copy[slot] = stack.copy();
                else page.usable().ifPresent(tree -> tree.put(treeSlot(slot), stack));
            }

            @Override
            public boolean accepts(int slot, ItemStack stack) {
                return page.organ().isEmpty()
                        ? copy[slot].isEmpty() || ItemStack.isSameItemSameComponents(copy[slot], stack)
                        : page.usable()
                                .map(tree -> tree.accepts(treeSlot(slot), stack))
                                .orElse(false);
            }

            @Override
            public boolean active(int slot) {
                return treeSlot(slot) < compartments();
            }

            @Override
            public int limit(int slot, int maxStackSize) {
                return page.organ().isEmpty()
                        ? CompartmentSlot.Source.super.limit(slot, maxStackSize)
                        : page.usable()
                                .map(tree -> tree.limit(treeSlot(slot), maxStackSize))
                                .orElse(0);
            }
        };
        SimpleContainer shared = new SimpleContainer(PAGE_SIZE);
        for (int slot = 0; slot < PAGE_SIZE; slot++)
            addSlot(new CompartmentSlot(
                    source,
                    shared,
                    slot,
                    Sheet.LEFT_X + 1 + slot % COLUMNS * PITCH,
                    GRID_Y + 1 + slot / COLUMNS * PITCH));
        addPlayer(inventory, STRIPS);
    }

    /** The root of the tree the page was opened from, while the organ is a standing stem cell. */
    private static Optional<CapsuleStemBlockEntity> rootOf(Page page) {
        return page.organ()
                .filter(CapsuleStemBlockEntity.class::isInstance)
                .map(CapsuleStemBlockEntity.class::cast)
                .filter(cell -> cell.getLevel() != null)
                .flatMap(CapsuleStemBlockEntity::root);
    }

    /** The server rechecks the standing tree and refuses page changes while anything is on the cursor. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer server)
                || player.containerMenu != this
                || !stillValid(player)
                || !getCarried().isEmpty()
                || player.level().getGameTime() < pageAfter) return false;
        int next = page.number() + (button == NEXT ? 1 : button == PREVIOUS ? -1 : 0);
        if (next == page.number() || next < 0 || next >= pages()) return false;
        BlockEntity organ = page.organ().orElseThrow();
        MenuProvider provider = (MenuProvider) organ;
        server.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, user) -> new ItemCapsuleMenu(
                                id, inventory, new Page(page.organ(), page.store(), next), at(organ)),
                        provider.getDisplayName()),
                extra -> {
                    extra.writeBlockPos(organ.getBlockPos());
                    extra.writeVarInt(next);
                });
        return true;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (page.organ().isPresent() && (player.containerMenu != this || !stillValid(player))) return;
        if (slot >= 0 && slot < PAGE_SIZE && (!getSlot(slot).isActive() || type == ClickType.SWAP)) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof CompartmentSlot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || player.containerMenu != this || !stillValid(player))
            return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem()) return ItemStack.EMPTY;
        if (index < PAGE_SIZE) {
            ItemStack held = slot.getItem();
            ItemStack moving = held.copyWithCount(Math.min(held.getCount(), held.getMaxStackSize()));
            int before = moving.getCount();
            moveItemStackTo(moving, PAGE_SIZE, slots.size(), true);
            int moved = before - moving.getCount();
            if (moved == 0) return ItemStack.EMPTY;
            slot.remove(moved);
            return held.copyWithCount(moved);
        }
        Optional<CapsuleTree> tree = page.usable();
        if (tree.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack rest = tree.get().insert(stack, false);
        if (rest.getCount() != stack.getCount()) slot.set(rest);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player)
                && (page.organ().isEmpty()
                        || page.organ().filter(organ -> !organ.isRemoved()).isPresent()
                                && page.usable().isPresent());
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

    int used() {
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

    int page() {
        return page.number();
    }

    int pages() {
        return Math.max(1, (compartments() + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}
