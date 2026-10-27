package org.jd.infestusfrontier.storage;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import org.jd.infestusfrontier.core.storage.Compartments;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * One pod on its own: its compartment, which a click or a shift-click fills and empties a stack at a time, and a
 * treatment well that grows the pod a grade for each Skeletal Graft put in it. The trunk opens the whole tree
 * ({@link ItemCapsuleMenu}); a pod opens only itself, so quick access and treatments never page through the stem.
 */
public final class ItemCapsulePodMenu extends OrganMenu {
    /** The treatment well comes first, so a shift-clicked graft grows the pod instead of filling it. */
    public static final int TREATMENT = 0;

    public static final int COMPARTMENT = 1;
    private static final int ORGAN_SLOTS = 2;
    private static final int GRAFT_PARTICLES = 8;
    /** The compartment well leads the parts row; the treatment well stands a label's width to its right. */
    static final int COMPARTMENT_X = Sheet.FIRST_PART_X;

    static final int SPACER = 20;
    static final int TREATMENT_X = COMPARTMENT_X + Sheet.SLOT.width() + Sheet.GAP + SPACER + Sheet.GAP;
    /** One body strip under the parts row, for the grade and fill lines. */
    static final int STRIPS = 1;

    private final Optional<ItemCapsuleBlockEntity> pod;

    ItemCapsulePodMenu(int id, Inventory inventory, ItemCapsuleBlockEntity pod) {
        this(id, inventory, Optional.of(pod), at(pod));
    }

    public ItemCapsulePodMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, Optional.empty(), at(inventory, extra));
    }

    private ItemCapsulePodMenu(
            int id, Inventory inventory, Optional<ItemCapsuleBlockEntity> pod, ContainerLevelAccess access) {
        super(
                StorageModule.ITEM_CAPSULE_POD_MENU.get(),
                id,
                access,
                StorageModule.ITEM_CAPSULE.get(),
                pod.isEmpty()
                        ? new SimpleContainerData(2)
                        : reading(
                                () -> pod.get().grade(),
                                () -> pod.get().isReadable() ? pod.get().count(0) : 0));
        this.pod = pod;
        ItemStack[] copy = {ItemStack.EMPTY};
        CompartmentSlot.Source source = new CompartmentSlot.Source() {
            @Override
            public ItemStack get(int compartment) {
                return pod.map(organ -> organ.held(compartment)).orElse(copy[0]);
            }

            @Override
            public void set(int compartment, ItemStack stack) {
                if (pod.isPresent()) pod.get().put(compartment, stack);
                else copy[0] = stack.copy();
            }

            @Override
            public boolean accepts(int compartment, ItemStack stack) {
                return pod.map(organ -> organ.accepts(compartment, stack))
                        .orElse(copy[0].isEmpty() || ItemStack.isSameItemSameComponents(copy[0], stack));
            }

            @Override
            public int limit(int compartment, int maxStackSize) {
                return Compartments.limit(CapsuleStock.stacks(grade()), maxStackSize);
            }
        };
        addSlot(new TreatmentSlot(TREATMENT_X + 1, Sheet.SLOT_Y));
        addSlot(new CompartmentSlot(source, new SimpleContainer(1), 0, COMPARTMENT_X + 1, Sheet.SLOT_Y));
        addPlayer(inventory, STRIPS);
    }

    /** The grade the server last sent; on the server, the pod's own. */
    int grade() {
        return pod.map(ItemCapsuleBlockEntity::grade).orElse(synced(0));
    }

    /** What the compartment holds, as the server last sent it. */
    int count() {
        return synced(1);
    }

    /** The most of what the compartment holds that fits at this grade. */
    int limit() {
        ItemStack held = getSlot(COMPARTMENT).getItem();
        return Compartments.limit(CapsuleStock.stacks(grade()), held.isEmpty() ? 64 : held.getMaxStackSize());
    }

    /**
     * A well that takes a Skeletal Graft while the pod can still grow and spends one a grade the moment it is put
     * there; what it cannot spend stays in the well for the player to take back. The client only predicts.
     */
    private final class TreatmentSlot extends Slot {
        TreatmentSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(EcologyModule.SKELETAL_GRAFT.get()) && grade() < CapsuleStock.LAST_GRADE;
        }

        @Override
        public void set(ItemStack stack) {
            super.set(pod.isPresent() ? treat(stack) : stack);
        }

        @Override
        public void setByPlayer(ItemStack stack, ItemStack previous) {
            super.setByPlayer(pod.isPresent() ? treat(stack) : stack, previous);
        }

        private ItemStack treat(ItemStack stack) {
            ItemCapsuleBlockEntity organ = pod.orElseThrow();
            if (!stack.is(EcologyModule.SKELETAL_GRAFT.get()) || organ.getLevel() == null) return stack;
            ItemStack rest = stack.copy();
            while (!rest.isEmpty() && organ.grow()) {
                rest.shrink(1);
                organ.getLevel()
                        .levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, organ.getBlockPos(), GRAFT_PARTICLES);
            }
            return rest;
        }
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (pod.isPresent() && (player.containerMenu != this || !stillValid(player))) return;
        if (slot == COMPARTMENT && type == ClickType.SWAP) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof CompartmentSlot);
    }

    /** The compartment gives a stack at a time; inventory items go to the treatment well, then the compartment. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || player.containerMenu != this || !stillValid(player))
            return ItemStack.EMPTY;
        if (index != COMPARTMENT) return super.quickMoveStack(player, index);
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moving = held.copyWithCount(Math.min(held.getCount(), held.getMaxStackSize()));
        int before = moving.getCount();
        moveItemStackTo(moving, ORGAN_SLOTS, slots.size(), true);
        int moved = before - moving.getCount();
        if (moved == 0) return ItemStack.EMPTY;
        slot.remove(moved);
        return held.copyWithCount(moved);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player)
                && pod.map(organ -> !organ.isRemoved() && organ.isReadable()).orElse(true);
    }

    /** What is left in the treatment well goes back to the player. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, getSlot(TREATMENT).container);
    }

    @Override
    public int stored() {
        return 0;
    }

    @Override
    public int capacity() {
        return 1;
    }
}
