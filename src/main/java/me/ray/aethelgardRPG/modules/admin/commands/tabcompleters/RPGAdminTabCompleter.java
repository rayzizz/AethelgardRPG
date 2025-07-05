package me.ray.aethelgardRPG.modules.admin.commands.tabcompleters;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RPGAdminTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("performance", "economy", "inspect");

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), SUBCOMMANDS, new ArrayList<>());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("inspect") && sender.hasPermission("aethelgardrpg.admin.rpgadmin.inspect")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
            }
        }
        return List.of();
    }
}