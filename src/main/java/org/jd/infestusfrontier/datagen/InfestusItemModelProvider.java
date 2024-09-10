package org.jd.infestusfrontier.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.item.InfestusItems;

public class InfestusItemModelProvider extends ItemModelProvider {
    public InfestusItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, InfestusFrontier.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItem(InfestusItems.EYE_OF_CORRUPTION);
        simpleItem(InfestusItems.FERMENTED_FLESH);
    }
    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(InfestusFrontier.MODID,"item/" + item.getId().getPath()));
    }
}
