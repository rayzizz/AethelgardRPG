package me.ray.aethelgardRPG.modules.dungeon.commands.tabcompleters;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.dungeon.DungeonModule;
import me.ray.aethelgardRPG.modules.dungeon.DungeonTemplate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonCommandTabCompleter implements TabCompleter {
    private final AethelgardRPG plugin;

    public DungeonCommandTabCompleter(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), List.of("enter"), new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("enter")) {
            DungeonModule dungeonModule = plugin.getModuleManager().getModule(DungeonModule.class);
            if (dungeonModule != null) {
                List<String> templateIds = new ArrayList<>(dungeonModule.getDungeonManager().getAllDungeonTemplateIds());
                return StringUtil.copyPartialMatches(args[1], templateIds, new ArrayList<>());
            }
        }
        return List.of();
    }
}