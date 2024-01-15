package org.jd.infestusfrontier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.checkerframework.checker.units.qual.C;
import org.jd.infestusfrontier.block.*;
import org.jd.infestusfrontier.item.ModCreativeModeTab;

import java.util.function.Supplier;

public class ZgBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, InfestusFrontier.MODID);
    public static final RegistryObject<Block> LIGHT_SPREADER = registerBlock(LightSpreader.ID,
            () -> new LightSpreader(BlockBehaviour.Properties.copy(Blocks.STONE)), ModCreativeModeTab.INFESTUS_TAB);
    public static final RegistryObject<Block> FLESH_BLOCK = registerBlock("flesh_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SHROOMLIGHT)), ModCreativeModeTab.INFESTUS_TAB);
    public static final RegistryObject<Block> CREEP = BLOCKS.register(Creep.ID, Creep::new);

    public static final RegistryObject<Block> CREEP_TWO = BLOCKS.register(CreepTwo.ID, CreepTwo::new);
    public static final RegistryObject<Block> NEST = registerBlock(Nest.ID,
            () -> new Nest(BlockBehaviour.Properties.of(Material.STONE)), ModCreativeModeTab.INFESTUS_TAB);
    public static final RegistryObject<Block> BIO_RESERVOIR = BLOCKS.register(BioReservoir.ID, BioReservoir::new);
    public static final RegistryObject<Block> TUMOR = BLOCKS.register(Tumor.ID, Tumor::new);

    private static  <T extends Block>RegistryObject<T> registerBlock(String name, Supplier<T> block, CreativeModeTab tab){
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn, tab);
        return toReturn;
    }
    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block,
                                                                            CreativeModeTab tab) {
        return ZgItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(tab)));
    }
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
