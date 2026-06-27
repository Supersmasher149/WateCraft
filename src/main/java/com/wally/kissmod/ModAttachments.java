package com.wally.kissmod;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, kissmod.MODID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<KissPlayerData>> KISS_DATA_HOLDER =
            (DeferredHolder<AttachmentType<?>, AttachmentType<KissPlayerData>>) (Object) ATTACHMENTS.register("kiss_data",
                    () -> AttachmentType.builder(KissPlayerData::new).serialize(KissPlayerData.CODEC).copyOnDeath().build());

    public static AttachmentType<KissPlayerData> kissData() {
        return KISS_DATA_HOLDER.get();
    }
}
