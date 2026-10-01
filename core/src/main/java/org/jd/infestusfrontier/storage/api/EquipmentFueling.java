package org.jd.infestusfrontier.storage.api;

import java.util.UUID;

/** Narrow service exposed by a tank that can fuel compatible equipment. */
public interface EquipmentFueling {
    EquipmentFuelPort.Result fillEquipment(UUID player, EquipmentFuelPort.Target target);
}
