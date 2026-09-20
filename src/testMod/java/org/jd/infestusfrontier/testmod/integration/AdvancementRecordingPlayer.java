package org.jd.infestusfrontier.testmod.integration;

import com.mojang.authlib.GameProfile;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Server fake whose advancement sink records awards instead of discarding them. */
public final class AdvancementRecordingPlayer extends FakePlayer {
    private final Set<ResourceLocation> awards = new HashSet<>();
    private final PlayerAdvancements advancements;

    public AdvancementRecordingPlayer(ServerLevel level, GameProfile profile) {
        super(level, profile);
        var server = level.getServer();
        advancements = new FakePlayerAdvancements(server.getFixerUpper(), server.getPlayerList(),
                server.getAdvancements(), Path.of("advancement-recording-player.dat"), this) {
            @Override
            public boolean award(AdvancementHolder advancement, String criterion) {
                return "observed".equals(criterion) && awards.add(advancement.id());
            }
        };
    }

    @Override
    public PlayerAdvancements getAdvancements() {
        return advancements == null ? super.getAdvancements() : advancements;
    }

    public boolean wasAwarded(ResourceLocation advancement) {
        return awards.contains(advancement);
    }
}
