package me.ray.aethelgardRPG.modules.preferences;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.preferences.commands.PreferencesCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PreferencesModule implements RPGModule {

    private final AethelgardRPG plugin;
    private PlayerPreferencesManager preferencesManager;

    public PreferencesModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Preferences";
    }

    @Override
    public void onLoad() {
        // Inicializa o gerenciador que cuida dos dados
        this.preferencesManager = new PlayerPreferencesManager(plugin);

        // Registra o comando que abrirá a GUI
        // CORREÇÃO: Removido o segundo argumento 'this'
        plugin.registerCommand("prefs", new PreferencesCommand(plugin));
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo de Preferências habilitado.");
        // Carrega as preferências de jogadores que já possam estar online (em caso de /reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            preferencesManager.loadPlayerPreferences(player);
        }
    }

    @Override
    public void onDisable() {
        // Garante que todas as preferências sejam salvas ao desabilitar/recarregar o plugin
        if (preferencesManager != null) {
            preferencesManager.saveAllOnlinePlayerPreferences();
        }
        plugin.getLogger().info("Módulo de Preferências desabilitado.");
    }

    public PlayerPreferencesManager getPreferencesManager() {
        return preferencesManager;
    }
}