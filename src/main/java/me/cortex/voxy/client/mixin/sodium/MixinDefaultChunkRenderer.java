package me.cortex.voxy.client.mixin.sodium;

import com.gtnewhorizons.angelica.compat.toremove.MatrixStack;
import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawChunkRenderBackend;
import org.joml.Matrix3fStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultidrawChunkRenderBackend.class, remap = false)
public class MixinDefaultChunkRenderer {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/ShaderChunkRenderer;end(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V", shift = At.Shift.BEFORE))
    private void injectRender(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform camera, CallbackInfo ci) {
        if (renderPass == DefaultTerrainRenderPasses.CUTOUT) {
            var core = ((IGetVoxelCore) MinecraftClient.getInstance().worldRenderer).getVoxelCore();
            if (core != null) {
                var stack = new Matrix3fStack();
                stack.loadIdentity();
                stack.multiplyPositionMatrix(new Matrix4f(matrices.modelView()));
                core.renderOpaque(stack, camera.x, camera.y, camera.z);
            }
        }
    }
}
