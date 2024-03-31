package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.mixin.interfaces.IEbsExtension;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ExtendedBlockStorage.class)
public class MixinExtendedBlockStorage implements IEbsExtension {
    @Shadow private byte[] blockLSBArray;
    @Shadow private NibbleArray blockMSBArray;

    @Override
    public int getBlockId(int x, int y, int z) {
        int id = this.blockLSBArray[y << 8 | z << 4 | x] & 255;

        if (this.blockMSBArray != null)
        {
            id |= this.blockMSBArray.get(x, y, z) << 8;
        }

        return id;
    }
}
