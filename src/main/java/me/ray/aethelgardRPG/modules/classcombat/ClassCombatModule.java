package me.ray.aethelgardRPG.modules.classcombat;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.classcombat.listeners.PlayerDamageListener;
import me.ray.aethelgardRPG.modules.classcombat.listeners.PlayerRegenListener;
import me.ray.aethelgardRPG.modules.character.CharacterModule; // Import CharacterModule

public class ClassCombatModule implements RPGModule {

    private final AethelgardRPG plugin;
    private CharacterModule characterModule; // Adicionar esta referência

    public ClassCombatModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ClassCombat";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo ClassCombat...");

        // Obter a instância do CharacterModule
        this.characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (this.characterModule == null) {
            // É crucial que o CharacterModule esteja carregado para o ClassCombatModule funcionar corretamente.
            // Se ele não for encontrado, logamos um erro grave.
            plugin.getLogger().severe("CharacterModule não encontrado! O ClassCombatModule pode não funcionar corretamente.");
            // Você pode considerar desabilitar o plugin ou o módulo aqui se a dependência for crítica.
            // Por exemplo: plugin.getModuleManager().disableModule(this);
        }

        // Registrar listeners para o sistema de combate customizado
        // *** CORREÇÃO APLICADA AQUI ***
        // O construtor de PlayerDamageListener só precisa da instância do plugin.
        plugin.getServer().getPluginManager().registerEvents(new PlayerDamageListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerRegenListener(plugin, this), plugin);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo ClassCombat habilitado.");
        // Lógica para quando o módulo é habilitado
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo ClassCombat desabilitado.");
        // Lógica para quando o módulo é desabilitado
    }
}