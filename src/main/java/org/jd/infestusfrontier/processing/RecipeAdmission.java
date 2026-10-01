package org.jd.infestusfrontier.processing;

import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import org.jd.infestusfrontier.processing.api.SharedRecipeCompletionAdmission;
import java.lang.ref.WeakReference;

/** Server identity, not dimension time, owns the completion budget. Weak keys do not retain stopped servers. */
public final class RecipeAdmission {
    private static final WeakHashMap<MinecraftServer, SharedRecipeCompletionAdmission> SERVERS = new WeakHashMap<>();
    public static boolean take(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, owner -> {
            var reference = new WeakReference<>(owner);
            return new SharedRecipeCompletionAdmission(() -> Integer.toUnsignedLong(reference.get().getTickCount()));
        }).take();
    }
    private RecipeAdmission() {}
}
