package org.jd.infestusfrontier.equipment;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The permanent branch a living tool has taken (ITEM_CATALOG I016): one per tool, installed by a graft in the Cradle,
 * never removed. Carried on the item as {@link EquipmentModule#TOOL_MUTATION}.
 */
public enum ToolMutation implements StringRepresentable {
    /** Contractile Fiber: the tool breaks the blocks around the one it mines, {@code SPREAD_COST} BU each. */
    CONTRACTILE("contractile");

    public static final Codec<ToolMutation> CODEC = StringRepresentable.fromEnum(ToolMutation::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolMutation> STREAM_CODEC =
            StreamCodec.of((buf, mutation) -> buf.writeEnum(mutation), buf -> buf.readEnum(ToolMutation.class));

    private final String name;

    ToolMutation(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The tooltip line naming this branch. */
    public String translationKey() {
        return "item.infestusfrontier.living_tool.mutation." + name;
    }
}
