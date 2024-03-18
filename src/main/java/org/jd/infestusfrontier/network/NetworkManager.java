package org.jd.infestusfrontier.network;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.level.LevelEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class NetworkManager extends SavedData {
    public static final String NETWORK_ID_TAG = "infestus_network_id";
    public static final String NETWORK_MANAGER_TAG = "infestus_network_manager_data";
    public static final String NETWORKS_TAG = "infestus_networks";
    public static final String NETWORK_TAG = "infestus_network";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Network> networks = new HashMap<>();

    public static void createNetwork(String networkId) {
        var network = new Network();
        network.setBiomassStorage(new BiomassStorage(1000, 1000));
        networks.put(networkId, network);
        LOGGER.info("Created new network for player {}", networkId);
    }

    public static Network getNetwork(String networkId) {
        return networks.get(networkId);
    }

    public static boolean doesNetworkExistForPlayer(String networkId) {
        return networks.containsKey(networkId);
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        LOGGER.warn("Saving network manager");
        ListTag list = new ListTag();
        networks.forEach((key, network) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Key", key);
            tag.put(NETWORK_TAG, network.save());
            list.add(tag);
        });
        compound.put(NETWORKS_TAG, list);
        return compound;
    }

    private static SavedData load(CompoundTag compoundTag) {
        LOGGER.warn("Loading network manager");
        NetworkManager data = new NetworkManager();
        ListTag list = compoundTag.getList(NETWORKS_TAG, 10); // 10 is the tag type for CompoundTag
        list.forEach(tag -> {
            CompoundTag t = (CompoundTag) tag;
            String key = t.getString("Key");
            networks.put(key, Network.load(t.getCompound(NETWORK_TAG)));
        });
        LOGGER.info("Loaded networks: {}", dump());
        return data;
    }

    private static String dump() {
        StringBuilder builder = new StringBuilder();
        for (String key : networks.keySet()) {
            builder.append(key).append(": ");
            networks.get(key).dump(builder);
        }
        return builder.toString();
    }

    @Mod.EventBusSubscriber
    public static class NetworkDataEventHandler {

        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getDataStorage().computeIfAbsent(
                        NetworkManager::load,
                        NetworkManager::new,
                        NetworkManager.NETWORK_MANAGER_TAG);
            }
        }

        @SubscribeEvent
        public static void onLevelSave(LevelEvent.Save event) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                var data = serverLevel.getDataStorage().get(NetworkManager::load, NETWORK_MANAGER_TAG);
                if (data != null) {
                    data.setDirty();
                }
            }
        }
    }
}
