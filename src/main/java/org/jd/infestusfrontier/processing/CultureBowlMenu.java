package org.jd.infestusfrontier.processing;

import java.util.Optional;
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
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/**
 * The Bowl screen's server half: nine ingredient slots and the output slot backed by the block entity's own handlers,
 * five synced ints (water, biomass, biomass capacity, progress percent, state) and the Start button. Nothing here
 * decides a batch; the block entity does.
 */
public final class CultureBowlMenu extends OrganMenu {
    public static final int START_BUTTON = 0;
    /** The water tank's size, for the client's gauge; the server never syncs it. */
    public static final int WATER_CAPACITY = CultureBowlBlockEntity.WATER_CAPACITY;

    public static final int IDLE = 0;
    public static final int WORKING = 1;
    public static final int PAUSED = 2;
    static final int INGREDIENTS = CultureBowlBlockEntity.INGREDIENT_SLOTS;
    public static final int OUTPUT_SLOT = INGREDIENTS;
    private static final int WATER = 0;
    private static final int STORED = 1;
    private static final int CAPACITY = 2;
    private static final int PERCENT = 3;
    private static final int STATE = 4;
    private static final int OUTPUT_X = Sheet.SLOT_X + 58;
    private static final int STRIPS = 3;

    private final Optional<CultureBowlBlockEntity> bowl;

    /** Server side. */
    CultureBowlMenu(int id, Inventory inventory, CultureBowlBlockEntity bowl) {
        this(
                id,
                inventory,
                bowl.ingredients(),
                bowl.output(),
                reading(
                        () -> bowl.water().getFluidAmount(),
                        () -> bowl.tank().stored().units(),
                        () -> bowl.tank().capacity().units(),
                        bowl::percent,
                        bowl::state),
                at(bowl),
                Optional.of(bowl));
    }

    /** Client side. The position arrives as extra data. */
    public CultureBowlMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(INGREDIENTS),
                new ItemStackHandler(1),
                new SimpleContainerData(5),
                at(inventory, extra),
                Optional.empty());
    }

    private CultureBowlMenu(
            int id,
            Inventory inventory,
            IItemHandler ingredients,
            IItemHandler output,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<CultureBowlBlockEntity> bowl) {
        super(ProcessingModule.CULTURE_BOWL_MENU.get(), id, access, ProcessingModule.CULTURE_BOWL.get(), data);
        this.bowl = bowl;
        SlotGrid grid = SlotGrid.standard3x3();
        for (int i = 0; i < INGREDIENTS; i++) {
            Bounds cell = grid.cell(i);
            addSlot(new SlotItemHandler(ingredients, i, cell.x() + 1, cell.y() + 1));
        }
        addSlot(new SlotItemHandler(output, 0, OUTPUT_X, Sheet.ROW2_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayer(inventory, STRIPS);
    }

    public int water() {
        return synced(WATER);
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    public int percent() {
        return Math.clamp(synced(PERCENT), 0, 100);
    }

    /** One of {@link #IDLE}, {@link #WORKING}, {@link #PAUSED}. */
    public int state() {
        return Math.clamp(synced(STATE), IDLE, PAUSED);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != START_BUTTON) return false;
        bowl.ifPresent(entity -> entity.start(player).ifPresent(refusal -> Refusals.show(player, refusal)));
        return true;
    }
}
