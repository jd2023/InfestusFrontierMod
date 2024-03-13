package org.jd.infestusfrontier.network;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class NetworkManager {
    public static final String NETWORK_ID_TAG = "network_id";
    public static final String NETWORK_MANAGER_TAG = "network_manager";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Summary> networks = new HashMap<>();

    public static void createNetwork(String networkId) {
        networks.put(networkId, new Summary(100, 10));
        LOGGER.info("Created new network for player {}", networkId);
    }

    public static Summary getSummary(String networkId) {
        return networks.get(networkId);
    }

    public static boolean doesNetworkExistForPlayer(String networkId) {
        return networks.containsKey(networkId);
    }

    public static void addTotalCapacity(String networkId, int additionalCapacity) {
        Summary summary = networks.get(networkId);
        summary.totalCapacity += additionalCapacity;
        LOGGER.info("Added {} to player {}'s total capacity. {}", additionalCapacity, networkId, summary.totalCapacity);
    }

    public static void reduceTotalCapacity(String networkId, int additionalCapacity) {
        Summary summary = networks.get(networkId);
        summary.totalCapacity -= additionalCapacity;
        if (summary.totalCapacity < 0) {
            summary.totalCapacity = 0;
        }
        System.out.println("Reduced " + additionalCapacity + " from player " + networkId + "'s total capacity. " + summary.totalCapacity);
        LOGGER.info("Reduced {} from player {} s total capacity. {}", additionalCapacity, networkId, summary.totalCapacity);
    }

    public static void addBiomass(String networkId, int amount) {
        Summary summary = networks.get(networkId);
        summary.usedCapacity += amount;
        if (summary.usedCapacity > summary.totalCapacity) {
            summary.usedCapacity = summary.totalCapacity;
        }
        LOGGER.info("Added {} to player {} s biomass. {}", amount, networkId, summary.usedCapacity);
    }

    public static void reduceBiomass(String networkId, int amount) {
        Summary summary = networks.get(networkId);
        summary.usedCapacity -= amount;
        if (summary.usedCapacity < 0) {
            summary.usedCapacity = 0;
        }
        LOGGER.info("Reduced {} from player {} s biomass. {}", amount, networkId, summary.usedCapacity);
    }
}
