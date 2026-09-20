package org.jd.infestusfrontier.ecology;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.bus.api.IEventBus;

/** Composition boundary for substrate content and deliberate conversion. */
public final class EcologyModule {
    private final EcologyContent content = new EcologyContent();
    private final CultureConversion conversion = new CultureConversion(content::livingSubstrate);

    public void register(IEventBus modBus) {
        content.register(modBus);
    }

    public InteractionResult applyCulture(UseOnContext context) {
        return conversion.apply(context);
    }
}
