package skid.krypton.manager.tunnel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Set;

public class FoodEater {
    private final MinecraftClient mc;
    private int foodSlot = -1;
    private int eatCooldown = 0;

    private static final Set<Item> FOOD_ITEMS = Set.of(
            Items.APPLE, Items.BREAD, Items.COOKED_BEEF, Items.COOKED_CHICKEN,
            Items.COOKED_PORKCHOP, Items.COOKED_MUTTON, Items.GOLDEN_APPLE,
            Items.GOLDEN_CARROT, Items.CARROT, Items.POTATO, Items.COOKIE
    );

    public FoodEater(MinecraftClient mc) {
        this.mc = mc;
    }

    public boolean needsFood() {
        return mc.player != null && mc.player.getHungerManager().getFoodLevel() <= 16;
    }

    public void tick() {
        if (mc.player == null) return;

        if (eatCooldown > 0) eatCooldown--;

        if (!needsFood() || eatCooldown > 0) return;
        eat();
    }

    public void eat() {
        if (mc.currentScreen != null) return;

        if (mc.player.isUsingItem()) return;

        if (foodSlot == -1) findFood();

        if (foodSlot != -1) {
            mc.player.getInventory().selectedSlot = foodSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            eatCooldown = 40; // 2s cooldown
        }
    }

    private void findFood() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && FOOD_ITEMS.contains(stack.getItem())) {
                foodSlot = i;
                return;
            }
        }
        foodSlot = -1;
    }

    public void reset() {
        foodSlot = -1;
    }
}

