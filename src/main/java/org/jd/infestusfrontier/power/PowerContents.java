package org.jd.infestusfrontier.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.power.PowerStore;

record PowerContents(PowerStore store, List<ItemStack> items, boolean discharge, int ticks) {
    static final PowerContents EMPTY = new PowerContents(
            PowerStore.EMPTY, List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY), false, 0);
    static final Codec<PowerStore> STORE = RecordCodecBuilder.create(i -> i.group(
                    Codec.intRange(0, PowerStore.CAPACITY).fieldOf("heat").forGetter(PowerStore::heat),
                    Codec.intRange(0, PowerStore.CAPACITY).fieldOf("water").forGetter(PowerStore::water),
                    Codec.intRange(0, PowerStore.CAPACITY).fieldOf("steam").forGetter(PowerStore::steam),
                    Codec.intRange(0, PowerStore.CAPACITY).fieldOf("charge").forGetter(PowerStore::charge),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("cycles").forGetter(PowerStore::cycles))
            .apply(i, PowerStore::new));
    static final MapCodec<PowerContents> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    STORE.fieldOf("power").forGetter(PowerContents::store),
                    ItemStack.OPTIONAL_CODEC.listOf(4, 4).fieldOf("items").forGetter(PowerContents::items),
                    Codec.BOOL.fieldOf("discharge").forGetter(PowerContents::discharge),
                    Codec.intRange(0, ThermalHeartBlockEntity.PULSE_TICKS)
                            .optionalFieldOf("ticks", 0)
                            .forGetter(PowerContents::ticks))
            .apply(i, PowerContents::new));

    PowerContents(PowerStore store, List<ItemStack> items, boolean discharge) {
        this(store, items, discharge, 0);
    }

    PowerContents {
        items = items.stream().map(ItemStack::copy).toList();
    }

    java.util.Optional<String> problem() {
        if (items.size() != 4 || items.stream().anyMatch(stack -> stack.getCount() > 1))
            return java.util.Optional.of("power slots must each hold at most one item");
        return java.util.Optional.empty();
    }

    @Override
    public List<ItemStack> items() {
        return items.stream().map(ItemStack::copy).toList();
    }
}
