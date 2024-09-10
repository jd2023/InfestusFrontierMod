package org.jd.infestusfrontier.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.InfestusBlocks;

public class InfestusBlockStateProvider extends BlockStateProvider {
    public InfestusBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, InfestusFrontier.MODID, exFileHelper);
    }
    @Override
    protected void registerStatesAndModels() {
        blockWithItem(InfestusBlocks.FERMENTED_FLESH_BLOCK);
        blockWithItem(InfestusBlocks.ROTTEN_FLESH_BLOCK);
        blockWithItem(InfestusBlocks.INFESTUS_NETWORK);



    }
    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));

    }
    private void blockWithoutItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));

    }

}
