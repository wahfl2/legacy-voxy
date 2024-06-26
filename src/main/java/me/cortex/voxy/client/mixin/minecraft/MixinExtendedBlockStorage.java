package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.common.voxelization.ISectionDataProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ExtendedBlockStorage.class)
public abstract class MixinExtendedBlockStorage implements ISectionDataProvider {
    @Shadow private byte[] blockLSBArray;
    @Shadow private NibbleArray blockMSBArray;

    @Shadow private NibbleArray blocklightArray;
    @Shadow private NibbleArray skylightArray;

    @Shadow
    public abstract int getExtBlockMetadata(int p_76665_1_, int p_76665_2_, int p_76665_3_);

    @Override
    public short getBlockId(int x, int y, int z) {
        int id = this.blockLSBArray[y << 8 | z << 4 | x] & 255;

        if (this.blockMSBArray != null)
        {
            id |= this.blockMSBArray.get(x, y, z) << 8;
        }

        return (short) id;
    }

    @Override
    public short getMeta(int x, int y, int z) {
        return (short) this.getExtBlockMetadata(x, y, z);
    }

    @Override
    public byte getLightCompact(int x, int y, int z) {
        return (byte) (this.skylightArray.get(x, y, z) | (this.blocklightArray.get(x, y, z) << 4));
    }
}
