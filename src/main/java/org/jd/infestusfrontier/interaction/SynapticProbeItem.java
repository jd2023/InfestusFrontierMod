package org.jd.infestusfrontier.interaction;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.jd.infestusfrontier.interaction.api.ProbeAccess;
import org.jd.infestusfrontier.interaction.api.ProbeTarget;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

/** Interaction owns target selection and authorization; target features own their mutations. */
final class SynapticProbeItem extends Item {
    private final DiscoveryObserver discovery;

    SynapticProbeItem(Properties properties, DiscoveryObserver discovery) {
        super(properties);
        this.discovery = discovery;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof ProbeTarget target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;

        var owner = target.probeOwner();
        var refusal = ProbeAccess.check(owner == null ? player.getUUID() : owner, player.getUUID(), true, true,
                Vec3.atCenterOf(pos).distanceToSqr(player.getEyePosition()), player.blockInteractionRange() + 1.0);
        if (refusal != ProbeAccess.Refusal.NONE && refusal != ProbeAccess.Refusal.WRONG_OWNER) {
            player.displayClientMessage(Component.translatable("message.infestusfrontier.probe."
                    + refusal.name().toLowerCase(Locale.ROOT)), true);
            return InteractionResult.FAIL;
        }
        if (owner == null && !target.claimProbeOwner(player.getUUID())) return InteractionResult.FAIL;
        target.openProbeMenu(serverPlayer);
        discovery.complete(serverPlayer, DiscoveryObserver.Milestone.SYNAPTIC_PROBE);
        return InteractionResult.CONSUME;
    }
}
