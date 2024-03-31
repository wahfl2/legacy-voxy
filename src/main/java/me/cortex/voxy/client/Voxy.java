package me.cortex.voxy.client;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.gtnewhorizons.angelica.proxy.CommonProxy;
import com.myname.mymodid.Tags;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.handshake.FMLHandshakeMessage;
import me.cortex.voxy.client.core.VoxelCore;
import me.cortex.voxy.client.saver.ContextSelectionSystem;
import me.cortex.voxy.common.config.Serialization;
import net.minecraft.client.multiplayer.WorldClient;

@Mod(
    modid = "voxy",
    name = "voxy",
    version = Tags.VERSION,
    dependencies = " before:lwjgl3ify@[1.5.3,);" + " after:angelica@[1.0.0-alpha35,);",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*"
)
public class Voxy {
    public static final String VERSION = Tags.VERSION;

    public static final Supplier<Boolean> NEIDS_PRESENT = Suppliers.memoize(
        () -> Loader.isModLoaded("NotEnoughIds")
    );

    @SidedProxy(clientSide = "me.cortex.voxy.proxy.ClientProxy", serverSide = "me.cortex.voxy.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        Serialization.init();
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }


    private static final ContextSelectionSystem selector = new ContextSelectionSystem();

    public static VoxelCore createVoxelCore(WorldClient world) {
        var selection = selector.getBestSelectionOrCreate(world);
        return new VoxelCore(selection);
    }
}
