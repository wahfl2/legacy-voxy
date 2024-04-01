package me.cortex.voxy.client.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import org.lwjgl.opengl.GL;

@Config(modid = "voxy", filename = "voxy-config")
public class VoxyConfig {
    @Config.DefaultBoolean(true)
    public static boolean enabled;

    @Config.DefaultBoolean(true)
    public static boolean ingestEnabled;

    @Config.DefaultInt(12)
    public static int qualityScale;

    @Config.DefaultInt(200_000)
    public static int maxSections;

    @Config.DefaultInt(128)
    public static int renderDistance;

    @Config.DefaultInt((1<<30)/8)
    public static int geometryBufferSize;

    @Config.DefaultInt(2)
    public static int ingestThreads;

    @Config.DefaultInt(4)
    public static int savingThreads;

    @Config.DefaultInt(5)
    public static int renderThreads;

    @Config.DefaultBoolean(true)
    public static boolean useMeshShaderIfPossible;

    public static boolean useMeshShaders() {
        var cap = GL.getCapabilities();
        return useMeshShaderIfPossible && cap.GL_NV_mesh_shader && cap.GL_NV_representative_fragment_test;
    }
}
