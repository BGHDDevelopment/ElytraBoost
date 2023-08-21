package com.bghddev.elytrabooost;

import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ElytraBooost extends JavaPlugin implements Listener {

    private final int CHECK_DISTANCE = 5; // How far we're checking for a ring
    private static final double MIN_HIT_RATIO = 0.20;
    private final Map<UUID, Long> lastBoostTime = new HashMap<>();


    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
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

        if (isInsideRing(player)) {
            Long lastBoost = lastBoostTime.get(player.getUniqueId());
            long currentTime = System.currentTimeMillis();

            // Check if 10 seconds (10000 milliseconds) have passed since the last boost
            if (lastBoost == null || (currentTime - lastBoost) >= 10000) {
                spawnInstantFirework(player.getLocation());
                player.setVelocity(player.getLocation().getDirection().multiply(7.0));

                // Update the player's last boost time
                lastBoostTime.put(player.getUniqueId(), currentTime);
            }
        }
    }

    private boolean isInsideRing(Player player) {
        int hits = 0;
        for (double theta = 0; theta < 2 * Math.PI; theta += 0.1) { // Casting rays in different directions
            Vector direction = new Vector(Math.cos(theta), 0, Math.sin(theta));
            RayTraceResult result = player.getWorld().rayTraceBlocks(player.getLocation(), direction, CHECK_DISTANCE);

            if (result != null && result.getHitBlock() != null) {
                Block hitBlock = result.getHitBlock();

                if (hitBlock.getType() == Material.DIAMOND_BLOCK) {
                    hits++;
                }
            }
        }

        return ((double) hits / (2 * Math.PI / 0.1)) >= MIN_HIT_RATIO;
    }

    private void spawnInstantFirework(Location location) {
        // Create the firework
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);

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
