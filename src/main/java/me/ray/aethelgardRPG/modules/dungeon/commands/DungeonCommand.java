package me.ray.aethelgardRPG.modules.dungeon.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.dungeon.DungeonModule;
import me.ray.aethelgardRPG.modules.dungeon.DungeonTemplate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections; // Para grupo de um jogador só

public class DungeonCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final DungeonModule dungeonModule;

    public DungeonCommand(AethelgardRPG plugin, DungeonModule dungeonModule) {
        this.plugin = plugin;
        this.dungeonModule = dungeonModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            // CORREÇÃO: Adicionado 'player' para obter a mensagem no idioma correto
            player.sendMessage(plugin.getMessage(player, "dungeon.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "enter": // /dungeon enter <templateId>
                if (args.length < 2) {
                    // CORREÇÃO: Adicionado 'player'
                    player.sendMessage(plugin.getMessage(player, "dungeon.enter-usage"));
                    return true;
                }
                String templateId = args[1];
                DungeonTemplate template = dungeonModule.getDungeonManager().getDungeonTemplate(templateId);
                if (template == null) {
                    // CORREÇÃO: Adicionado 'player'
                    player.sendMessage(plugin.getMessage(player, "dungeon.template-not-found", templateId));
                    return true;
                }
                // TODO: Implementar sistema de grupo. Por agora, permite entrada solo.
                dungeonModule.getDungeonManager().startDungeon(template, Collections.singletonList(player));
                return true;
            // TODO: Subcomandos para criar grupo, convidar, sair, listar, etc.
            default:
                // CORREÇÃO: Adicionado 'player'
                player.sendMessage(plugin.getMessage(player, "general.unknown-subcommand", subCommand));
                return true;
        }
    }
}