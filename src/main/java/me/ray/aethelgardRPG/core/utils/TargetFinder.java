package me.ray.aethelgardRPG.core.utils;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.function.Predicate;

public class TargetFinder {

    private final AethelgardRPG plugin;

    public TargetFinder(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Finds the living entity a player is looking at within a certain range.
     * @param player The player looking.
     * @param maxRange The maximum range to check.
     * @return An Optional containing the targeted LivingEntity, or empty if none found.
     */
    public Optional<LivingEntity> findTarget(Player player, double maxRange) {
        Predicate<Entity> filter = entity -> entity instanceof LivingEntity && !entity.equals(player) && entity.isValid();
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), maxRange, 0.5, filter);

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return Optional.of((LivingEntity) result.getHitEntity());
        }
        return Optional.empty();
    }
}