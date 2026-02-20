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
                PlayerListEntry targetEntry = mc.getNetworkHandler().getPlayerListEntry(targetPlayer);
                
                if (targetEntry != null) {
                    // In Yarn mappings, it's just getSkinTexture() on PlayerListEntry
                    Identifier skinTexture = targetEntry.getSkinTexture();
                    if (skinTexture != null) {
                        cir.setReturnValue(skinTexture);
                    }
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
                PlayerListEntry targetEntry = mc.getNetworkHandler().getPlayerListEntry(targetPlayer);
                
                if (targetEntry != null) {
                    // getModel() exists in PlayerListEntry in Yarn
                    String model = targetEntry.getModel();
                    if (model != null) {
                        cir.setReturnValue(model);
                    }
                }
            }
        }
    }
}
