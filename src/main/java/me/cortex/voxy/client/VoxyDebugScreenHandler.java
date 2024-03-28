package me.cortex.voxy.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import me.cortex.voxy.client.core.IGetVoxelCore;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class VoxyDebugScreenHandler {
    public static final VoxyDebugScreenHandler INSTANCE = new VoxyDebugScreenHandler();

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderGameOverlayTextEvent(RenderGameOverlayEvent.Text event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo) {
            var core = ((IGetVoxelCore) mc.renderGlobal).getVoxelCore();
            if (core != null) {
                core.addDebugInfo(event.right);
            }
        }
    }
}
