package me.cortex.voxy.client.importers.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;

public class ChunkBiomes {
    private final byte[] inner; // 256 bytes

    public ChunkBiomes(byte[] bytes) {
        this.inner = bytes;
    }

    public ChunkBiomes(NBTTagCompound chunk) {
        this(chunk.getByteArray("Biomes"));
    }

    public ChunkBiomes(Chunk chunk) {
        this(chunk.getBiomeArray());
    }

    private int computeIndex(int x, int z) {
        return z * 16 + x;
    }

    public byte getBiomeId(int x, int z) {
        return this.inner[computeIndex(x, z)];
    }


}
