package me.ray.aethelgardRPG.modules.admin.commands.tabcompleters;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetAccessoryTabCompleter implements TabCompleter {

    private final AethelgardRPG plugin;

    public GetAccessoryTabCompleter(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
            if (itemModule != null) {
                ItemAPI itemAPI = itemModule;
                List<String> accessoryIds = itemAPI.getAccessoryIds();
                return StringUtil.copyPartialMatches(args[0], accessoryIds, new ArrayList<>());
            }
        }
        return Collections.emptyList();
    }
}
