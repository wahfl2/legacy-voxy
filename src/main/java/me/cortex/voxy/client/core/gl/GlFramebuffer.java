package me.cortex.voxy.client.core.gl;

import static org.lwjgl.opengl.GL45C.*;

public class GlFramebuffer {
    public final int id;
    public GlFramebuffer() {
        this.id = glCreateFramebuffers();
    }

    public GlFramebuffer bind(int attachment, GlTexture texture) {
        glNamedFramebufferTexture(this.id, attachment, texture.id, 0);
        return this;
    }

    public void free() {
        glDeleteFramebuffers(this.id);
    }

    public GlFramebuffer verify() {
        int code;
        if ((code = glCheckNamedFramebufferStatus(this.id, GL_FRAMEBUFFER)) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete with error code: " + code);
        }
        return this;
    }
}
