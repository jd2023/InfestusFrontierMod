package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
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
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.Galleries;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.RootMutations;
import org.jd.infestusfrontier.core.mining.Rootstocking;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Tunnel Root screen's server half: the segment slot, the four take-only output slots, and as synced ints the
 * biomass, the heading, the steering, the cells laid, the stop, the seconds to the next step and the installed
 * mutations; for a
 * Rootstock working as a minehead also that it is one, the steps it has advanced and its section's built and planned
 * steps, and for each of its gallery faces what holds it up, its built and planned steps and whether it is wide.
 * Buttons 0, 1 and 2 steer the next step straight, left and right; the block entity checks and applies the choice.
 */
public final class TunnelRootMenu extends OrganMenu {
    public static final int STRAIGHT_BUTTON = 0;
    public static final int LEFT_BUTTON = 1;
    public static final int RIGHT_BUTTON = 2;
    static final int STRIPS = 4;
    /** The segment well and the output wells sit in one row, this far apart, under the steering and the progress. */
    static final int OUTPUT_PITCH = Sheet.SLOT.width() + Sheet.GAP;
    /** Where the screen puts that row's slots: one pixel inside its wells. */
    static final int WELL_SLOT_Y = 80;
    /** The first output slot: set off from the segment well, so each well's label has room. */
    static final int OUTPUT_X = Sheet.SLOT_X + OUTPUT_PITCH + 16;

    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int STEER = 2;
    private static final int CELLS = 3;
    private static final int STOP = 4;
    private static final int SECONDS = 5;
    private static final int MUTATIONS = 6;
    private static final int MINEHEAD = 7;
    private static final int DESCENDED = 8;
    private static final int BUILT = 9;
    private static final int STEPS = 10;
    private static final int FACES = 11;
    /** The way the head faces, as a horizontal direction's 2D data value. */
    private static final int HEADING = 12;
    /** Each face's stop, built steps, planned steps and width, from here on. */
    private static final int FACE_DATA = 13;

    private static final int PER_FACE = 4;
    private static final int SYNCED = FACE_DATA + PER_FACE * Galleries.MAX_FACES;
    private static final int TICKS_PER_SECOND = 20;

    /**
     * Every stop the head names, in a fixed order: a Tunnel Root's, then a minehead's own and those of the organs it
     * shows. The menu sends one by its index.
     */
    public static final List<Refusal> STOPS = Stream.of(
                    Tunnelling.STOPS,
                    Rootstocking.STOPS,
                    Galleries.STOPS,
                    Fracturing.STOPS,
                    Surveying.STOPS,
                    WorkOrganMenu.Kind.CILIA.stops)
            .flatMap(List::stream)
            .distinct()
            .toList();

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
        addSlot(new SlotItemHandler(segments, 0, Sheet.SLOT_X, WELL_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MiningModule.ROOT_SEGMENT.get()) && super.mayPlace(stack);
            }
        });
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++)
            addSlot(new SlotItemHandler(output, slot, OUTPUT_X + slot * OUTPUT_PITCH, WELL_SLOT_Y) {
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
        reads[STOP] = () -> head.stop().map(stop -> STOPS.indexOf(stop) + 1).orElse(0);
        reads[SECONDS] = () -> (head.progress().ticksLeft() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        reads[MUTATIONS] = () -> head.mutations().bits();
        reads[MINEHEAD] = () -> head.minehead() ? 1 : 0;
        reads[DESCENDED] = head::descended;
        reads[BUILT] = head::built;
        reads[STEPS] = head::steps;
        reads[FACES] = () -> head.faces().size();
        reads[HEADING] =
                () -> head.getBlockState().getValue(TunnelRootBlock.FACING).get2DDataValue();
        for (int face = 0; face < Galleries.MAX_FACES; face++) {
            int index = face;
            int at = FACE_DATA + PER_FACE * face;
            reads[at] = () -> face(head, index)
                    .flatMap(Minehead.FaceLook::stop)
                    .map(stop -> STOPS.indexOf(stop) + 1)
                    .orElse(0);
            reads[at + 1] =
                    () -> face(head, index).map(Minehead.FaceLook::built).orElse(0);
            reads[at + 2] =
                    () -> face(head, index).map(Minehead.FaceLook::steps).orElse(0);
            reads[at + 3] =
                    () -> face(head, index).map(look -> look.wide() ? 1 : 0).orElse(0);
        }
        return reads;
    }

    private static Optional<Minehead.FaceLook> face(TunnelRootBlockEntity head, int index) {
        return index < head.faces().size() ? Optional.of(head.faces().get(index)) : Optional.empty();
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

    /** The way the head faces, as last synced. */
    public Direction heading() {
        return Direction.from2DDataValue(synced(HEADING));
    }

    /** The way the next step goes: the heading turned as steered. */
    public Direction next() {
        return MiningDirections.turn(heading(), steer().quarters());
    }

    public int cells() {
        return synced(CELLS);
    }

    /** Why the head stopped, as last synced, or empty while it works. */
    public Optional<Refusal> stop() {
        int index = synced(STOP) - 1;
        return index >= 0 && index < STOPS.size() ? Optional.of(STOPS.get(index)) : Optional.empty();
    }

    /** The translation key of the reason the head stopped, or empty while it works. */
    public Optional<String> stopKey() {
        return stop().map(stop -> "infestusfrontier.refusal." + stop.key());
    }

    /** Seconds until the next step is due. */
    public int seconds() {
        return synced(SECONDS);
    }

    /** True while the head works as a Rootstock's minehead, as last synced. */
    public boolean minehead() {
        return synced(MINEHEAD) == 1;
    }

    /** Staircase steps built under the Rootstock's order: its minehead history. */
    public int descended() {
        return synced(DESCENDED);
    }

    /** Steps of the minehead's section built, as last looked at. */
    public int built() {
        return synced(BUILT);
    }

    /** Steps the minehead's section has in all: its treads and its landing. */
    public int steps() {
        return synced(STEPS);
    }

    /** One gallery face as last synced: what holds it up (empty while it works), its built and planned steps, its width. */
    public record Face(Optional<Refusal> stop, int built, int steps, boolean wide) {}

    /** The minehead's gallery faces as last synced, in the order they take turns. */
    public List<Face> faces() {
        List<Face> faces = new ArrayList<>();
        for (int face = 0; face < Math.min(synced(FACES), Galleries.MAX_FACES); face++) {
            int at = FACE_DATA + PER_FACE * face;
            int stop = synced(at) - 1;
            faces.add(new Face(
                    stop >= 0 && stop < STOPS.size() ? Optional.of(STOPS.get(stop)) : Optional.empty(),
                    synced(at + 1),
                    synced(at + 2),
                    synced(at + 3) == 1));
        }
        return List.copyOf(faces);
    }

    /** The head's mutations, as last synced, in their fixed order. */
    public List<RootMutation> mutations() {
        return RootMutations.fromBits(synced(MUTATIONS));
    }
}
