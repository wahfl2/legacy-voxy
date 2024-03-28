package me.cortex.voxy.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import me.cortex.voxy.client.VoxyDebugScreenHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        MinecraftForge.EVENT_BUS.register(VoxyDebugScreenHandler.INSTANCE);
    }
}
