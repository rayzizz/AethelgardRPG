package me.ray.aethelgardRPG.modules.classcombat.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.classcombat.ClassCombatModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class PlayerRegenListener implements Listener {

    private final AethelgardRPG plugin;
    private final ClassCombatModule classCombatModule;

    public PlayerRegenListener(AethelgardRPG plugin, ClassCombatModule classCombatModule) {
        this.plugin = plugin;
        this.classCombatModule = classCombatModule;
    }

    @EventHandler
    public void onPlayerRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Cancela a regeneração de vida padrão do Minecraft.
        // A regeneração será controlada pelo sistema de RPG (ex: por feitiços, poções, passivas de classe).
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setCancelled(true);
            // Removido: plugin.getLogger().info("[DEBUG] Regeneração padrão cancelada para " + ((Player) event.getEntity()).getName());
        }
    }
}