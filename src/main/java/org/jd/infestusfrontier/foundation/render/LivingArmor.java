package org.jd.infestusfrontier.foundation.render;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client side of living armor: vanilla armor (its layer, dye, trims and glint drawn by the vanilla armor layer) with
 * growths on top, drawn by {@link LivingArmorLayer} on every humanoid renderer. A module calls {@link #register} on
 * the client with each set's id and its four pieces; a set's growths are read on first use and dropped when
 * resources reload.
 */
public final class LivingArmor {
    private static final int WHITE = 0xFFFFFF;
    /** Which set each piece belongs to, filled when the items exist. */
    private static final Map<Item, ResourceLocation> SETS = new HashMap<>();

    private static final Map<ResourceLocation, LivingArmorModel> MODELS = new HashMap<>();
    private static boolean shared;

    private LivingArmor() {}

    /** The set {@code id} reads its growths from {@code assets/<ns>/armor/<path>.json}. */
    @SafeVarargs
    public static void register(IEventBus modBus, ResourceLocation id, Supplier<? extends Item>... pieces) {
        modBus.addListener((RegisterClientExtensionsEvent event) -> {
            Item[] items = Arrays.stream(pieces).map(Supplier::get).toArray(Item[]::new);
            for (Item item : items) SETS.put(item, id);
            event.registerItem(UNDYED_IS_WHITE, items);
        });
        // the icons' layer0 is the dyeable part, tinted as vanilla tints leather; layer1 keeps its colours
        modBus.addListener((RegisterColorHandlersEvent.Item event) -> event.register(
                (stack, tint) -> tint == 0 ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, WHITE)) : -1,
                Arrays.stream(pieces).map(Supplier::get).toArray(Item[]::new)));
        if (shared) return;
        shared = true;
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) resources -> MODELS.clear()));
        modBus.addListener(LivingArmor::addLayers);
    }

    /** The growths model for a piece of living armor, or null for any other item. */
    static @Nullable LivingArmorModel model(Item item) {
        ResourceLocation id = SETS.get(item);
        if (id == null) return null;
        return MODELS.computeIfAbsent(
                id,
                key -> new LivingArmorModel(
                        ArmorGrowths.load(Minecraft.getInstance().getResourceManager(), key)));
    }

    /** Every humanoid renderer, players' included, gets the growth layer beside its vanilla armor layer. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins())
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer)
                renderer.addLayer(new LivingArmorLayer(renderer));
        for (EntityType<?> type : event.getEntityTypes())
            if (event.getRenderer(type) instanceof LivingEntityRenderer renderer
                    && renderer.getModel() instanceof HumanoidModel) renderer.addLayer(new LivingArmorLayer(renderer));
    }

    /** Undyed, a set keeps its own colours instead of vanilla's leather brown. */
    private static final IClientItemExtensions UNDYED_IS_WHITE = new IClientItemExtensions() {
        @Override
        public int getDefaultDyeColor(ItemStack stack) {
            return FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, WHITE));
        }
    };
}
