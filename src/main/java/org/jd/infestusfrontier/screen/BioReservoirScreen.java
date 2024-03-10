package org.jd.infestusfrontier.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.entity.BioReservoirBlockEntity;

public class BioReservoirScreen extends AbstractContainerScreen<BioReservoirMenu> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(InfestusFrontier.MODID, "textures/gui/background.png");
    public int biomass =
            0;

    public BioReservoirScreen(BioReservoirMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 176;
        this.imageHeight = 184;

    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(PoseStack stack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        blit(stack, this.leftPos+140, this.topPos+16, 177, 94, 18, 56);
        blit(stack, this.leftPos+100, this.topPos+20, 177, 61, 22, 15);
        renderProgressArrow(stack, this.leftPos, this.topPos);
        renderBiomassStorage(stack, this.leftPos, this.topPos);
        blit(stack, this.leftPos+70, this.topPos+20, 1, 185, 18, 18);
//        this.font.draw(stack, "Progress: " + this.menu.data.get(0), this.leftPos+20, this.topPos+40, 0xFFFFFF);
//        this.font.draw(stack, "Biomass Stored: " + this.menu.data.get(2), this.leftPos+60, this.topPos+50, 0xFFFFFF);
    }
    private void renderBiomassStorage(PoseStack stack, int x, int y) {

        int scaledHeight = (int) this.menu.data.get(2) * 50 / 70;
        blit(stack, x +142, y+68-scaledHeight, 196, 97, 12, scaledHeight);
    }

    private void renderProgressArrow(PoseStack stack, int x, int y) {
        if (menu.isCrafting()) {
            blit(stack, x +100, y+20, 177, 77, menu.getScaledProgress(), 16);
        }
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float delta) {
        renderBackground(stack);
        super.render(stack, mouseX, mouseY, delta);
        renderTooltip(stack, mouseX, mouseY);
    }
}

