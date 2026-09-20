package org.jd.infestusfrontier.processing.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jd.infestusfrontier.interaction.api.IntentAdmission;
import org.jd.infestusfrontier.interaction.api.ProbeAccess;
import org.jd.infestusfrontier.processing.ProcessingModule;

/** One player's bounded view of one loaded Bowl. */
public final class CultureBowlMenu extends AbstractContainerMenu {
    private final BlockPos targetPosition;
    private final ServerPlayer serverPlayer;
    private final BowlMenuTarget target;
    private final IntentAdmission playerAdmission = new IntentAdmission();
    private CultureBowlMenuSnapshot snapshot;
    private CultureBowlMenuSnapshot lastSent;
    private BowlRefusal pendingRefusal = BowlRefusal.NONE;
    private long lastSnapshotTick = Long.MIN_VALUE;

    public CultureBowlMenu(int id, Inventory inventory, BlockPos position, CultureBowlMenuSnapshot initial) {
        super(ProcessingModule.BOWL_MENU.get(), id);
        this.targetPosition = position.immutable();
        this.snapshot = initial;
        this.lastSent = initial;
        this.serverPlayer = null;
        this.target = null;
    }

    public CultureBowlMenu(int id, Inventory inventory, BowlMenuTarget target) {
        super(ProcessingModule.BOWL_MENU.get(), id);
        this.targetPosition = target.menuPosition().immutable();
        this.serverPlayer = (ServerPlayer) inventory.player;
        this.target = target;
        this.snapshot = target.menuSnapshot(BowlRefusal.NONE);
        this.lastSent = snapshot;
        this.lastSnapshotTick = Integer.toUnsignedLong(serverPlayer.server.getTickCount());
    }

    public BlockPos targetPosition() { return targetPosition; }
    public CultureBowlMenuSnapshot snapshot() { return snapshot; }

    public void acceptSnapshot(CultureBowlMenuSnapshot update) {
        if (serverPlayer == null) snapshot = update;
    }

    public void submit(BowlIntentPayload.Intent intent, int slot, String recipe) {
        PacketDistributor.sendToServer(new BowlIntentPayload(containerId, targetPosition, snapshot.revision(), intent, slot, recipe));
    }

    public void handleIntent(ServerPlayer player, BowlIntentPayload payload) {
        long tick = Integer.toUnsignedLong(player.server.getTickCount());
        if (!playerAdmission.take(tick)) {
            pendingRefusal = BowlRefusal.RATE_LIMITED;
        } else if (!BowlIntentTraffic.take(player.server)) {
            pendingRefusal = BowlRefusal.SERVER_BUSY;
        } else if (!payload.target().equals(targetPosition)) {
            pendingRefusal = BowlRefusal.REMOTE_TARGET;
        } else {
            pendingRefusal = authorize(player);
            if (pendingRefusal == BowlRefusal.NONE) {
                if (payload.intent() == BowlIntentPayload.Intent.EXTRACT_SLOT && !getCarried().isEmpty()) {
                    pendingRefusal = BowlRefusal.CARRIED_STACK;
                } else {
                    var result = target.applyMenuIntent(payload);
                    pendingRefusal = result.refusal();
                    if (!result.extracted().isEmpty()) setCarried(result.extracted());
                }
            }
        }
        broadcastChanges();
    }

    private BowlRefusal authorize(ServerPlayer player) {
        boolean loaded = player.serverLevel().hasChunkAt(targetPosition);
        boolean sameDimension = target != null && target.isPresentIn(player.serverLevel());
        var refusal = ProbeAccess.check(target == null ? null : target.menuOwner(), player.getUUID(), loaded, sameDimension,
                Vec3.atCenterOf(targetPosition).distanceToSqr(player.getEyePosition()), player.blockInteractionRange() + 1.0);
        return switch (refusal) {
            case NONE -> BowlRefusal.NONE;
            case WRONG_OWNER -> BowlRefusal.WRONG_OWNER;
            case REMOTE_TARGET -> BowlRefusal.REMOTE_TARGET;
            case UNLOADED_TARGET -> BowlRefusal.TARGET_UNLOADED;
            case OUT_OF_REACH -> BowlRefusal.OUT_OF_REACH;
            case MALFORMED -> BowlRefusal.REMOTE_TARGET;
        };
    }

    private BowlRefusal authorizeView(ServerPlayer player) {
        boolean loaded = player.serverLevel().hasChunkAt(targetPosition);
        boolean sameDimension = target != null && target.isPresentIn(player.serverLevel());
        var refusal = ProbeAccess.check(player.getUUID(), player.getUUID(), loaded, sameDimension,
                Vec3.atCenterOf(targetPosition).distanceToSqr(player.getEyePosition()), player.blockInteractionRange() + 1.0);
        return switch (refusal) {
            case NONE -> BowlRefusal.NONE;
            case REMOTE_TARGET -> BowlRefusal.REMOTE_TARGET;
            case UNLOADED_TARGET -> BowlRefusal.TARGET_UNLOADED;
            case OUT_OF_REACH -> BowlRefusal.OUT_OF_REACH;
            case MALFORMED, WRONG_OWNER -> BowlRefusal.REMOTE_TARGET;
        };
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null || target == null || !stillValid(serverPlayer)) return;
        var current = target.menuSnapshot(pendingRefusal);
        snapshot = current;
        if (current.equals(lastSent)) return;
        long tick = Integer.toUnsignedLong(serverPlayer.server.getTickCount());
        if (tick - lastSnapshotTick < 10) return;
        PacketDistributor.sendToPlayer(serverPlayer, new BowlSnapshotPayload(containerId, current));
        lastSent = current;
        lastSnapshotTick = tick;
    }

    @Override public boolean stillValid(Player player) {
        return serverPlayer == null || (player == serverPlayer && authorizeView(serverPlayer) == BowlRefusal.NONE);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
