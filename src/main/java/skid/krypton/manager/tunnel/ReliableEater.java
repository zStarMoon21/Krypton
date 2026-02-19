package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Set;

public class ReliableEater {
    private final MinecraftClient mc;
    private int foodSlot = -1;
    private int eatCooldown = 0;
    private boolean isEating = false;
    
    // Complete food list
    private static final Set<Item> FOOD_ITEMS = Set.of(
        Items.APPLE, Items.BAKED_POTATO, Items.BEEF, Items.BREAD,
        Items.CARROT, Items.CHICKEN, Items.COOKED_BEEF, Items.COOKED_CHICKEN,
        Items.COOKED_COD, Items.COOKED_MUTTON, Items.COOKED_PORKCHOP,
        Items.COOKED_RABBIT, Items.COOKED_SALMON, Items.COOKIE,
        Items.DRIED_KELP, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE,
        Items.GOLDEN_CARROT, Items.MELON_SLICE, Items.MUSHROOM_STEW,
        Items.MUTTON, Items.PORKCHOP, Items.POTATO, Items.PUMPKIN_PIE,
        Items.RABBIT, Items.RABBIT_STEW, Items.SALMON, Items.SUSPICIOUS_STEW,
        Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.CHORUS_FRUIT,
        Items.HONEY_BOTTLE, Items.COOKED_RABBIT, Items.COOKED_MUTTON
    );
    
    public ReliableEater(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public boolean needsFood() {
        if (mc.player == null) return false;
        return mc.player.getHungerManager().getFoodLevel() <= 16; // 8 drumsticks
    }
    
    public void tick() {
        if (mc.player == null) return;
        
        if (eatCooldown > 0) {
            eatCooldown--;
        }
        
        // Check if still eating
        if (mc.player.isUsingItem()) {
            isEating = true;
            return;
        } else {
            isEating = false;
        }
        
        if (!needsFood() || eatCooldown > 0) return;
        
        findAndEat();
    }
    
    private void findAndEat() {
        if (mc.player == null || mc.currentScreen != null) return;
        
        // Find food in hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && FOOD_ITEMS.contains(stack.getItem())) {
                foodSlot = i;
                break;
            }
        }
        
        // Eat if found
        if (foodSlot != -1) {
            mc.player.getInventory().selectedSlot = foodSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            eatCooldown = 40; // 2 second cooldown
            isEating = true;
        }
    }
    
    public void reset() {
        foodSlot = -1;
        isEating = false;
        eatCooldown = 0;
    }
    
    public boolean isEating() {
        return isEating;
    }
}
