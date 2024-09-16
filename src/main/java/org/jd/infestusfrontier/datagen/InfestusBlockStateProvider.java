package org.jd.infestusfrontier.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
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
        simpleBlockWithItem(InfestusBlocks.CORRUPTION_CORE.get(), models().cube("corruption_core",
                blockTexture(InfestusBlocks.INFESTUS_NETWORK.get()), modLoc("block/corruption_core_top")
                , modLoc("block/corruption_core_side_1")
                , modLoc("block/corruption_core_side_1")
                , modLoc("block/corruption_core_side_2")
                , modLoc("block/corruption_core_side_2")));
        simpleBlockWithItem(InfestusBlocks.BIOMASS_RESERVOIR.get(), new ModelFile.UncheckedModelFile(modLoc("block/bio_reserve_001")));



    }



    private void registerOnlyState(Block block, String registry) {
        simpleBlock(block, getUncheckedModel(registry));
    }
    public static ModelFile getUncheckedModel(String registry) {
        return new ModelFile.UncheckedModelFile("infestusfrontier:block/" + registry);
    }
    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));

    }
    private void blockWithoutItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));

    }

}
