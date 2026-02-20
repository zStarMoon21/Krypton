package skid.krypton.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import skid.krypton.module.modules.misc.SkinProtect;

@Mixin(AbstractClientPlayerEntity.class)
public class SkinProtectMixin {
    
    @Inject(method = "getSkinTexture", at = @At("HEAD"), cancellable = true)
    private void onGetSkinTexture(CallbackInfoReturnable<Identifier> cir) {
        SkinProtect skinProtect = SkinProtect.getInstance();
        
        if (skinProtect != null && skinProtect.isEnabled()) {
            String targetPlayer = skinProtect.getTargetPlayer();
            MinecraftClient mc = MinecraftClient.getInstance();
            
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(targetPlayer);
                
                if (playerEntry != null && playerEntry.getSkinTexture() != null) {
                    cir.setReturnValue(playerEntry.getSkinTexture());
                }
            }
        }
    }
    
    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void onGetModel(CallbackInfoReturnable<String> cir) {
        SkinProtect skinProtect = SkinProtect.getInstance();
        
        if (skinProtect != null && skinProtect.isEnabled()) {
            String targetPlayer = skinProtect.getTargetPlayer();
            MinecraftClient mc = MinecraftClient.getInstance();
            
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(targetPlayer);
                
                if (playerEntry != null) {
                    // Also copy the player model type (slim/wide arms)
                    cir.setReturnValue(playerEntry.getModel());
                }
            }
        }
    }
}
