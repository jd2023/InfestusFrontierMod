package org.jd.infestusfrontier.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.InfestusBlocks;
import org.jd.infestusfrontier.block.custom.CorruptionCoreBlock;
import org.jd.infestusfrontier.block.custom.CorruptionPodBlock;

public class InfestusBlockStateProvider extends BlockStateProvider {
    public InfestusBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, InfestusFrontier.MODID, exFileHelper);
    }
    @Override
    protected void registerStatesAndModels() {
        blockWithItem(InfestusBlocks.FERMENTED_FLESH_BLOCK);
        blockWithItem(InfestusBlocks.ROTTEN_FLESH_BLOCK);
        blockWithItem(InfestusBlocks.BIOMASS_CRYSTAL_BLOCK);
        simpleBlockWithItem(InfestusBlocks.CORRUPTION_CORE.get(), models().cube(
                CorruptionCoreBlock.ID,
                modLoc("block/corruption_core_top"),
                modLoc("block/corruption_core_top"),
                modLoc("block/corruption_core_side_1"),
                modLoc("block/corruption_core_side_1"),
                modLoc("block/corruption_core_side_2"),
                modLoc("block/corruption_core_side_2")
        ));
        simpleBlockWithItem(InfestusBlocks.CORRUPTION_POD.get(), models().cube(
                CorruptionPodBlock.ID,
                modLoc("block/corruption_pod_top"),
                modLoc("block/corruption_pod_top"),
                modLoc("block/corruption_pod_side"),
                modLoc("block/corruption_pod_side"),
                modLoc("block/corruption_pod_side"),
                modLoc("block/corruption_pod_side")
        ));

        simpleBlockWithItem(InfestusBlocks.BIOMASS_RESERVOIR.get(), getUncheckedModel("bio_reserve_1"));
        simpleBlockWithItem(InfestusBlocks.MUTATION_POOL.get(), getUncheckedModel("mutation_pool"));

        blockWithItem(InfestusBlocks.DEAD_CORRUPTED_BLOCK);
        blockWithItem(InfestusBlocks.BASIC_CORRUPTED_BLOCK);
        blockWithItem(InfestusBlocks.DENSE_CORRUPTED_BLOCK);
        blockWithItem(InfestusBlocks.ADVANCE_CORRUPTED_BLOCK);
        blockWithItem(InfestusBlocks.FINAL_CORRUPTED_BLOCK);

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
