package com.dougg865.bloomberto.impl.render;

import com.dougg865.bloomberto.platform.Services;
import java.lang.reflect.Method;

/**
 * Detects shader mods (Iris, Oculus, OptiFine). Custom post-processing is skipped while one of them is rendering with a
 * shader pack, because they replace the vanilla render pipeline the effects rely on.
 */
public final class RenderCompat {
    private static Method irisInUse;
    private static Object irisApi;
    private static Method optifineShaders;
    private static boolean resolved;

    private RenderCompat() {
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (Services.PLATFORM.isModLoaded("iris") || Services.PLATFORM.isModLoaded("oculus")) {
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                irisInUse = api.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                irisApi = null;
                irisInUse = null;
            }
        }
        try {
            optifineShaders = Class.forName("net.optifine.Config").getMethod("isShaders");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            optifineShaders = null;
        }
    }

    /** {@code true} if a shader mod is currently rendering with a shader pack. */
    public static boolean shaderPackActive() {
        resolve();
        try {
            if (irisInUse != null && Boolean.TRUE.equals(irisInUse.invoke(irisApi))) {
                return true;
            }
            return optifineShaders != null && Boolean.TRUE.equals(optifineShaders.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** {@code true} if any supported shader mod is installed, even without a pack. */
    public static boolean shaderModPresent() {
        resolve();
        return irisInUse != null || optifineShaders != null;
    }
}
