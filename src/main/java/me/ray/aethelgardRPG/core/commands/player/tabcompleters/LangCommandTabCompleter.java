package me.ray.aethelgardRPG.core.commands.player.tabcompleters; // Ou similar

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class LangCommandTabCompleter implements TabCompleter {

    private final AethelgardRPG plugin;

    public LangCommandTabCompleter(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            Set<String> availableLangs = plugin.getLanguageManager().getLoadedLanguages();
            return StringUtil.copyPartialMatches(args[0], availableLangs, new ArrayList<>());
        }
        return List.of();
    }
}