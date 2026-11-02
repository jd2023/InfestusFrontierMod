package org.jd.infestusfrontier.storage;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import org.jd.infestusfrontier.core.storage.Compartments;
import org.jd.infestusfrontier.core.storage.PodKind;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * One pod on its own: its compartments, a bulk pod's one or a Mixed Craw's four, which a click or a shift-click fills
 * and empties a stack at a time, and a treatment well. A Skeletal Graft put in the well grows the pod a grade each; an
 * Elastic Gel put in a bulk pod's well turns the empty pod into a Mixed Craw, and a pod that holds anything refuses it
 * and says so. The trunk opens the whole tree ({@link ItemCapsuleMenu}); a pod opens only itself, so quick access and
 * treatments never page through the stem.
 */
public final class ItemCapsulePodMenu extends OrganMenu {
    /** The treatment well comes first, so a shift-clicked graft grows the pod instead of filling it. */
    public static final int TREATMENT = 0;

    /** The first compartment's slot; the rest follow it. */
    public static final int COMPARTMENT = 1;

    private static final int GRAFT_PARTICLES = 8;
    /** The player's inventory rows above the hotbar. */
    private static final int INVENTORY_SLOTS = 27;
    /** The compartment wells lead the parts row; the treatment well stands to their right. */
    static final int COMPARTMENT_X = Sheet.FIRST_PART_X;

    /** A bulk pod's lone well leaves room for its label before the treatment well; a Craw's four need the row. */
    private static final int BULK_SPACER = 20;

    private static final int CRAW_SPACER = 6;
    /** One body strip under the parts row, for the grade and fill lines. */
    static final int STRIPS = 1;

    private final Optional<ItemCapsuleBlockEntity> pod;
    private final PodKind kind;
    private Optional<String> refusal = Optional.empty();

    /** The gap on the parts row between {@code compartments} wells and the treatment well. */
    static int spacer(int compartments) {
        return compartments == 1 ? BULK_SPACER : CRAW_SPACER;
    }

    /** The x of the treatment well beside {@code compartments} wells. */
    static int treatmentX(int compartments) {
        return wellX(compartments) + spacer(compartments) + Sheet.GAP;
    }

    /** The x of compartment well {@code compartment}; {@code compartment} may be the count, for the end of the row. */
    static int wellX(int compartment) {
        return COMPARTMENT_X + compartment * (Sheet.SLOT.width() + Sheet.GAP);
    }

    ItemCapsulePodMenu(int id, Inventory inventory, ItemCapsuleBlockEntity pod) {
        this(id, inventory, Optional.of(pod), at(pod), pod.kind());
    }

    public ItemCapsulePodMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, Optional.empty(), at(inventory, extra));
    }

    private ItemCapsulePodMenu(
            int id, Inventory inventory, Optional<ItemCapsuleBlockEntity> pod, ContainerLevelAccess access) {
        this(id, inventory, pod, access, kindAt(access));
    }

    /** The kind of pod the client's block at the menu's position is. */
    private static PodKind kindAt(ContainerLevelAccess access) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof ItemCapsuleBlock block
                        ? block.kind()
                        : PodKind.BULK,
                PodKind.BULK);
    }

    private ItemCapsulePodMenu(
            int id,
            Inventory inventory,
            Optional<ItemCapsuleBlockEntity> pod,
            ContainerLevelAccess access,
            PodKind kind) {
        super(
                StorageModule.ITEM_CAPSULE_POD_MENU.get(),
                id,
                access,
                kind == PodKind.CRAW ? StorageModule.MIXED_CRAW.get() : StorageModule.ITEM_CAPSULE.get(),
                pod.isEmpty() ? new SimpleContainerData(1 + kind.compartments()) : readingOf(pod.get()));
        this.pod = pod;
        this.kind = kind;
        ItemStack[] copy = new ItemStack[kind.compartments()];
        Arrays.fill(copy, ItemStack.EMPTY);
        CompartmentSlot.Source source = new CompartmentSlot.Source() {
            @Override
            public ItemStack get(int compartment) {
                return pod.map(organ -> organ.held(compartment)).orElse(copy[compartment]);
            }

            @Override
            public void set(int compartment, ItemStack stack) {
                if (pod.isPresent()) pod.get().put(compartment, stack);
                else copy[compartment] = stack.copy();
            }

            @Override
            public boolean accepts(int compartment, ItemStack stack) {
                return pod.map(organ -> organ.accepts(compartment, stack))
                        .orElse(copy[compartment].isEmpty()
                                || ItemStack.isSameItemSameComponents(copy[compartment], stack));
            }

            @Override
            public int limit(int compartment, int maxStackSize) {
                return Compartments.limit(kind.stacks(grade()), maxStackSize);
            }
        };
        addSlot(new TreatmentSlot(treatmentX(kind.compartments()) + 1, Sheet.SLOT_Y));
        SimpleContainer shared = new SimpleContainer(kind.compartments());
        for (int compartment = 0; compartment < kind.compartments(); compartment++)
            addSlot(new CompartmentSlot(source, shared, compartment, wellX(compartment) + 1, Sheet.SLOT_Y));
        addPlayer(inventory, STRIPS);
    }

    /** The grade, then what each compartment holds, read live on the server. */
    private static ContainerData readingOf(ItemCapsuleBlockEntity pod) {
        IntSupplier[] reads = new IntSupplier[1 + pod.compartments()];
        reads[0] = pod::grade;
        for (int compartment = 0; compartment < pod.compartments(); compartment++) {
            int which = compartment;
            reads[1 + compartment] = () -> pod.isReadable() ? pod.count(which) : 0;
        }
        return OrganMenu.reading(reads);
    }

    PodKind kind() {
        return kind;
    }

    int compartments() {
        return kind.compartments();
    }

    /** The grade the server last sent; on the server, the pod's own. */
    int grade() {
        return pod.map(ItemCapsuleBlockEntity::grade).orElse(synced(0));
    }

    /** What compartment {@code compartment} holds, as the server last sent it. */
    int count(int compartment) {
        return synced(1 + compartment);
    }

    /** What every compartment holds together. */
    int count() {
        int total = 0;
        for (int compartment = 0; compartment < compartments(); compartment++) total += count(compartment);
        return total;
    }

    /** How many compartments hold something. */
    int used() {
        int used = 0;
        for (int compartment = 0; compartment < compartments(); compartment++) if (count(compartment) > 0) used++;
        return used;
    }

    /** The most of what compartment {@code compartment} holds that fits at this grade. */
    int limit(int compartment) {
        ItemStack held = getSlot(COMPARTMENT + compartment).getItem();
        return Compartments.limit(kind.stacks(grade()), held.isEmpty() ? 64 : held.getMaxStackSize());
    }

    /**
     * A well that takes a Skeletal Graft while the pod can still grow, and an Elastic Gel while it is a bulk pod, and
     * spends one the moment it is put there: a graft a grade, the gel the pod's kind. What it cannot spend, such as a
     * gel for a pod that holds items, stays in the well for the player to take back, with the reason said. The client
     * only predicts.
     */
    private final class TreatmentSlot extends Slot {
        TreatmentSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(EcologyModule.SKELETAL_GRAFT.get()) && grade() < CapsuleStock.LAST_GRADE
                    || stack.is(StorageModule.CRAW_GEL) && kind == PodKind.BULK;
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
            if (organ.getLevel() == null) return stack;
            if (stack.is(StorageModule.CRAW_GEL)) return mutate(organ, stack);
            if (!stack.is(EcologyModule.SKELETAL_GRAFT.get())) return stack;
            ItemStack rest = stack.copy();
            while (!rest.isEmpty() && organ.grow()) {
                rest.shrink(1);
                organ.getLevel()
                        .levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, organ.getBlockPos(), GRAFT_PARTICLES);
            }
            return rest;
        }

        private ItemStack mutate(ItemCapsuleBlockEntity organ, ItemStack stack) {
            Level level = organ.getLevel();
            BlockPos at = organ.getBlockPos();
            refusal = organ.craw();
            if (refusal.isPresent() || !organ.mutateToCraw()) return stack;
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, at, GRAFT_PARTICLES);
            ItemStack rest = stack.copy();
            rest.shrink(1);
            return rest;
        }
    }

    /** Says why the well kept a gel, once, to the player whose click it was. */
    private void announce(Player player) {
        refusal.ifPresent(why -> player.displayClientMessage(
                Component.translatable("gui.infestusfrontier.item_capsule.pod.gel." + why), true));
        refusal = Optional.empty();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (pod.isPresent() && (player.containerMenu != this || !stillValid(player))) return;
        if (isCompartment(slot) && type == ClickType.SWAP) return;
        super.clicked(slot, button, type, player);
        announce(player);
    }

    private boolean isCompartment(int slot) {
        return slot >= COMPARTMENT && slot < COMPARTMENT + compartments();
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof CompartmentSlot);
    }

    /**
     * The compartments give a stack at a time to the inventory. Inventory items go to the treatment well when it takes
     * them, otherwise on through the compartments in order, a kind filling its own before it opens an empty one. An
     * Elastic Gel for a pod that holds items is stored, not offered to the well to be refused.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || player.containerMenu != this || !stillValid(player))
            return ItemStack.EMPTY;
        if (index < COMPARTMENT) return super.quickMoveStack(player, index);
        if (!isCompartment(index)) return inventoryToPod(player, index);
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moving = held.copyWithCount(Math.min(held.getCount(), held.getMaxStackSize()));
        int before = moving.getCount();
        moveItemStackTo(moving, COMPARTMENT + compartments(), slots.size(), true);
        int moved = before - moving.getCount();
        if (moved == 0) return ItemStack.EMPTY;
        slot.remove(moved);
        return held.copyWithCount(moved);
    }

    private ItemStack inventoryToPod(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int organs = COMPARTMENT + compartments();
        int hotbar = organs + INVENTORY_SLOTS;
        boolean toWell = slots.get(TREATMENT).mayPlace(stack) && !refusedGel(stack);
        boolean moved = toWell
                ? moveItemStackTo(stack, TREATMENT, TREATMENT + 1, false)
                : moveItemStackTo(stack, COMPARTMENT, organs, false);
        if (!moved)
            moved = index < hotbar
                    ? moveItemStackTo(stack, hotbar, slots.size(), false)
                    : moveItemStackTo(stack, organs, hotbar, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        announce(player);
        return before;
    }

    /** True for a gel the pod would refuse now. */
    private boolean refusedGel(ItemStack stack) {
        return pod.isPresent()
                && stack.is(StorageModule.CRAW_GEL)
                && pod.get().craw().isPresent();
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
