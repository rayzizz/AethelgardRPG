package me.ray.aethelgardRPG.modules.admin.commands.tabcompleters;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GamemodeTabCompleter implements TabCompleter {
    private static final List<String> GAMEMODES = Arrays.stream(GameMode.values()).map(gm -> gm.name().toLowerCase()).collect(Collectors.toList());

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), GAMEMODES, new ArrayList<>());
        } else if (args.length == 2 && sender.hasPermission("aethelgardrpg.admin.gamemode.others")) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }
        return List.of();
    }
}