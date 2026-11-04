package org.jd.infestusfrontier.storage;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jd.infestusfrontier.core.storage.Compartments;
import org.jd.infestusfrontier.core.storage.PodKind;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.organ.api.WorkshopActionEvent;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * One pod on its own: its compartments, a bulk pod's one, a Mixed Craw's four or a Larder's three, which a click or a
 * shift-click fills and empties a stack at a time, and a treatment well. A Skeletal Graft put in the well grows the pod
 * a grade each; an Elastic Gel put in a bulk pod's well turns the empty pod into a Mixed Craw and a Sealing Resin put
 * in an empty Craw's well turns it into a Larder; a pod that holds anything refuses them and says so. A Larder's
 * screen also has a label slot, a reserve step and a Withdraw reserve button for each compartment: a click on a
 * compartment takes only what its reserve leaves, and Withdraw reserve is the one way to take the reserve itself, a
 * stack of it a click into the cursor. The trunk opens the whole tree ({@link ItemCapsuleMenu}); a pod opens only
 * itself, so quick access and treatments never page through the stem.
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

    /** A Larder's rows of well, label, reserve and withdraw need more body. */
    static final int LARDER_STRIPS = 3;

    /** The menu buttons of a Larder: an action and a compartment, {@code action * BUTTON_STEP + compartment}. */
    public static final int RESERVE_UP = 0;

    public static final int RESERVE_DOWN = 1;
    public static final int WITHDRAW = 2;
    public static final int BUTTON_STEP = 4;

    /** A Larder's compartment rows: the well, its label well, the reserve step and Withdraw reserve, then the line. */
    static final int ROW_Y = 18;

    static final int ROW_PITCH = 27;
    static final int LABEL_X = Sheet.LEFT_X + 22;
    static final int RESERVE_X = LABEL_X + 22;
    static final int WITHDRAW_X = RESERVE_X + 4 + 52;
    /** The treatment well under the last row, with the grade and fill lines beside it. */
    static final int LARDER_TREATMENT_Y = 102;

    private final Optional<ItemCapsuleBlockEntity> pod;
    private final PodKind kind;
    /** True while a click has put something into the pod that no one has been credited for yet. */
    private boolean stored;
    /** The language key of why the well kept an item, said once to the player whose click it was. */
    private Optional<String> refusal = Optional.empty();
    /** The client's copy of the labels, for the click it predicts; the server's are the pod's. */
    private final ItemStack[] labelCopy = new ItemStack[PodKind.LARDER_COMPARTMENTS];

    /** Body strips under the parts row of a pod of {@code kind}. */
    static int strips(PodKind kind) {
        return kind.reserves() ? LARDER_STRIPS : STRIPS;
    }

    /** The y of compartment {@code compartment}'s well in a Larder's screen. */
    static int rowY(int compartment) {
        return ROW_Y + compartment * ROW_PITCH;
    }

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
                blockOf(kind),
                pod.isEmpty() ? new SimpleContainerData(dataSize(kind)) : readingOf(pod.get()));
        this.pod = pod;
        this.kind = kind;
        Arrays.fill(labelCopy, ItemStack.EMPTY);
        ItemStack[] copy = new ItemStack[kind.compartments()];
        Arrays.fill(copy, ItemStack.EMPTY);
        CompartmentSlot.Source source = new CompartmentSlot.Source() {
            @Override
            public ItemStack get(int compartment) {
                return pod.map(organ -> organ.held(compartment)).orElse(copy[compartment]);
            }

            @Override
            public void set(int compartment, ItemStack stack) {
                if (pod.isPresent()) {
                    ItemStack before = pod.get().held(compartment);
                    pod.get().put(compartment, stack);
                    boolean more = before.isEmpty()
                            || ItemStack.isSameItemSameComponents(before, stack)
                                    && stack.getCount() > before.getCount();
                    if (!stack.isEmpty() && more) stored = true;
                } else copy[compartment] = stack.copy();
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

            @Override
            public int takeable(int compartment) {
                return pod.map(organ -> organ.automatable(compartment))
                        .orElse(Math.max(0, copy[compartment].getCount() - reserve(compartment)));
            }
        };
        addSlot(new TreatmentSlot(
                kind.reserves() ? Sheet.LEFT_X + 1 : treatmentX(kind.compartments()) + 1,
                kind.reserves() ? LARDER_TREATMENT_Y + 1 : Sheet.SLOT_Y));
        SimpleContainer shared = new SimpleContainer(kind.compartments());
        for (int compartment = 0; compartment < kind.compartments(); compartment++)
            addSlot(new CompartmentSlot(
                    source,
                    shared,
                    compartment,
                    kind.reserves() ? Sheet.LEFT_X + 1 : wellX(compartment) + 1,
                    kind.reserves() ? rowY(compartment) + 1 : Sheet.SLOT_Y));
        if (kind.reserves())
            for (int compartment = 0; compartment < kind.compartments(); compartment++)
                addSlot(new LabelSlot(shared, compartment, LABEL_X + 1, rowY(compartment) + 1));
        addPlayer(inventory, strips(kind));
    }

    private static Block blockOf(PodKind kind) {
        return switch (kind) {
            case BULK -> StorageModule.ITEM_CAPSULE.get();
            case CRAW -> StorageModule.MIXED_CRAW.get();
            case LARDER -> StorageModule.LARDER.get();
        };
    }

    /** The grade, what each compartment holds, and for a Larder each compartment's reserve. */
    private static int dataSize(PodKind kind) {
        return 1 + kind.compartments() * (kind.reserves() ? 2 : 1);
    }

    /** The grade, then what each compartment holds, then a Larder's reserves, read live on the server. */
    private static ContainerData readingOf(ItemCapsuleBlockEntity pod) {
        IntSupplier[] reads = new IntSupplier[dataSize(pod.kind())];
        reads[0] = pod::grade;
        for (int compartment = 0; compartment < pod.compartments(); compartment++) {
            int which = compartment;
            reads[1 + compartment] = () -> pod.isReadable() ? pod.count(which) : 0;
            if (pod.kind().reserves())
                reads[1 + pod.compartments() + compartment] = () -> pod.isReadable() ? pod.reserve(which) : 0;
        }
        return OrganMenu.reading(reads);
    }

    /** The slot where a Larder's compartment {@code compartment} shows its label. */
    public int labelSlot(int compartment) {
        return COMPARTMENT + compartments() + compartment;
    }

    /** The slots of the pod itself: the treatment well, the compartments and a Larder's labels. */
    private int organSlots() {
        return COMPARTMENT + compartments() * (kind.reserves() ? 2 : 1);
    }

    /** A Larder's label well: it shows the compartment's label and never gives or takes an item. */
    private final class LabelSlot extends Slot {
        LabelSlot(Container container, int compartment, int x, int y) {
            super(container, compartment, x, y);
        }

        @Override
        public ItemStack getItem() {
            return pod.map(organ -> organ.label(getContainerSlot())).orElse(labelCopy[getContainerSlot()]);
        }

        @Override
        public void set(ItemStack stack) {
            if (pod.isEmpty())
                labelCopy[getContainerSlot()] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean allowModification(Player player) {
            return false;
        }
    }

    /** True for a Larder's label well. */
    private boolean isLabel(int slot) {
        return kind.reserves() && slot >= labelSlot(0) && slot < labelSlot(0) + compartments();
    }

    /** A click on a label well labels the compartment with the item in the cursor, or clears an empty one. */
    private void clickLabel(int slot, int button, ClickType type, Player player) {
        int compartment = slot - labelSlot(0);
        ItemStack carried = getCarried();
        boolean clear = type == ClickType.QUICK_MOVE || type == ClickType.PICKUP && carried.isEmpty();
        if (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE) return;
        if (pod.isEmpty()) {
            if (!clear && Stems.takes(carried) && count(compartment) == 0)
                labelCopy[compartment] = carried.copyWithCount(1);
            else if (clear && count(compartment) == 0) labelCopy[compartment] = ItemStack.EMPTY;
            return;
        }
        ItemCapsuleBlockEntity organ = pod.get();
        if (clear) {
            if (!organ.clearLabel(compartment)) refuse("label.held", organ.labelled(compartment));
        } else if (!organ.setLabel(compartment, carried)) refuse("label.refused", true);
    }

    private void refuse(String why, boolean say) {
        if (say) refusal = Optional.of("gui.infestusfrontier.item_capsule.pod." + why);
    }

    PodKind kind() {
        return kind;
    }

    int compartments() {
        return kind.compartments();
    }

    /** The reserve of a Larder's compartment, as the server last sent it; 0 for a pod that keeps none. */
    public int reserve(int compartment) {
        return kind.reserves() ? synced(1 + compartments() + compartment) : 0;
    }

    /** What a Larder's reserve keeps of compartment {@code compartment}'s items: the reserve, or all if less. */
    public int kept(int compartment) {
        return Math.min(count(compartment), reserve(compartment));
    }

    /** What an external extraction may take of compartment {@code compartment}. */
    public int free(int compartment) {
        return count(compartment) - kept(compartment);
    }

    /** The grade the server last sent; on the server, the pod's own. */
    int grade() {
        return pod.map(ItemCapsuleBlockEntity::grade).orElse(synced(0));
    }

    /** What compartment {@code compartment} holds, as the server last sent it. */
    public int count(int compartment) {
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
                    || stack.is(StorageModule.CRAW_GEL) && kind == PodKind.BULK
                    || stack.is(StorageModule.LARDER_RESIN) && kind == PodKind.CRAW;
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
            if (stack.is(StorageModule.CRAW_GEL)) return mutate(organ, stack, false);
            if (stack.is(StorageModule.LARDER_RESIN)) return mutate(organ, stack, true);
            if (!stack.is(EcologyModule.SKELETAL_GRAFT.get())) return stack;
            ItemStack rest = stack.copy();
            while (!rest.isEmpty() && organ.grow()) {
                rest.shrink(1);
                organ.getLevel()
                        .levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, organ.getBlockPos(), GRAFT_PARTICLES);
            }
            return rest;
        }

        private ItemStack mutate(ItemCapsuleBlockEntity organ, ItemStack stack, boolean larder) {
            Level level = organ.getLevel();
            BlockPos at = organ.getBlockPos();
            Optional<String> why = larder ? organ.larder() : organ.craw();
            refusal =
                    why.map(reason -> "gui.infestusfrontier.item_capsule.pod." + (larder ? "resin." : "gel.") + reason);
            if (why.isPresent() || !(larder ? organ.mutateToLarder() : organ.mutateToCraw())) return stack;
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, at, GRAFT_PARTICLES);
            ItemStack rest = stack.copy();
            rest.shrink(1);
            return rest;
        }
    }

    /** Says why the well kept an item or a label was refused, once, to the player whose click it was. */
    private void announce(Player player) {
        refusal.ifPresent(why -> player.displayClientMessage(Component.translatable(why), true));
        refusal = Optional.empty();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (pod.isPresent() && (player.containerMenu != this || !stillValid(player))) return;
        if (isCompartment(slot) && type == ClickType.SWAP) return;
        if (isLabel(slot)) {
            clickLabel(slot, button, type, player);
            announce(player);
            return;
        }
        super.clicked(slot, button, type, player);
        announce(player);
        credit(player);
    }

    /** Tells the world a player stored something in a pod of a standing tree, once for the click that did it. */
    private void credit(Player player) {
        if (!stored) return;
        stored = false;
        if (pod.filter(Stems::inTree).isPresent()
                && player instanceof ServerPlayer server
                && !(server instanceof FakePlayer))
            NeoForge.EVENT_BUS.post(new WorkshopActionEvent(server, WorkshopActionEvent.Action.TREE_STORED));
    }

    /** The Larder's buttons, sent by id: a reserve step up or down, or Withdraw reserve, for one compartment. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        int action = id / BUTTON_STEP;
        int compartment = id % BUTTON_STEP;
        if (id < 0 || !kind.reserves() || compartment >= compartments() || action > WITHDRAW) return false;
        if (pod.isEmpty()) return true;
        if (player.containerMenu != this || !stillValid(player)) return false;
        ItemCapsuleBlockEntity organ = pod.get();
        if (action == WITHDRAW) {
            boolean moved = withdraw(organ, compartment);
            if (!moved && organ.reserved(compartment) > 0) refuse("withdraw.busy", true);
            announce(player);
            return moved;
        }
        return organ.stepReserve(compartment, action == RESERVE_UP);
    }

    /**
     * Withdraw reserve: up to one stack of the reserved part of a compartment into the cursor, which must be empty or
     * hold that kind with room. The reserve setting does not change. False, changing nothing, when there is nothing
     * reserved or the cursor cannot take it.
     */
    private boolean withdraw(ItemCapsuleBlockEntity organ, int compartment) {
        ItemStack kept = organ.held(compartment);
        int part = Math.min(organ.reserved(compartment), kept.getMaxStackSize());
        if (part <= 0) return false;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) setCarried(kept.copyWithCount(part));
        else if (ItemStack.isSameItemSameComponents(carried, kept)) {
            part = Math.min(part, carried.getMaxStackSize() - carried.getCount());
            if (part <= 0) return false;
            carried.grow(part);
            setCarried(carried);
        } else return false;
        organ.take(compartment, part);
        return true;
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
        if (isLabel(index)) return ItemStack.EMPTY;
        if (!isCompartment(index)) return inventoryToPod(player, index);
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        int takeable = ((CompartmentSlot) slot).takeable();
        if (takeable <= 0) return ItemStack.EMPTY;
        ItemStack moving = held.copyWithCount(Math.min(takeable, held.getMaxStackSize()));
        int before = moving.getCount();
        moveItemStackTo(moving, organSlots(), slots.size(), true);
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
        int organs = organSlots();
        int hotbar = organs + INVENTORY_SLOTS;
        boolean toWell = slots.get(TREATMENT).mayPlace(stack) && !refusedGel(stack);
        boolean moved = toWell
                ? moveItemStackTo(stack, TREATMENT, TREATMENT + 1, false)
                : moveItemStackTo(stack, COMPARTMENT, COMPARTMENT + compartments(), false);
        if (!moved)
            moved = index < hotbar
                    ? moveItemStackTo(stack, hotbar, slots.size(), false)
                    : moveItemStackTo(stack, organs, hotbar, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        announce(player);
        credit(player);
        return before;
    }

    /** True for a gel or resin the pod would refuse now. */
    private boolean refusedGel(ItemStack stack) {
        return pod.isPresent()
                && (stack.is(StorageModule.CRAW_GEL) && pod.get().craw().isPresent()
                        || stack.is(StorageModule.LARDER_RESIN)
                                && pod.get().larder().isPresent());
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
