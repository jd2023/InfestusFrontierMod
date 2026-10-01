package org.jd.infestusfrontier.ecology;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.bus.api.IEventBus;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

/** Composition boundary for substrate content and deliberate conversion. */
public final class EcologyModule {
    private final EcologyContent content = new EcologyContent();
    private final CultureConversion conversion;

    public EcologyModule(DiscoveryObserver discovery) {
        conversion = new CultureConversion(content::livingSubstrate, discovery);
    }

    public void register(IEventBus modBus) {
        content.register(modBus);
    }

    public InteractionResult applyCulture(UseOnContext context) {
        return conversion.apply(context);
    }
}
