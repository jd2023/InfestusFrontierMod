package org.jd.infestusfrontier.processing.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BowlSnapshotPayload(int menuId, CultureBowlMenuSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<BowlSnapshotPayload> TYPE = new Type<>(ResourceLocation.parse("infestusfrontier:processing/bowl_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BowlSnapshotPayload> CODEC = new StreamCodec<>() {
        @Override public BowlSnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BowlSnapshotPayload(buffer.readVarInt(), CultureBowlMenuSnapshot.CODEC.decode(buffer));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, BowlSnapshotPayload value) {
            buffer.writeVarInt(value.menuId);
            CultureBowlMenuSnapshot.CODEC.encode(buffer, value.snapshot);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
