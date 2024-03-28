package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxelCore;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkProviderClient.class, remap = false)
public class MixinChunkProviderClient {
    @Shadow private WorldClient worldObj;

    @Inject(method="unloadChunk", at=@At("TAIL"))
    private void injectIngest(int x, int z, CallbackInfo ci) {
        var core = ((IGetVoxelCore)()).getVoxelCore();
        if (core != null && VoxyConfig.CONFIG.ingestEnabled) {
            core.enqueueIngest(worldObj.getChunk(x, z));
        }
    }
}
