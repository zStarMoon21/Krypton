package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoEatManager {
    private final MinecraftClient mc;
    private int eatTimer = 0;
    private int foodSlot = -1;
    private boolean isEating = false;
    
    // List of food items
    private final ItemStack[] FOOD_ITEMS = {
        new ItemStack(Items.APPLE),
        new ItemStack(Items.BAKED_POTATO),
        new ItemStack(Items.BEEF),
        new ItemStack(Items.BREAD),
        new ItemStack(Items.CARROT),
        new ItemStack(Items.CHICKEN),
        new ItemStack(Items.COOKED_BEEF),
        new ItemStack(Items.COOKED_CHICKEN),
        new ItemStack(Items.COOKED_COD),
        new ItemStack(Items.COOKED_MUTTON),
        new ItemStack(Items.COOKED_PORKCHOP),
        new ItemStack(Items.COOKED_RABBIT),
        new ItemStack(Items.COOKED_SALMON),
        new ItemStack(Items.COOKIE),
        new ItemStack(Items.DRIED_KELP),
        new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
        new ItemStack(Items.GOLDEN_APPLE),
        new ItemStack(Items.GOLDEN_CARROT),
        new ItemStack(Items.MELON_SLICE),
        new ItemStack(Items.MUSHROOM_STEW),
        new ItemStack(Items.MUTTON),
        new ItemStack(Items.PORKCHOP),
        new ItemStack(Items.POTATO),
        new ItemStack(Items.PUMPKIN_PIE),
        new ItemStack(Items.RABBIT),
        new ItemStack(Items.RABBIT_STEW),
        new ItemStack(Items.SALMON),
        new ItemStack(Items.SUSPICIOUS_STEW),
        new ItemStack(Items.SWEET_BERRIES),
        new ItemStack(Items.GLOW_BERRIES),
        new ItemStack(Items.CHORUS_FRUIT)
    };
    
    public AutoEatManager(MinecraftClient mc) {
        this.mc = mc;
    }
    
    public boolean shouldEat() {
        if (mc.player == null) return false;
        
        int foodLevel = mc.player.getHungerManager().getFoodLevel();
        return foodLevel < 18; // Eat when less than 9 drumsticks
    }
    
    public void eat() {
        if (mc.player == null) return;
        
        // If we're already eating, continue
        if (mc.player.isUsingItem()) {
            isEating = true;
            return;
        } else {
            isEating = false;
        }
        
        // Find food in hotbar if we don't have a slot
        if (foodSlot == -1) {
            findFood();
        }
        
        // If we found food, eat it
        if (foodSlot != -1) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            if (currentSlot != foodSlot) {
                mc.player.getInventory().selectedSlot = foodSlot;
            }
            
            // Right-click to eat
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            isEating = true;
        }
    }
    
    private void findFood() {
        if (mc.player == null) return;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFood(stack)) {
                foodSlot = i;
                return;
            }
        }
        foodSlot = -1;
    }
    
    private boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        for (ItemStack food : FOOD_ITEMS) {
            if (stack.getItem() == food.getItem()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isEating() {
        return isEating;
    }
}
