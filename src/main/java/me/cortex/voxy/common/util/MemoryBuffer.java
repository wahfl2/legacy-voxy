package me.cortex.voxy.common.util;

import org.lwjgl.system.MemoryUtil;

public class MemoryBuffer {
    public final long address;
    public final long size;

    public MemoryBuffer(long size) {
        this.size = size;
        this.address = MemoryUtil.nmemAlloc(size);
    }

    public void cpyTo(long dst) {
        // super.assertNotFreed();
        MemoryUtil.memCopy(this.address, dst, this.size);
    }

    public void free() {
        MemoryUtil.nmemFree(this.address);
    }

    public MemoryBuffer copy() {
        var copy = new MemoryBuffer(this.size);
        this.cpyTo(copy.address);
        return copy;
    }
}
