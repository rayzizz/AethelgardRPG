package me.ray.aethelgardRPG.modules.admin.commands.tabcompleters;

import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import org.bukkit.Bukkit;
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

public class SetClassTabCompleter implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[0], playerNames, new ArrayList<>());
        } else if (args.length == 2) {
            List<String> classNames = Arrays.stream(RPGClass.values())
                    .filter(rpgClass -> rpgClass != RPGClass.NONE) // Não sugerir a classe NONE
                    .map(Enum::name)
                    .collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1].toUpperCase(), classNames, new ArrayList<>());
        }
        return List.of();
    }
}
