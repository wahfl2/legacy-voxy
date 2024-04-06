package me.cortex.voxy.client.mixin.sodium;

import com.gtnewhorizons.angelica.compat.toremove.MatrixStack;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxelCore;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkRenderManager.class, remap = false)
public class MixinChunkRenderManager {

    @Shadow @Final private WorldClient world;

    @Inject(method="unloadChunk", at=@At("TAIL"))
    private void injectIngest(int x, int z, CallbackInfo ci) {
        var core = ((IGetVoxelCore)Minecraft.getMinecraft().renderGlobal).getVoxelCore();
        if (core != null && VoxyConfig.ingestEnabled) {
            core.enqueueIngest(world.getChunkProvider().provideChunk(x, z));
        }
    }

    @Inject(method = "renderLayer", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderBackend;end(Lcom/gtnewhorizons/angelica/compat/toremove/MatrixStack;)V", shift = At.Shift.BEFORE))
    private void injectRender(MatrixStack matrixStack, BlockRenderPass pass, double x, double y, double z, CallbackInfo ci) {
        if (!pass.isTranslucent()) {
            var core = ((IGetVoxelCore) Minecraft.getMinecraft().renderGlobal).getVoxelCore();
            if (core != null) {
                core.renderOpaque(matrixStack, x, y, z);
            }
        }
    }
}
