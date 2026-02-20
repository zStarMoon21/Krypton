package skid.krypton.module.modules.misc;

import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.EncryptedString;

public class SkinProtect extends Module {
    private final StringSetting targetPlayer = new StringSetting("Target Player", "Steve");
    private final StringSetting fakeName = new StringSetting("Display Name", "");
    
    public SkinProtect() {
        super(EncryptedString.of("Skin Protect"), EncryptedString.of("Spoofs your skin to look like another player."), -1, Category.MISC);
        this.addSettings(this.targetPlayer, this.fakeName);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public String getTargetPlayer() {
        return this.targetPlayer.getValue();
    }
    
    public String getFakeName() {
        String name = this.fakeName.getValue();
        return name.isEmpty() ? this.targetPlayer.getValue() : name;
    }
}
