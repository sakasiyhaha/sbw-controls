package com.sakasiyhaha.sbwcontrol.client;

import net.minecraft.client.Camera;

import java.lang.reflect.Method;

/**
 * 通过反射调用 {@code Camera.setPosition(double,double,double)}（1.20.1 中为 protected）。
 *
 * <p>为什么不使用 AccessTransformer：反射同时兼容开发环境（official 名 setPosition）与
 * 生产环境（SRG 名 m_90584_），并且不依赖 FG 的 AT 配置；失败时只是相机位置动态失效。
 */
public final class CameraAccess {

    private static final Method SET_POSITION = findSetPosition();

    private CameraAccess() {
    }

    private static Method findSetPosition() {
        for (String name : new String[]{"setPosition", "m_90584_"}) {
            try {
                Method method = Camera.class.getDeclaredMethod(name, double.class, double.class, double.class);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException ignored) {
                // try next name
            }
        }
        // 最后的兜底：按签名扫描（不同映射/环境仍能找到 setPosition(double,double,double)）
        for (Method method : Camera.class.getDeclaredMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (method.getReturnType() == void.class && params.length == 3
                    && params[0] == double.class && params[1] == double.class && params[2] == double.class) {
                try {
                    method.setAccessible(true);
                    return method;
                } catch (RuntimeException ignored) {
                    // ignore and keep looking
                }
            }
        }
        return null;
    }

    public static boolean available() {
        return SET_POSITION != null;
    }

    public static void setPosition(Camera camera, double x, double y, double z) {
        if (SET_POSITION == null) {
            return;
        }
        try {
            SET_POSITION.invoke(camera, x, y, z);
        } catch (ReflectiveOperationException ignored) {
            // 相机位置动态是纯视觉功能，失败不影响游戏
        }
    }
}
