package org.jd.infestusfrontier.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;

public class InfestusRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, InfestusFrontier.MODID);

    public static final RegistryObject<RecipeSerializer<MutationByMutationPoolRecipe>> MUTATION_POOL_RECIPE_SERIALIZER =
            SERIALIZERS.register("mutation_by_mutation_pool", ()->MutationByMutationPoolRecipe.Serializer.INSTANCE);
    public static void register(IEventBus bus){
        SERIALIZERS.register(bus);
    }
}
