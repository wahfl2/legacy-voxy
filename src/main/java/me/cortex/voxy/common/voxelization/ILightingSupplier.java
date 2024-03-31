package me.cortex.voxy.common.voxelization;

import net.minecraft.block.Block;

public interface ILightingSupplier {
    byte supply(int x, int y, int z, Block state);
}
