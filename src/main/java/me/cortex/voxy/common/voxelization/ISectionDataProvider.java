package me.cortex.voxy.common.voxelization;

public interface ISectionDataProvider {
    short getBlockId(int x, int y, int z);
    short getMeta(int x, int y, int z);
    byte getLightCompact(int x, int y, int z);
}
