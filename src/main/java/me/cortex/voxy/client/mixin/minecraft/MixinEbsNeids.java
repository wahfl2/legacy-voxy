package me.cortex.voxy.client.mixin.minecraft;

import com.gtnewhorizons.neid.mixins.interfaces.IExtendedBlockStorageMixin;
import me.cortex.voxy.common.util.Util;
import me.cortex.voxy.common.voxelization.ISectionDataProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

// Mixin should only be loaded if NotEnoughIds is installed
@Mixin(ExtendedBlockStorage.class)
public class MixinEbsNeids implements ISectionDataProvider {
    @Shadow private NibbleArray skylightArray;
    @Shadow private NibbleArray blocklightArray;

    @Unique
    private final IExtendedBlockStorageMixin ebsMixin = (IExtendedBlockStorageMixin) this;

    @Override
    public short getBlockId(int x, int y, int z) {
        return ebsMixin.getBlock16BArray()[Util.computeIndex(x, y, z)];
    }

    @Override
    public short getMeta(int x, int y, int z) {
        return ebsMixin.getBlock16BMetaArray()[Util.computeIndex(x, y, z)];
    }

    @Override
    public byte getLightCompact(int x, int y, int z) {
        return (byte) (this.skylightArray.get(x, y, z) | (this.blocklightArray.get(x, y, z) << 4));
    }
}
