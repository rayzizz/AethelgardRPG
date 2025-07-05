package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class KickCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public KickCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String noPermissionMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "general.no-permission") : plugin.getMessage("general.no-permission");
        String usageMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.kick.usage") : plugin.getMessage("admin.chat.kick.usage");
        String defaultReasonMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.kick.default-reason") : plugin.getMessage("admin.chat.kick.default-reason");

        if (!sender.hasPermission("aethelgardrpg.admin.chat.kick")) {
            sender.sendMessage(noPermissionMsg);
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(usageMsg);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "general.player-not-found", args[0]));
            } else {
                sender.sendMessage(plugin.getMessage("general.player-not-found", args[0]));
            }
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : defaultReasonMsg;

        // CORREÇÃO: Usa o idioma do jogador alvo para a mensagem de kick.
        target.kickPlayer(plugin.getMessage(target, "admin.chat.kick.kick-message", reason));

        if (sender instanceof Player) {
            sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.kick.success", target.getName(), reason));
        } else {
            sender.sendMessage(plugin.getMessage("admin.chat.kick.success", target.getName(), reason));
        }
        return true;
    }
}