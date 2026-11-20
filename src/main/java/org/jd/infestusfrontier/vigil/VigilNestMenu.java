package org.jd.infestusfrontier.vigil;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;

public final class VigilNestMenu extends OrganMenu {
    private final Optional<VigilNestBlockEntity> nest;
    static final int STRIPS = 4;

    VigilNestMenu(int id, Inventory inventory, VigilNestBlockEntity nest) {
        this(
                id,
                inventory,
                at(nest),
                reading(
                        () -> nest.tank().stored().units(),
                        nest::mask,
                        nest::paid,
                        () -> nest.enabled() ? 1 : 0,
                        nest::stop,
                        nest::capacity,
                        () -> nest.egg() ? 1 : 0,
                        nest::health,
                        () -> nest.hungry() ? 1 : 0),
                Optional.of(nest));
    }

    public VigilNestMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, at(inventory, extra), new SimpleContainerData(9), Optional.empty());
    }

    private VigilNestMenu(
            int id,
            Inventory inventory,
            ContainerLevelAccess access,
            ContainerData data,
            Optional<VigilNestBlockEntity> nest) {
        super(VigilModule.MENU.get(), id, access, VigilModule.NEST.get(), data);
        this.nest = nest;
        addPlayer(inventory, STRIPS);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || button < 0 || button > 9 || nest.isEmpty()) return false;
        nest.get().action(button);
        return true;
    }

    @Override
    public int stored() {
        return synced(0);
    }

    @Override
    public int capacity() {
        return NestContents.CAPACITY;
    }

    public int mask() {
        return synced(1);
    }

    public int paid() {
        return synced(2);
    }

    public boolean enabled() {
        return synced(3) != 0;
    }

    public int stop() {
        return synced(4);
    }

    public int allowance() {
        return synced(5);
    }

    public int health() {
        return synced(7);
    }

    public boolean hungry() {
        return synced(8) != 0;
    }

    public boolean egg() {
        return synced(6) != 0;
    }
}
