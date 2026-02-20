package skid.krypton.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
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
                // Try to get the target player's entry
                PlayerListEntry targetEntry = mc.getNetworkHandler().getPlayerListEntry(targetPlayer);
                
                if (targetEntry != null) {
                    // In some versions, we need to get the texture through the game profile
                    // or through the skin textures
                    Identifier skinTexture = null;
                    
                    // Try different methods based on Minecraft version
                    try {
                        // Method 1: Try to get skin textures (1.20+)
                        SkinTextures skinTextures = targetEntry.getSkinTextures();
                        if (skinTextures != null) {
                            skinTexture = skinTextures.texture();
                        }
                    } catch (NoSuchMethodError e) {
                        // Method 2: Older versions might have getSkinTexture() directly
                        try {
                            skinTexture = targetEntry.getSkinTexture();
                        } catch (NoSuchMethodError e2) {
                            // Method 3: Try to get through reflection or other means
                            GameProfile profile = targetEntry.getProfile();
                            // In this case, we might need to use a different approach
                        }
                    }
                    
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
                    String model = null;
                    
                    try {
                        // Try to get model from skin textures (1.20+)
                        SkinTextures skinTextures = targetEntry.getSkinTextures();
                        if (skinTextures != null) {
                            model = skinTextures.model().getName();
                        }
                    } catch (NoSuchMethodError e) {
                        // Try direct getModel() method
                        try {
                            model = targetEntry.getModel();
                        } catch (NoSuchMethodError e2) {
                            // Default to something
                        }
                    }
                    
                    if (model != null) {
                        cir.setReturnValue(model);
                    }
                }
            }
        }
    }
}
