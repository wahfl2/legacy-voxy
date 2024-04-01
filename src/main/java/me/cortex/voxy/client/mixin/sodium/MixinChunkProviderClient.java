package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxelCore;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkRenderManager.class, remap = false)
public class MixinChunkProviderClient {

    @Shadow @Final private WorldClient world;

    @Inject(method="unloadChunk", at=@At("TAIL"))
    private void injectIngest(int x, int z, CallbackInfo ci) {
        var core = ((IGetVoxelCore)(world.mc.renderGlobal)).getVoxelCore();
        if (core != null && VoxyConfig.ingestEnabled) {
            core.enqueueIngest(world.getChunkProvider().provideChunk(x, z));
        }
    }
}
