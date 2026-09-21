package org.jd.infestusfrontier.processing.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BowlIntentPayload(int menuId, BlockPos target, long revision, Intent intent, int slot, String recipe)
        implements CustomPacketPayload {
    public static final Type<BowlIntentPayload> TYPE = new Type<>(ResourceLocation.parse("infestusfrontier:processing/bowl_intent"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BowlIntentPayload> CODEC = new StreamCodec<>() {
        @Override public BowlIntentPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BowlIntentPayload(buffer.readVarInt(), buffer.readBlockPos(), buffer.readVarLong(),
                    buffer.readEnum(Intent.class), buffer.readVarInt(), buffer.readUtf(16));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, BowlIntentPayload value) {
            buffer.writeVarInt(value.menuId);
            buffer.writeBlockPos(value.target);
            buffer.writeVarLong(value.revision);
            buffer.writeEnum(value.intent);
            buffer.writeVarInt(value.slot);
            buffer.writeUtf(value.recipe, 16);
        }
    };
    public BowlIntentPayload {
        if (menuId < 0 || target == null || revision < 0 || intent == null || recipe == null || recipe.length() > 16) {
            throw new IllegalArgumentException("Invalid Bowl intent");
        }
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Intent { START, CANCEL, EXTRACT_SLOT }
}
