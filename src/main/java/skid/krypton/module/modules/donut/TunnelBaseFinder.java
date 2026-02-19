package skid.krypton.module.modules.donut;

import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.Krypton;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.manager.tunnel.*;
import skid.krypton.mixin.MobSpawnerLogicAccessor;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.modules.combat.AutoTotem;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.*;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public final class TunnelBaseFinder extends Module {

    // SETTINGS
    private final NumberSetting minimumStorage = new NumberSetting("Minimum Storage", 1, 500, 100, 1);
    private final BooleanSetting spawners = new BooleanSetting("Spawners", true);
    private final BooleanSetting autoEat = new BooleanSetting("Auto Eat", true);
    private final BooleanSetting disconnectOnBase = new BooleanSetting("Disconnect on Base", true);
    private final BooleanSetting discordNotification = new BooleanSetting("Discord Notification", false);
    private final StringSetting webhook = new StringSetting("Webhook", "");
    private final BooleanSetting totemCheck = new BooleanSetting("Totem Check", true);

    // LEGACY
    private final BooleanSetting autoTotemBuy = new BooleanSetting("Auto Totem Buy", true);
    private final NumberSetting totemSlot = new NumberSetting("Totem Slot", 1, 9, 8, 1);
    private final BooleanSetting autoMend = new BooleanSetting("Auto Mend", true);
    private final NumberSetting xpBottleSlot = new NumberSetting("XP Bottle Slot", 1, 9, 9, 1);
    private final NumberSetting totemCheckTime = new NumberSetting("Totem Check Time", 1, 120, 20, 1);

    // Direction
    private TunnelDirection currentDirection;
    private Direction mcDirection;

    // Tunnel components
    private PathScanner pathScanner;
    private PathRenderer pathRenderer;
    private PreciseMiner miner;
    private PixelGlide pixelGlide;
    private RandomPause randomPause;
    private FoodEater foodEater;
    private PlayerDetectionManager playerDetector;
    private HazardAvoidanceManager hazardAvoid;

    private List<BlockPos> currentPath = new ArrayList<>();

    // State
    private int pathScanCooldown = 0;
    private int discoveryCooldown = 0;
    private boolean waitingForBlockBreak = false;
    private double actionDelay = 0;
    
    // FIX 1: Add digging flag for AutoTotem compatibility
    private boolean digging = false;

    public TunnelBaseFinder() {
        super("Tunnel Base Finder", "Advanced tunnel finder", -1, Category.DONUT);
        addSettings(minimumStorage, spawners, autoEat, disconnectOnBase, discordNotification, webhook, totemCheck,
                autoTotemBuy, totemSlot, autoMend, xpBottleSlot, totemCheckTime);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            toggle();
            return;
        }

        playerDetector = new PlayerDetectionManager(mc);
        hazardAvoid = new HazardAvoidanceManager(mc);
        pathScanner = new PathScanner(mc);
        pathRenderer = new PathRenderer(mc);
        miner = new PreciseMiner(mc);
        pixelGlide = new PixelGlide(mc);
        randomPause = new RandomPause();
        foodEater = new FoodEater(mc);

        currentDirection = TunnelUtils.getInitialDirection(mc.player);
        mcDirection = toMinecraftDirection(currentDirection);

        currentPath = pathScanner.scanPath(currentDirection);
        pathRenderer.setPathScanner(pathScanner);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        stopMovement();
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        miner.stopMining();
        super.onDisable();
    }

    // ================= TICK LOOP =================
    @EventListener
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // Override player input
        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;
        mc.player.input.jumping = false;
        mc.player.input.sneaking = false;

        // Player detection
        PlayerEntity detected = playerDetector.checkForPlayers(20);
        if (detected != null) {
            disconnectWithMessage(Text.of("Player detected"));
            return;
        }

        // Totem safety
        if (!handleTotemSafety()) return;

        // Auto eat
        if (autoEat.getValue() && foodEater.needsFood()) {
            foodEater.eat();
            pauseAll();
            return;
        }

        // Random pauses
        if (randomPause.shouldPause()) {
            pauseAll();
            return;
        }

        // Re-scan path every 10 ticks
        if (--pathScanCooldown <= 0) {
            currentPath = pathScanner.scanPath(currentDirection);
            pathScanCooldown = 10;
        }

        // FIX 2: Use getFirstTarget() instead of getNextTarget()
        BlockPos target = pathScanner.getFirstTarget();

        updateDirection();

        // Hazard avoidance hook (optional future detour logic)
        BlockPos hazard = hazardAvoid.detectHazard();
        if (hazard != null) {
            stopMovement();
            return;
        }

        // Mining sync
        if (target != null) {
            miner.mine();
            // FIX 3: Set digging flag for AutoTotem
            digging = true;
            
            if (miner.isBlockMined(target)) {
                // FIX 4: Use removeFirstTarget() instead of removeTarget()
                pathScanner.removeFirstTarget();
                waitingForBlockBreak = false;
            } else {
                waitingForBlockBreak = true;
            }
        } else {
            digging = false;
        }

        // Pixel glide aim
        if (target != null) pixelGlide.update(target);

        // Movement only if block broken
        if (!waitingForBlockBreak) controlMovement();
        else stopMovement();

        // Base scanning every 40 ticks
        if (--discoveryCooldown <= 0) {
            checkForDiscoveries();
            discoveryCooldown = 40;
        }
    }

    @EventListener
    public void onRender3D(Render3DEvent e) {
        if (pathRenderer != null) {
            pathRenderer.render(e.matrixStack, e.tickDelta);
        }
    }

    // ================= MOVEMENT =================
    private void controlMovement() {
        if (currentPath.isEmpty()) return;

        mc.options.forwardKey.setPressed(true);

        double xDiff = (mc.player.getX() % 1) - 0.5;
        double zDiff = (mc.player.getZ() % 1) - 0.5;
        double threshold = 0.4;

        boolean correct = false;

        // Only correct the axis we're tunneling in
        if (mcDirection.getAxis() == Direction.Axis.X) {
            if (Math.abs(zDiff) > threshold) {
                correct = true;
                mc.options.rightKey.setPressed(zDiff > 0);
                mc.options.leftKey.setPressed(zDiff < 0);
            }
        } else { // Z axis
            if (Math.abs(xDiff) > threshold) {
                correct = true;
                mc.options.rightKey.setPressed(xDiff > 0);
                mc.options.leftKey.setPressed(xDiff < 0);
            }
        }

        if (!correct) {
            mc.options.rightKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
        }
    }

    private void stopMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
    }

    private void pauseAll() {
        stopMovement();
        miner.stopMining();
    }

    // ================= ROTATION =================
    private void updateDirection() {
        // FIX 5: Comment out isGliding check or add it to PixelGlide
        // if (pixelGlide.isGliding()) return;

        float targetYaw = TunnelUtils.getDirectionYaw(currentDirection);
        float yaw = mc.player.getYaw();
        float diff = MathHelper.wrapDegrees(targetYaw - yaw);

        if (Math.abs(diff) > 2f) {
            mc.player.setYaw(yaw + diff * 0.05f);
        }

        mc.player.setPitch(1.5f);
    }

    private Direction toMinecraftDirection(TunnelDirection d) {
        return switch (d) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
        };
    }

    // ================= SAFETY =================
    private boolean handleTotemSafety() {
        if (!totemCheck.getValue()) return true;

        boolean hasTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        Module autoTotem = Krypton.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoTotem.class);

        if (!hasTotem) {
            if (autoTotem.isEnabled()) actionDelay = 0;
            else actionDelay++;

            if (actionDelay > totemCheckTime.getValue()) {
                toggle();
                return false;
            }
        } else actionDelay = 0;

        return true;
    }

    // ================= BASE DETECTION =================
    private void checkForDiscoveries() {
        int storage = 0;
        BlockPos spawnerPos = null;

        for (WorldChunk chunk : BlockUtil.getLoadedChunks().toList()) {
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
                BlockEntity e = mc.world.getBlockEntity(pos);
                if (e == null) continue;

                if (spawners.getValue() && e instanceof MobSpawnerBlockEntity spawner) {
                    try {
                        String id = ((MobSpawnerLogicAccessor) spawner.getLogic())
                                .getSpawnEntry().getNbt().getString("id");
                        if (!id.contains("spider")) spawnerPos = pos;
                    } catch (Exception ignored) {}
                }

                if (e instanceof ChestBlockEntity || e instanceof ShulkerBoxBlockEntity ||
                        e instanceof BarrelBlockEntity || e instanceof FurnaceBlockEntity) {
                    storage++;
                }
            }
        }

        if (spawnerPos != null) disconnectWithMessage(Text.of("Spawner Found"));
        if (storage >= minimumStorage.getIntValue()) disconnectWithMessage(Text.of("Base Found"));
    }

    // ================= DISCONNECT =================
    private void disconnectWithMessage(Text t) {
        MutableText m = Text.literal("[TunnelBaseFinder] ").append(t);
        toggle();
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(m));
    }
    
    // FIX 6: Add isDigging() method for AutoTotem compatibility
    public boolean isDigging() {
        return digging;
    }
}
}
