package me.justeli.coins.util;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlockDisplay manager for coin visuals
 * Supports custom heads, items, scaling, positioning, and rotation
 */
public final class BlockDisplayManager {
    
    private static final Map<UUID, BlockDisplayHologram> activeDisplays = new ConcurrentHashMap<>();
    private static BukkitTask rotationTask = null;
    private static Coins plugin;
    
    private BlockDisplayManager() {}
    
    public static void init(Coins coins) {
        plugin = coins;
    }
    
    public static boolean isBlockDisplaySupported() {
        try {
            Class.forName("org.bukkit.entity.BlockDisplay");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static BlockDisplayHologram createCoinDisplay(Location location, double amount, Player player) {
        if (!Config.BLOCK_DISPLAY_ENABLED || !isBlockDisplaySupported()) {
            return null;
        }
        
        Location displayLocation = location.clone();
        
        displayLocation.add(
            Config.BLOCK_DISPLAY_OFFSET_X,
            Config.BLOCK_DISPLAY_OFFSET_Y,
            Config.BLOCK_DISPLAY_OFFSET_Z
        );
        
        if (Config.BLOCK_DISPLAY_CENTER_ON_BLOCK) {
            displayLocation.setX(location.getBlockX() + 0.5);
            displayLocation.setZ(location.getBlockZ() + 0.5);
        }
        
        BlockDisplayHologram display = new BlockDisplayHologram(displayLocation, amount, player);
        activeDisplays.put(display.getId(), display);
        
        if (Config.BLOCK_DISPLAY_ROTATION_ENABLED && rotationTask == null) {
            startRotationTask();
        }
        
        return display;
    }
    
    public static void createPickupDisplay(Location location, double amount, Player player) {
        BlockDisplayHologram display = createCoinDisplay(location, amount, player);
        if (display != null && Config.BLOCK_DISPLAY_PICKUP_DURATION > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, 
                display::remove, 
                Config.BLOCK_DISPLAY_PICKUP_DURATION * 20L
            );
        }
    }
    
    private static void startRotationTask() {
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (BlockDisplayHologram display : activeDisplays.values()) {
                if (display.isValid() && display.shouldRotate()) {
                    display.updateRotation();
                }
            }
        }, 0L, 1L);
    }
    
    public static void stopRotationTask() {
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
    }
    
    public static void removeAll() {
        for (BlockDisplayHologram display : activeDisplays.values()) {
            display.remove();
        }
        activeDisplays.clear();
        stopRotationTask();
    }
    
    public static void removeForPlayer(Player player) {
        activeDisplays.values().removeIf(display -> {
            if (display.getPlayer() != null && display.getPlayer().equals(player)) {
                display.remove();
                return true;
            }
            return false;
        });
    }
    
    public static int getActiveCount() {
        return activeDisplays.size();
    }
    
    static void removeDisplay(UUID id) {
        activeDisplays.remove(id);
        if (activeDisplays.isEmpty()) {
            stopRotationTask();
        }
    }
    
    public static final class BlockDisplayHologram {
        private final BlockDisplay display;
        private final UUID id;
        private final Location originalLocation;
        private final Player player;
        private float currentRotation = 0f;
        private BukkitTask animationTask;
        private BukkitTask removeTask;
        
        public BlockDisplayHologram(Location location, double amount, Player player) {
            this.id = UUID.randomUUID();
            this.originalLocation = location.clone();
            this.player = player;
            
            this.display = location.getWorld().spawn(location, BlockDisplay.class);
            
            setupDisplay();
            startAnimation();
            
            if (Config.BLOCK_DISPLAY_DURATION > 0) {
                this.removeTask = Bukkit.getScheduler().runTaskLater(plugin, 
                    this::remove, 
                    Config.BLOCK_DISPLAY_DURATION * 20L
                );
            }
        }
        
        private void setupDisplay() {
            BlockData blockData = getBlockData();
            display.setBlock(blockData);
            
            display.setBillboard(getBillboardMode());
            
            if (Config.BLOCK_DISPLAY_BRIGHTNESS_ENABLED) {
                display.setBrightness(new Display.Brightness(
                    Config.BLOCK_DISPLAY_BRIGHTNESS_BLOCK,
                    Config.BLOCK_DISPLAY_BRIGHTNESS_SKY
                ));
            }
            
            display.setViewRange(Config.BLOCK_DISPLAY_VIEW_RANGE);
            
            applyTransformation(0f);
            
            if (Config.BLOCK_DISPLAY_GLOW_ENABLED) {
                display.setGlowing(true);
                if (!Config.BLOCK_DISPLAY_GLOW_COLOR.isEmpty()) {
                    try {
                        display.setGlowColorOverride(org.bukkit.Color.fromRGB(
                            Integer.parseInt(Config.BLOCK_DISPLAY_GLOW_COLOR, 16)
                        ));
                    } catch (Exception ignored) {}
                }
            }
        }
        
        private BlockData getBlockData() {
            String type = Config.BLOCK_DISPLAY_TYPE.toUpperCase();
            
            switch (type) {
                case "HEAD":
                case "SKULL":
                    return getSkullBlockData();
                case "ITEM":
                    return getItemBlockData();
                default:
                    return getMaterialBlockData();
            }
        }
        
        private BlockData getSkullBlockData() {
            Material skullType;
            
            if (!Config.BLOCK_DISPLAY_SKULL_TEXTURE.isEmpty()) {
                skullType = Material.PLAYER_HEAD;
            } else if (!Config.BLOCK_DISPLAY_SKULL_TYPE.isEmpty()) {
                try {
                    skullType = Material.valueOf(Config.BLOCK_DISPLAY_SKULL_TYPE.toUpperCase());
                } catch (IllegalArgumentException e) {
                    skullType = Material.PLAYER_HEAD;
                }
            } else {
                skullType = Material.PLAYER_HEAD;
            }
            
            return skullType.createBlockData();
        }
        
        private BlockData getItemBlockData() {
            Material itemType;
            try {
                itemType = Material.valueOf(Config.BLOCK_DISPLAY_ITEM_TYPE.toUpperCase());
            } catch (IllegalArgumentException e) {
                itemType = Material.GOLD_INGOT;
            }
            return itemType.createBlockData();
        }
        
        private BlockData getMaterialBlockData() {
            Material material;
            try {
                material = Material.valueOf(Config.BLOCK_DISPLAY_MATERIAL.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.GOLD_BLOCK;
            }
            return material.createBlockData();
        }
        
        private Display.Billboard getBillboardMode() {
            String mode = Config.BLOCK_DISPLAY_BILLBOARD.toUpperCase();
            try {
                return Display.Billboard.valueOf(mode);
            } catch (IllegalArgumentException e) {
                return Display.Billboard.CENTER;
            }
        }
        
        private void applyTransformation(float rotationAngle) {
            Transformation transformation = display.getTransformation();
            
            Vector3f scale = new Vector3f(
                Config.BLOCK_DISPLAY_SCALE_X,
                Config.BLOCK_DISPLAY_SCALE_Y,
                Config.BLOCK_DISPLAY_SCALE_Z
            );
            transformation.getScale().set(scale);
            
            Vector3f translation = new Vector3f(
                Config.BLOCK_DISPLAY_TRANSLATION_X,
                Config.BLOCK_DISPLAY_TRANSLATION_Y,
                Config.BLOCK_DISPLAY_TRANSLATION_Z
            );
            transformation.getTranslation().set(translation);
            
            if (Config.BLOCK_DISPLAY_ROTATION_ENABLED) {
                Quaternionf rotation = createRotation(rotationAngle);
                transformation.getLeftRotation().set(rotation);
            }
            
            display.setTransformation(transformation);
        }
        
        private Quaternionf createRotation(float angle) {
            String axis = Config.BLOCK_DISPLAY_ROTATION_AXIS.toUpperCase();
            float radians = (float) Math.toRadians(angle);
            
            switch (axis) {
                case "X":
                    return new Quaternionf(new AxisAngle4f(radians, 1, 0, 0));
                case "Z":
                    return new Quaternionf(new AxisAngle4f(radians, 0, 0, 1));
                default:
                    return new Quaternionf(new AxisAngle4f(radians, 0, 1, 0));
            }
        }
        
        private void startAnimation() {
            if (!Config.BLOCK_DISPLAY_ANIMATION_ENABLED) {
                return;
            }
            
            this.animationTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                private int tick = 0;
                private final int duration = Config.BLOCK_DISPLAY_ANIMATION_DURATION;
                
                @Override
                public void run() {
                    if (!isValid()) {
                        if (animationTask != null) {
                            animationTask.cancel();
                        }
                        return;
                    }
                    
                    float progress = (float) tick / duration;
                    
                    if (Config.BLOCK_DISPLAY_ANIMATION_BOBBING) {
                        double bobOffset = Math.sin(progress * 2 * Math.PI) * Config.BLOCK_DISPLAY_ANIMATION_BOB_HEIGHT;
                        Location bobLocation = originalLocation.clone().add(0, bobOffset, 0);
                        display.teleport(bobLocation);
                    }
                    
                    if (Config.BLOCK_DISPLAY_ANIMATION_PULSE) {
                        float pulseScale = 1f + (float) Math.sin(progress * 2 * Math.PI) * Config.BLOCK_DISPLAY_ANIMATION_PULSE_AMOUNT;
                        Vector3f scale = new Vector3f(
                            Config.BLOCK_DISPLAY_SCALE_X * pulseScale,
                            Config.BLOCK_DISPLAY_SCALE_Y * pulseScale,
                            Config.BLOCK_DISPLAY_SCALE_Z * pulseScale
                        );
                        display.getTransformation().getScale().set(scale);
                    }
                    
                    tick++;
                    if (tick >= duration) {
                        tick = 0;
                    }
                }
            }, 0L, 1L);
        }
        
        public void updateRotation() {
            if (!Config.BLOCK_DISPLAY_ROTATION_ENABLED) {
                return;
            }
            
            currentRotation += Config.BLOCK_DISPLAY_ROTATION_SPEED;
            if (currentRotation >= 360f) {
                currentRotation -= 360f;
            }
            
            applyTransformation(currentRotation);
        }
        
        public boolean shouldRotate() {
            return Config.BLOCK_DISPLAY_ROTATION_ENABLED && isValid();
        }
        
        public boolean isValid() {
            return display != null && !display.isDead();
        }
        
        public void remove() {
            if (animationTask != null) {
                animationTask.cancel();
                animationTask = null;
            }
            if (removeTask != null) {
                removeTask.cancel();
                removeTask = null;
            }
            if (display != null && !display.isDead()) {
                display.remove();
            }
            BlockDisplayManager.removeDisplay(id);
        }
        
        public UUID getId() {
            return id;
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public BlockDisplay getDisplay() {
            return display;
        }
    }
}
