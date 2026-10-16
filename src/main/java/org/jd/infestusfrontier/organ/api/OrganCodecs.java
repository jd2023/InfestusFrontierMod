package org.jd.infestusfrontier.organ.api;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jd.infestusfrontier.core.organ.OrganHistory;

/** Serialization of what an organ remembers. */
public final class OrganCodecs {
    public static final Codec<OrganHistory> HISTORY = Codec.intRange(0, Integer.MAX_VALUE)
            .fieldOf("completed_batches")
            .codec()
            .xmap(OrganHistory::new, OrganHistory::completedBatches);
    public static final StreamCodec<ByteBuf, OrganHistory> HISTORY_STREAM = ByteBufCodecs.fromCodec(HISTORY);

    private OrganCodecs() {}
}
