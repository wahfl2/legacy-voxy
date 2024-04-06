package me.cortex.voxy.client.importers.util;

import me.cortex.voxy.client.Voxy;
import me.cortex.voxy.common.voxelization.ISectionDataProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.NibbleArray;

import javax.annotation.Nullable;

public class SectionBlockData implements ISectionDataProvider {
    private final boolean isBlockIds16;

    private final @Nullable byte[] blockIds16;

    private final @Nullable byte[] blockIdsLSB;
    private final @Nullable NibbleArray blockIdsMSB;

    private final boolean isMetadata16;

    private final byte @Nullable [] metadata16;

    private final @Nullable NibbleArray metadata;

    private final NibbleArray blockLight;
    private final NibbleArray skyLight;

    public SectionBlockData(NBTTagCompound sectionNbt) {
        if (Voxy.NEIDS_PRESENT.get() && sectionNbt.hasKey("Blocks16")) {
            this.blockIds16 = sectionNbt.getByteArray("Blocks16");

            this.isBlockIds16 = true;
            this.blockIdsLSB = null; this.blockIdsMSB = null;
        } else {
            // Screw the 12-bit format
            this.blockIdsLSB = sectionNbt.getByteArray("Blocks");

            byte[] nibbles = sectionNbt.getByteArray("Add");
            this.blockIdsMSB = new NibbleArray(nibbles, 4);

            this.isBlockIds16 = false;
            this.blockIds16 = null;
        }

        if (Voxy.NEIDS_PRESENT.get() && sectionNbt.hasKey("Data16")) {
            this.metadata16 = sectionNbt.getByteArray("Data16");

            this.isMetadata16 = true;
            this.metadata = null;
        } else {
            byte[] nibbles = sectionNbt.getByteArray("Data");
            this.metadata = new NibbleArray(nibbles, 4);

            this.isMetadata16 = false;
            this.metadata16 = null;
        }

        this.blockLight = new NibbleArray(sectionNbt.getByteArray("BlockLight"), 4);
        this.skyLight = new NibbleArray(sectionNbt.getByteArray("SkyLight"), 4);
    }

    @SuppressWarnings("DataFlowIssue")
    public short getBlockId(int x, int y, int z) {
        int idx = computeIndex(x, y, z);

        if (isBlockIds16) {
            return byteArrayAsShort(this.blockIds16, idx);
        } else {
            byte lsb = blockIdsLSB[idx];
            byte msb = (byte) blockIdsMSB.get(x, y, z);

            return (short) ((msb & 0xF) << 8 | (lsb & 0xFF));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public short getMeta(int x, int y, int z) {
        if (isMetadata16) {
            return byteArrayAsShort(this.metadata16, computeIndex(x, y, z));
        } else {
            return (short) this.metadata.get(x, y, z);
        }
    }

    public byte getLightCompact(int x, int y, int z) {
        return (byte) (getSkyLight(x, y, z)|(getBlockLight(x, y, z)<<4));
    }

    public int getBlockLight(int x, int y, int z) {
        return blockLight.get(x, y, z);
    }

    public int getSkyLight(int x, int y, int z) {
        return skyLight.get(x, y, z);
    }

    private static int computeIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private static short byteArrayAsShort(byte[] arr, int shortIndex) {
        int idx = shortIndex * 2;
        return (short) ((arr[idx + 1] & 0xFF) << 8 | (arr[idx] & 0xFF));
    }
}
