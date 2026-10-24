package org.jd.infestusfrontier.organ;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.organ.api.OrganCodecs;
import org.jd.infestusfrontier.organ.api.Organs;

/** Owner of organ identity and history. An organ's history travels on its item when it is broken and placed again. */
public final class OrganModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OrganHistory>> HISTORY =
            COMPONENTS.registerComponentType(
                    "organ_history",
                    builder -> builder.persistent(OrganCodecs.HISTORY).networkSynchronized(OrganCodecs.HISTORY_STREAM));

    /** The saved bytes of an organ whose state could not be read, carried on its item unchanged. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> UNREADABLE_SAVE =
            COMPONENTS.registerComponentType(
                    "unreadable_save",
                    builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

    public static final DeferredBlock<Block> ORGAN_BUD = BLOCKS.registerBlock(
            "organ_bud",
            OrganBudBlock::new,
            Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> ORGAN_BUD_ITEM = ITEMS.registerSimpleBlockItem(ORGAN_BUD);

    private OrganModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
    }
}
