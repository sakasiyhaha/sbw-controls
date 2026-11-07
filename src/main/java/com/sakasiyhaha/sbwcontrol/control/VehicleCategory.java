package com.sakasiyhaha.sbwcontrol.control;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;

/**
 * 载具大类：本附属只接管固定翼与直升机，其他载具保持原版行为。
 *
 * <p>用于区分“固定翼默认流派 / 直升机默认流派 / 其他载具”三种情况，
 * 避免 B 键在坦克、汽车等载具上误改固定翼默认值。
 */
public enum VehicleCategory {
    AIRPLANE,
    HELICOPTER,
    OTHER;

    public static VehicleCategory of(VehicleEntity vehicle) {
        if (vehicle == null || vehicle.getVehicleType() == null) {
            return OTHER;
        }
        VehicleType type = vehicle.getVehicleType();
        if (type == VehicleType.AIRPLANE) {
            return AIRPLANE;
        }
        if (type == VehicleType.HELICOPTER) {
            return HELICOPTER;
        }
        return OTHER;
    }

    public boolean isFlying() {
        return this == AIRPLANE || this == HELICOPTER;
    }
}
