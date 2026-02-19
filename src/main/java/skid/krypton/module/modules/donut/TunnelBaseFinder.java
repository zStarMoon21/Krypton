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
import skid.krypton.utils.embed.DiscordWebhook; // FIXED: Added explicit import

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public final class TunnelBaseFinder extends Module {

    private final NumberSetting minimumStorage = new NumberSetting("Minimum Storage", 1, 500, 100, 1);
    private final BooleanSetting spawners = new BooleanSetting("Spawners", true);
    private final BooleanSetting autoEat = new BooleanSetting("Auto Eat", true);
    private final BooleanSetting disconnectOnBase = new BooleanSetting("Disconnect on Base", true);
    private final BooleanSetting discordNotification = new BooleanSetting("Discord Notification", false);
    private final StringSetting webhook = new StringSetting("Webhook", "");
    private final BooleanSetting totemCheck = new BooleanSetting("Totem Check", true);
    private final BooleanSetting autoTotemBuy = new BooleanSetting("Auto Totem Buy", true);
    private final NumberSetting totemSlot = new NumberSetting("Totem Slot", 1, 9, 8, 1);
    private final BooleanSetting autoMend = new BooleanSetting("Auto Mend", true);
    private final NumberSetting xpBottleSlot = new NumberSetting("XP Bottle Slot", 1, 9, 9, 1);
    private final NumberSetting totemCheckTime = new NumberSetting("Totem Check Time", 1, 120, 20, 1);

    private TunnelDirection currentDirection;
    private Direction mcDirection;

    private AStarPathfinder pathfinder;
    private PathRenderer pathRenderer;
    private SmartMiner miner;
    private NaturalMovement movement;
    private SmartPauser pauser;
    private ReliableEater eater;
    private PlayerDetectionManager playerDetector;
    private HazardAvoidanceManager hazardAvoid;

    private List<BlockPos> currentPath = new ArrayList<>();
    private boolean mining = false;
    private int pathUpdateCooldown = 0;
    private int discoveryCooldown = 0;
    private double actionDelay = 0;

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
        pathfinder = new AStarPathfinder(mc);
        pathRenderer = new PathRenderer(mc);
        miner = new SmartMiner(mc);
        movement = new NaturalMovement(mc);
        pauser = new SmartPauser();
        eater = new ReliableEater(mc);

        pathRenderer.setPathfinder(pathfinder);

        currentDirection = TunnelUtils.getInitialDirection(mc.player);
        mcDirection = toMinecraftDirection(currentDirection);
        currentPath = pathfinder.findPath(currentDirection);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        movement.stopAll();
        if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        miner.stopMining();
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;
        mc.player.input.jumping = false;
        mc.player.input.sneaking = false;

        PlayerEntity detected = playerDetector.checkForPlayers(20);
        if (detected != null) {
            disconnectWithMessage(Text.of("Player detected"));
            return;
        }

        if (autoEat.getValue()) {
            eater.tick();
            if (eater.isEating()) {
                pauseAll();
                return;
            }
        }

        if (pauser.shouldPause()) {
            pauseAll();
            return;
        }

        if (!handleTotemSafety()) return;

        if (--pathUpdateCooldown <= 0) {
            currentPath = pathfinder.findPath(currentDirection);
            pathUpdateCooldown = 10;
        }

        BlockPos target = pathfinder.getFirstTarget();

        if (target != null) {
            movement.updateAim(target);
        }

        if (target != null && isPlayerFacingTarget(target)) {
            miner.mine();
            mining = true;
            
            if (miner.isBlockMined(target)) {
                pathfinder.removeFirstTarget();
                mining = false;
            }
        } else {
            miner.stopMining();
            mining = false;
        }

        updateMovement(target);

        if (--discoveryCooldown <= 0) {
            checkForDiscoveries();
            discoveryCooldown = 40;
        }

        handleLegacyFeatures();
    }

    @EventListener
    public void onRender3D(Render3DEvent e) {
        if (pathRenderer != null) {
            pathRenderer.render(e.matrixStack, e.tickDelta);
        }
    }

    private void updateMovement(BlockPos target) {
        if (target == null || pauser.isPaused() || eater.isEating()) {
            movement.stopAll();
            return;
        }

        boolean needsStrafe = false;
        float strafeDirection = 0;
        
        double xDiff = (mc.player.getX() % 1) - 0.5;
        double zDiff = (mc.player.getZ() % 1) - 0.5;
        double threshold = 0.35;

        if (mcDirection.getAxis() == Direction.Axis.X) {
            if (Math.abs(zDiff) > threshold) {
                needsStrafe = true;
                strafeDirection = zDiff > 0 ? 1 : -1;
            }
        } else {
            if (Math.abs(xDiff) > threshold) {
                needsStrafe = true;
                strafeDirection = xDiff > 0 ? -1 : 1;
            }
        }

        movement.updateMovement(!miner.isBlockMined(target), needsStrafe, strafeDirection);
    }

    private boolean isPlayerFacingTarget(BlockPos target) {
        if (target == null) return false;
        
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        Vec3d targetVec = new Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5)
                .subtract(mc.player.getEyePos()).normalize();
        
        double dot = lookVec.dotProduct(targetVec);
        return dot > 0.95;
    }

    private void pauseAll() {
        movement.stopAll();
        miner.stopMining();
    }

    private void handleLegacyFeatures() {
        if (autoMend.getValue() && !mining) {
            checkMending();
        }
        
        if (autoTotemBuy.getValue()) {
            handleTotemBuy();
        }
    }

    private Direction toMinecraftDirection(TunnelDirection d) {
        return switch (d) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
        };
    }

    private boolean handleTotemSafety() {
        if (!totemCheck.getValue()) return true;

        boolean hasTotem = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        Module autoTotem = Krypton.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoTotem.class);

        if (!hasTotem && (!autoTotem.isEnabled() || ((AutoTotem) autoTotem).findItemSlot(Items.TOTEM_OF_UNDYING) == -1)) {
            toggle();
            return false;
        }
        return true;
    }

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

        if (spawnerPos != null) {
            sendDiscordNotification("Spawner Found", "Location: " + spawnerPos.toShortString(), "Spawner", spawnerPos.toShortString(), Color.ORANGE);
            if (disconnectOnBase.getValue()) disconnectWithMessage(Text.of("Spawner Found"));
        }
        
        if (storage >= minimumStorage.getIntValue()) {
            sendDiscordNotification("Base Found", "Storage: " + storage, "Base", mc.player.getBlockPos().toShortString(), Color.GREEN);
            if (disconnectOnBase.getValue()) disconnectWithMessage(Text.of("Base Found"));
        }
    }

    private void sendDiscordNotification(String title, String desc, String field, String value, Color color) {
        if (!discordNotification.getValue() || webhook.value.isEmpty()) return;
        
        try {
            DiscordWebhook webhook = new DiscordWebhook(this.webhook.value);
            DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
            
            embed.setTitle(title);
            embed.setThumbnail("https://render.crafty.gg/3d/bust/" + 
                MinecraftClient.getInstance().getSession().getUuidOrNull() + "?format=webp");
            embed.setDescription(desc);
            embed.setColor(color);
            embed.setFooter(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), null);
            embed.addField(field, value, true);
            
            webhook.addEmbed(embed);
            webhook.a(""); // content
            webhook.b("Krypton Tunnel Finder"); // username
            webhook.c(""); // avatar URL (optional)
            webhook.a(false); // tts (optional)
            
            webhook.execute();
        } catch (Throwable ignored) {}
    }

    private void disconnectWithMessage(Text t) {
        MutableText m = Text.literal("[TunnelBaseFinder] ").append(t);
        toggle();
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(m));
    }

    private void checkMending() {
        ItemStack mainHand = mc.player.getMainHandStack();
        if (EnchantmentUtil.hasEnchantment(mainHand, Enchantments.MENDING) && 
            mainHand.getMaxDamage() - mainHand.getDamage() < 100) {
            // Mending logic would go here
        }
    }

    private void handleTotemBuy() {
        // Legacy totem buy logic would go here
    }

    public boolean isDigging() {
        return mining;
    }
}
