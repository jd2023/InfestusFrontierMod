package org.jd.infestusfrontier.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.item.InfestusItems;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class InfestusItemTagGenerator extends ItemTagsProvider {
    public InfestusItemTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> providerCompletableFuture, CompletableFuture<TagLookup<Block>> completableFuture, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, providerCompletableFuture, completableFuture, InfestusFrontier.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(ItemTags.TRIMMABLE_ARMOR)
                .add(InfestusItems.MEAT_CHESTPLATE.get(), InfestusItems.MEAT_HELMET.get()
                        , InfestusItems.MEAT_LEGGINGS.get(), InfestusItems.MEAT_BOOTS.get());
    }
}
