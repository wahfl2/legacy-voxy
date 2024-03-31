package me.cortex.voxy.client.importers.util;

import com.gtnewhorizons.neid.mixins.interfaces.IExtendedBlockStorageMixin;
import me.cortex.voxy.client.Voxy;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.Nullable;

public class SectionBlockData {
    private final int blockIdsType;

    // Type 1
    private final @Nullable byte[] blockIds16;

    // Type 2
    private final @Nullable short[] blockIdsShort;

    // Type 3
    private final @Nullable byte[] blockIdsLSB;
    private final @Nullable NibbleArray blockIdsMSB;

    private final int metadataType;

    // Type 1
    private final byte @Nullable [] metadata16;

    // Type 2
    private final short @Nullable [] metadataShort;

    // Type 3
    private final @Nullable NibbleArray metadata;

    public SectionBlockData(NBTTagCompound sectionNbt) {
        this.blockIdsShort = null; this.metadataShort = null;

        if (Voxy.NEIDS_PRESENT.get() && sectionNbt.hasKey("Blocks16")) {
            this.blockIds16 = sectionNbt.getByteArray("Blocks16");

            this.blockIdsType = 1;
            this.blockIdsLSB = null; this.blockIdsMSB = null;
        } else {
            // Screw the 12-bit format
            this.blockIdsLSB = sectionNbt.getByteArray("Blocks");

            byte[] nibbles = sectionNbt.getByteArray("Add");
            this.blockIdsMSB = new NibbleArray(nibbles, 4);

            this.blockIdsType = 3;
            this.blockIds16 = null;
        }

        if (Voxy.NEIDS_PRESENT.get() && sectionNbt.hasKey("Data16")) {
            this.metadata16 = sectionNbt.getByteArray("Data16");

            this.metadataType = 1;
            this.metadata = null;
        } else {
            byte[] nibbles = sectionNbt.getByteArray("Data");
            this.metadata = new NibbleArray(nibbles, 4);

            this.metadataType = 3;
            this.metadata16 = null;
        }
    }

    public SectionBlockData(ExtendedBlockStorage section) {
        this.blockIds16 = null; this.metadata16 = null;
        if (Voxy.NEIDS_PRESENT.get()) {
            IExtendedBlockStorageMixin ebsMixin = (IExtendedBlockStorageMixin) section;
            this.blockIdsShort = ebsMixin.getBlock16BArray();

            this.blockIdsType = 2;
            this.blockIdsLSB = null; this.blockIdsMSB = null;

            this.metadataShort = ebsMixin.getBlock16BMetaArray();

            this.metadataType = 2;
            this.metadata = null;
        } else {
            this.blockIdsLSB = section.getBlockLSBArray();
            this.blockIdsMSB = section.getBlockMSBArray();

            this.blockIdsType = 3;
            this.blockIdsShort = null;

            this.metadata = section.getMetadataArray();

            this.metadataType = 3;
            this.metadataShort = null;
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public short getBlockId(int x, int y, int z) {
        int idx = computeIndex(x, y, z);

        switch (blockIdsType) {
            case 1 -> {
                return byteArrayAsShort(this.blockIds16, idx);
            }
            case 2 -> {
                return this.blockIdsShort[idx];
            }
            case 3 -> {
                byte lsb = blockIdsLSB[idx];
                byte msb = (byte) blockIdsMSB.get(x, y, z);

                return (short) ((msb & 0xF) << 8 | (lsb & 0xFF));
            }
            default -> throw new IllegalStateException("Unexpected blockId type: " + blockIdsType);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public short getMeta(int x, int y, int z) {
        switch (metadataType) {
            case 1 -> {
                return byteArrayAsShort(this.metadata16, computeIndex(x, y, z));
            }
            case 2 -> {
                return this.metadataShort[computeIndex(x, y, z)];
            }
            case 3 -> {
                return (short) this.metadata.get(x, y, z);
            }
            default -> throw new IllegalStateException("Unexpected metadata type: " + blockIdsType);
        }
    }

    private static int computeIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private static short byteArrayAsShort(byte[] arr, int shortIndex) {
        int idx = shortIndex * 2;
        return (short) ((arr[idx + 1] & 0xFF) << 8 | (arr[idx] & 0xFF));
    }
}
