package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.client.render.page.PageRendererRegistry;
import com.klikli_dev.modonomicon.data.LoaderRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

/** The guide's own page types. Data under {@code modonomicon/books} names them by {@link OrganBatchPage#TYPE}. */
public final class GuidePages {
    private GuidePages() {}

    public static void register(IEventBus modBus) {
        LoaderRegistry.registerPageLoader(OrganBatchPage.TYPE, OrganBatchPage::fromJson, OrganBatchPage::fromNetwork);
        if (FMLEnvironment.dist == Dist.CLIENT)
            modBus.addListener((FMLClientSetupEvent event) -> PageRendererRegistry.registerPageRenderer(
                    OrganBatchPage.TYPE, page -> new OrganBatchPageRenderer((OrganBatchPage) page)));
    }
}
