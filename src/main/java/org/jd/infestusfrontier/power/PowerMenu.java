package org.jd.infestusfrontier.power;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

public final class PowerMenu extends OrganMenu {
    static final int STRIPS = 4;
    private final Optional<ThermalHeartBlockEntity> heart;
    private final boolean driver;

    PowerMenu(int id, Inventory inventory, ThermalHeartBlockEntity heart) {
        this(
                id,
                inventory,
                at(heart),
                heart.getBlockState().getBlock(),
                heart.items(),
                reading(
                        () -> heart.store().heat(),
                        () -> heart.store().water(),
                        () -> heart.store().steam(),
                        () -> heart.store().charge(),
                        heart::stop,
                        () -> heart.discharge() ? 1 : 0),
                Optional.of(heart));
    }

    public PowerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, extra.readBlockPos());
    }

    private PowerMenu(int id, Inventory inventory, net.minecraft.core.BlockPos pos) {
        this(
                id,
                inventory,
                ContainerLevelAccess.create(inventory.player.level(), pos),
                inventory.player.level().getBlockState(pos).getBlock(),
                new ItemStackHandler(4),
                new SimpleContainerData(6),
                Optional.empty());
    }

    private PowerMenu(
            int id,
            Inventory inventory,
            ContainerLevelAccess access,
            Block block,
            ItemStackHandler items,
            ContainerData data,
            Optional<ThermalHeartBlockEntity> heart) {
        super(PowerModule.MENU.get(), id, access, block, data);
        this.heart = heart;
        this.driver = block == PowerModule.DRIVER.get();
        for (int i = 0; i < (driver ? 0 : 4); i++)
            addSlot(new SlotItemHandler(items, i, Sheet.SLOT_X + i * 32, Sheet.SLOT_Y));
        addPlayer(inventory, STRIPS);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || button != 0 || heart.isEmpty()) return false;
        heart.get().toggle();
        return true;
    }

    @Override
    public int stored() {
        return 0;
    }

    @Override
    public int capacity() {
        return 1;
    }

    public boolean driver() {
        return driver;
    }

    public int amount(int store) {
        return store >= 0 && store < 4 ? synced(store) : 0;
    }

    public int stop() {
        return synced(4);
    }

    public boolean discharge() {
        return synced(5) != 0;
    }
}
