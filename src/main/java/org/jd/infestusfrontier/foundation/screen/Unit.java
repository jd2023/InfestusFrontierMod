package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.network.chat.Component;

/** How a tank writes its amount: a short form under the tank and a long form in the tooltip. */
public record Unit(String shortKey, String longKey) {
    public static final Unit BIOMASS = new Unit("gui.infestusfrontier.biomass_short", "gui.infestusfrontier.biomass");
    public static final Unit WATER = new Unit("gui.infestusfrontier.water_short", "gui.infestusfrontier.water");

    Component shortText(int value) {
        return Component.translatable(shortKey, value);
    }

    Component longText(int value, int max) {
        return Component.translatable(longKey, value, max);
    }
}
