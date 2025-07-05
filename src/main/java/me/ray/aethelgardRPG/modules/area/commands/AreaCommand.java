package me.ray.aethelgardRPG.modules.area.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.area.AreaModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AreaCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final AreaModule areaModule;

    public AreaCommand(AethelgardRPG plugin, AreaModule areaModule) {
        this.plugin = plugin;
        this.areaModule = areaModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("aethelgardrpg.admin.area")) {
            // CORREÇÃO: Verifica o tipo do sender para a mensagem de permissão
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "general.no-permission"));
            } else {
                sender.sendMessage(plugin.getMessage("general.no-permission"));
            }
            return true;
        }

        if (args.length == 0) {
            // CORREÇÃO: Verifica o tipo do sender para a mensagem de uso
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "area.admin.usage"));
            } else {
                sender.sendMessage(plugin.getMessage("area.admin.usage"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // CORREÇÃO: Verifica o tipo do sender para todas as mensagens de placeholder e erro
        if (sender instanceof Player) {
            Player player = (Player) sender;
            switch (subCommand) {
                case "define":
                    player.sendMessage(plugin.getMessage(player, "area.admin.define-placeholder"));
                    return true;
                case "delete":
                    player.sendMessage(plugin.getMessage(player, "area.admin.delete-placeholder"));
                    return true;
                case "list":
                    player.sendMessage(plugin.getMessage(player, "area.admin.list-placeholder"));
                    return true;
                default:
                    player.sendMessage(plugin.getMessage(player, "general.unknown-subcommand", subCommand));
                    return true;
            }
        } else {
            // Lógica para o console
            switch (subCommand) {
                case "define":
                    sender.sendMessage(plugin.getMessage("area.admin.define-placeholder"));
                    return true;
                case "delete":
                    sender.sendMessage(plugin.getMessage("area.admin.delete-placeholder"));
                    return true;
                case "list":
                    sender.sendMessage(plugin.getMessage("area.admin.list-placeholder"));
                    return true;
                default:
                    sender.sendMessage(plugin.getMessage("general.unknown-subcommand", subCommand));
                    return true;
            }
        }
    }
}