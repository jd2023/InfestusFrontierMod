package org.jd.infestusfrontier.mining;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.RootMutations;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Tunnel Root screen's server half: the segment slot, the four take-only output slots, and as synced ints the
 * biomass, the steering, the cells laid, the stop, the seconds to the next step and the installed mutations. Buttons 0, 1 and 2 steer the next
 * step straight, left and right; the block entity checks and applies the choice.
 */
public final class TunnelRootMenu extends OrganMenu {
    public static final int STRAIGHT_BUTTON = 0;
    public static final int LEFT_BUTTON = 1;
    public static final int RIGHT_BUTTON = 2;
    static final int STRIPS = 3;
    /** The output wells sit on the second row, this far apart. */
    static final int OUTPUT_PITCH = Sheet.SLOT.width() + Sheet.GAP;

    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int STEER = 2;
    private static final int CELLS = 3;
    private static final int STOP = 4;
    private static final int SECONDS = 5;
    private static final int MUTATIONS = 6;
    private static final int SYNCED = 7;
    private static final int TICKS_PER_SECOND = 20;

    private final Optional<TunnelRootBlockEntity> head;

    /** Server side. */
    TunnelRootMenu(int id, Inventory inventory, TunnelRootBlockEntity head) {
        this(id, inventory, head.segments(), head.output(), reading(reads(head)), at(head), Optional.of(head));
    }

    /** Client side. The position arrives as extra data. */
    public TunnelRootMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(1),
                new ItemStackHandler(Tunnelling.OUTPUT_SLOTS),
                new SimpleContainerData(SYNCED),
                at(inventory, extra),
                Optional.empty());
    }

    private TunnelRootMenu(
            int id,
            Inventory inventory,
            IItemHandler segments,
            IItemHandler output,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<TunnelRootBlockEntity> head) {
        super(MiningModule.TUNNEL_ROOT_MENU.get(), id, access, MiningModule.TUNNEL_ROOT.get(), data);
        this.head = head;
        addSlot(new SlotItemHandler(segments, 0, Sheet.SLOT_X, Sheet.SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MiningModule.ROOT_SEGMENT.get()) && super.mayPlace(stack);
            }
        });
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++)
            addSlot(new SlotItemHandler(output, slot, Sheet.SLOT_X + slot * OUTPUT_PITCH, Sheet.ROW2_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        addPlayer(inventory, STRIPS);
    }

    private static IntSupplier[] reads(TunnelRootBlockEntity head) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[STORED] = () -> head.tank().stored().units();
        reads[CAPACITY] = () -> head.tank().capacity().units();
        reads[STEER] = () -> head.steer().ordinal();
        reads[CELLS] = () -> head.progress().cells();
        reads[STOP] = () ->
                head.stop().map(stop -> Tunnelling.STOPS.indexOf(stop) + 1).orElse(0);
        reads[SECONDS] = () -> (head.progress().ticksLeft() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        reads[MUTATIONS] = () -> head.mutations().bits();
        return reads;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Optional<Steer> way = Steer.at(id);
        if (way.isEmpty()) return false;
        head.ifPresent(h -> h.steer(way.get()));
        return true;
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    /** The way the next step goes, as last synced. */
    public Steer steer() {
        return Steer.at(synced(STEER)).orElse(Steer.STRAIGHT);
    }

    public int cells() {
        return synced(CELLS);
    }

    /** Why the head stopped, as last synced, or empty while it works. */
    public Optional<Refusal> stop() {
        int index = synced(STOP) - 1;
        return index >= 0 && index < Tunnelling.STOPS.size()
                ? Optional.of(Tunnelling.STOPS.get(index))
                : Optional.empty();
    }

    /** The translation key of the reason the head stopped, or empty while it works. */
    public Optional<String> stopKey() {
        return stop().map(stop -> "infestusfrontier.refusal." + stop.key());
    }

    /** Seconds until the next step is due. */
    public int seconds() {
        return synced(SECONDS);
    }

    /** The head's mutations, as last synced, in their fixed order. */
    public List<RootMutation> mutations() {
        return RootMutations.fromBits(synced(MUTATIONS));
    }
}
