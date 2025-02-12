package org.jd.infestusfrontier.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.InfestusFrontier;

public class MutationPoolScreen extends AbstractContainerScreen<MutationPoolMenu> {
    final int NUMBER_OF_SLOTS = 8;
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(InfestusFrontier.MODID, "textures/gui/gui.png");

    public MutationPoolScreen(MutationPoolMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected void init() {
        super.init();

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        inventoryLabelY=inventoryLabelY-18;
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight+16);

        renderProgressArrow(guiGraphics, x, y);
        for (int i =menu.slots.size()-NUMBER_OF_SLOTS; i<menu.slots.size();i++){
            renderCustomSlots(guiGraphics, x+menu.slots.get(i).x-1, y+menu.slots.get(i).y-1);
        }
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {

        guiGraphics.blit(TEXTURE, x + 102, y + 45, 199, 61, menu.getScaledProgress()-22, 15);
        guiGraphics.blit(TEXTURE, x +80, y + 45, 177, 77, menu.getScaledProgress(), 15);

    }
    private void renderCustomSlots(GuiGraphics guiGraphics, int x, int y) {

        guiGraphics.blit(TEXTURE, x, y, 1, 185, 18, 18);

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
