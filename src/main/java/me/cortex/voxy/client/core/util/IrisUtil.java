package me.cortex.voxy.client.core.util;

import net.coderbot.iris.pipeline.ShadowRenderer;

public class IrisUtil {


    private static boolean irisShadowActive0() {
        return ShadowRenderer.ACTIVE;
    }

    public static boolean irisShadowActive() {
        //return IRIS_INSTALLED && irisShadowActive0();
        return false;
    }
}
