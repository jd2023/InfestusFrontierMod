package org.jd.infestusfrontier.processing.digestion;

import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.construction.api.BudConstructionRecipe;
import org.jd.infestusfrontier.construction.api.BudRecipeRegistrar;

/** Registers the two starter biomass organs, their finite bucket, and Bud constructions. */
public final class DigestionModule {
    public static final String SAC = "processing/digestive_sac";
    public static final String BLADDER = "storage/biomass_bladder";
    public static final String BUCKET = "storage/biomass_bucket";

    private final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private final DeferredRegister.Items items = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private final DiscoveryObserver discovery;
    private final Supplier<Item> biomassBucket;
    private final Supplier<DigestiveSacBlock> sac;
    private final Supplier<BiomassBladderBlock> bladder;
    private final Supplier<BlockEntityType<DigestiveSacEntity>> sacEntity;
    private final Supplier<BlockEntityType<BiomassBladderEntity>> bladderEntity;

    public DigestionModule(BudRecipeRegistrar construction, DiscoveryObserver discovery) {
        this.discovery = discovery;
        biomassBucket = items.register(BUCKET, () -> new BiomassBucketItem(new Item.Properties()));
        sac = blocks.register(SAC, this::createSac);
        bladder = blocks.register(BLADDER, this::createBladder);
        sacEntity = entities.register(SAC, this::createSacEntity);
        bladderEntity = entities.register(BLADDER, this::createBladderEntity);
        items.register(SAC, () -> new StoredOrganItem(sac.get(), new Item.Properties()));
        items.register(BLADDER, () -> new StoredOrganItem(bladder.get(), new Item.Properties()));

        construction.register(new BudConstructionRecipe(
                "infestusfrontier:digestive_sac", "minecraft:bowl",
                "infestusfrontier:" + SAC, Map.of("minecraft:bowl", 1, "minecraft:rotten_flesh", 2)));
        construction.register(new BudConstructionRecipe(
                "infestusfrontier:biomass_bladder", "minecraft:slime_ball",
                "infestusfrontier:" + BLADDER, Map.of("minecraft:slime_ball", 1, "minecraft:glass", 2)));
    }

    private DigestiveSacBlock createSac() {
        return new DigestiveSacBlock(properties(MapColor.COLOR_RED), sacEntity, biomassBucket);
    }
    private BiomassBladderBlock createBladder() {
        return new BiomassBladderBlock(properties(MapColor.COLOR_GREEN), bladderEntity, biomassBucket);
    }
    private BlockEntityType<DigestiveSacEntity> createSacEntity() {
        return BlockEntityType.Builder.of(
                (pos, state) -> new DigestiveSacEntity(pos, state, sacEntity, discovery), sac.get()).build(null);
    }
    private BlockEntityType<BiomassBladderEntity> createBladderEntity() {
        return BlockEntityType.Builder.of(
                (pos, state) -> new BiomassBladderEntity(pos, state, bladderEntity, discovery), bladder.get()).build(null);
    }
    private static BlockBehaviour.Properties properties(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.7F).noOcclusion()
                .sound(SoundType.WART_BLOCK).pushReaction(PushReaction.BLOCK);
    }

    public void register(IEventBus bus) {
        blocks.register(bus);
        items.register(bus);
        entities.register(bus);
    }

    static boolean isRooted(LevelReader level, BlockPos pos) {
        if (!level.hasChunkAt(pos.below())) return false;
        var substrate = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(
                "infestusfrontier:ecology/living_substrate"));
        return level.getBlockState(pos.below()).is(substrate);
    }
}
