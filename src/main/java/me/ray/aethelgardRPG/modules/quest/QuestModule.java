package me.ray.aethelgardRPG.modules.quest;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.quest.listeners.NPCInteractQuestListener;
import me.ray.aethelgardRPG.modules.quest.listeners.QuestObjectiveListener;
import me.ray.aethelgardRPG.modules.quest.quests.QuestManager;

public class QuestModule implements RPGModule {

    private final AethelgardRPG plugin;
    private QuestManager questManager;

    public QuestModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Quest";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Quest...");
        this.questManager = new QuestManager(plugin, this);

        // Registrar listeners
        plugin.getServer().getPluginManager().registerEvents(new NPCInteractQuestListener(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new QuestObjectiveListener(plugin, this), plugin);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo Quest habilitado.");
        questManager.loadQuestConfigurations(); // Carregar definições de quests
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Quest desabilitado.");
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    // TODO: Métodos para iniciar cutscenes, gerenciar diálogos
}