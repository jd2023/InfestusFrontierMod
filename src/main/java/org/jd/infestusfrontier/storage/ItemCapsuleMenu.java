package org.jd.infestusfrontier.storage;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/** Sixteen compartments of one shared body. Each page has a fresh menu id and an immutable slot mapping. */
public final class ItemCapsuleMenu extends OrganMenu {
    public static final int PAGE_SIZE = 16;
    public static final int PREVIOUS = 0;
    public static final int NEXT = 1;
    static final int COLUMNS = 8;
    static final int PITCH = 20;
    static final int STRIPS = 2;
    static final int GRID_Y = 36;
    static final int BUTTON_Y = 82;
    private final Page page;
    private final long pageAfter;

    private record Page(Optional<ItemCapsuleBlockEntity> cell, Optional<CapsuleBody> body, int number) {
        static Page server(ItemCapsuleBlockEntity cell, int number) {
            return new Page(Optional.of(cell), Capsules.of(cell), number);
        }

        Optional<CapsuleBody> usable() {
            return body.filter(CapsuleBody::isUsable);
        }
    }

    ItemCapsuleMenu(int id, Inventory inventory, ItemCapsuleBlockEntity cell) {
        this(id, inventory, Page.server(cell, 0), at(cell));
    }

    public ItemCapsuleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, at(inventory, extra), extra.readableBytes() > 0 ? extra.readVarInt() : 0);
    }

    private ItemCapsuleMenu(int id, Inventory inventory, ContainerLevelAccess access, int number) {
        this(id, inventory, new Page(Optional.empty(), Optional.empty(), number), access);
    }

    private ItemCapsuleMenu(int id, Inventory inventory, Page page, ContainerLevelAccess access) {
        super(
                StorageModule.ITEM_CAPSULE_MENU.get(),
                id,
                access,
                StorageModule.ITEM_CAPSULE.get(),
                page.cell().isEmpty()
                        ? new SimpleContainerData(4)
                        : reading(
                                () -> page.cell().orElseThrow().body().sizeX(),
                                () -> page.cell().orElseThrow().body().sizeY(),
                                () -> page.cell().orElseThrow().body().sizeZ(),
                                () -> page.usable().map(CapsuleBody::used).orElse(0)));
        this.page = page;
        pageAfter = inventory.player.level().getGameTime() + 10;
        ItemStack[] copy = new ItemStack[PAGE_SIZE];
        Arrays.fill(copy, ItemStack.EMPTY);
        CompartmentSlot.Source source = new CompartmentSlot.Source() {
            private int bodySlot(int slot) {
                return page.number() * PAGE_SIZE + slot;
            }

            @Override
            public ItemStack get(int slot) {
                return page.cell().isEmpty()
                        ? copy[slot]
                        : page.usable().map(body -> body.stack(bodySlot(slot))).orElse(ItemStack.EMPTY);
            }

            @Override
            public void set(int slot, ItemStack stack) {
                if (page.cell().isEmpty()) copy[slot] = stack.copy();
                else page.usable().ifPresent(body -> body.put(bodySlot(slot), stack));
            }

            @Override
            public boolean accepts(int slot, ItemStack stack) {
                return page.cell().isEmpty()
                        || page.usable()
                                .map(body -> body.accepts(bodySlot(slot), stack))
                                .orElse(false);
            }

            @Override
            public boolean active(int slot) {
                return bodySlot(slot) < compartments();
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

    /** The server rechecks the standing body and refuses page changes while anything is on the cursor. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer server)
                || player.containerMenu != this
                || !stillValid(player)
                || !getCarried().isEmpty()
                || player.level().getGameTime() < pageAfter) return false;
        int next = page.number() + (button == NEXT ? 1 : button == PREVIOUS ? -1 : 0);
        if (next == page.number() || next < 0 || next >= pages()) return false;
        ItemCapsuleBlockEntity cell = page.cell().orElseThrow();
        server.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, user) ->
                                new ItemCapsuleMenu(id, inventory, new Page(page.cell(), page.body(), next), at(cell)),
                        cell.getDisplayName()),
                extra -> {
                    extra.writeBlockPos(cell.getBlockPos());
                    extra.writeVarInt(next);
                });
        return true;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (page.cell().isPresent() && (player.containerMenu != this || !stillValid(player))) return;
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
        Optional<CapsuleBody> body = page.usable();
        if (body.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack rest = body.get().insert(stack, false);
        if (rest.getCount() != stack.getCount()) slot.set(rest);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player)
                && (page.cell().isEmpty()
                        || page.cell().filter(cell -> !cell.isRemoved()).isPresent()
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

    int sizeX() {
        return synced(0);
    }

    int sizeY() {
        return synced(1);
    }

    int sizeZ() {
        return synced(2);
    }

    int compartments() {
        return sizeX() * sizeY() * sizeZ() * ItemCapsuleBlockEntity.COMPARTMENTS;
    }

    int used() {
        return synced(3);
    }

    int page() {
        return page.number();
    }

    int pages() {
        return Math.max(1, (compartments() + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}
