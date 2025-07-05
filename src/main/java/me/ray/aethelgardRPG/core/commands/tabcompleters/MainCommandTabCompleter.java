package me.ray.aethelgardRPG.core.commands.tabcompleters;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MainCommandTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS_ADMIN = List.of("reload");
    // Add other subcommands like "help", "version" if they exist

    public MainCommandTabCompleter() {
        // Constructor can be empty if no plugin instance is needed
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if (sender.hasPermission("aethelgardrpg.admin.reload")) {
                suggestions.addAll(SUBCOMMANDS_ADMIN);
            }
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), suggestions, new ArrayList<>());
        }
        return List.of();
    }
}