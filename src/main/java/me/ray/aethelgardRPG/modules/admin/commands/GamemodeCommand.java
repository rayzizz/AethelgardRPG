package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GamemodeCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public GamemodeCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String noPermissionMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "general.no-permission") : plugin.getMessage("general.no-permission");
        String usageMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.gamemode.usage") : plugin.getMessage("admin.chat.gamemode.usage");
        String playerOnlyOrSpecifyMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.gamemode.player-only-or-specify") : plugin.getMessage("admin.chat.gamemode.player-only-or-specify");

        if (!sender.hasPermission("aethelgardrpg.admin.chat.gamemode")) {
            sender.sendMessage(noPermissionMsg);
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(usageMsg);
            return true;
        }

        GameMode gameMode;
        try {
            // Suporta nome (survival, creative, adventure, spectator) ou número (0, 1, 2, 3)
            if (args[0].matches("\\d+")) {
                gameMode = GameMode.getByValue(Integer.parseInt(args[0]));
            } else {
                gameMode = GameMode.valueOf(args[0].toUpperCase());
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.gamemode.invalid-mode", args[0]));
            } else {
                sender.sendMessage(plugin.getMessage("admin.chat.gamemode.invalid-mode", args[0]));
            }
            return true;
        }

        if (gameMode == null) {
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.gamemode.invalid-mode", args[0]));
            } else {
                sender.sendMessage(plugin.getMessage("admin.chat.gamemode.invalid-mode", args[0]));
            }
            return true;
        }

        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("aethelgardrpg.admin.chat.gamemode.others")) {
                sender.sendMessage(noPermissionMsg);
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                if (sender instanceof Player) {
                    sender.sendMessage(plugin.getMessage((Player) sender, "general.player-not-found", args[1]));
                } else {
                    sender.sendMessage(plugin.getMessage("general.player-not-found", args[1]));
                }
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(playerOnlyOrSpecifyMsg);
            return true;
        }

        target.setGameMode(gameMode);
        if (sender instanceof Player) {
            sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.gamemode.success", target.getName(), gameMode.name().toLowerCase()));
        } else {
            sender.sendMessage(plugin.getMessage("admin.chat.gamemode.success", target.getName(), gameMode.name().toLowerCase()));
        }
        if (target != sender) {
            // Message to target should be in target's language
            target.sendMessage(plugin.getMessage(target, "admin.chat.gamemode.changed-by-other", gameMode.name().toLowerCase()));
        }
        return true;
    }
}