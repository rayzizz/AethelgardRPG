package me.ray.aethelgardRPG.modules.spell.spells.warrior;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.WorldEffectManager;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.preferences.PlayerPreferences;
import me.ray.aethelgardRPG.modules.preferences.PlayerPreferencesManager;
import me.ray.aethelgardRPG.modules.spell.spells.BaseSpell;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class HeroicLeapSpell extends BaseSpell {

    // --- CONFIGURABLE VALUES ---
    private static final String ID = "warrior_heroic_leap";
    private static final String NAME_KEY = "spell.warrior.heroic_leap.name";
    private static final String DESCRIPTION_KEY = "spell.warrior.heroic_leap.description";
    private static final int COOLDOWN_SECONDS = 15;
    private static final double MANA_COST = 30.0;
    private static final RPGClass REQUIRED_CLASS = RPGClass.GUERREIRO;
    private static final int REQUIRED_LEVEL = 1;
    private static final double MAX_RANGE = 0;

    private final double leapPower = 1.8;
    private final double leapVerticalBoost = 1.2;
    private final int slamRadius = 5;
    private final double slamDamage = 25.0;
    private final double slamKnockback = 1.5;
    private final double blockEffectChance = 0.65;
    private final List<Material> ignoredMaterials = List.of(Material.BEDROCK, Material.BARRIER);

    // --- NEW: Damage Falloff Configuration ---
    // Minimum damage as a ratio of base damage (e.g., 0.25 means 25% of slamDamage)
    private final double slamMinDamageRatio = 0.25;

    private final PlayerPreferencesManager preferencesManager;

    public HeroicLeapSpell(AethelgardRPG plugin) {
        super(plugin, ID, NAME_KEY, DESCRIPTION_KEY, COOLDOWN_SECONDS, MANA_COST, REQUIRED_CLASS, REQUIRED_LEVEL, MAX_RANGE);
        this.preferencesManager = plugin.getPlayerPreferencesManager();
    }

    /**
     * Helper method to get players within a certain radius of a location.
     */
    private List<Player> getPlayersInRadius(Location center, double radius) {
        List<Player> playersInRange = new ArrayList<>();
        if (center.getWorld() == null) return playersInRange;
        double radiusSquared = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= radiusSquared) {
                playersInRange.add(p);
            }
        }
        return playersInRange;
    }

    @Override
    public SpellCastResult execute(SpellCastContext context) {
        Player player = context.getCaster();

        Vector direction = player.getLocation().getDirection().normalize();
        Vector leapVelocity = direction.multiply(leapPower).setY(leapVerticalBoost);
        player.setVelocity(leapVelocity);

        // Play sound based on preferences
        List<Player> nearbyPlayers = getPlayersInRadius(player.getLocation(), 64); // 64 block radius for sound
        for (Player viewer : nearbyPlayers) {
            if (preferencesManager.getPreferences(viewer).canShowSpellParticles()) { // Using particle pref for sound too
                viewer.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.2F);
            }
        }

        new GroundSlamTask(player).runTaskTimer(plugin, 5L, 1L);

        return SpellCastResult.SUCCESS;
    }

    @Override
    public String getDisplayDescription(Player player) {
        // Updated description to reflect damage falloff
        return plugin.getMessage(player, getDescriptionKey(),
                String.valueOf(slamDamage),
                String.valueOf(slamRadius),
                String.valueOf((int)(slamMinDamageRatio * 100)) // Pass min damage ratio as percentage
        );
    }

    private class GroundSlamTask extends BukkitRunnable {
        private final Player player;
        private int ticksInAir = 0;
        private static final int MAX_AIR_TIME_TICKS = 20 * 10;

        public GroundSlamTask(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            ticksInAir++;
            if (!player.isOnline() || player.isDead() || ticksInAir > MAX_AIR_TIME_TICKS) {
                this.cancel();
                return;
            }
            if (player.isOnGround() && ticksInAir > 5) {
                performSlam(player.getLocation());
                this.cancel();
            }
        }

        private void performSlam(Location center) {
            List<Player> nearbyPlayers = getPlayersInRadius(center, 64);

            // World Effect: Create flying blocks
            List<Block> affectedBlocks = WorldEffectManager.getBlocksInExplosionRadius(center, slamRadius, blockEffectChance, ignoredMaterials);
            List<FallingBlock> visualBlocks = new ArrayList<>();
            for (Block block : affectedBlocks) {
                FallingBlock fb = WorldEffectManager.createFlyingBlockVisual(block);
                if (fb != null) {
                    visualBlocks.add(fb);
                }
            }

            // Loop through viewers to show effects based on their settings
            for (Player viewer : nearbyPlayers) {
                PlayerPreferences prefs = preferencesManager.getPreferences(viewer);

                // Check for particle/sound preferences
                if (prefs.canShowSpellParticles()) {
                    viewer.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.8F);
                    viewer.playSound(center, Sound.BLOCK_STONE_BREAK, 2.0F, 0.7F);
                    viewer.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                    viewer.spawnParticle(Particle.BLOCK_CRUMBLE, center.clone().add(0, 0.2, 0), 100, slamRadius * 0.5, 0.2, slamRadius * 0.5, Material.DIRT.createBlockData());
                }

                // Check for map effect preferences
                if (!prefs.canShowMapEffectVisuals()) {
                    // If the player has map effects disabled, hide the flying blocks from them
                    for (FallingBlock fb : visualBlocks) {
                        viewer.hideEntity(plugin, fb);
                    }
                }
            }

            // Damage and Knockback (Gameplay effect, always runs)
            List<Entity> nearbyEntities = player.getNearbyEntities(slamRadius, slamRadius, slamRadius);
            for (Entity entity : nearbyEntities) {
                if (entity instanceof LivingEntity && !entity.equals(player)) {
                    LivingEntity target = (LivingEntity) entity;

                    // --- NEW: Damage Falloff Calculation ---
                    double distance = target.getLocation().distance(center);
                    double damageMultiplier = 1.0 - (distance / slamRadius); // 1.0 at center, 0.0 at radius edge
                    damageMultiplier = Math.max(0.0, damageMultiplier); // Ensure it doesn't go negative

                    double calculatedDamage = slamDamage * damageMultiplier;
                    double minAllowedDamage = slamDamage * slamMinDamageRatio;

                    // Final damage is clamped between minAllowedDamage and slamDamage
                    double finalDamage = Math.max(minAllowedDamage, calculatedDamage);
                    finalDamage = Math.min(slamDamage, finalDamage); // Ensure it doesn't exceed max damage

                    target.damage(finalDamage, player); // Apply the calculated damage

                    // Apply knockback
                    Vector knockbackDirection = target.getLocation().toVector().subtract(center.toVector()).normalize();
                    knockbackDirection.setY(Math.max(0.4, knockbackDirection.getY()));
                    target.setVelocity(knockbackDirection.multiply(slamKnockback));
                }
            }
        }
    }
}