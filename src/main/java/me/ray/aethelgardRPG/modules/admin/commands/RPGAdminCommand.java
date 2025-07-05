package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.admin.guis.RPGAdminMainGUI;
import me.ray.aethelgardRPG.modules.admin.guis.PlayerInspectGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RPGAdminCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public RPGAdminCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                new RPGAdminMainGUI(plugin).open((Player) sender);
                return true;
            } else {
                plugin.sendMessage(sender, "admin.rpgadmin.usage");
                return true;
            }
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "performance":
                if (!sender.hasPermission("aethelgardrpg.admin.rpgadmin.performance")) {
                    plugin.sendMessage(sender, "general.no-permission");
                    return true;
                }
                displayPerformance(sender);
                break;
            case "economy":
                if (!sender.hasPermission("aethelgardrpg.admin.rpgadmin.economy")) {
                    plugin.sendMessage(sender, "general.no-permission");
                    return true;
                }
                plugin.sendMessage(sender, "admin.rpgadmin.economy-placeholder");
                break;
            case "inspect":
                if (!sender.hasPermission("aethelgardrpg.admin.rpgadmin.inspect")) {
                    plugin.sendMessage(sender, "general.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.sendMessage(sender, "admin.rpgadmin.inspect-usage");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    plugin.sendMessage(sender, "general.player-not-found", args[1]);
                    return true;
                }
                if (sender instanceof Player) {
                    new PlayerInspectGUI(plugin, target).open((Player) sender);
                } else {
                    plugin.sendMessage(sender, "admin.rpgadmin.inspect-console-placeholder", target.getName());
                }
                break;
            default:
                plugin.sendMessage(sender, "general.unknown-subcommand", subCommand);
                break;
        }
        return true;
    }

    private void displayPerformance(CommandSender sender) {
        // Correção: Verifique se é Player ou console
        plugin.sendMessage(sender, "admin.rpgadmin.performance-header");

        Map<String, Long> timings = plugin.getPerformanceMonitor().getExecutionTimes();
        if (timings.isEmpty()) {
            plugin.sendMessage(sender, "admin.rpgadmin.performance-no-data");
            return;
        }

        timings.forEach((id, time) -> {
            double timeMs = time / 1_000_000.0; // Convert nanoseconds to milliseconds
            plugin.sendMessage(sender, "admin.rpgadmin.performance-entry", id, String.format("%.3f", timeMs));
        });
    }
}