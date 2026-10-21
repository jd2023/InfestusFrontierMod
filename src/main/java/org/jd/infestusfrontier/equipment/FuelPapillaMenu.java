package org.jd.infestusfrontier.equipment;

import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;

/**
 * The Fuel Papilla screen's server half: the buffer, which pieces it fills and the fill limit, as synced ints. Buttons
 * 0 to 3 toggle head, body, legs and feet; button 4 steps the limit. The block entity changes the setting.
 */
public final class FuelPapillaMenu extends OrganMenu {
    public static final int LIMIT_BUTTON = 4;
    static final int STRIPS = 3;
    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int PIECES = 2;
    private static final int LIMIT = 3;
    private static final int SYNCED = 4;

    private final Optional<FuelPapillaBlockEntity> papilla;

    /** Server side. */
    FuelPapillaMenu(int id, Inventory inventory, FuelPapillaBlockEntity papilla) {
        this(id, inventory, reading(reads(papilla)), at(papilla), Optional.of(papilla));
    }

    /** Client side. The position arrives as extra data. */
    public FuelPapillaMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new SimpleContainerData(SYNCED), at(inventory, extra), Optional.empty());
    }

    private FuelPapillaMenu(
            int id,
            Inventory inventory,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<FuelPapillaBlockEntity> papilla) {
        super(EquipmentModule.FUEL_PAPILLA_MENU.get(), id, access, EquipmentModule.FUEL_PAPILLA.get(), data);
        this.papilla = papilla;
        addPlayer(inventory, STRIPS);
    }

    private static IntSupplier[] reads(FuelPapillaBlockEntity papilla) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[STORED] = () -> papilla.tank().stored().units();
        reads[CAPACITY] = () -> papilla.tank().capacity().units();
        reads[PIECES] = papilla::pieces;
        reads[LIMIT] = papilla::limit;
        return reads;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > LIMIT_BUTTON) return false;
        papilla.ifPresent(p -> {
            if (id == LIMIT_BUTTON) p.cycleLimit();
            else p.togglePiece(id);
        });
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

    /** Whether the piece at {@code index} (head, body, legs, feet) is filled. */
    public boolean fills(int index) {
        return (synced(PIECES) & 1 << index) != 0;
    }

    /** The fill limit, percent of a piece's capacity. */
    public int limit() {
        return synced(LIMIT);
    }
}
