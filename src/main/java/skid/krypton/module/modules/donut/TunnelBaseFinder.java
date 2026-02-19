package skid.krypton.module.modules.donut;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.Krypton;
import skid.krypton.module.modules.misc.AutoEat;
import skid.krypton.module.modules.combat.AutoTotem;
import skid.krypton.utils.embed.DiscordWebhook;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.TickEvent;
import skid.krypton.mixin.MobSpawnerLogicAccessor;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.manager.tunnel.*;
import skid.krypton.utils.BlockUtil;
import skid.krypton.utils.EnchantmentUtil;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.InventoryUtil;
import skid.krypton.utils.TunnelUtils;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public final class TunnelBaseFinder extends Module {
    // Settings
    private final NumberSetting minimumStorage = new NumberSetting(EncryptedString.of("Minimum Storage"), 1.0, 500.0, 100.0, 1.0);
    private final BooleanSetting spawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
    private final BooleanSetting autoEat = new BooleanSetting(EncryptedString.of("Auto Eat"), true);
    private final BooleanSetting disconnectOnBase = new BooleanSetting(EncryptedString.of("Disconnect on Base"), true);
    private final BooleanSetting discordNotification = new BooleanSetting(EncryptedString.of("Discord Notification"), false);
    private final StringSetting webhook = new StringSetting(EncryptedString.of("Webhook"), "");
    private final BooleanSetting totemCheck = new BooleanSetting(EncryptedString.of("Totem Check"), true);
    
    // Legacy settings
    private final BooleanSetting autoTotemBuy = new BooleanSetting(EncryptedString.of("Auto Totem Buy"), true);
    private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1.0, 9.0, 8.0, 1.0);
    private final BooleanSetting autoMend = new BooleanSetting(EncryptedString.of("Auto Mend"), true);
    private final NumberSetting xpBottleSlot = new NumberSetting(EncryptedString.of("XP Bottle Slot"), 1.0, 9.0, 9.0, 1.0);
    private final NumberSetting totemCheckTime = new NumberSetting(EncryptedString.of("Totem Check Time"), 1.0, 120.0, 20.0, 1.0);

    // Direction and movement
    private TunnelDirection currentDirection;
    private int blocksMined;
    private int spawnerCount;
    private int idleTicks;
    private Vec3d lastPosition;
    private boolean isDigging = false;
    private boolean shouldDig = false;
    private int totemCheckCounter = 0;
    private int totemBuyCounter = 0;
    private double actionDelay = 0.0;

    // Managers from skid.krypton.manager.tunnel
    private RandomStopManager stopManager;
    private MouseGlideManager mouseGlide;
    private PlayerDetectionManager playerDetector;
    private HazardAvoidanceManager hazardAvoid;
    private AutoEatManager autoEatManager;
    private TunnelPathManager pathFinder;
    private TunnelMiningManager miner;
    private List<BlockPos> currentPath = new ArrayList<>();

    public TunnelBaseFinder() {
        super(EncryptedString.of("Tunnel Base Finder"), EncryptedString.of("Advanced tunnel base finder with anti-detection"), -1, Category.DONUT);
        this.addSettings(
            this.minimumStorage, 
            this.spawners, 
            this.autoEat, 
            this.disconnectOnBase, 
            this.discordNotification, 
            this.webhook, 
            this.totemCheck,
            this.autoTotemBuy, 
            this.totemSlot, 
            this.autoMend, 
            this.xpBottleSlot, 
            this.totemCheckTime
        );
    }

    @Override
    public void onEnable() {
        if (this.mc.player == null) {
            this.toggle();
            return;
        }
        
        // Initialize all managers from manager.tunnel package
        this.stopManager = new RandomStopManager();
        this.mouseGlide = new MouseGlideManager(this.mc);
        this.playerDetector = new PlayerDetectionManager(this.mc);
        this.hazardAvoid = new HazardAvoidanceManager(this.mc);
        this.autoEatManager = new AutoEatManager(this.mc);
        this.pathFinder = new TunnelPathManager(this.mc);
        this.miner = new TunnelMiningManager(this.mc);
        
        this.currentDirection = TunnelUtils.getInitialDirection(this.mc.player);
        this.blocksMined = 0;
        this.idleTicks = 0;
        this.spawnerCount = 0;
        this.lastPosition = null;
        this.currentPath.clear();
        this.isDigging = false;
        this.shouldDig = false;
        this.totemBuyCounter = 0;
        this.actionDelay = 0.0;
        
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Reset all key presses
        this.mc.options.leftKey.setPressed(false);
        this.mc.options.rightKey.setPressed(false);
        this.mc.options.forwardKey.setPressed(false);
        if (this.mc.interactionManager != null) {
            this.mc.interactionManager.cancelBlockBreaking();
        }
    }

    @EventListener
    public void onTick(final TickEvent event) {
        if (this.currentDirection == null || this.mc.player == null || this.mc.world == null) {
            return;
        }

        // 1. PLAYER DETECTION (Highest Priority)
        PlayerEntity detected = playerDetector.checkForPlayers(20);
        if (detected != null) {
            handlePlayerDetected(detected);
            return;
        }

        // 2. AUTO EAT
        if (autoEat.getValue() && autoEatManager.shouldEat()) {
            autoEatManager.eat();
            pauseMining();
            return;
        }

        // 3. RANDOM STOPS
        if (stopManager.shouldStop()) {
            stopManager.executeStop();
            pauseMining();
            return;
        }

        // 4. TOTEM SAFETY CHECK
        if (!handleTotemSafety()) {
            return;
        }

        // 5. AUTO MEND CHECK
        if (this.autoMend.getValue() && !this.isDigging) {
            checkMending();
        }

        // 6. XP BOTTLE MENDING
        if (this.isDigging) {
            handleMending();
            return;
        }

        // 7. AUTO TOTEM BUY (Legacy)
        if (this.autoTotemBuy.getValue() && !handleTotemBuy()) {
            return;
        }

        // 8. HAZARD AVOIDANCE
        BlockPos hazard = hazardAvoid.detectHazard();
        if (hazard != null) {
            currentPath = hazardAvoid.findSafePath(hazard, currentDirection);
            miner.minePath(currentPath);
            return;
        }

        // 9. PATH FINDING
        if (currentPath.isEmpty() || miner.isPathFinished(currentPath)) {
            currentPath = pathFinder.findPath(currentDirection, 5);
        }

        // 10. MINING WITH MOUSE GLIDE
        if (!currentPath.isEmpty()) {
            BlockPos target = currentPath.get(0);
            mouseGlide.glideToBlock(target);
            miner.mineBlock(target);
            
            if (miner.isBlockMined(target)) {
                currentPath.remove(0);
            }
        }

        // 11. DIRECTION ALIGNMENT
        alignToDirection();
        
        // 12. BASE/SPAWNER DETECTION
        checkForDiscoveries();
    }

    private void handlePlayerDetected(PlayerEntity player) {
        String location = String.format("x: %d y: %d z: %d", 
            (int)player.getX(), (int)player.getY(), (int)player.getZ());
        sendDiscordNotification("Player Detected", 
            "Player found at " + location, 
            "Player Detected", location, Color.RED);
        this.disconnectWithMessage(Text.of("(TunnelBaseFinder) Player Detected!"));
    }

    private void pauseMining() {
        this.mc.options.forwardKey.setPressed(false);
        this.mc.interactionManager.cancelBlockBreaking();
    }

    private boolean handleTotemSafety() {
        if (!this.totemCheck.getValue()) return true;
        
        final boolean hasTotem = this.mc.player.getOffHandStack().getItem().equals(Items.TOTEM_OF_UNDYING);
        final Module autoTotem = Krypton.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoTotem.class);
        
        if (!hasTotem) {
            if (autoTotem.isEnabled() && ((AutoTotem) autoTotem).findItemSlot(Items.TOTEM_OF_UNDYING) != -1) {
                this.actionDelay = 0.0;
            } else {
                this.actionDelay++;
            }
            
            if (this.actionDelay > this.totemCheckTime.getValue()) {
                sendDiscordNotification("Totem Missing", 
                    "No totem in offhand - disabling", 
                    "Totem Check Failed", "Module disabled", Color.RED);
                this.toggle();
                return false;
            }
        } else {
            this.actionDelay = 0.0;
        }
        return true;
    }

    private void checkMending() {
        final ItemStack mainHand = this.mc.player.getMainHandStack();
        if (EnchantmentUtil.hasEnchantment(mainHand, Enchantments.MENDING) && 
            mainHand.getMaxDamage() - mainHand.getDamage() < 100) {
            this.isDigging = true;
            this.totemCheckCounter = this.mc.player.getInventory().selectedSlot;
        }
    }

    private void handleMending() {
        final int xpSlot = this.xpBottleSlot.getIntValue() - 1;
        final ItemStack xpStack = this.mc.player.getInventory().getStack(xpSlot);
        
        if (this.mc.player.getInventory().selectedSlot != xpSlot) {
            InventoryUtil.swap(xpSlot);
        }

        if (!xpStack.isOf(Items.EXPERIENCE_BOTTLE)) {
            handleXpShop();
            return;
        }

        if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
            this.blocksMined = 20;
            return;
        }

        if (!EnchantmentUtil.hasEnchantment(this.mc.player.getOffHandStack(), Enchantments.MENDING)) {
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 
                36 + this.totemCheckCounter, 40, SlotActionType.SWAP, this.mc.player);
            this.blocksMined = 20;
            return;
        }

        if (this.mc.player.getOffHandStack().getDamage() > 0) {
            final ActionResult result = this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
            if (result.isAccepted() && result.shouldSwingHand()) {
                this.mc.player.swingHand(Hand.MAIN_HAND);
            }
            this.blocksMined = 1;
            return;
        }

        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 
            36 + this.totemCheckCounter, 40, SlotActionType.SWAP, this.mc.player);
        this.isDigging = false;
    }

    private void handleXpShop() {
        final ScreenHandler screen = this.mc.player.currentScreenHandler;
        
        if (!(screen instanceof GenericContainerScreenHandler) || ((GenericContainerScreenHandler) screen).getRows() != 3) {
            this.mc.getNetworkHandler().sendChatCommand("shop");
            this.blocksMined = 10;
            return;
        }

        if (screen.getSlot(11).getStack().isOf(Items.END_STONE)) {
            this.mc.interactionManager.clickSlot(screen.syncId, 13, 0, SlotActionType.PICKUP, this.mc.player);
        } else if (screen.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
            this.mc.interactionManager.clickSlot(screen.syncId, 16, 0, SlotActionType.PICKUP, this.mc.player);
        } else if (screen.getSlot(17).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
            this.mc.interactionManager.clickSlot(screen.syncId, 17, 0, SlotActionType.PICKUP, this.mc.player);
        } else if (screen.getSlot(23).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
            this.mc.interactionManager.clickSlot(screen.syncId, 23, 0, SlotActionType.PICKUP, this.mc.player);
        }
        
        this.blocksMined = 10;
    }

    private boolean handleTotemBuy() {
        final int totemInvSlot = this.totemSlot.getIntValue() - 1;
        
        if (this.mc.player.getInventory().getStack(totemInvSlot).isOf(Items.TOTEM_OF_UNDYING)) {
            if (this.shouldDig) {
                if (this.mc.currentScreen != null) {
                    this.mc.player.closeHandledScreen();
                    this.blocksMined = 20;
                }
                this.shouldDig = false;
                this.totemBuyCounter = 0;
            }
            return true;
        }

        if (this.totemBuyCounter < 30 && !this.shouldDig) {
            this.totemBuyCounter++;
            return false;
        }

        this.totemBuyCounter = 0;
        this.shouldDig = true;
        
        if (this.mc.player.getInventory().selectedSlot != totemInvSlot) {
            InventoryUtil.swap(totemInvSlot);
        }

        final ScreenHandler screen = this.mc.player.currentScreenHandler;
        
        if (!(screen instanceof GenericContainerScreenHandler) || ((GenericContainerScreenHandler) screen).getRows() != 3) {
            this.mc.getNetworkHandler().sendChatCommand("shop");
            this.blocksMined = 10;
            return false;
        }

        if (screen.getSlot(11).getStack().isOf(Items.END_STONE)) {
            this.mc.interactionManager.clickSlot(screen.syncId, 13, 0, SlotActionType.PICKUP, this.mc.player);
        } else if (screen.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
            this.mc.interactionManager.clickSlot(screen.syncId, 13, 0, SlotActionType.PICKUP, this.mc.player);
        } else if (screen.getSlot(23).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
            this.mc.interactionManager.clickSlot(screen.syncId, 23, 0, SlotActionType.PICKUP, this.mc.player);
        }
        
        this.blocksMined = 10;
        return false;
    }

    private void alignToDirection() {
        float targetYaw = TunnelUtils.getDirectionYaw(currentDirection);
        float currentYaw = this.mc.player.getYaw();
        
        float diff = targetYaw - currentYaw;
        if (Math.abs(diff) > 1) {
            this.mc.player.setYaw(currentYaw + diff * 0.1f);
        }
        this.mc.player.setPitch(2.0f);
    }

    private void checkForDiscoveries() {
        int storageCount = 0;
        BlockPos spawnerPos = null;
        
        for (WorldChunk chunk : BlockUtil.getLoadedChunks().toList()) {
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
                BlockEntity entity = this.mc.world.getBlockEntity(pos);
                
                if (entity == null) continue;
                
                // Check for spawners
                if (spawners.getValue() && entity instanceof MobSpawnerBlockEntity) {
                    try {
                        String entityType = ((MobSpawnerLogicAccessor)((MobSpawnerBlockEntity) entity).getLogic())
                            .getSpawnEntry().getNbt().getString("id");
                        if (!entityType.contains("spider") && !entityType.contains("cave_spider")) {
                            spawnerPos = pos;
                            spawnerCount++;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                
                // Check for storage
                if (entity instanceof ChestBlockEntity || 
                    entity instanceof EnderChestBlockEntity ||
                    entity instanceof ShulkerBoxBlockEntity ||
                    entity instanceof BarrelBlockEntity ||
                    entity instanceof FurnaceBlockEntity) {
                    storageCount++;
                }
            }
        }
        
        // Handle spawner discovery
        if (spawnerCount > 3 && spawnerPos != null) {
            String location = String.format("x: %d y: %d z: %d", 
                spawnerPos.getX(), spawnerPos.getY(), spawnerPos.getZ());
            sendDiscordNotification("Spawner Found!", location, "Spawner Found", location, Color.ORANGE);
            
            if (disconnectOnBase.getValue()) {
                this.disconnectWithMessage(Text.of("(TunnelBaseFinder) Spawner Found!"));
            }
        }
        
        // Handle base discovery
        if (storageCount >= minimumStorage.getIntValue()) {
            String location = String.format("x: %d y: %d z: %d", 
                (int)this.mc.player.getX(), (int)this.mc.player.getY(), (int)this.mc.player.getZ());
            sendDiscordNotification("Base Found!", location, "Base Found", location, Color.GREEN);
            
            if (disconnectOnBase.getValue()) {
                this.disconnectWithMessage(Text.of("(TunnelBaseFinder) Base Found!"));
            }
        }
    }

    private void sendDiscordNotification(String title, String description, String fieldName, String fieldValue, Color color) {
        if (!this.discordNotification.getValue() || this.webhook.value.isEmpty()) return;
        
        try {
            DiscordWebhook webhook = new DiscordWebhook(this.webhook.value);
            DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
            
            embed.setTitle(title);
            embed.setThumbnail("https://render.crafty.gg/3d/bust/" + 
                MinecraftClient.getInstance().getSession().getUuidOrNull() + "?format=webp");
            embed.setDescription(description + " - " + MinecraftClient.getInstance().getSession().getUsername());
            embed.setColor(color);
            embed.setFooter(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), null);
            embed.addField(fieldName, fieldValue, true);
            
            webhook.addEmbed(embed);
            webhook.execute();
        } catch (Exception e) {
            // Ignore
        }
    }

    private void disconnectWithMessage(final Text text) {
        final MutableText literal = Text.literal("[TunnelBaseFinder] ");
        literal.append(text);
        this.toggle();
        this.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(literal));
    }

    // Legacy methods kept for compatibility
    private int calculateDirection(final TunnelDirection dir) {
        switch(dir) {
            case NORTH: return 180;
            case SOUTH: return 0;
            case EAST: return 270;
            case WEST: return 90;
            default: return Math.round(this.mc.player.getYaw());
        }
    }

    public boolean isDigging() {
        return this.isDigging;
    }
}
