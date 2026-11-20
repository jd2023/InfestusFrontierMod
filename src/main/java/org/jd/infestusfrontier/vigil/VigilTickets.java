package org.jd.infestusfrontier.vigil;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.vigil.Territory;

/** The sole chunk-ticket owner. The server attachment holds references, never fuel or paid time. */
final class VigilTickets {
    static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "vigil"), VigilTickets::validate);

    private record Owner(ResourceKey<Level> dimension, BlockPos pos) {}

    private int startupInspected;
    private final Map<Owner, Territory> admitted = new HashMap<>();

    static VigilTickets forLevel(ServerLevel level) {
        return level.getServer().overworld().getData(VigilModule.TERRITORIES);
    }

    boolean admit(ServerLevel level, BlockPos pos, Territory territory) {
        Owner owner = new Owner(level.dimension(), pos.immutable());
        Territory previous = admitted.get(owner);
        if (previous != null) return previous.equals(territory);
        int dimension = 0;
        int server = 0;
        for (var entry : admitted.entrySet()) {
            server = Math.addExact(server, entry.getValue().chunks());
            if (entry.getKey().dimension().equals(level.dimension()))
                dimension = Math.addExact(dimension, entry.getValue().chunks());
        }
        if (!territory.fits(dimension, server)) return false;
        admitted.put(owner, territory);
        return true;
    }

    void acquire(ServerLevel level, BlockPos pos, Territory territory) {
        for (int i = 0; i < 9; i++)
            if (territory.includes(i)) {
                ChunkPos chunk = chunk(pos, i);
                CONTROLLER.forceChunk(level, pos, chunk.x, chunk.z, true, true);
            }
    }

    void release(ServerLevel level, BlockPos pos) {
        Territory territory = admitted.remove(new Owner(level.dimension(), pos));
        if (territory == null) return;
        for (int i = 0; i < 9; i++)
            if (territory.includes(i)) {
                ChunkPos chunk = chunk(pos, i);
                CONTROLLER.forceChunk(level, pos, chunk.x, chunk.z, false, true);
            }
    }

    static ChunkPos chunk(BlockPos pos, int index) {
        ChunkPos centre = new ChunkPos(pos);
        return new ChunkPos(Math.addExact(centre.x, index % 3 - 1), Math.addExact(centre.z, index / 3 - 1));
    }

    private static void validate(ServerLevel level, TicketHelper helper) {
        // No entity may own tickets. Keepers never choose or expand territory.
        for (var id : helper.getEntityTickets().keySet()) helper.removeAllTickets(id);
        VigilTickets tickets = forLevel(level);
        int inspected = 0;
        for (var entry : helper.getBlockTickets().entrySet()) {
            BlockPos pos = entry.getKey();
            var saved = entry.getValue();
            // Bound even explicit startup loads before reading an owner's block entity.
            if (tickets.startupInspected >= Territory.SERVER_CHUNKS
                    || inspected >= Territory.DIMENSION_CHUNKS
                    || saved.ticking().size() > 9
                    || saved.ticking().isEmpty()
                    || !saved.nonTicking().isEmpty()) {
                helper.removeAllTickets(pos);
                continue;
            }
            inspected = Math.incrementExact(inspected);
            tickets.startupInspected = Math.incrementExact(tickets.startupInspected);
            int mask = 0;
            for (int i = 0; i < 9; i++)
                if (saved.ticking().contains(chunk(pos, i).toLong())) mask |= 1 << i;
            if (Integer.bitCount(mask) != saved.ticking().size() || (mask & Territory.CENTRE) == 0) {
                helper.removeAllTickets(pos);
                continue;
            }
            Territory territory = new Territory(mask);
            if (!tickets.admit(level, pos, territory)) {
                helper.removeAllTickets(pos);
                continue;
            }
            // Explicit bounded service exception: load only the already-admitted owner's chunk to validate its fuel.
            level.getChunkAt(pos);
            if (!(level.getBlockEntity(pos) instanceof VigilNestBlockEntity nest) || !nest.resume(territory)) {
                tickets.admitted.remove(new Owner(level.dimension(), pos));
                helper.removeAllTickets(pos);
            }
        }
    }
}
