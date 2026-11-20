package org.jd.infestusfrontier.mining;

import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Structure Grower screen's server half: the segment, tread, light and rib slots, and as synced ints the stop, the
 * steps of the plan under it that are built and how many it has, the steps this grower has built in all and the
 * seconds to the next try.
 */
public final class StructureGrowerMenu extends OrganMenu {
    /** The stock wells sit on the first row, this far apart. */
    static final int PITCH = Sheet.SLOT.width() + Sheet.GAP;

    static final int STRIPS = 1;

    private static final int STOP = 0;
    private static final int BUILT = 1;
    private static final int STEPS = 2;
    private static final int GROWN = 3;
    private static final int SECONDS = 4;
    private static final int SYNCED = 5;
    private static final int TICKS_PER_SECOND = 20;

    /** Server side. */
    StructureGrowerMenu(int id, Inventory inventory, StructureGrowerBlockEntity grower) {
        this(
                id,
                inventory,
                grower.segments(),
                grower.treads(),
                grower.lights(),
                grower.ribStock(),
                reading(reads(grower)),
                at(grower));
    }

    /** Client side. The position arrives as extra data. */
    public StructureGrowerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(1),
                new ItemStackHandler(1),
                new ItemStackHandler(1),
                new ItemStackHandler(1),
                new SimpleContainerData(SYNCED),
                at(inventory, extra));
    }

    private StructureGrowerMenu(
            int id,
            Inventory inventory,
            IItemHandler segments,
            IItemHandler treads,
            IItemHandler lights,
            IItemHandler ribs,
            ContainerData data,
            ContainerLevelAccess access) {
        super(MiningModule.STRUCTURE_GROWER_MENU.get(), id, access, MiningModule.STRUCTURE_GROWER.get(), data);
        addSlot(new SlotItemHandler(segments, 0, Sheet.SLOT_X, Sheet.SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MiningModule.ROOT_SEGMENT.get()) && super.mayPlace(stack);
            }
        });
        addSlot(new SlotItemHandler(treads, 0, Sheet.SLOT_X + PITCH, Sheet.SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StructureGrowerBlockEntity.tread(stack) && super.mayPlace(stack);
            }
        });
        addSlot(new SlotItemHandler(lights, 0, Sheet.SLOT_X + 2 * PITCH, Sheet.SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StructureGrowerBlockEntity.light(stack) && super.mayPlace(stack);
            }
        });
        addSlot(new SlotItemHandler(ribs, 0, Sheet.SLOT_X + 3 * PITCH, Sheet.SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StructureGrowerBlockEntity.rib(stack) && super.mayPlace(stack);
            }
        });
        addPlayer(inventory, STRIPS);
    }

    private static IntSupplier[] reads(StructureGrowerBlockEntity grower) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[STOP] = () ->
                grower.stop().map(stop -> Surveying.STOPS.indexOf(stop) + 1).orElse(0);
        reads[BUILT] = () -> grower.survey().map(Survey::built).orElse(0);
        reads[STEPS] = () -> grower.survey().map(s -> s.plan().steps().size()).orElse(0);
        reads[GROWN] = () -> Math.min(Short.MAX_VALUE, grower.progress().steps());
        reads[SECONDS] =
                () -> (Surveying.GROW_TICKS - grower.progress().ticks() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        return reads;
    }

    @Override
    public int stored() {
        return 0;
    }

    @Override
    public int capacity() {
        return 1;
    }

    /** Why the grower stopped, as last synced, or empty while it works or waits. */
    public Optional<Refusal> stop() {
        int index = synced(STOP) - 1;
        return index >= 0 && index < Surveying.STOPS.size()
                ? Optional.of(Surveying.STOPS.get(index))
                : Optional.empty();
    }

    /** The translation key of the reason the grower stopped, or empty while it works. */
    public Optional<String> stopKey() {
        return stop().map(stop -> "infestusfrontier.refusal." + stop.key());
    }

    /** Steps of the plan under the grower that are built. */
    public int built() {
        return synced(BUILT);
    }

    /** Steps the plan under the grower has; 0 when there is none. */
    public int steps() {
        return synced(STEPS);
    }

    /** Steps this grower has built in all, its history. */
    public int grown() {
        return synced(GROWN);
    }

    public int seconds() {
        return synced(SECONDS);
    }
}
