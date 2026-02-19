package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoEatManager {
    private final MinecraftClient mc;
    private int eatTimer = 0;
    
    // List of food items in Minecraft
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
        new ItemStack(Items.SWEET_BERRIES)
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
        if (eatTimer > 0) {
            eatTimer--;
            return;
        }
        
        if (mc.player == null) return;
        
        // Find food in hotbar by checking against food items list
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFood(stack)) {
                mc.player.getInventory().selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                eatTimer = 30; // 1.5 second cooldown between eats
                break;
            }
        }
    }
    
    private boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Check against known food items
        for (ItemStack food : FOOD_ITEMS) {
            if (stack.getItem() == food.getItem()) {
                return true;
            }
        }
        return false;
    }
}
