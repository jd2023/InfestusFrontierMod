package org.jd.infestusfrontier.storage;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The capsule screen's server half: the cell's own four compartments and the body's size and use as synced ints.
 * Shift-click from the inventory puts the stack into the whole body by its rules; shift-click on a compartment moves a
 * stack at a time into the inventory.
 */
public final class ItemCapsuleMenu extends OrganMenu {
    private static final int SIZE_X = 0;
    private static final int SIZE_Y = 1;
    private static final int SIZE_Z = 2;
    private static final int USED = 3;
    private static final int COMPARTMENTS = ItemCapsuleBlockEntity.COMPARTMENTS;

    private final Optional<ItemCapsuleBlockEntity> cell;

    /** Server side. */
    ItemCapsuleMenu(int id, Inventory inventory, ItemCapsuleBlockEntity cell) {
        this(
                id,
                inventory,
                new CompartmentSlot.Source() {
                    @Override
                    public ItemStack get(int compartment) {
                        return cell.held(compartment);
                    }

                    @Override
                    public void set(int compartment, ItemStack stack) {
                        cell.put(compartment, stack);
                    }

                    @Override
                    public boolean accepts(int compartment, ItemStack stack) {
                        return cell.accepts(compartment, stack);
                    }
                },
                reading(
                        () -> cell.body().sizeX(),
                        () -> cell.body().sizeY(),
                        () -> cell.body().sizeZ(),
                        () -> Capsules.of(cell).map(CapsuleBody::used).orElse(0)),
                at(cell),
                Optional.of(cell));
    }

    /** Client side. The position arrives as extra data; the compartments arrive as slot updates. */
    public ItemCapsuleMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, copy(), new SimpleContainerData(4), at(inventory, extra), Optional.empty());
    }

    private ItemCapsuleMenu(
            int id,
            Inventory inventory,
            CompartmentSlot.Source source,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<ItemCapsuleBlockEntity> cell) {
        super(StorageModule.ITEM_CAPSULE_MENU.get(), id, access, StorageModule.ITEM_CAPSULE.get(), data);
        this.cell = cell;
        SimpleContainer shared = new SimpleContainer(COMPARTMENTS);
        for (int compartment = 0; compartment < COMPARTMENTS; compartment++)
            addSlot(new CompartmentSlot(
                    source,
                    shared,
                    compartment,
                    Sheet.SLOT_X + compartment * (Sheet.SLOT.width() + Sheet.GAP),
                    Sheet.SLOT_Y));
        addPlayer(inventory, 0);
    }

    /** The client's copy of the compartments; the server decides what fits, this only shows it. */
    private static CompartmentSlot.Source copy() {
        ItemStack[] held = new ItemStack[COMPARTMENTS];
        Arrays.fill(held, ItemStack.EMPTY);
        return new CompartmentSlot.Source() {
            @Override
            public ItemStack get(int compartment) {
                return held[compartment];
            }

            @Override
            public void set(int compartment, ItemStack stack) {
                held[compartment] = stack;
            }

            @Override
            public boolean accepts(int compartment, ItemStack stack) {
                return held[compartment].isEmpty() || ItemStack.isSameItemSameComponents(held[compartment], stack);
            }
        };
    }

    /** Drags and hotbar swaps would move more than a stack at once, so compartments refuse them. */
    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (type == ClickType.SWAP && slot >= 0 && slot < COMPARTMENTS) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof CompartmentSlot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (index < COMPARTMENTS) {
            ItemStack held = slot.getItem();
            ItemStack moving = held.copyWithCount(Math.min(held.getCount(), held.getMaxStackSize()));
            int before = moving.getCount();
            moveItemStackTo(moving, COMPARTMENTS, slots.size(), true);
            int moved = before - moving.getCount();
            if (moved == 0) return ItemStack.EMPTY;
            slot.remove(moved);
            return held.copyWithCount(moved);
        }
        Optional<CapsuleBody> body = cell.flatMap(Capsules::of);
        if (body.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack rest = body.get().insert(stack, false);
        if (rest.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.set(rest);
        return ItemStack.EMPTY;
    }

    /** The cell itself must still stand there: a cell broken and placed again is another cell. */
    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player)
                && cell.map(standing -> !standing.isRemoved()).orElse(true);
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
        return synced(SIZE_X);
    }

    int sizeY() {
        return synced(SIZE_Y);
    }

    int sizeZ() {
        return synced(SIZE_Z);
    }

    int compartments() {
        return sizeX() * sizeY() * sizeZ() * COMPARTMENTS;
    }

    int used() {
        return synced(USED);
    }
}
