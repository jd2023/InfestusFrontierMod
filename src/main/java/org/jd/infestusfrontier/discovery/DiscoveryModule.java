package org.jd.infestusfrontier.discovery;

import java.util.List;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.control.ControlModule;
import org.jd.infestusfrontier.cultivation.CultivationModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.OrganBatchCompletedEvent;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassBucketEvent;

/** Owner of recipe reveal, advancements and guide prerequisites. Ships the creative tab. */
public final class DiscoveryModule {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfestusFrontier.MOD_ID);

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, InfestusFrontier.MOD_ID);

    static final DeferredHolder<CriterionTrigger<?>, OrganTriggers.OrganBatchCompleted> ORGAN_BATCH_COMPLETED =
            TRIGGERS.register("organ_batch_completed", OrganTriggers.OrganBatchCompleted::new);
    static final DeferredHolder<CriterionTrigger<?>, OrganTriggers.BiomassBucket> BIOMASS_BUCKET =
            TRIGGERS.register("biomass_bucket", OrganTriggers.BiomassBucket::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.infestusfrontier.main"))
                    .icon(() -> new ItemStack(EcologyModule.SPORE_CULTURE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(EcologyModule.SPORE_CULTURE.get());
                        output.accept(EcologyModule.LIVING_SUBSTRATE_ITEM.get());
                        output.accept(OrganModule.ORGAN_BUD_ITEM.get());
                        output.accept(ProcessingModule.DIGESTIVE_SAC_ITEM.get());
                        output.accept(StorageModule.BIOMASS_BLADDER_ITEM.get());
                        output.accept(StorageModule.BIOMASS_BUCKET.get());
                        output.accept(StorageModule.RESERVOIR_CELL_ITEM.get());
                        output.accept(ProcessingModule.CULTURE_BOWL_ITEM.get());
                        output.accept(ProcessingModule.MEMBRANE_RACK_ITEM.get());
                        output.accept(ProcessingModule.MEMBRANE_SHEET.get());
                        output.accept(ProcessingModule.BONE_LOOM_ITEM.get());
                        output.accept(ProcessingModule.BONE_PLATE.get());
                        output.accept(ProcessingModule.AWAKENING_CRADLE_ITEM.get());
                        output.accept(ProcessingModule.BIO_FURNACE_ITEM.get());
                        output.accept(ProcessingModule.ACTIVATION_CYST_ITEM.get());
                        for (var treatment : List.of(
                                ProcessingModule.FUSION_BINDER,
                                ProcessingModule.ELASTIC_GEL,
                                CultivationModule.ROOTING_GEL,
                                LogisticsModule.CAPILLARY_GEL,
                                LogisticsModule.CONTRACTILE_FIBER,
                                LogisticsModule.FILTER_MEMBRANE,
                                LogisticsModule.SEALING_RESIN,
                                ProcessingModule.SYNAPTIC_GEL)) output.accept(treatment.get());
                        output.accept(LogisticsModule.BIOMASS_VEIN_ITEM.get());
                        output.accept(LogisticsModule.FLUID_VEIN_ITEM.get());
                        output.accept(LogisticsModule.ITEM_VEIN_ITEM.get());
                        output.accept(LogisticsModule.VASCULAR_JUNCTION_ITEM.get());
                        output.accept(LogisticsModule.MOUTH_ITEM.get());
                        output.accept(LogisticsModule.FEEDING_LOBE_ITEM.get());
                        output.accept(LogisticsModule.YIELD_LOBE_ITEM.get());
                        output.accept(LogisticsModule.FILTER_VALVE_ITEM.get());
                        output.accept(CultivationModule.CULTIVATION_TISSUE_ITEM.get());
                        output.accept(CultivationModule.SEED_POUCH_ITEM.get());
                        output.accept(CultivationModule.PLANTING_PROBOSCIS_ITEM.get());
                        output.accept(CultivationModule.HARVEST_COROLLA_ITEM.get());
                        output.accept(ProcessingModule.COMPOST_GLAND_ITEM.get());
                        output.accept(ControlModule.SENSOR_POLYP_ITEM.get());
                        output.accept(EquipmentModule.FUEL_PAPILLA_ITEM.get());
                        for (var piece : List.of(
                                EquipmentModule.DORMANT_HOOD,
                                EquipmentModule.DORMANT_THORAX,
                                EquipmentModule.DORMANT_LEGWRAPS,
                                EquipmentModule.DORMANT_TREADS,
                                EquipmentModule.AWAKENED_HOOD,
                                EquipmentModule.AWAKENED_THORAX,
                                EquipmentModule.AWAKENED_LEGWRAPS,
                                EquipmentModule.AWAKENED_TREADS)) output.accept(piece.get());
                        output.accept(InteractionModule.SYNAPTIC_PROBE.get());
                    })
                    .build());

    private DiscoveryModule() {}

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
        TRIGGERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(DiscoveryModule::onBatchCompleted);
        NeoForge.EVENT_BUS.addListener(DiscoveryModule::onBucket);
        NeoForge.EVENT_BUS.addListener(DiscoveryModule::onLogin);
    }

    /** Fires for an online feeder; an offline feeder's credit waits in {@link PendingCredits}. */
    private static void onBatchCompleted(OrganBatchCompletedEvent event) {
        ServerPlayer feeder = event.level().getServer().getPlayerList().getPlayer(event.feeder());
        if (feeder != null) ORGAN_BATCH_COMPLETED.get().trigger(feeder, event.organ());
        else PendingCredits.get(event.level()).remember(event.feeder(), event.organ());
    }

    private static void onBucket(BiomassBucketEvent event) {
        if (event.player() instanceof ServerPlayer player)
            BIOMASS_BUCKET.get().trigger(player, event.block(), event.inserted());
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            PendingCredits.get(player.serverLevel()).award(player);
    }
}
