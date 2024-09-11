package org.jd.infestusfrontier.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.custom.CorruptionCoreBlock;
import org.jd.infestusfrontier.block.custom.CorruptionCoreBlock;
import org.jd.infestusfrontier.item.InfestusItems;

import java.util.function.Supplier;

public class InfestusBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, InfestusFrontier.MODID);

    public static final RegistryObject<Block> ROTTEN_FLESH_BLOCK = registerBlockWithItem("rotten_flesh_block",
            ()->new Block(BlockBehaviour.Properties.copy(Blocks.SLIME_BLOCK).strength(0.5f)));

    public static final RegistryObject<Block> FERMENTED_FLESH_BLOCK = registerBlockWithItem("fermented_flesh_block",
            ()->new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(1)));

    public static final RegistryObject<Block> INFESTUS_NETWORK = registerBlockWithItem("infestus_network",
            ()->new Block(BlockBehaviour.Properties.copy(Blocks.SCULK).strength(1).noLootTable()));
    public static final RegistryObject<Block> CORRUPTION_CORE = registerBlockWithItem("corruption_core",
            ()->new CorruptionCoreBlock(BlockBehaviour.Properties.copy(Blocks.SCULK_CATALYST).strength(1)));
    private static <T extends Block> RegistryObject<T> registerBlockWithItem(String name, Supplier<T> block){
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }
    private static <T extends Block> RegistryObject<T> registerBlockNoItem(String name, Supplier<T> block){
        return BLOCKS.register(name, block);
    }



    private static <T extends Block>RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block){
        return InfestusItems.ITEMS.register(name, ()->new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
