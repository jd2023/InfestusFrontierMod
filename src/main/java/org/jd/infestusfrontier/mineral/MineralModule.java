package org.jd.infestusfrontier.mineral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.mineral.Metal;
import org.jd.infestusfrontier.core.mineral.Parcel;

/**
 * Owner of the mineral items of the automated workshop: the three dusts, Mineral Tailings with their {@code parcel}
 * state, and Rock Residue. The Mineral Gizzard that makes them and the Bio-Furnace that smelts the dust are
 * {@code processing} organs; this module has no recipe that makes dust, Tailings or residue from anything else.
 */
public final class MineralModule {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> ROCK_RESIDUE = ITEMS.registerSimpleItem("rock_residue");

    /** What a Tailings still holds: its metal and its residual units. A stack without it is not a Tailings anything uses. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Parcel>> PARCEL =
            COMPONENTS.registerComponentType(
                    "parcel",
                    builder -> builder.persistent(parcelCodec())
                            .networkSynchronized(ByteBufCodecs.fromCodec(parcelCodec())));

    public static final DeferredItem<TailingsItem> MINERAL_TAILINGS =
            ITEMS.register("mineral_tailings", () -> new TailingsItem(new Item.Properties()));

    private MineralModule() {}

    /** The dust of {@code metal}. */
    public static Item dust(Metal metal) {
        return switch (metal) {
            case IRON -> IRON_DUST.get();
            case GOLD -> GOLD_DUST.get();
            case COPPER -> COPPER_DUST.get();
        };
    }

    /** One Mineral Tailings holding {@code parcel}. */
    public static ItemStack tailings(Parcel parcel) {
        ItemStack stack = new ItemStack(MINERAL_TAILINGS.get());
        stack.set(PARCEL.get(), parcel);
        return stack;
    }

    /** The Tailings a Creative player can pick: one fresh grind of each metal. */
    public static List<ItemStack> freshTailings() {
        List<ItemStack> all = new ArrayList<>();
        for (Metal metal : Metal.values()) all.add(tailings(Parcel.ground(metal)));
        return all;
    }

    /** {@code {"metal": "iron", "units": 4}}; a metal the workshop does not know or units out of range fail to decode. */
    private static Codec<Parcel> parcelCodec() {
        return RecordCodecBuilder.<Raw>create(instance -> instance.group(
                                Codec.STRING.fieldOf("metal").forGetter(Raw::metal),
                                Codec.INT.fieldOf("units").forGetter(Raw::units))
                        .apply(instance, Raw::new))
                .flatXmap(
                        Raw::toParcel,
                        parcel -> DataResult.success(new Raw(parcel.metal().id(), parcel.units())));
    }

    private record Raw(String metal, int units) {
        DataResult<Parcel> toParcel() {
            return Metal.of(metal)
                    .map(known -> {
                        try {
                            return DataResult.success(new Parcel(known, units));
                        } catch (IllegalArgumentException e) {
                            return DataResult.<Parcel>error(e::getMessage);
                        }
                    })
                    .orElseGet(() -> DataResult.error(() -> "No metal " + metal));
        }
    }

    /** The tint of the metal flecks on a Tailings: the colour of the metal in its parcel. */
    private static int fleck(ItemStack stack, int layer) {
        Parcel parcel = stack.get(PARCEL.get());
        if (layer != 1 || parcel == null) return 0xFFFFFFFF;
        return switch (parcel.metal()) {
            case IRON -> 0xFFC6C8CE;
            case GOLD -> 0xFFE8C854;
            case COPPER -> 0xFFC8603A;
        };
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT)
            modBus.addListener((RegisterColorHandlersEvent.Item event) ->
                    event.register(MineralModule::fleck, MINERAL_TAILINGS.get()));
    }
}
