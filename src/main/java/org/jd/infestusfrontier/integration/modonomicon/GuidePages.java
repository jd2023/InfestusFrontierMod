package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.client.render.page.PageRendererRegistry;
import com.klikli_dev.modonomicon.data.LoaderRegistry;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jd.infestusfrontier.InfestusFrontier;

/** The guide's own page types. Data under {@code modonomicon/books} names them by {@link OrganBatchPage#TYPE}. */
public final class GuidePages {
    private GuidePages() {}

    public static void register(IEventBus modBus) {
        LoaderRegistry.registerPageLoader(OrganBatchPage.TYPE, OrganBatchPage::fromJson, OrganBatchPage::fromNetwork);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((FMLClientSetupEvent event) -> PageRendererRegistry.registerPageRenderer(
                    OrganBatchPage.TYPE, page -> new OrganBatchPageRenderer((OrganBatchPage) page)));
            ResourceLocation cover =
                    ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "the_waking_genome");
            ModelResourceLocation model = ModelResourceLocation.standalone(cover.withPrefix("item/"));
            modBus.addListener((ModelEvent.RegisterAdditional event) -> event.register(model));
            // Modonomicon looks the cover up under the "inventory" variant, and NeoForge accepts side-loaded models
            // only as "standalone": load it standalone, then publish it under the key Modonomicon reads.
            modBus.addListener((ModelEvent.ModifyBakingResult event) -> event.getModels()
                    .put(
                            new ModelResourceLocation(cover, "inventory"),
                            event.getModels().get(model)));
        }
    }
}
