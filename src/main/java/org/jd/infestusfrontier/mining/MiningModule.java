package org.jd.infestusfrontier.mining;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.organ.api.Organs;
import org.slf4j.Logger;

/**
 * Owner of mining: the Leaching Pod and the Leached Rock it leaves, and the Tunnel Root with its Root Segments and the
 * root cells it lays.
 */
public final class MiningModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

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

    /** Plain ground a Tunnel Root may replace with a root cell in the floor of its tunnel. */
    public static final TagKey<Block> TUNNEL_FLOOR = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "tunnel_floor"));

    /** What a head lays: one segment is one root cell. Loaded into the head by hand. */
    public static final DeferredItem<Item> ROOT_SEGMENT = ITEMS.registerSimpleItem("root_segment");

    /** A cell of a root in a tunnel floor. Grown tissue: it never moves and breaking it gives nothing back. */
    public static final DeferredBlock<RootCellBlock> ROOT_CELL = BLOCKS.registerBlock(
            "root_cell",
            RootCellBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(RootCellBlock.HARDNESS)
                    .sound(SoundType.ROOTED_DIRT));

    public static final DeferredItem<BlockItem> ROOT_CELL_ITEM = ITEMS.registerSimpleBlockItem(ROOT_CELL);

    /** The head of a Tunnel Root: grown from an Organ Bud, fed and steered by hand, it cuts a 1x2 tunnel. */
    public static final DeferredBlock<TunnelRootBlock> TUNNEL_ROOT = BLOCKS.registerBlock(
            "tunnel_root",
            TunnelRootBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.ROOTED_DIRT)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> TUNNEL_ROOT_ITEM = ITEMS.registerSimpleBlockItem(TUNNEL_ROOT);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TunnelRootBlockEntity>> TUNNEL_ROOT_ENTITY =
            BLOCK_ENTITIES.register(
                    "tunnel_root",
                    () -> BlockEntityType.Builder.of(TunnelRootBlockEntity::new, TUNNEL_ROOT.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<TunnelRootMenu>> TUNNEL_ROOT_MENU =
            MENUS.register("tunnel_root", () -> IMenuTypeExtension.create(TunnelRootMenu::new));

    /** What a broken head carries on its item: everything it held and the cells it has laid. */
    static final DeferredHolder<DataComponentType<?>, DataComponentType<TunnelRootContents>> TUNNEL_ROOT_CONTENTS =
            COMPONENTS.registerComponentType(
                    "tunnel_root", builder -> builder.persistent(TunnelRootContents.CODEC.codec()));

    private MiningModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT)
            modBus.addListener(
                    (RegisterMenuScreensEvent event) -> event.register(TUNNEL_ROOT_MENU.get(), TunnelRootScreen::new));
    }
}
