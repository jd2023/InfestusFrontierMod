package org.jd.infestusfrontier.logistics;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jd.infestusfrontier.core.logistics.NetworkIndex;
import org.jetbrains.annotations.Nullable;

/** Passive access to loaded fluid endpoints. A simulated offer reserves admission for its matching execution. */
final class NetworkFluid implements IFluidHandler {
    private static final int OFFER_LIMIT = 16;
    private static final int TRANSFER_LIMIT = 250;
    private final ServerLevel level;
    private final BlockPos pos;
    private final @Nullable Direction side;
    private Optional<Offer> prepared = Optional.empty();

    private record Offer(
            NetworkTopology.Port port,
            FluidStack fluid,
            boolean fill,
            long tick,
            NetworkIndex.Component<NetworkTopology.Node> network,
            int next) {}

    NetworkFluid(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        this.level = level;
        this.pos = pos.immutable();
        this.side = side;
    }

    private boolean carries(FluidStack fluid) {
        if (!level.hasChunkAt(pos)) return false;
        return NetworkTopology.tissue(level.getBlockState(pos))
                .map(tissue -> tissue.carrier().carries(fluid))
                .orElse(false);
    }

    private @Nullable IFluidHandler target(NetworkTopology.Port port) {
        if (!level.hasChunkAt(port.pos()) || Routes.isRoute(level.getBlockState(port.pos()))) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, port.pos(), port.side());
    }

    private FluidStack transfer(FluidStack requested, int maximum, boolean fill, FluidAction action) {
        if (maximum <= 0 || !NetworkWork.enter(level)) return FluidStack.EMPTY;
        try {
            NetworkTopology topology = NetworkTopology.get(level);
            Optional<NetworkIndex.Component<NetworkTopology.Node>> found = topology.network(level, pos, side);
            if (found.isEmpty() || found.get().status() != NetworkIndex.Status.READY) return FluidStack.EMPTY;
            NetworkIndex.Component<NetworkTopology.Node> network = found.get();
            long now = level.getGameTime();
            Optional<Offer> offer = prepared.filter(p -> action.execute()
                    && p.network() == network
                    && p.tick() == now
                    && p.fill() == fill
                    && (requested.isEmpty() || FluidStack.isSameFluidSameComponents(requested, p.fluid())));
            if (offer.isEmpty()) {
                if (!NetworkWork.take(level)) return FluidStack.EMPTY;
                prepared = Optional.empty();
                List<NetworkTopology.Port> ports = topology.endpoints(network);
                int size = ports.size();
                int start = size == 0 ? 0 : Math.floorMod(topology.turn(network), size);
                for (int step = 0; step < Math.min(size, OFFER_LIMIT); step++) {
                    int index = (start + step) % size;
                    NetworkTopology.Port port = ports.get(index);
                    if (side != null && port.pos().equals(pos.relative(side))) continue;
                    IFluidHandler handler = target(port);
                    if (handler == null) continue;
                    FluidStack candidate;
                    if (fill)
                        candidate = requested.copyWithAmount(
                                handler.fill(requested.copyWithAmount(maximum), FluidAction.SIMULATE));
                    else
                        candidate = requested.isEmpty()
                                ? handler.drain(maximum, FluidAction.SIMULATE)
                                : handler.drain(requested.copyWithAmount(maximum), FluidAction.SIMULATE);
                    if (candidate.isEmpty() || !carries(candidate)) continue;
                    offer = Optional.of(new Offer(port, candidate.copy(), fill, now, network, index + 1));
                    break;
                }
                if (offer.isEmpty()) {
                    topology.turn(network, start + OFFER_LIMIT);
                    return FluidStack.EMPTY;
                }
                prepared = offer;
            }
            Offer selected = offer.orElseThrow();
            FluidStack amount = selected.fluid()
                    .copyWithAmount(Math.min(maximum, selected.fluid().getAmount()));
            if (action.simulate()) return amount;
            prepared = Optional.empty();
            IFluidHandler handler = target(selected.port());
            if (handler == null) return FluidStack.EMPTY;
            topology.turn(network, selected.next());
            return fill
                    ? amount.copyWithAmount(handler.fill(amount, FluidAction.EXECUTE))
                    : handler.drain(amount, FluidAction.EXECUTE);
        } finally {
            NetworkWork.leave(level);
        }
    }

    @Override
    public int getTanks() {
        return 0;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && carries(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !carries(resource)) return 0;
        return transfer(resource, Math.min(TRANSFER_LIMIT, resource.getAmount()), true, action)
                .getAmount();
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !carries(resource)) return FluidStack.EMPTY;
        return transfer(resource, Math.min(TRANSFER_LIMIT, resource.getAmount()), false, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return transfer(FluidStack.EMPTY, Math.min(TRANSFER_LIMIT, maxDrain), false, action);
    }
}
