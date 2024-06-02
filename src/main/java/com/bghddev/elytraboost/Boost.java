package com.bghddev.elytraboost;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class Boost extends JavaPlugin implements Listener {

    private static final double MIN_HIT_RATIO = 0.10;
    private static final int COOLDOWN_SECONDS = 10;
    private final Set<Location> onCooldown = new HashSet<>();
    private final Map<Location, Material> ringBlocks = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("generatering") && sender instanceof Player) {
            Player player = (Player) sender;
            generateRing(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Ring generated!");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onRightClickFirework(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() == Material.SNOWBALL &&
                player.isGliding() &&
                event.getHand() == EquipmentSlot.HAND) {

            player.setVelocity(player.getLocation().getDirection().multiply(2.0));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isGliding()) {
            return;
        }

        List<Block> ringBlocks = getRingBlocks(player);
        if (!ringBlocks.isEmpty()) {
            if (!isOnCooldown(ringBlocks)) {
                setRingOnCooldown(ringBlocks);

                spawnInstantFirework(player.getLocation());
                player.setVelocity(player.getLocation().getDirection().multiply(7.0));
                player.sendMessage(ChatColor.GREEN + "You have been boosted!");
            } else {
                player.sendMessage(ChatColor.RED + "This ring is on cooldown.");
            }
        }
    }

    private void generateRing(Location center) {
        int radius = 5;

        // Algorithm to generate a circle, removing the blocks that stick out
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                int distanceSquared = x * x + y * y;
                if (distanceSquared <= radius * radius && distanceSquared >= (radius - 1) * (radius - 1)) {
                    if (!(Math.abs(x) == radius || Math.abs(y) == radius)) { // Remove blocks that stick out
                        Block block = center.clone().add(x, y, 0).getBlock();
                        block.setType(Material.YELLOW_TERRACOTTA);
                        ringBlocks.put(block.getLocation(), block.getType());
                    }
                }
            }
        }
    }

    private List<Block> getRingBlocks(Player player) {
        List<Block> ringBlocksList = new ArrayList<>();
        Location loc = player.getLocation();
        World world = player.getWorld();
        int hits = 0;
        int checks = 0;

        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                Block block = world.getBlockAt(loc.clone().add(x, y, 0));
                checks++;
                if (block.getType() == Material.YELLOW_TERRACOTTA || block.getType() == Material.RED_TERRACOTTA) {
                    ringBlocksList.add(block);
                    hits++;
                }
            }
        }

        double hitRatio = (double) hits / checks;

        if (hitRatio >= MIN_HIT_RATIO) {
            return ringBlocksList;
        }
        return Collections.emptyList();
    }

    private boolean isOnCooldown(List<Block> ringBlocks) {
        for (Block block : ringBlocks) {
            if (onCooldown.contains(block.getLocation())) {
                return true;
            }
        }
        return false;
    }

    private void setRingOnCooldown(List<Block> ringBlocks) {
        Set<Block> uniqueBlocks = new HashSet<>(ringBlocks);

        for (Block block : uniqueBlocks) {
            onCooldown.add(block.getLocation());
            block.setType(Material.RED_TERRACOTTA);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : uniqueBlocks) {
                    block.setType(Material.YELLOW_TERRACOTTA);
                    onCooldown.remove(block.getLocation());
                }
            }
        }.runTaskLater(this, COOLDOWN_SECONDS * 20L); // 20 ticks per second
    }

    private void spawnInstantFirework(Location location) {
        // Create the firework
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);

        // Customize firework (optional)
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
                .flicker(true)
                .withColor(Color.BLUE)
                .withFade(Color.RED)
                .with(FireworkEffect.Type.BURST)
                .trail(true)
                .build();
        meta.addEffect(effect);

        // Set it to detonate instantly
        meta.setPower(0);
        firework.setFireworkMeta(meta);
    }
}
