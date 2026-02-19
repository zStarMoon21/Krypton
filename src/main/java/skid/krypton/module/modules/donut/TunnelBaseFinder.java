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
import net.minecraft.util.math.MathHelper;
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
import skid.krypton.utils.BlockUtil;
import skid.krypton.utils.EnchantmentUtil;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.InventoryUtil;
import skid.krypton.utils.RenderUtils;

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
    
    // Legacy settings (keeping for compatibility)
    private final BooleanSetting autoTotemBuy = new BooleanSetting(EncryptedString.of("Auto Totem Buy"), true);
    private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1.0, 9.0, 8.0, 1.0);
    private final BooleanSetting autoMend = new BooleanSetting(EncryptedString.of("Auto Mend"), true);
    private final NumberSetting xpBottleSlot = new NumberSetting(EncryptedString.of("XP Bottle Slot"), 1.0, 9.0, 9.0, 1.0);
    private final NumberSetting totemCheckTime = new NumberSetting(EncryptedString.of("Totem Check Time"), 1.0, 120.0, 20.0, 1.0);

    // Direction and movement
    private Direction currentDirection;
    private int blocksMined;
    private int spawnerCount;
    private int idleTicks;
    private Vec3d lastPosition;
    private boolean isDigging = false;
    private boolean shouldDig = false;
    private int totemCheckCounter = 0;
    private int totemBuyCounter = 0;
    private double actionDelay = 0.0;

    // NEW FEATURES: Anti-detection systems
    private RandomStopManager stopManager;
    private MouseGlideManager mouseGlide;
    private PlayerDetector playerDetector;
    private HazardAvoidance hazardAvoid;
    private AutoEatManager autoEatManager;
    private PathRenderer pathRenderer;
    private List<BlockPos> currentPath = new ArrayList<>();
    private BlockPos currentTarget = null;

    public TunnelBaseFinder() {
        super(EncryptedString.of("Tunnel Base Finder"), EncryptedString.of("Advanced tunnel base finder with anti-detection"), -1, Category.DONUT);
        this.addSettings(this.minimumStorage, this.spawners, this.autoEat, this.disconnectOnBase, 
                        this.discordNotification, this.webhook, this.totemCheck,
                        this.autoTotemBuy, this.totemSlot, this.autoMend, this.xpBottleSlot, this.totemCheckTime);
    }

    @Override
    public void onEnable() {
        if (this.mc.player == null) {
            this.toggle();
            return;
        }
        
        // Initialize anti-detection systems
        this.stopManager = new RandomStopManager();
        this.mouseGlide = new MouseGlideManager(this.mc);
        this.playerDetector = new PlayerDetector(this.mc);
        this.hazardAvoid = new HazardAvoidance(this.mc);
        this.autoEatManager = new AutoEatManager(this.mc);
        this.pathRenderer = new PathRenderer();
        
        this.currentDirection = this.getInitialDirection();
        this.blocksMined = 0;
        this.idleTicks = 0;
        this.spawnerCount = 0;
        this.lastPosition = null;
        this.currentPath.clear();
        
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
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

        // ===== PLAYER DETECTION =====
        PlayerEntity detected = playerDetector.checkForPlayers(20);
        if (detected != null) {
            String location = String.format("x: %d y: %d z: %d", 
                (int)detected.getX(), (int)detected.getY(), (int)detected.getZ());
            sendDiscordNotification("Player Detected", 
                "Player found at " + location, 
                "Player Detected", location, Color.RED);
            this.disconnectWithMessage(Text.of("(TunnelBaseFinder) Player Detected!"));
            return;
        }

        // ===== AUTO EAT =====
        if (autoEat.getValue() && autoEatManager.shouldEat()) {
            autoEatManager.eat();
            this.mc.options.forwardKey.setPressed(false);
            this.mc.interactionManager.cancelBlockBreaking();
            return;
        }

        // ===== RANDOM STOPS =====
        if (stopManager.shouldStop()) {
            stopManager.executeStop();
            this.mc.options.forwardKey.setPressed(false);
            this.mc.interactionManager.cancelBlockBreaking();
            return;
        }

        // ===== TOTEM CHECK =====
        if (this.totemCheck.getValue()) {
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
                    return;
                }
            } else {
                this.actionDelay = 0.0;
            }
        }

        // ===== AUTO MEND =====
        if (this.autoMend.getValue() && !this.isDigging) {
            final ItemStack mainHand = this.mc.player.getMainHandStack();
            if (EnchantmentUtil.hasEnchantment(mainHand, Enchantments.MENDING) && 
                mainHand.getMaxDamage() - mainHand.getDamage() < 100) {
                this.isDigging = true;
                this.totemCheckCounter = this.mc.player.getInventory().selectedSlot;
            }
        }

        // ===== XP BOTTLE MENDING =====
        if (this.isDigging) {
            handleMending();
            return;
        }

        // ===== AUTO TOTEM BUY (legacy) =====
        if (this.autoTotemBuy.getValue() && !handleTotemBuy()) {
            return;
        }

        // ===== HAZARD AVOIDANCE =====
        BlockPos hazard = hazardAvoid.detectHazard();
        if (hazard != null) {
            currentPath = hazardAvoid.findSafePath(hazard, currentDirection);
            if (!currentPath.isEmpty()) {
                minePath(currentPath);
            }
            return;
        }

        // ===== PATH FINDING =====
        if (currentPath.isEmpty() || (currentTarget != null && isBlockMined(currentTarget))) {
            currentPath = findTunnelPath(5);
        }

        // ===== MINING WITH MOUSE GLIDE =====
        if (!currentPath.isEmpty()) {
            currentTarget = currentPath.get(0);
            
            // Apply mouse glide for natural movement
            mouseGlide.glideToBlock(currentTarget);
            
            // Mine the block
            mineBlock(currentTarget);
            
            // Remove mined block from path
            if (isBlockMined(currentTarget)) {
                currentPath.remove(0);
            }
        }

        // ===== DIRECTION ALIGNMENT =====
        alignToDirection();
        
        // ===== BASE/SPAWNER DETECTION =====
        checkForDiscoveries();
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

    private List<BlockPos> findTunnelPath(int length) {
        List<BlockPos> path = new ArrayList<>();
        net.minecraft.util.math.Direction dir = toMinecraftDirection(currentDirection);
        BlockPos startPos = this.mc.player.getBlockPos();
        
        // Mine in a 3x3 pattern for a proper tunnel
        for (int i = 0; i < length; i++) {
            BlockPos forward = startPos.offset(dir, i + 1);
            
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    BlockPos minePos;
                    if (dir == net.minecraft.util.math.Direction.NORTH || dir == net.minecraft.util.math.Direction.SOUTH) {
                        minePos = forward.add(x, y, 0);
                    } else {
                        minePos = forward.add(0, y, x);
                    }
                    
                    if (shouldMine(minePos)) {
                        path.add(minePos);
                    }
                }
            }
        }
        
        return path;
    }

    private boolean shouldMine(BlockPos pos) {
        Block block = this.mc.world.getBlockState(pos).getBlock();
        return block != Blocks.AIR && 
               block != Blocks.BEDROCK && 
               block != Blocks.LAVA && 
               block != Blocks.WATER &&
               block != Blocks.CHEST && // Don't mine chests
               block != Blocks.TRAPPED_CHEST &&
               block != Blocks.ENDER_CHEST;
    }

    private boolean isBlockMined(BlockPos pos) {
        return this.mc.world.getBlockState(pos).isAir();
    }

    private void mineBlock(BlockPos pos) {
        if (pos == null || this.mc.world.getBlockState(pos).isAir()) return;
        
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, 
            net.minecraft.util.math.Direction.UP, pos, false);
        
        this.mc.interactionManager.updateBlockBreakingProgress(pos, hitResult.getSide());
        this.mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void minePath(List<BlockPos> path) {
        if (path.isEmpty()) return;
        
        for (BlockPos pos : path) {
            if (!this.mc.world.getBlockState(pos).isAir()) {
                mineBlock(pos);
                break;
            }
        }
    }

    private void alignToDirection() {
        float targetYaw = getDirectionYaw(currentDirection);
        float currentYaw = this.mc.player.getYaw();
        
        float diff = targetYaw - currentYaw;
        if (Math.abs(diff) > 1) {
            this.mc.player.setYaw(currentYaw + diff * 0.1f);
        }
        this.mc.player.setPitch(2.0f);
    }

    private float getDirectionYaw(Direction dir) {
        switch(dir) {
            case NORTH: return 180;
            case SOUTH: return 0;
            case EAST: return -90;
            case WEST: return 90;
            default: return 0;
        }
    }

    private net.minecraft.util.math.Direction toMinecraftDirection(Direction dir) {
        switch(dir) {
            case NORTH: return net.minecraft.util.math.Direction.NORTH;
            case SOUTH: return net.minecraft.util.math.Direction.SOUTH;
            case EAST: return net.minecraft.util.math.Direction.EAST;
            case WEST: return net.minecraft.util.math.Direction.WEST;
            default: return net.minecraft.util.math.Direction.NORTH;
        }
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

    private int calculateDirection(final Direction dir) {
        switch(dir) {
            case NORTH: return 180;
            case SOUTH: return 0;
            case EAST: return 270;
            case WEST: return 90;
            default: return Math.round(this.mc.player.getYaw());
        }
    }

    private boolean isBlockInDirection(final net.minecraft.util.math.Direction direction, final int distance) {
        final BlockPos down = this.mc.player.getBlockPos().down();
        final BlockPos playerPos = this.mc.player.getBlockPos();
        
        for (int i = 0; i < distance; i++) {
            final BlockPos offset = down.offset(direction, i);
            final BlockPos offset2 = playerPos.offset(direction, i);
            
            if (this.mc.world.getBlockState(offset).isAir() || !isBlockSafe(offset)) {
                return false;
            }
            if (!this.mc.world.getBlockState(offset2).isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlockPositionValid(final BlockPos pos, final net.minecraft.util.math.Direction direction) {
        // Simplified version - original logic was broken
        return isBlockSafe(pos) && isBlockSafe(pos.offset(direction));
    }

    private boolean isBlockSafe(final BlockPos pos) {
        Block block = this.mc.world.getBlockState(pos).getBlock();
        return block != Blocks.LAVA && block != Blocks.WATER && block != Blocks.GRAVEL;
    }

    private void updateDirection(final Direction dir) {
        double x = this.mc.player.getX();
        double z = this.mc.player.getZ();
        double floorX = Math.floor(x) + 0.5 - x;
        double floorZ = Math.floor(z) + 0.5 - z;
        
        this.mc.options.leftKey.setPressed(false);
        this.mc.options.rightKey.setPressed(false);
        
        boolean right = false;
        boolean left = false;
        
        switch(dir) {
            case SOUTH:
                if (floorX > 0.1) left = true;
                else if (floorX < -0.1) right = true;
                break;
            case NORTH:
                if (floorX > 0.1) right = true;
                else if (floorX < -0.1) left = true;
                break;
            case WEST:
                if (floorZ > 0.1) left = true;
                else if (floorZ < -0.1) right = true;
                break;
            case EAST:
                if (floorZ > 0.1) right = true;
                else if (floorZ < -0.1) left = true;
                break;
        }
        
        if (right) this.mc.options.rightKey.setPressed(true);
        if (left) this.mc.options.leftKey.setPressed(true);
    }

    private void handleBlockBreaking(final boolean shouldBreak) {
        if (this.mc.player.isUsingItem()) return;
        
        if (shouldBreak && this.mc.crosshairTarget instanceof BlockHitResult) {
            final BlockHitResult hitResult = (BlockHitResult) this.mc.crosshairTarget;
            final BlockPos pos = hitResult.getBlockPos();
            
            if (!this.mc.world.getBlockState(pos).isAir()) {
                if (this.mc.interactionManager.updateBlockBreakingProgress(pos, hitResult.getSide())) {
                    this.mc.particleManager.addBlockBreakingParticles(pos, hitResult.getSide());
                    this.mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        } else {
            this.mc.interactionManager.cancelBlockBreaking();
        }
    }

    private Direction getInitialDirection() {
        float yaw = this.mc.player.getYaw() % 360.0f;
        if (yaw < 0.0f) yaw += 360.0f;
        
        if (yaw >= 45.0f && yaw < 135.0f) return Direction.WEST;
        if (yaw >= 135.0f && yaw < 225.0f) return Direction.NORTH;
        if (yaw >= 225.0f && yaw < 315.0f) return Direction.EAST;
        return Direction.SOUTH;
    }

    public boolean isDigging() {
        return this.isDigging;
    }

    // ===== INNER CLASSES =====

    private class RandomStopManager {
        private int stopTimer = 0;
        private boolean isStopped = false;
        private int stopDuration = 0;
        private Random random = new Random();
        
        public boolean shouldStop() {
            if (isStopped) {
                stopDuration--;
                if (stopDuration <= 0) {
                    isStopped = false;
                }
                return true;
            }
            
            // Random stop chance (about every 30-60 seconds)
            if (random.nextInt(1200) == 0) { // 1200 ticks = 60 seconds
                stopDuration = 20 + random.nextInt(70); // 1 to 3.5 seconds
                isStopped = true;
                return true;
            }
            
            return false;
        }
        
        public void executeStop() {
            // Just stop moving - handled in main loop
        }
    }

    private class MouseGlideManager {
        private final MinecraftClient mc;
        private Random random = new Random();
        private float targetYaw;
        private float targetPitch;
        private int glideTimer = 0;
        private boolean isGliding = false;
        
        public MouseGlideManager(MinecraftClient mc) {
            this.mc = mc;
        }
        
        public void glideToBlock(BlockPos pos) {
            if (pos == null || mc.player == null) return;
            
            Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3d playerPos = mc.player.getEyePos();
            Vec3d direction = blockCenter.subtract(playerPos).normalize();
            
            float targetPitch = (float) Math.toDegrees(Math.asin(-direction.y));
            float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            
            if (!isGliding) {
                // Start new glide with slight offset from center
                this.targetYaw = targetYaw + (random.nextFloat() - 0.5f) * 3.0f;
                this.targetPitch = targetPitch + (random.nextFloat() - 0.5f) * 2.0f;
                this.glideTimer = 40 + random.nextInt(60); // 2-5 seconds
                this.isGliding = true;
            }
            
            // Move towards target
            if (glideTimer > 20) {
                // Move away from center
                mc.player.setYaw(mc.player.getYaw() + (this.targetYaw - mc.player.getYaw()) * 0.05f);
                mc.player.setPitch(mc.player.getPitch() + (this.targetPitch - mc.player.getPitch()) * 0.05f);
            } else {
                // Move back to center
                mc.player.setYaw(mc.player.getYaw() + (targetYaw - mc.player.getYaw()) * 0.1f);
                mc.player.setPitch(mc.player.getPitch() + (targetPitch - mc.player.getPitch()) * 0.1f);
            }
            
            glideTimer--;
            if (glideTimer <= 0) {
                isGliding = false;
            }
        }
    }

    private class PlayerDetector {
        private final MinecraftClient mc;
        
        public PlayerDetector(MinecraftClient mc) {
            this.mc = mc;
        }
        
        public PlayerEntity checkForPlayers(int radius) {
            if (mc.world == null || mc.player == null) return null;
            
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                
                double distance = mc.player.distanceTo(player);
                
                // Check if player is in spectator or within radius
                if (player.isSpectator() || distance < radius) {
                    return player;
                }
            }
            return null;
        }
    }

    private class HazardAvoidance {
        private final MinecraftClient mc;
        
        public HazardAvoidance(MinecraftClient mc) {
            this.mc = mc;
        }
        
        public BlockPos detectHazard() {
            if (mc.player == null) return null;
            
            BlockPos playerPos = mc.player.getBlockPos();
            
            // Check for lava or water in vicinity
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 3; y++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockPos checkPos = playerPos.add(x, y, z);
                        Block block = mc.world.getBlockState(checkPos).getBlock();
                        
                        if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) {
                            return checkPos;
                        }
                        if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) {
                            return checkPos;
                        }
                    }
                }
            }
            return null;
        }
        
        public List<BlockPos> findSafePath(BlockPos hazard, Direction tunnelDir) {
            List<BlockPos> safePath = new ArrayList<>();
            net.minecraft.util.math.Direction dir = toMinecraftDirection(tunnelDir);
            
            // Simple path around hazard
            BlockPos ahead = mc.player.getBlockPos().offset(dir, 2);
            safePath.add(ahead.up());
            safePath.add(ahead);
            
            return safePath;
        }
    }

    private class AutoEatManager {
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
            
            // Find food in hotbar
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem().isFood()) {
                    mc.player.getInventory().selectedSlot = i;
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    eatTimer = 30; // 1.5 second cooldown between eats
                    break;
                }
            }
        }
    }

    private class PathRenderer {
        // This would be called from a Render3DEvent
        public void renderPath(MatrixStack matrices, List<BlockPos> path) {
            if (path.isEmpty()) return;
            
            for (int i = 0; i < path.size() - 1; i++) {
                BlockPos current = path.get(i);
                BlockPos next = path.get(i + 1);
                
                Vec3d currentPos = new Vec3d(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5);
                Vec3d nextPos = new Vec3d(next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5);
                
                // Render line between points
                RenderUtils.renderLine(matrices, Color.CYAN, currentPos, nextPos);
            }
        }
    }

    enum Direction {
        NORTH, SOUTH, EAST, WEST
    }
}
