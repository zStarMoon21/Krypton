package skid.krypton.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import skid.krypton.module.modules.misc.SkinProtect;

import java.util.Map;

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
                    // Get the game profile which contains skin information
                    GameProfile profile = targetEntry.getProfile();
                    
                    // Try to get the skin texture from the session service
                    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = 
                        mc.getSkinProvider().getTextures(profile);
                    
                    if (textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                        MinecraftProfileTexture skinTexture = textures.get(MinecraftProfileTexture.Type.SKIN);
                        Identifier skinId = mc.getSkinProvider().loadSkin(skinTexture, MinecraftProfileTexture.Type.SKIN);
                        cir.setReturnValue(skinId);
                    }
                }
            }
        }
    }
}
