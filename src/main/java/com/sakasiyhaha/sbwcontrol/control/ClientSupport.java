package com.sakasiyhaha.sbwcontrol.control;

/**
 * 客户端保存的服务端支持状态（由 SupportPacket 更新）。
 * 服务端没装本附属时永远保持 supported = false，客户端自动降级为“原版方案 + 相机/参数”。
 */
public final class ClientSupport {

    private static volatile boolean supported = false;
    private static volatile int allowedMask = 0;
    private static volatile long lastUpdate = 0L;

    private ClientSupport() {
    }

    public static void accept(boolean serverSupported, int mask) {
        supported = serverSupported;
        allowedMask = mask;
        lastUpdate = System.currentTimeMillis();
    }

    public static void reset() {
        supported = false;
        allowedMask = 0;
    }

    public static boolean isSupported() {
        return supported;
    }

    public static boolean isAllowed(ControlScheme scheme) {
        return supported && (allowedMask & bit(scheme)) != 0;
    }

    public static int allowedMask() {
        return allowedMask;
    }

    public static long lastUpdate() {
        return lastUpdate;
    }

    public static int bit(ControlScheme scheme) {
        if (scheme == null) {
            return 0;
        }
        return 1 << scheme.ordinal();
    }
}
