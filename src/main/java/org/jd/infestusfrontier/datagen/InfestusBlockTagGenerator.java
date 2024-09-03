package org.jd.infestusfrontier.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class InfestusBlockTagGenerator extends BlockTagsProvider {

    public InfestusBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, InfestusFrontier.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
//ex :  this.tag(BlockTags.NEEDS_IRON_TOOL).add(InfestusBlocks.ROTTEN_FLESH_BLOCK);
        this.tag(BlockTags.NEEDS_IRON_TOOL);
        this.tag(BlockTags.NEEDS_DIAMOND_TOOL);
        this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL);
        this.tag(BlockTags.NEEDS_STONE_TOOL);
        this.tag(BlockTags.MINEABLE_WITH_AXE);
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE);
        this.tag(BlockTags.MINEABLE_WITH_SHOVEL);
        this.tag(BlockTags.MINEABLE_WITH_HOE);




    }
}
