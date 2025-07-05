package me.ray.aethelgardRPG.modules.admin.commands.tabcompleters;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SpawnTestMobTabCompleter implements TabCompleter {

    private final AethelgardRPG plugin;

    public SpawnTestMobTabCompleter(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            CustomMobModule cmm = plugin.getModuleManager().getModule(CustomMobModule.class);
            if (cmm != null && cmm.getCustomMobManager() != null) {
                Set<String> mobIds = cmm.getCustomMobManager().getRegisteredMobTypeIds();
                return StringUtil.copyPartialMatches(args[0], mobIds, new ArrayList<>());
            }
        }
        return Collections.emptyList(); // Use Collections.emptyList() for an immutable empty list
    }
}