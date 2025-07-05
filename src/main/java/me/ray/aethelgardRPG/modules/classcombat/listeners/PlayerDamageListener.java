package me.ray.aethelgardRPG.modules.classcombat.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class PlayerDamageListener implements Listener {

    private final AethelgardRPG plugin;

    public PlayerDamageListener(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return; // We are only interested when a player is damaged
        }

        Optional<CharacterData> victimDataOpt = plugin.getCharacterAPI().getCharacterData(victim);
        if (victimDataOpt.isEmpty()) {
            return; // Victim doesn't have character data, let vanilla handle it
        }

        CharacterData victimData = victimDataOpt.get();
        PlayerAttributes victimAttributes = victimData.getAttributes();

        // Placeholder for your complex damage calculation logic
        double originalDamage = event.getDamage();
        double finalDamage = originalDamage; // In a real scenario, you'd calculate this based on stats

        // ... (imagine complex damage calculation logic here) ...

        // *** CORRECTION APPLIED HERE ***
        // The error was `cannot find symbol: method applyDamage(double)`.
        // The correct way to apply damage is to reduce the current health.
        victimAttributes.setCurrentHealth(victimAttributes.getCurrentHealth() - finalDamage);

        // We set the event damage to 0 because we are handling health manually.
        // This prevents vanilla from applying damage on top of our system.
        event.setDamage(0);

        // Optional: Update the player's health bar visually if it's not handled by another system
        // This ensures the vanilla health bar reflects the new custom health value.
        double maxVanillaHealth = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newVanillaHealth = (victimAttributes.getCurrentHealth() / victimAttributes.getMaxHealth()) * maxVanillaHealth;
        victim.setHealth(Math.max(0.001, newVanillaHealth)); // Set health, ensuring it's not 0 unless they are dead
    }
}