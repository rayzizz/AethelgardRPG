package me.ray.aethelgardRPG.modules.skill;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.skill.api.SkillAPI;
import me.ray.aethelgardRPG.modules.skill.listeners.BlockBreakSkillListener;
import me.ray.aethelgardRPG.modules.skill.listeners.PlayerFishSkillListener;
import me.ray.aethelgardRPG.modules.skill.skills.SkillManager;
import org.bukkit.entity.Player;

public class SkillModule implements RPGModule, SkillAPI {

    private final AethelgardRPG plugin;
    private SkillManager skillManager;

    public SkillModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Skill";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Skill...");
        this.skillManager = new SkillManager(plugin, this);

        // Registrar listeners para ganho de XP em habilidades
        plugin.getServer().getPluginManager().registerEvents(new BlockBreakSkillListener(plugin, this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerFishSkillListener(plugin, this), plugin);
        // Outros listeners (Alquimia, Culinária) podem ser mais complexos e envolver eventos customizados ou CraftItemEvent
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo Skill habilitado.");
        skillManager.loadSkillConfigurations(); // Carregar configurações de XP por nível, etc.
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Skill desabilitado.");
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    @Override
    public int getSkillLevel(Player player, SkillType skillType) {
        return skillManager.getPlayerSkillLevel(player, skillType);
    }

    @Override
    public void addSkillExperience(Player player, SkillType skillType, double amount) {
        skillManager.addExperience(player, skillType, amount);
    }
}