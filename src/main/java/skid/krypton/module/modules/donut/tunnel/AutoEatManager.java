package skid.krypton.module.modules.donut;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class AutoEatManager {
    private final MinecraftClient mc;
    private int eatTimer = 0;
    
    public AutoEatManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public boolean shouldEat() {
        if (mc.player == null) return false;
        
        int foodLevel = mc.player.getHungerManager().getFoodLevel();
        return foodLevel < 18; // Eat when less than 9 drumsticks
    }
    
    public void eat() {
        if (eatTimer > 0) {
            eatTimer--;
            return;
        }
        
        if (mc.player == null) return;
        
        // Find food in hotbar - use getFoodComponent() instead of isFood()
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().getFoodComponent() != null) {
                mc.player.getInventory().selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                eatTimer = 30; // 1.5 second cooldown between eats
                break;
            }
        }
    }
}
