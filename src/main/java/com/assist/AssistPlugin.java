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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
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
    private boolean enableNightVision;

    // 缓存 Material.values()，避免重复创建数组
    private static final Material[] MATERIALS = Material.values();

    // 中文名称映射
    private static final Map<Material, String> CN = new HashMap<>();
    // 切石机配方: 输入 -> [(输出ordinal, 输出数量)]
    private static final Map<Material, List<int[]>> CUTTING_RECIPES = new LinkedHashMap<>();
    // 玩家正在切割的材料（玩家离线时清理）
    private final Map<UUID, Material> playerInput = new HashMap<>();

    // 定时任务引用，用于动态启停
    private BukkitRunnable nightVisionTask;
    private BukkitRunnable petHealthTask;
    private BukkitRunnable phantomRepelTask;

    // 配置文件热更新
    private long lastConfigModified = 0;

    static {
        // ===== 中文名 =====
        String[][] names = {
            {"OAK_LOG", "\u6a61\u6728\u539f\u6728"}, {"OAK_WOOD", "\u6a61\u6728"}, {"OAK_PLANKS", "\u6a61\u6728\u6728\u677f"},
            {"OAK_SLAB", "\u6a61\u6728\u53f0\u9636"}, {"OAK_STAIRS", "\u6a61\u6728\u697c\u68af"}, {"OAK_FENCE", "\u6a61\u6728\u6805\u680f"},
            {"OAK_FENCE_GATE", "\u6a61\u6728\u6805\u680f\u95e8"}, {"OAK_PRESSURE_PLATE", "\u6a61\u6728\u538b\u529b\u677f"},
            {"OAK_BUTTON", "\u6a61\u6728\u6309\u94ae"},

            {"SPRUCE_LOG", "\u4e91\u6749\u539f\u6728"}, {"SPRUCE_WOOD", "\u4e91\u6749\u6728"}, {"SPRUCE_PLANKS", "\u4e91\u6749\u6728\u677f"},
            {"SPRUCE_SLAB", "\u4e91\u6749\u53f0\u9636"}, {"SPRUCE_STAIRS", "\u4e91\u6749\u697c\u68af"}, {"SPRUCE_FENCE", "\u4e91\u6749\u6805\u680f"},
            {"SPRUCE_FENCE_GATE", "\u4e91\u6749\u6805\u680f\u95e8"}, {"SPRUCE_PRESSURE_PLATE", "\u4e91\u6749\u538b\u529b\u677f"},
            {"SPRUCE_BUTTON", "\u4e91\u6749\u6309\u94ae"},

            {"BIRCH_LOG", "\u767d\u68d3\u539f\u6728"}, {"BIRCH_WOOD", "\u767d\u68d3\u6728"}, {"BIRCH_PLANKS", "\u767d\u68d3\u6728\u677f"},
            {"BIRCH_SLAB", "\u767d\u68d3\u53f0\u9636"}, {"BIRCH_STAIRS", "\u767d\u68d3\u697c\u68af"}, {"BIRCH_FENCE", "\u767d\u68d3\u6805\u680f"},
            {"BIRCH_FENCE_GATE", "\u767d\u68d3\u6805\u680f\u95e8"}, {"BIRCH_PRESSURE_PLATE", "\u767d\u68d3\u538b\u529b\u677f"},
            {"BIRCH_BUTTON", "\u767d\u68d3\u6309\u94ae"},

            {"JUNGLE_LOG", "\u4e1b\u6797\u539f\u6728"}, {"JUNGLE_WOOD", "\u4e1b\u6797\u6728"}, {"JUNGLE_PLANKS", "\u4e1b\u6797\u6728\u677f"},
            {"JUNGLE_SLAB", "\u4e1b\u6797\u53f0\u9636"}, {"JUNGLE_STAIRS", "\u4e1b\u6797\u697c\u68af"}, {"JUNGLE_FENCE", "\u4e1b\u6797\u6805\u680f"},
            {"JUNGLE_FENCE_GATE", "\u4e1b\u6797\u6805\u680f\u95e8"}, {"JUNGLE_PRESSURE_PLATE", "\u4e1b\u6797\u538b\u529b\u677f"},
            {"JUNGLE_BUTTON", "\u4e1b\u6797\u6309\u94ae"},

            {"ACACIA_LOG", "\u91d1\u5408\u6b22\u539f\u6728"}, {"ACACIA_WOOD", "\u91d1\u5408\u6b22\u6728"}, {"ACACIA_PLANKS", "\u91d1\u5408\u6b22\u6728\u677f"},
            {"ACACIA_SLAB", "\u91d1\u5408\u6b22\u53f0\u9636"}, {"ACACIA_STAIRS", "\u91d1\u5408\u6b22\u697c\u68af"}, {"ACACIA_FENCE", "\u91d1\u5408\u6b22\u6805\u680f"},
            {"ACACIA_FENCE_GATE", "\u91d1\u5408\u6b22\u6805\u680f\u95e8"}, {"ACACIA_PRESSURE_PLATE", "\u91d1\u5408\u6b22\u538b\u529b\u677f"},
            {"ACACIA_BUTTON", "\u91d1\u5408\u6b22\u6309\u94ae"},

            {"DARK_OAK_LOG", "\u6df1\u8272\u6a61\u6728\u539f\u6728"}, {"DARK_OAK_WOOD", "\u6df1\u8272\u6a61\u6728"}, {"DARK_OAK_PLANKS", "\u6df1\u8272\u6a61\u6728\u6728\u677f"},
            {"DARK_OAK_SLAB", "\u6df1\u8272\u6a61\u6728\u53f0\u9636"}, {"DARK_OAK_STAIRS", "\u6df1\u8272\u6a61\u6728\u697c\u68af"}, {"DARK_OAK_FENCE", "\u6df1\u8272\u6a61\u6728\u6805\u680f"},
            {"DARK_OAK_FENCE_GATE", "\u6df1\u8272\u6a61\u6728\u6805\u680f\u95e8"}, {"DARK_OAK_PRESSURE_PLATE", "\u6df1\u8272\u6a61\u6728\u538b\u529b\u677f"},
            {"DARK_OAK_BUTTON", "\u6df1\u8272\u6a61\u6728\u6309\u94ae"},

            {"MANGROVE_LOG", "\u7ea2\u6811\u539f\u6728"}, {"MANGROVE_WOOD", "\u7ea2\u6811\u6728"}, {"MANGROVE_PLANKS", "\u7ea2\u6811\u6728\u677f"},
            {"MANGROVE_SLAB", "\u7ea2\u6811\u53f0\u9636"}, {"MANGROVE_STAIRS", "\u7ea2\u6811\u697c\u68af"}, {"MANGROVE_FENCE", "\u7ea2\u6811\u6805\u680f"},
            {"MANGROVE_FENCE_GATE", "\u7ea2\u6811\u6805\u680f\u95e8"}, {"MANGROVE_PRESSURE_PLATE", "\u7ea2\u6811\u538b\u529b\u677f"},
            {"MANGROVE_BUTTON", "\u7ea2\u6811\u6309\u94ae"},

            {"CHERRY_LOG", "\u6a31\u82b1\u539f\u6728"}, {"CHERRY_WOOD", "\u6a31\u82b1\u6728"}, {"CHERRY_PLANKS", "\u6a31\u82b1\u6728\u677f"},
            {"CHERRY_SLAB", "\u6a31\u82b1\u53f0\u9636"}, {"CHERRY_STAIRS", "\u6a31\u82b1\u697c\u68af"}, {"CHERRY_FENCE", "\u6a31\u82b1\u6805\u680f"},
            {"CHERRY_FENCE_GATE", "\u6a31\u82b1\u6805\u680f\u95e8"}, {"CHERRY_PRESSURE_PLATE", "\u6a31\u82b1\u538b\u529b\u677f"},
            {"CHERRY_BUTTON", "\u6a31\u82b1\u6309\u94ae"},

            {"BAMBOO_BLOCK", "\u7af9\u5757"}, {"BAMBOO_PLANKS", "\u7af9\u6728\u677f"},
            {"BAMBOO_SLAB", "\u7af9\u53f0\u9636"}, {"BAMBOO_STAIRS", "\u7af9\u697c\u68af"}, {"BAMBOO_FENCE", "\u7af9\u6805\u680f"},
            {"BAMBOO_FENCE_GATE", "\u7af9\u6805\u680f\u95e8"}, {"BAMBOO_PRESSURE_PLATE", "\u7af9\u538b\u529b\u677f"},
            {"BAMBOO_BUTTON", "\u7af9\u6309\u94ae"},

            {"WARPED_STEM", "\u8be1\u5f02\u83cc\u6811\u5e72"}, {"WARPED_HYPHAE", "\u8be1\u5f02\u83cc\u6811\u5e72\u53cc\u5c42"},
            {"WARPED_PLANKS", "\u8be1\u5f02\u6728\u677f"}, {"WARPED_SLAB", "\u8be1\u5f02\u53f0\u9636"}, {"WARPED_STAIRS", "\u8be1\u5f02\u697c\u68af"},
            {"WARPED_FENCE", "\u8be1\u5f02\u6805\u680f"}, {"WARPED_FENCE_GATE", "\u8be1\u5f02\u6805\u680f\u95e8"},
            {"WARPED_PRESSURE_PLATE", "\u8be1\u5f02\u538b\u529b\u677f"}, {"WARPED_BUTTON", "\u8be1\u5f02\u6309\u94ae"},

            {"CRIMSON_STEM", "\u7eaa\u7ea2\u83cc\u6811\u5e72"}, {"CRIMSON_HYPHAE", "\u7eaa\u7ea2\u83cc\u6811\u5e72\u53cc\u5c42"},
            {"CRIMSON_PLANKS", "\u7eaa\u7ea2\u6728\u677f"}, {"CRIMSON_SLAB", "\u7eaa\u7ea2\u53f0\u9636"}, {"CRIMSON_STAIRS", "\u7eaa\u7ea2\u697c\u68af"},
            {"CRIMSON_FENCE", "\u7eaa\u7ea2\u6805\u680f"}, {"CRIMSON_FENCE_GATE", "\u7eaa\u7ea2\u6805\u680f\u95e8"},
            {"CRIMSON_PRESSURE_PLATE", "\u7eaa\u7ea2\u538b\u529b\u677f"}, {"CRIMSON_BUTTON", "\u7eaa\u7ea2\u6309\u94ae"},

            {"GLOW_BERRIES", "\u53d1\u5149\u6d46\u679c"},
        };
        for (String[] pair : names) {
            Material m = Material.matchMaterial(pair[0]);
            if (m != null) CN.put(m, pair[1]);
        }

        // ===== 切石机配方: 1原木 -> 多种产物 =====
        String[] woodTypes = {
            "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK", "MANGROVE", "CHERRY"
        };
        String[] woodSources = {"_LOG", "_WOOD"};

        for (String wood : woodTypes) {
            String plank = wood + "_PLANKS";
            String slab = wood + "_SLAB";
            String stairs = wood + "_STAIRS";
            String fence = wood + "_FENCE";
            String gate = wood + "_FENCE_GATE";
            String plate = wood + "_PRESSURE_PLATE";
            String button = wood + "_BUTTON";

            for (String src : woodSources) {
                Material input = Material.matchMaterial(wood + src);
                if (input == null) continue;
                addRecipe(input, plank, 4);
                addRecipe(input, slab, 6);
                addRecipe(input, stairs, 2);
                addRecipe(input, fence, 3);
                addRecipe(input, gate, 1);
                addRecipe(input, plate, 1);
                addRecipe(input, button, 1);
            }
        }
        // 竹
        addRecipe(Material.BAMBOO_BLOCK, "BAMBOO_PLANKS", 4);
        addRecipe(Material.BAMBOO_BLOCK, "BAMBOO_SLAB", 6);
        addRecipe(Material.BAMBOO_BLOCK, "BAMBOO_STAIRS", 2);
        addRecipe(Material.BAMBOO_BLOCK, "BAMBOO_FENCE", 3);
        addRecipe(Material.BAMBOO_BLOCK, "BAMBOO_FENCE_GATE", 1);
        addRecipe(Material.BAMBOO_BLOCK, "BAMBOO_PRESSURE_PLATE", 1);
        addRecipe(Material.BAMBOO_BLOCK, "BAMBOO_BUTTON", 1);
        // 下界
        for (String stem : new String[]{"WARPED", "CRIMSON"}) {
            for (String src : new String[]{"_STEM", "_HYPHAE"}) {
                Material input = Material.matchMaterial(stem + src);
                if (input == null) continue;
                addRecipe(input, stem + "_PLANKS", 4);
                addRecipe(input, stem + "_SLAB", 6);
                addRecipe(input, stem + "_STAIRS", 2);
                addRecipe(input, stem + "_FENCE", 3);
                addRecipe(input, stem + "_FENCE_GATE", 1);
                addRecipe(input, stem + "_PRESSURE_PLATE", 1);
                addRecipe(input, stem + "_BUTTON", 1);
            }
        }
    }

    private static void addRecipe(Material input, String outputName, int amount) {
        Material output = Material.matchMaterial(outputName);
        if (input == null || output == null) return;
        CUTTING_RECIPES.computeIfAbsent(input, k -> new ArrayList<>())
            .add(new int[]{output.ordinal(), amount});
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nv").setExecutor(new NightVisionCommand());
        getCommand("assist").setExecutor(new AssistCommand());
        startTasks();
        startConfigWatcher();
        getLogger().info("AssistPlugin loaded - 8 features active!");
    }

    @Override
    public void onDisable() {
        cancelTasks();
    }

    private void loadConfig() {
        enableStonecutter = getConfig().getBoolean("stonecutter", true);
        enablePhantomRepel = getConfig().getBoolean("phantom-repel", true);
        enablePetHealth = getConfig().getBoolean("pet-health", true);
        enableGlowBerries = getConfig().getBoolean("glow-berries", true);
        enableCatChest = getConfig().getBoolean("cat-chest", true);
        enableBlockChest = getConfig().getBoolean("block-chest", true);
        enableSnowballExtinguish = getConfig().getBoolean("snowball-extinguish", true);
        enableNightVision = getConfig().getBoolean("night-vision", true);
    }

    // ==================== 定时任务管理（支持动态启停） ====================
    private void startTasks() {
        if (enableNightVision) startNightVisionTask();
        if (enablePetHealth) startPetHealthTask();
        if (enablePhantomRepel) startPhantomRepelTask();
    }

    private void cancelTasks() {
        if (nightVisionTask != null) { nightVisionTask.cancel(); nightVisionTask = null; }
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
                    reloadConfig();
                    loadConfig();
                    startTasks();
                    getLogger().info("config.yml 已自动重载!");
                }
            }
        }.runTaskTimer(AssistPlugin.this, 20L, 20L);
    }

    // ==================== 功能9: 夜视开关指令 /nv ====================
    private final Set<UUID> nightVisionPlayers = new HashSet<>();

    private class NightVisionCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "此指令只能由玩家使用");
                return true;
            }

            UUID uuid = player.getUniqueId();
            if (nightVisionPlayers.contains(uuid)) {
                nightVisionPlayers.remove(uuid);
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                player.sendMessage(ChatColor.YELLOW + "夜视效果已关闭");
            } else {
                nightVisionPlayers.add(uuid);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20, 0, true, true, true));
                player.sendMessage(ChatColor.GREEN + "夜视效果已开启");
            }
            return true;
        }
    }

    private class AssistCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                cancelTasks();
                reloadConfig();
                loadConfig();
                startTasks();
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

    private void startNightVisionTask() {
        nightVisionTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<UUID> it = nightVisionPlayers.iterator();
                while (it.hasNext()) {
                    UUID uuid = it.next();
                    Player player = getServer().getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20, 0, true, true, true));
                        }
                    } else {
                        it.remove();
                    }
                }
            }
        };
        nightVisionTask.runTaskTimer(AssistPlugin.this, 0L, 30L);
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

    // ==================== 功能1: 万能切石机 ====================
    @EventHandler
    public void onStonecutterUse(PlayerInteractEvent event) {
        if (!enableStonecutter) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.STONECUTTER) return;

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        Material inputType = handItem.getType();
        if (!CUTTING_RECIPES.containsKey(inputType)) return;

        event.setCancelled(true);
        openCuttingGUI(player, inputType);
    }

    private void openCuttingGUI(Player player, Material inputType) {
        playerInput.put(player.getUniqueId(), inputType);
        String inputCN = CN.getOrDefault(inputType, inputType.name());
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GOLD + "\u5207\u77f3\u673a - " + inputCN);

        List<int[]> recipes = CUTTING_RECIPES.get(inputType);
        if (recipes == null) return;

        int slot = 10;
        for (int[] recipe : recipes) {
            if (slot >= 17) break;
            Material outputType = MATERIALS[recipe[0]];
            int amount = recipe[1];

            ItemStack item = new ItemStack(outputType, amount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String outCN = CN.getOrDefault(outputType, outputType.name());
                meta.setDisplayName(ChatColor.GREEN + outCN + " x" + amount);
                meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "\u70b9\u51fb\u5207\u5272",
                    ChatColor.GRAY + "\u6d88\u8017 1 \u4e2a " + inputCN
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
            if (slot == 17) slot = 19;
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title == null || !title.contains("\u5207\u77f3\u673a")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        UUID uuid = player.getUniqueId();
        Material inputType = playerInput.get(uuid);
        if (inputType == null) { player.closeInventory(); return; }

        List<int[]> recipes = CUTTING_RECIPES.get(inputType);
        if (recipes == null) { player.closeInventory(); return; }

        // 匹配配方
        int[] matched = null;
        for (int[] r : recipes) {
            if (MATERIALS[r[0]] == clicked.getType()) { matched = r; break; }
        }
        if (matched == null) { player.closeInventory(); return; }

        PlayerInventory inv = player.getInventory();
        ItemStack mainHand = inv.getItemInMainHand();

        // 验证手中材料
        if (mainHand.getType() != inputType || mainHand.getAmount() < 1) {
            player.sendMessage(ChatColor.RED + "\u624b\u91cc\u6ca1\u6709\u8db3\u591f\u7684\u6750\u6599!");
            player.closeInventory();
            return;
        }

        // 消耗1个输入
        mainHand.setAmount(mainHand.getAmount() - 1);
        if (mainHand.getAmount() <= 0) inv.setItemInMainHand(null);

        // 给予输出
        Material outputType = MATERIALS[matched[0]];
        int outputAmount = matched[1];
        ItemStack resultItem = new ItemStack(outputType, outputAmount);
        HashMap<Integer, ItemStack> overflow = inv.addItem(resultItem);
        if (!overflow.isEmpty()) {
            for (ItemStack over : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), over);
            }
        }

        String outCN = CN.getOrDefault(outputType, outputType.name());
        player.sendMessage(ChatColor.GREEN + "\u5207\u5272\u6210\u529f: " + outCN + " x" + outputAmount);

        // 刷新GUI
        player.closeInventory();
        if (inv.getItemInMainHand() != null && inv.getItemInMainHand().getType() == inputType) {
            openCuttingGUI(player, inputType);
        }
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

    // ==================== 功能7: 箱子上有方块也能打开（修复：不检查 isCancelled） ====================
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

    // ==================== 玩家离线清理（防内存泄漏） ====================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerInput.remove(uuid);
        nightVisionPlayers.remove(uuid);
    }

    // ==================== 工具方法 ====================
    private boolean isChest(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST;
    }
}
