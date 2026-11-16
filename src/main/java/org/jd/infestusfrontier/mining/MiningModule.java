package org.jd.infestusfrontier.mining;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.slf4j.Logger;

/** Owner of mining: the Leaching Pod and the Leached Rock it leaves. */
public final class MiningModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);

    /** Ground a Leaching Pod may weaken; the pod also needs it soft enough, loose of fluid and free of a block entity. */
    public static final TagKey<Block> LEACHABLE = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "leachable"));

    /** Weakened ground: translucent, solid, breaks by hand and drops nothing. */
    public static final DeferredBlock<LeachedRockBlock> LEACHED_ROCK = BLOCKS.registerBlock(
            "leached_rock",
            LeachedRockBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(LeachedRockBlock.HARDNESS)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .lightLevel(LeachedRockBlock::light)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    public static final DeferredItem<BlockItem> LEACHED_ROCK_ITEM = ITEMS.registerSimpleBlockItem(LEACHED_ROCK);

    /** A one-use organ that leaches the ground below it. Grown from an Organ Bud; never picked up again. */
    public static final DeferredBlock<LeachingPodBlock> LEACHING_POD = BLOCKS.registerBlock(
            "leaching_pod",
            LeachingPodBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(LeachingPodBlock.HARDNESS, LeachingPodBlock.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> LEACHING_POD_ITEM = ITEMS.registerSimpleBlockItem(LEACHING_POD);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeachingPodBlockEntity>> LEACHING_POD_ENTITY =
            BLOCK_ENTITIES.register(
                    "leaching_pod",
                    () -> BlockEntityType.Builder.of(LeachingPodBlockEntity::new, LEACHING_POD.get())
                            .build(null));

    private MiningModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
