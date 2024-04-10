package me.cortex.voxy.client.core.gl;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL45C.glCreateBuffers;
import static org.lwjgl.opengl.GL45C.glNamedBufferStorage;

public class GlBuffer {
    public final int id;
    private final long size;

    public GlBuffer(long size) {
        this(size, 0);
    }

    public GlBuffer(long size, int flags) {
        this.id = glCreateBuffers();
        this.size = size;
        glNamedBufferStorage(this.id, size, flags);
    }

    public void free() {
        glDeleteBuffers(this.id);
    }

    public long size() {
        return this.size;
    }
}
