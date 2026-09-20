package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jd.infestusfrontier.discovery.GuideAccess;
import org.lwjgl.glfw.GLFW;

/** Client-only Modonomicon boundary; the key opens the guide without owning progress. */
@EventBusSubscriber(modid = "infestusfrontier", value = Dist.CLIENT)
final class DiscoveryClient {
    private static final KeyMapping OPEN_GUIDE = new KeyMapping(
            "key.infestusfrontier.open_guide", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "key.categories.infestusfrontier");

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUIDE);
        NeoForge.EVENT_BUS.addListener(DiscoveryClient::clientTick);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        while (minecraft.player != null && OPEN_GUIDE.consumeClick()) {
            BookGuiManager.get().openBook(BookAddress.defaultFor(GuideAccess.BOOK_ID));
        }
    }

    private DiscoveryClient() {}
}
