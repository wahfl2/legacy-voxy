package me.cortex.voxy.client.mixin.minecraft;

import com.gtnewhorizons.angelica.compat.mojang.Camera;
import me.cortex.voxy.client.Voxy;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.client.core.VoxelCore;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Apply after Angelica mixins/overwrites
@Mixin(value = RenderGlobal.class, priority = 2000)
public abstract class MixinRenderGlobal implements IGetVoxelCore {

    @Shadow private WorldClient theWorld;
    @Unique private VoxelCore core;

    @Inject(method = "sortAndRender", at = @At("HEAD"))
    private void injectSetup(EntityLivingBase entity, int pass, double partialTicks, CallbackInfoReturnable<Integer> cir) {
        if (this.core != null) {
            Camera camera = new Camera(entity, (float) partialTicks);
            this.core.renderSetup(camera);
        }
    }

    @Unique
    public void populateCore() {
        if (this.core != null) {
            throw new IllegalStateException("Trying to create new core while a core already exists");
        }
        this.core = Voxy.createVoxelCore(this.theWorld);
    }

    public VoxelCore getVoxelCore() {
        return this.core;
    }

    @Inject(method = "loadRenderers", at = @At("TAIL"))
    private void resetVoxelCore(CallbackInfo ci) {
        if (this.theWorld != null && this.core != null) {
            this.core.shutdown();
            this.core = null;
            if (VoxyConfig.enabled) {
                this.populateCore();
            }
        }
    }

    @Inject(method = "setWorldAndLoadRenderers", at = @At("TAIL"))
    private void initVoxelCore(WorldClient world, CallbackInfo ci) {
        if (world == null) {
            if (this.core != null) {
                this.core.shutdown();
                this.core = null;
            }
            return;
        }

        if (this.core != null) {
            this.core.shutdown();
            this.core = null;
        }
        if (VoxyConfig.enabled) {
            this.populateCore();
        }
    }

    @Override
    public void reloadVoxelCore() {
        if (this.core != null) {
            this.core.shutdown();
            this.core = null;
        }
        if (this.theWorld != null && VoxyConfig.enabled) {
            this.populateCore();
        }
    }

//    @Inject(method = "close", at = @At("HEAD"))
//    private void injectClose(CallbackInfo ci) {
//        if (this.core != null) {
//            this.core.shutdown();
//            this.core = null;
//        }
//    }
}
