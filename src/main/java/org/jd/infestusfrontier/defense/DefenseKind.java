package org.jd.infestusfrontier.defense;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jd.infestusfrontier.core.defense.Sentry;

/** Both rooted defenders share fuel and loot storage; their weapons and target ranges differ. */
enum DefenseKind {
    SPINE("spine_sentry", Sentry.COST, Sentry.PULSE_TICKS),
    AIR("aerocyte_bloom", 5, 40);

    private final String id;
    private final int cost;
    private final int pulse;

    DefenseKind(String id, int cost, int pulse) {
        this.id = id;
        this.cost = cost;
        this.pulse = pulse;
    }

    String id() {
        return id;
    }

    int cost() {
        return cost;
    }

    int pulse() {
        return pulse;
    }

    Block block() {
        return this == SPINE ? DefenseModule.SPINE_SENTRY.get() : DefenseModule.AEROCYTE_BLOOM.get();
    }

    BlockEntityType<DefenseOrganBlockEntity> entityType() {
        return this == SPINE ? DefenseModule.SPINE_SENTRY_ENTITY.get() : DefenseModule.AEROCYTE_BLOOM_ENTITY.get();
    }

    MenuType<DefenseMenu> menuType() {
        return this == SPINE ? DefenseModule.SPINE_SENTRY_MENU.get() : DefenseModule.AEROCYTE_BLOOM_MENU.get();
    }
}
