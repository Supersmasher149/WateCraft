package com.wally.kissmod;

import com.wally.kissmod.network.ModPayloads;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

@Mod(kissmod.MODID)
public class kissmod {
    public static final String MODID = "kissmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public kissmod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENTS.register(modEventBus);
        Sounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(Config::onConfigLoad);

        NeoForge.EVENT_BUS.register(new KissDetectionHandler());

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
