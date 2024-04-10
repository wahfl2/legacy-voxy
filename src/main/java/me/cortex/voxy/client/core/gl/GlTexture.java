package me.cortex.voxy.client.core.gl;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glDeleteTextures;
import static org.lwjgl.opengl.GL45C.glCreateTextures;
import static org.lwjgl.opengl.GL45C.glTextureStorage2D;

public class GlTexture {
    public final int id;
    private final int type;
    public GlTexture() {
        this(GL_TEXTURE_2D);
    }

    public GlTexture(int type) {
        this.id = glCreateTextures(type);
        this.type = type;
    }

    public GlTexture store(int format, int levels, int width, int height) {
        if (this.type == GL_TEXTURE_2D) {
            glTextureStorage2D(this.id, levels, format, width, height);
        } else {
            throw new IllegalStateException("Unknown texture type");
        }
        return this;
    }

    public void free() {
        glDeleteTextures(this.id);
    }
}
