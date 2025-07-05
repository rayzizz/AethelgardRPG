package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;

public class PlayerRespawnListener implements Listener {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;

    public PlayerRespawnListener(AethelgardRPG plugin, CharacterModule characterModule) {
        this.plugin = plugin;
        this.characterModule = characterModule;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);

        playerDataOpt.ifPresent(playerData -> {
            PlayerAttributes attributes = playerData.getAttributes();
            if (attributes != null) {
                attributes.setCurrentHealth(attributes.getMaxHealth());
                attributes.setCurrentMana(attributes.getMaxMana());
                // Force an action bar update by clearing the cache for this player
                if (characterModule.getStatusDisplayManager() != null) {
                    characterModule.getStatusDisplayManager().clearPlayerFromCache(player.getUniqueId());
                }
                plugin.getLogger().fine("Player " + player.getName() + " respawned. Health and Mana reset for action bar display.");
            }
            characterModule.giveOrUpdateProfileCompass(player); // Give compass on respawn
        });
    }
}