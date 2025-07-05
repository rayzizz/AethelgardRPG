package me.ray.aethelgardRPG.modules.spell;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.spell.api.SpellAPI;
import me.ray.aethelgardRPG.modules.spell.commands.SpellAdminCommand;
import me.ray.aethelgardRPG.modules.spell.commands.SpellbookCommand;
import me.ray.aethelgardRPG.modules.spell.combo.ComboDetector;
import me.ray.aethelgardRPG.modules.spell.combo.ComboListener;
import me.ray.aethelgardRPG.modules.spell.combo.PlayerSneakListener;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import me.ray.aethelgardRPG.modules.spell.animation.AnimationManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class SpellModule implements RPGModule, SpellAPI {

    private final AethelgardRPG plugin;
    private SpellManager spellManager;
    private SpellRegistry spellRegistry;
    private ComboDetector comboDetector;
    private AnimationManager animationManager;
    private PlayerSpellManager playerSpellManager;

    public SpellModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Spell";
    }

    @Override
    public void onLoad() {
        this.spellRegistry = new SpellRegistry(plugin);
        this.animationManager = new AnimationManager(plugin);
        this.spellManager = new SpellManager(plugin, this.spellRegistry, this.animationManager);
        this.playerSpellManager = new PlayerSpellManager(plugin, this.spellRegistry);
        this.comboDetector = new ComboDetector(plugin, this.spellManager, this.playerSpellManager);

        plugin.getServer().getPluginManager().registerEvents(new ComboListener(plugin, this.comboDetector), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerSneakListener(this.comboDetector), plugin);

        // Registro dos comandos do módulo
        plugin.registerCommand("spellbook", new SpellbookCommand(plugin, this));

        // --- CORREÇÃO APLICADA AQUI ---
        // Use o mesmo método de registro para manter a consistência.
        plugin.registerCommand("aethelspell", new SpellAdminCommand(plugin, this));
    }

    @Override
    public void onEnable() {
        spellRegistry.initializeSpells();
        plugin.getLogger().info("Módulo Spell habilitado com " + spellRegistry.getSpellCount() + " spells registradas.");
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Spell desabilitado.");
        if (comboDetector != null) {
            comboDetector.clearAllPlayerInputs();
        }
    }

    @Override
    public boolean castSpellById(Player player, String spellId) {
        Optional<Spell> spellOpt = spellRegistry.getSpellById(spellId);
        if (spellOpt.isPresent()) {
            spellManager.castSpell(player, spellOpt.get());
            return true;
        }
        player.sendMessage(plugin.getMessage(player, "spell.cast.fail.spell_not_found", spellId));
        return false;
    }

    public SpellManager getSpellManager() {
        return spellManager;
    }

    public SpellRegistry getSpellRegistry() {
        return spellRegistry;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public PlayerSpellManager getPlayerSpellManager() {
        return playerSpellManager;
    }
}