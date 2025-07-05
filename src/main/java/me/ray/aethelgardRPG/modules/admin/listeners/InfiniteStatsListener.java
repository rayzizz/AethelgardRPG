package me.ray.aethelgardRPG.modules.admin.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.admin.AdminModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.Optional;

public class InfiniteStatsListener implements Listener {

    private final AdminModule adminModule;
    private final CharacterAPI characterAPI;

    public InfiniteStatsListener(AethelgardRPG plugin, AdminModule adminModule) {
        this.adminModule = adminModule;
        this.characterAPI = plugin.getCharacterAPI();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (adminModule.isInfiniteStatsEnabled(player.getUniqueId())) {
            // Cancela o dano
            event.setCancelled(true);
            // Restaura a vida
            Optional<CharacterData> charDataOpt = characterAPI.getCharacterData(player);
            charDataOpt.ifPresent(characterData -> characterData.getAttributes().setCurrentHealth(characterData.getAttributes().getMaxHealth()));
            // Aplica a vida máxima ao jogador
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && adminModule.isInfiniteStatsEnabled(player.getUniqueId())) {
            event.setCancelled(true);
            // Restaura a fome e a saturação
            player.setFoodLevel(20);
            player.setSaturation(20f);
            // Restaura a stamina
            Optional<CharacterData> charDataOpt = characterAPI.getCharacterData(player);
            charDataOpt.ifPresent(characterData -> characterData.getAttributes().setCurrentStamina(characterData.getAttributes().getMaxStamina()));
        }
    }
}