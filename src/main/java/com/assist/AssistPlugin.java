package com.assist;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class AssistPlugin extends JavaPlugin implements Listener {

    // 配置项
    private boolean enableStonecutter;
    private boolean enablePhantomRepel;
    private boolean enablePetHealth;
    private boolean enableGlowBerries;
    private boolean enableCatChest;
    private boolean enableBlockChest;
    private boolean enableSnowballExtinguish;
    private boolean enableShiftF;
    private String shiftFCommand;
    private boolean shiftFAsOp;

    // 切石机配方key列表，用于移除
    private final List<NamespacedKey> stonecutterRecipeKeys = new ArrayList<>();

    // 定时任务引用，用于动态启停
    private BukkitRunnable petHealthTask;
    private BukkitRunnable phantomRepelTask;

    // 配置文件热更新
    private long lastConfigModified = 0;

    // 木材类型
    private static final String[] WOOD_TYPES = {
        "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK", "MANGROVE", "CHERRY"
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("assist").setExecutor(new AssistCommand());
        startTasks();
        startConfigWatcher();
        registerStonecutterRecipes();
        getLogger().info("AssistPlugin loaded - 8 features active!");
    }

    @Override
    public void onDisable() {
        cancelTasks();
        removeStonecutterRecipes();
    }

    private void loadConfig() {
        enableStonecutter = getConfig().getBoolean("stonecutter", true);
        enablePhantomRepel = getConfig().getBoolean("phantom-repel", true);
        enablePetHealth = getConfig().getBoolean("pet-health", true);
        enableGlowBerries = getConfig().getBoolean("glow-berries", true);
        enableCatChest = getConfig().getBoolean("cat-chest", true);
        enableBlockChest = getConfig().getBoolean("block-chest", true);
        enableSnowballExtinguish = getConfig().getBoolean("snowball-extinguish", true);
        enableShiftF = getConfig().getBoolean("shift-f.enabled", false);
        shiftFCommand = getConfig().getString("shift-f.command", "say Shift+F pressed!");
        shiftFAsOp = getConfig().getBoolean("shift-f.as-op", false);
    }

    // ==================== 切石机原生配方注册 ====================
    private void registerStonecutterRecipes() {
        if (!enableStonecutter) return;

        for (String wood : WOOD_TYPES) {
            String[] sources = {(wood + "_LOG"), (wood + "_WOOD")};
            String plank = wood + "_PLANKS";
            String slab = wood + "_SLAB";
            String stairs = wood + "_STAIRS";
            String fence = wood + "_FENCE";
            String gate = wood + "_FENCE_GATE";
            String plate = wood + "_PRESSURE_PLATE";
            String button = wood + "_BUTTON";

            for (String src : sources) {
                Material input = Material.matchMaterial(src);
                if (input == null) continue;

                addStonecuttingRecipe(input, plank, 4);
                addStonecuttingRecipe(input, slab, 6);
                addStonecuttingRecipe(input, stairs, 2);
                addStonecuttingRecipe(input, fence, 3);
                addStonecuttingRecipe(input, gate, 1);
                addStonecuttingRecipe(input, plate, 2);
                addStonecuttingRecipe(input, button, 4);
            }
        }

        // 竹
        Material bamboo = Material.BAMBOO_BLOCK;
        addStonecuttingRecipe(bamboo, "BAMBOO_PLANKS", 4);
        addStonecuttingRecipe(bamboo, "BAMBOO_SLAB", 6);
        addStonecuttingRecipe(bamboo, "BAMBOO_STAIRS", 2);
        addStonecuttingRecipe(bamboo, "BAMBOO_FENCE", 3);
        addStonecuttingRecipe(bamboo, "BAMBOO_FENCE_GATE", 1);
        addStonecuttingRecipe(bamboo, "BAMBOO_PRESSURE_PLATE", 2);
        addStonecuttingRecipe(bamboo, "BAMBOO_BUTTON", 4);

        // 下界
        for (String stem : new String[]{"WARPED", "CRIMSON"}) {
            String[] sources = {(stem + "_STEM"), (stem + "_HYPHAE")};
            for (String src : sources) {
                Material input = Material.matchMaterial(src);
                if (input == null) continue;

                addStonecuttingRecipe(input, stem + "_PLANKS", 4);
                addStonecuttingRecipe(input, stem + "_SLAB", 6);
                addStonecuttingRecipe(input, stem + "_STAIRS", 2);
                addStonecuttingRecipe(input, stem + "_FENCE", 3);
                addStonecuttingRecipe(input, stem + "_FENCE_GATE", 1);
                addStonecuttingRecipe(input, stem + "_PRESSURE_PLATE", 2);
                addStonecuttingRecipe(input, stem + "_BUTTON", 4);
            }
        }

        getLogger().info("已注册 " + stonecutterRecipeKeys.size() + " 个切石机配方");
    }

    private void addStonecuttingRecipe(Material input, String outputName, int amount) {
        Material output = Material.matchMaterial(outputName);
        if (input == null || output == null) return;

        NamespacedKey key = new NamespacedKey(this, "stonecut_" + input.name().toLowerCase() + "_to_" + output.name().toLowerCase());
        ItemStack result = new ItemStack(output, amount);

        StonecuttingRecipe recipe = new StonecuttingRecipe(key, result, input);
        getServer().addRecipe(recipe);
        stonecutterRecipeKeys.add(key);
    }

    private void removeStonecutterRecipes() {
        for (NamespacedKey key : stonecutterRecipeKeys) {
            getServer().removeRecipe(key);
        }
        stonecutterRecipeKeys.clear();
    }

    // ==================== 定时任务管理（支持动态启停） ====================
    private void startTasks() {
        if (enablePetHealth) startPetHealthTask();
        if (enablePhantomRepel) startPhantomRepelTask();
    }

    private void cancelTasks() {
        if (petHealthTask != null) { petHealthTask.cancel(); petHealthTask = null; }
        if (phantomRepelTask != null) { phantomRepelTask.cancel(); phantomRepelTask = null; }
    }

    // ==================== 配置文件热更新（文件修改时间轮询） ====================
    private void startConfigWatcher() {
        File configFile = new File(getDataFolder(), "config.yml");
        lastConfigModified = configFile.lastModified();

        new BukkitRunnable() {
            @Override
            public void run() {
                File file = new File(getDataFolder(), "config.yml");
                long currentModified = file.lastModified();
                if (currentModified != lastConfigModified) {
                    lastConfigModified = currentModified;
                    cancelTasks();
                    removeStonecutterRecipes();
                    reloadConfig();
                    loadConfig();
                    startTasks();
                    registerStonecutterRecipes();
                    getLogger().info("config.yml 已自动重载!");
                }
            }
        }.runTaskTimer(AssistPlugin.this, 20L, 20L);
    }

    private class AssistCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                cancelTasks();
                removeStonecutterRecipes();
                reloadConfig();
                loadConfig();
                startTasks();
                registerStonecutterRecipes();
                File configFile = new File(getDataFolder(), "config.yml");
                lastConfigModified = configFile.lastModified();
                sender.sendMessage(ChatColor.GREEN + "AssistPlugin 配置已重载!");
                return true;
            }
            sender.sendMessage(ChatColor.GOLD + "=== AssistPlugin ===");
            sender.sendMessage(ChatColor.YELLOW + "/assist reload" + ChatColor.GRAY + " - 重载配置文件");
            return true;
        }
    }

    // ==================== 功能4: 宠物生命提升 (狗和猫 -> 20HP) ====================
    private void startPetHealthTask() {
        petHealthTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : getServer().getWorlds()) {
                    for (Wolf wolf : world.getEntitiesByClass(Wolf.class)) {
                        if (!wolf.isTamed()) continue;
                        var attr = wolf.getAttribute(Attribute.MAX_HEALTH);
                        if (attr != null && attr.getBaseValue() < 20.0) {
                            attr.setBaseValue(20.0);
                            if (wolf.getHealth() > 20) wolf.setHealth(20);
                        }
                    }
                    for (Cat cat : world.getEntitiesByClass(Cat.class)) {
                        if (!cat.isTamed()) continue;
                        var attr = cat.getAttribute(Attribute.MAX_HEALTH);
                        if (attr != null && attr.getBaseValue() < 20.0) {
                            attr.setBaseValue(20.0);
                            if (cat.getHealth() > 20) cat.setHealth(20);
                        }
                    }
                }
            }
        };
        petHealthTask.runTaskTimer(this, 40L, 60L);
    }

    // ==================== 功能3: 火把驱赶幻翼（半径8格，频率2秒） ====================
    private void startPhantomRepelTask() {
        phantomRepelTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : getServer().getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    for (Phantom phantom : world.getEntitiesByClass(Phantom.class)) {
                        Location loc = phantom.getLocation();
                        Block nearestTorch = findNearestTorch(loc, 8);
                        if (nearestTorch != null) {
                            org.bukkit.util.Vector dir = loc.toVector()
                                .subtract(nearestTorch.getLocation().add(0.5, 0.5, 0.5).toVector())
                                .normalize().multiply(0.8);
                            dir.setY(0.4);
                            phantom.setVelocity(dir);
                        }
                    }
                }
            }
        };
        phantomRepelTask.runTaskTimer(this, 40L, 40L);
    }

    private Block findNearestTorch(Location loc, int radius) {
        Block center = loc.getBlock();
        Block nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = center.getRelative(x, y, z);
                    Material type = b.getType();
                    if (type == Material.TORCH || type == Material.WALL_TORCH
                            || type == Material.SOUL_TORCH || type == Material.SOUL_WALL_TORCH
                            || type == Material.REDSTONE_TORCH || type == Material.REDSTONE_WALL_TORCH) {
                        double dist = loc.distanceSquared(b.getLocation().add(0.5, 0.5, 0.5));
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = b;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    // ==================== 功能5: 发光浆果 -> 发光效果 ====================
    @EventHandler
    public void onGlowBerriesEat(PlayerItemConsumeEvent event) {
        if (!enableGlowBerries) return;
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType() != Material.GLOW_BERRIES) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, true, true, true));
                    player.sendMessage(ChatColor.YELLOW + "\u4f60\u98df\u7528\u4e86\u53d1\u5149\u6d46\u679c\uff0c\u83b7\u5f97\u4e86\u53d1\u5149\u6548\u679c!");
                }
            }
        }.runTaskLater(AssistPlugin.this, 1L);
    }

    // ==================== 功能6: 猫坐在箱子上也能打开 ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onCatBlockChest(PlayerInteractEvent event) {
        if (!enableCatChest) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.isCancelled()) return;
        Block block = event.getClickedBlock();
        if (block == null || !isChest(block.getType())) return;

        for (Entity entity : block.getRelative(BlockFace.UP).getWorld()
                .getNearbyEntities(block.getRelative(BlockFace.UP).getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
            if (entity instanceof Cat cat && cat.isSitting()) {
                event.setCancelled(false);
                return;
            }
        }
    }

    // ==================== 功能7: 箱子上有方块也能打开 ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockAboveChest(PlayerInteractEvent event) {
        if (!enableBlockChest) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !isChest(block.getType())) return;

        Block above = block.getRelative(BlockFace.UP);
        Material aboveType = above.getType();
        if (aboveType.isSolid()) {
            event.setCancelled(true);
            if (block.getState() instanceof InventoryHolder holder) {
                event.getPlayer().openInventory(holder.getInventory());
            }
        }
    }

    // ==================== 功能8: 雪球灭火 (蜡烛/篝火/火焰) ====================
    @EventHandler
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!enableSnowballExtinguish) return;
        if (!(event.getEntity() instanceof Snowball)) return;
        Block block = event.getHitBlock();
        if (block == null) return;

        Material type = block.getType();
        Location loc = block.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        boolean extinguished = false;

        // 蜡烛
        if (type.name().contains("CANDLE") && !type.name().contains("CAKE")) {
            block.setType(Material.AIR);
            extinguished = true;
        }
        // 篝火 - 熄灭而不是移除
        if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE) {
            BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.type.Campfire cf) {
                if (cf.isLit()) {
                    cf.setLit(false);
                    block.setBlockData(cf);
                    extinguished = true;
                }
            }
        }
        // 火焰
        if (type == Material.FIRE || type == Material.SOUL_FIRE) {
            block.setType(Material.AIR);
            extinguished = true;
        }

        if (extinguished) {
            world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.02);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 1.0f);
        }
    }

    // ==================== 功能9: Shift+F 快捷键执行自定义命令 ====================
    @EventHandler
    public void onShiftF(PlayerSwapHandItemsEvent event) {
        if (!enableShiftF) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        event.setCancelled(true);

        // 支持 %player% 变量
        final String command = shiftFCommand.replace("%player%", player.getName());
        getServer().getScheduler().runTask(this, () -> {
            if (!player.isOnline()) return;
            if (shiftFAsOp) {
                boolean wasOp = player.isOp();
                player.setOp(true);
                try {
                    getServer().dispatchCommand(player, command);
                } finally {
                    player.setOp(wasOp);
                }
            } else {
                getServer().dispatchCommand(player, command);
            }
        });
    }

    // ==================== 工具方法 ====================
    private boolean isChest(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }
}
