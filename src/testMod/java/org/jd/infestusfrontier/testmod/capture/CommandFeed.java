package org.jd.infestusfrontier.testmod.capture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Sends a scenario's commands to the world, one a tick, as the player. It holds the queue after a
 * {@code forceload add} until the server has those chunks loaded, so the {@code setblock} and {@code fill} commands
 * that follow never meet an unloaded position. A command that still reports one (the vanilla "That position is not
 * loaded") is a failure: {@link #check} throws, so the exhibit it would have built cannot go missing unseen.
 */
final class CommandFeed {
    private static final Pattern FORCELOAD_ADD =
            Pattern.compile("^forceload add (-?\\d+) (-?\\d+)(?: (-?\\d+) (-?\\d+))?$");
    private static final String UNLOADED = "argument.pos.unloaded";
    /** Ticks the server's reply to a command may take to reach the client. */
    private static final int REPLY_TICKS = 5;
    /** How many of the latest commands an error names: the reply lags the command that caused it. */
    private static final int RECENT = REPLY_TICKS + 1;

    private final Deque<String> queue = new ArrayDeque<>();
    private final Deque<String> recent = new ArrayDeque<>();
    private final List<String> unloaded = new ArrayList<>();
    private List<ChunkPos> awaited = List.of();
    private CompletableFuture<Boolean> loadCheck;
    private int quietTicks;

    CommandFeed() {
        NeoForge.EVENT_BUS.addListener(this::onChat);
    }

    void addAll(Collection<String> commands) {
        queue.addAll(commands);
        quietTicks = 0;
    }

    int size() {
        return queue.size();
    }

    /** Fails when a command's reply said a position was not loaded. */
    void check() {
        if (unloaded.isEmpty()) return;
        throw new IllegalStateException(
                "A command reported an unloaded position (" + UNLOADED + "); one of the latest commands: " + recent);
    }

    /**
     * Sends the next command unless chunks are still loading; true once the queue is empty and every reply is in.
     */
    boolean tick(Minecraft minecraft) {
        check();
        if (!awaited.isEmpty()) {
            if (!awaitedChunksLoaded(minecraft)) return false;
            awaited = List.of();
        }
        String command = queue.poll();
        if (command == null) return ++quietTicks > REPLY_TICKS;
        quietTicks = 0;
        FlatWorld.command(minecraft, command);
        recent.addLast(command);
        if (recent.size() > RECENT) recent.removeFirst();
        Matcher force = FORCELOAD_ADD.matcher(command);
        if (force.matches()) awaited = chunks(force);
        return false;
    }

    private static List<ChunkPos> chunks(Matcher force) {
        int x1 = Integer.parseInt(force.group(1));
        int z1 = Integer.parseInt(force.group(2));
        int x2 = force.group(3) == null ? x1 : Integer.parseInt(force.group(3));
        int z2 = force.group(4) == null ? z1 : Integer.parseInt(force.group(4));
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = Math.min(x1, x2) >> 4; x <= Math.max(x1, x2) >> 4; x++)
            for (int z = Math.min(z1, z2) >> 4; z <= Math.max(z1, z2) >> 4; z++) chunks.add(new ChunkPos(x, z));
        return chunks;
    }

    /** Asks the integrated server, on its own thread, whether the awaited chunks are loaded; never blocks. */
    private boolean awaitedChunksLoaded(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("A forceload needs the integrated server");
        if (loadCheck == null) {
            List<ChunkPos> chunks = awaited;
            loadCheck = server.submit(() -> {
                ServerLevel level = server.overworld();
                return chunks.stream().allMatch(chunk -> level.hasChunk(chunk.x, chunk.z));
            });
        }
        if (!loadCheck.isDone()) return false;
        boolean loaded = loadCheck.getNow(false);
        loadCheck = null;
        return loaded;
    }

    private void onChat(ClientChatReceivedEvent.System event) {
        if (!event.isOverlay() && names(event.getMessage()))
            unloaded.add(event.getMessage().getString());
    }

    private static boolean names(Component message) {
        if (message.getContents() instanceof TranslatableContents translatable
                && UNLOADED.equals(translatable.getKey())) return true;
        return message.getSiblings().stream().anyMatch(CommandFeed::names);
    }
}
