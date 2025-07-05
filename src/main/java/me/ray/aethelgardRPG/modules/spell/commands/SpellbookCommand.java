package me.ray.aethelgardRPG.modules.spell.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.spell.SpellModule;
import me.ray.aethelgardRPG.modules.spell.guis.SpellbookGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpellbookCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final SpellModule spellModule;

    public SpellbookCommand(AethelgardRPG plugin, SpellModule spellModule) {
        this.plugin = plugin;
        this.spellModule = spellModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("aethelgardrpg.player.spellbook")) {
            player.sendMessage(plugin.getMessage(player, "general.no-permission"));
            return true;
        }

        // Abre a GUI principal no modo NORMAL e sem nenhuma magia pré-selecionada.
        // Usa o método 'open' simplificado para a abertura inicial.
        new SpellbookGUI(plugin, spellModule).open(player);

        return true;
    }
}