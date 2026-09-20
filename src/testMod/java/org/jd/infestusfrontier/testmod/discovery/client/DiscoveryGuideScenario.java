package org.jd.infestusfrontier.testmod.discovery.client;

import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.data.BookDataManager;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.discovery.GuideAccess;
import org.jd.infestusfrontier.testmod.integration.ContentRequirements;
import org.jd.infestusfrontier.testmod.integration.client.ContentGuideScenario;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Opens every eligible owner-contributed entry from the actual synchronized guide book. */
public final class DiscoveryGuideScenario implements ContentGuideScenario, ContentVisualScenario {
    private static final ResourceLocation FIRST_ENTRY = ResourceLocation.fromNamespaceAndPath(
            "infestusfrontier", "discovery/waking_genome");
    private final ArrayDeque<ContentRequirements.GuideRequirement> pending = new ArrayDeque<>(
            ContentRequirements.read().guideRequirements());
    private ContentRequirements.GuideRequirement opened;

    @Override
    public String assertion() {
        return "infestusfrontier_client:discovery.waking_genome.guide";
    }

    @Override
    public List<String> assertions() {
        return pending.stream().map(ContentRequirements.GuideRequirement::assertion).toList();
    }

    @Override
    public boolean ready(Minecraft minecraft) {
        return BookDataManager.get().areBooksBuilt()
                && BookDataManager.get().getBook(GuideAccess.BOOK_ID) != null;
    }

    @Override
    public List<UiView> uiViews() {
        return List.of(new UiView("waking-genome-guide.png", 3));
    }

    @Override
    public boolean prepareUi(Minecraft minecraft, UiView view) {
        if (minecraft.screen instanceof BookEntryScreen screen && screen.getEntry().getId().equals(FIRST_ENTRY)) {
            return true;
        }
        BookGuiManager.get().openEntry(GuideAccess.BOOK_ID, FIRST_ENTRY, 0);
        return false;
    }

    @Override
    public boolean tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) return false;
        if (opened != null) {
            if (!(minecraft.screen instanceof BookEntryScreen screen)
                    || !screen.getEntry().getId().equals(ResourceLocation.parse(opened.entry()))) {
                throw new IllegalStateException("Guide did not render contributed entry " + opened.entry());
            }
            pending.remove();
            opened = null;
        }
        var next = pending.peek();
        if (next == null) return true;
        var book = BookDataManager.get().getBook(GuideAccess.BOOK_ID);
        if (book == null || !BookDataManager.get().areBooksBuilt()) return false;
        var entryId = ResourceLocation.parse(next.entry());
        var entry = book.getEntry(entryId);
        if (entry == null || entry.getPages().isEmpty()) {
            throw new IllegalStateException("Guide is missing contributed entry pages " + next.entry());
        }
        BookGuiManager.get().openEntry(GuideAccess.BOOK_ID, entryId, 0);
        opened = next;
        return false;
    }
}
