package com.wally.kissmod.mixin;

import com.wally.kissmod.KissAnimator;
import com.wally.kissmod.KissPlayerData;
import com.wally.kissmod.ModAttachments;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerRendererMixin {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void kissmod$onSetupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        KissPlayerData data = player.getData(ModAttachments.kissData());
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        KissAnimator.apply(model, entity, data, ageInTicks);
    }
}
