package org.jd.infestusfrontier.testmod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Real server data reloads for tests: a world data pack that overrides the Membrane Rack's sheet recipe is selected and
 * removed again through the server's own pack repository and {@link MinecraftServer#reloadResources}. A test that uses
 * it runs in a batch of its own, so no other test sees the changed recipe, and removes the pack before it ends; the
 * batch's after-hook removes it again if the test failed first.
 */
final class RecipeReloads {
    private static final String PACK = "infestus_recipe_reload_test";
    private static final String PACK_ID = "file/" + PACK;
    private static final String RECIPE = "data/infestusfrontier/recipe/preparing/membrane_sheet.json";

    private RecipeReloads() {}

    /** Reloads the data as it is. */
    static CompletableFuture<Void> reload(MinecraftServer server) {
        return reloadWith(server, selected(server));
    }

    /** Reloads with the sheet recipe asking {@code waterMb} of water instead of the shipped 100. */
    static CompletableFuture<Void> reloadWithWater(MinecraftServer server, int waterMb) {
        Path pack = pack(server);
        try {
            Files.createDirectories(pack.resolve(RECIPE).getParent());
            Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":48,\"description\":\"test\"}}");
            Files.writeString(pack.resolve(RECIPE), recipe(waterMb));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.getPackRepository().reload();
        List<String> ids = selected(server);
        if (!ids.contains(PACK_ID)) ids.add(PACK_ID);
        return reloadWith(server, ids);
    }

    /** Deletes the pack and reloads without it; a no-op reload when it is not there. */
    static CompletableFuture<Void> restore(MinecraftServer server) {
        Path pack = pack(server);
        if (Files.exists(pack))
            try (Stream<Path> files = Files.walk(pack)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        server.getPackRepository().reload();
        List<String> ids = selected(server);
        ids.remove(PACK_ID);
        return reloadWith(server, ids);
    }

    private static CompletableFuture<Void> reloadWith(MinecraftServer server, List<String> ids) {
        return server.reloadResources(ids);
    }

    private static List<String> selected(MinecraftServer server) {
        return new ArrayList<>(server.getPackRepository().getSelectedIds());
    }

    private static Path pack(MinecraftServer server) {
        return server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK);
    }

    private static String recipe(int waterMb) {
        return """
                {
                  "type": "infestusfrontier:organ_batch",
                  "organ": "membrane_rack",
                  "ingredients": [{"item": "minecraft:rotten_flesh"}, {"item": "minecraft:string"}],
                  "water": %d,
                  "biomass": 0,
                  "result": {"id": "infestusfrontier:membrane_sheet", "count": 1},
                  "ticks": 400
                }
                """.formatted(waterMb);
    }
}
