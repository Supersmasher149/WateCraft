package com.wally.kissmod.mixin;

import com.wally.kissmod.Config;
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
        // Check if the entity is an instance of AbstractClientPlayer
        if (!(entity instanceof AbstractClientPlayer player)) return;

        // Check if head tilting is disabled in the configuration
        if (Config.DISABLE_HEAD_TILT.get()) return;

        // Get the KissPlayerData for the player
        KissPlayerData data = player.getData(ModAttachments.kissData());

        // Check if the player is currently kissing someone
        if (!data.isKissing()) return;

        // Cast the current instance to PlayerModel and apply custom animations
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        float tilt = 0.12F; // Define the amount of tilt

        // Apply head rotation to create a kiss effect
        model.head.xRot += tilt; // Tilt the head forward slightly
        model.head.zRot += tilt * 0.3F; // Tilt the head side-to-side slightly
        model.body.xRot += tilt * 0.4F; // Tilt the body forward slightly to match head movement
    }
}
