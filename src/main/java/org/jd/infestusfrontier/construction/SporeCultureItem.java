package org.jd.infestusfrontier.construction;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.jd.infestusfrontier.construction.api.CultureUse;

final class SporeCultureItem extends Item {
    private final CultureUse use;

    SporeCultureItem(Properties properties, CultureUse use) {
        super(properties);
        this.use = use;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use.apply(context);
    }
}
