package me.ray.aethelgardRPG.modules.preferences;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gerencia as preferências de todos os jogadores, lidando com o carregamento e salvamento.
 */
public class PlayerPreferencesManager implements Listener {

    private final AethelgardRPG plugin;
    private final Map<UUID, PlayerPreferences> preferencesCache = new HashMap<>();
    private final File preferencesFile;
    private FileConfiguration preferencesConfig;

    public PlayerPreferencesManager(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.preferencesFile = new File(plugin.getDataFolder(), "player_preferences.yml");
        loadConfigFile();

        // Registra os eventos para carregar/salvar as preferências
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfigFile() {
        if (!preferencesFile.exists()) {
            try {
                if (preferencesFile.createNewFile()) {
                    plugin.getLogger().info("Arquivo player_preferences.yml criado.");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Não foi possível criar o arquivo player_preferences.yml", e);
            }
        }
        this.preferencesConfig = YamlConfiguration.loadConfiguration(preferencesFile);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerPreferences(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        savePlayerPreferences(event.getPlayer());
        preferencesCache.remove(event.getPlayer().getUniqueId()); // Limpa o cache
    }

    public void loadPlayerPreferences(Player player) {
        UUID playerUUID = player.getUniqueId();
        String basePath = "players." + playerUUID;

        PlayerPreferences prefs = new PlayerPreferences();
        // Carrega os valores, usando os padrões da classe se não existirem no arquivo
        prefs.setShowSpellParticles(preferencesConfig.getBoolean(basePath + ".show-spell-particles", true));
        // Usando o novo nome da chave
        prefs.setShowMapEffectVisuals(preferencesConfig.getBoolean(basePath + ".show-map-effect-visuals", true));

        preferencesCache.put(playerUUID, prefs);
        plugin.getLogger().fine("Preferências de " + player.getName() + " carregadas.");
    }

    public void savePlayerPreferences(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerPreferences prefs = preferencesCache.get(playerUUID);

        if (prefs == null) return; // Não há nada para salvar

        String basePath = "players." + playerUUID;
        preferencesConfig.set(basePath + ".show-spell-particles", prefs.canShowSpellParticles());
        // Usando o novo nome da chave
        preferencesConfig.set(basePath + ".show-map-effect-visuals", prefs.canShowMapEffectVisuals());

        // Salva de forma assíncrona para não travar a thread principal
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                preferencesConfig.save(preferencesFile);
                plugin.getLogger().fine("Preferências de " + player.getName() + " salvas.");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar as preferências para " + player.getName(), e);
            }
        });
    }

    public void saveAllOnlinePlayerPreferences() {
        plugin.getLogger().info("Salvando preferências de todos os jogadores online...");
        preferencesCache.keySet().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                savePlayerPreferences(player);
            }
        });
    }

    public PlayerPreferences getPreferences(Player player) {
        // Garante que o jogador sempre tenha um objeto de preferências.
        return preferencesCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            // Se não estiver no cache, carrega (pode acontecer em um /reload)
            loadPlayerPreferences(player);
            return preferencesCache.get(uuid);
        });
    }
}