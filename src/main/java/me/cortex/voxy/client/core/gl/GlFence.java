package me.cortex.voxy.client.core.gl;

import static org.lwjgl.opengl.GL32.*;

public class GlFence {
    private final long fence;
    private boolean signaled;

    public GlFence() {
        this.fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    public boolean signaled() {
        if (!this.signaled) {
            int ret = glClientWaitSync(this.fence, 0, 0);
            if (ret == GL_ALREADY_SIGNALED || ret == GL_CONDITION_SATISFIED) {
                this.signaled = true;
            } else if (ret != GL_TIMEOUT_EXPIRED) {
                throw new IllegalStateException("Poll for fence failed, glError: " + glGetError());
            }
        }
        return this.signaled;
    }

    public void free() {
        glDeleteSync(this.fence);
    }
}
