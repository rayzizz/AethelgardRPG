package me.ray.aethelgardRPG.modules.character.display;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.Optional;

public class PlayerStatusDisplayManager {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private BukkitTask displayTask;

    // Configurações
    private String format;
    private boolean enabled;
    private long updateIntervalTicks;

    private String healthPrefix, healthSuffix, healthTextColor;
    private boolean healthUseVisualBar;
    private int healthBarLength;
    private String healthBarCharFilled, healthBarCharEmpty;
    private String healthBarColorFilled, healthBarColorEmpty;

    private String manaPrefix, manaSuffix, manaTextColor;
    private boolean manaUseVisualBar;
    private int manaBarLength;
    private String manaBarCharFilled, manaBarCharEmpty;
    private String manaBarColorFilled, manaBarColorEmpty;

    private String separatorDisplay;

    // Novas configurações para defesa
    private String defensePrefix, defenseSuffix;
    private String defensePhysicalPrefix, defensePhysicalTextColor;
    private String defenseMagicalPrefix, defenseMagicalTextColor;
    private String defenseSeparator;


    public PlayerStatusDisplayManager(AethelgardRPG plugin, CharacterModule characterModule) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        loadConfigValues();
    }

    private void loadConfigValues() {
        FileConfiguration charSettings = plugin.getConfigManager().getCharacterSettingsConfig();
        this.enabled = charSettings.getBoolean("actionbar-display.enabled", true);
        this.updateIntervalTicks = charSettings.getLong("actionbar-display.update-interval-ticks", 20L); // 1 segundo
        // Formato padrão sem o level/xp na action bar
        this.format = charSettings.getString("actionbar-display.format", "{health_display} {separator_display} {mana_display} {separator_display} {defense_display}");

        this.healthPrefix = charSettings.getString("actionbar-display.health.prefix", "&c❤ ");
        this.healthSuffix = charSettings.getString("actionbar-display.health.suffix", "");
        this.healthTextColor = charSettings.getString("actionbar-display.health.text-color", "&c");
        this.healthUseVisualBar = charSettings.getBoolean("actionbar-display.health.use-visual-bar", false);
        this.healthBarLength = charSettings.getInt("actionbar-display.health.visual-bar-length", 10);
        this.healthBarCharFilled = charSettings.getString("actionbar-display.health.visual-bar-char-filled", "❤");
        this.healthBarCharEmpty = charSettings.getString("actionbar-display.health.visual-bar-char-empty", "♡");
        this.healthBarColorFilled = charSettings.getString("actionbar-display.health.visual-bar-color-filled", "&c");
        this.healthBarColorEmpty = charSettings.getString("actionbar-display.health.visual-bar-color-empty", "&7");

        this.manaPrefix = charSettings.getString("actionbar-display.mana.prefix", "&9★ ");
        this.manaSuffix = charSettings.getString("actionbar-display.mana.suffix", "");
        this.manaTextColor = charSettings.getString("actionbar-display.mana.text-color", "&9");
        this.manaUseVisualBar = charSettings.getBoolean("actionbar-display.mana.use-visual-bar", false);
        this.manaBarLength = charSettings.getInt("actionbar-display.mana.visual-bar-length", 10);
        this.manaBarCharFilled = charSettings.getString("actionbar-display.mana.visual-bar-char-filled", "★");
        this.manaBarCharEmpty = charSettings.getString("actionbar-display.mana.visual-bar-char-empty", "☆");
        this.manaBarColorFilled = charSettings.getString("actionbar-display.mana.visual-bar-color-filled", "&9");
        this.manaBarColorEmpty = charSettings.getString("actionbar-display.mana.visual-bar-color-empty", "&7");

        this.separatorDisplay = charSettings.getString("actionbar-display.separator.display", "&7|");

        this.defensePrefix = charSettings.getString("actionbar-display.defense.prefix", "");
        this.defenseSuffix = charSettings.getString("actionbar-display.defense.suffix", "");
        this.defensePhysicalPrefix = charSettings.getString("actionbar-display.defense.physical-prefix", "&7🛡 ");
        this.defensePhysicalTextColor = charSettings.getString("actionbar-display.defense.physical-text-color", "&f");
        this.defenseMagicalPrefix = charSettings.getString("actionbar-display.defense.magical-prefix", "&9🔮 ");
        this.defenseMagicalTextColor = charSettings.getString("actionbar-display.defense.magical-text-color", "&f");
        this.defenseSeparator = charSettings.getString("actionbar-display.defense.separator", "&8/");
    }

    public void startDisplayTask() {
        if (!enabled) {
            plugin.getLogger().info(plugin.getMessage("character.chat.module.actionbar-disabled"));
            return;
        }

        if (displayTask != null && !displayTask.isCancelled()) {
            displayTask.cancel();
        }

        displayTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
                    if (playerDataOpt.isPresent()) {
                        CharacterData characterData = playerDataOpt.get();
                        PlayerAttributes attributes = characterData.getAttributes();

                        if (attributes != null) {
                            String healthDisplay = buildStatusDisplay(attributes.getCurrentHealth(), attributes.getMaxHealth(),
                                    healthUseVisualBar, healthBarLength, healthBarCharFilled, healthBarCharEmpty,
                                    healthBarColorFilled, healthBarColorEmpty, healthTextColor, healthPrefix, healthSuffix);

                            String manaDisplay = buildStatusDisplay(attributes.getCurrentMana(), attributes.getMaxMana(),
                                    manaUseVisualBar, manaBarLength, manaBarCharFilled, manaBarCharEmpty,
                                    manaBarColorFilled, manaBarColorEmpty, manaTextColor, manaPrefix, manaSuffix);

                            // --- ATUALIZAÇÃO: Barra de XP para representar a stamina e Nível do Jogador ---
                            // Calcula a porcentagem de stamina atual em relação à stamina máxima
                            double staminaPercentage = attributes.getMaxStamina() > 0 ? attributes.getCurrentStamina() / attributes.getMaxStamina() : 0.0;
                            // Define o progresso da barra de XP (0.0f a 1.0f) para a stamina
                            player.setExp(Math.max(0f, Math.min((float) staminaPercentage, 1.0f)));
                            // Define o nível de XP para o nível real do jogador
                            player.setLevel(characterData.getLevel());
                            // --- FIM DA ATUALIZAÇÃO DA BARRA DE XP ---

                            // *** CORRECTION APPLIED HERE ***
                            // Cast the double values from get...Defense() to int before passing them.
                            String defenseCombinedDisplay = buildDefenseDisplay((int) attributes.getPhysicalDefense(), (int) attributes.getMagicalDefense());

                            String finalMessage = format
                                    .replace("{health_display}", healthDisplay)
                                    .replace("{mana_display}", manaDisplay)
                                    .replace("{separator_display}", separatorDisplay)
                                    .replace("{defense_display}", defenseCombinedDisplay);

                            sendActionBar(player, finalMessage);

                        } else {
                            clearActionBar(player);
                            // Reseta XP e Nível se não houver atributos (pode acontecer durante o logout)
                            player.setLevel(0);
                            player.setExp(0f);
                            // Garante que a barra de fome também seja resetada para o padrão do Minecraft
                            player.setFoodLevel(20);
                            player.setSaturation(5f); // Saturação padrão
                        }
                    } else {
                        clearActionBar(player);
                        player.setLevel(0);
                        player.setExp(0f);
                        // Garante que a barra de fome também seja resetada para o padrão do Minecraft
                        player.setFoodLevel(20);
                        player.setSaturation(5f); // Saturação padrão
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, updateIntervalTicks);
        plugin.getLogger().info(plugin.getMessage("character.chat.module.actionbar-started"));
    }

    public void stopDisplayTask() {
        if (displayTask != null && !displayTask.isCancelled()) {
            displayTask.cancel();
            plugin.getLogger().info(plugin.getMessage("character.chat.module.actionbar-stopped"));
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearActionBar(player);
            // Reseta XP e Nível ao parar a task
            player.setLevel(0);
            player.setExp(0f);
            // Garante que a barra de fome também seja resetada para o padrão do Minecraft
            player.setFoodLevel(20);
            player.setSaturation(5f); // Saturação padrão
        }
    }

    private String buildStatusDisplay(double current, double max, boolean useVisualBar, int barLength,
                                      String charFilled, String charEmpty, String colorFilled, String colorEmpty,
                                      String textColor, String prefix, String suffix) {
        StringBuilder display = new StringBuilder(prefix);
        if (useVisualBar) {
            int filledCount = 0;
            if (max > 0) {
                filledCount = (int) Math.round((current / max) * barLength);
            }
            if (current > 0 && filledCount == 0) filledCount = 1;
            if (current <= 0) filledCount = 0;

            for (int i = 0; i < barLength; i++) {
                if (i < filledCount) {
                    display.append(colorFilled).append(charFilled);
                } else {
                    display.append(colorEmpty).append(charEmpty);
                }
            }
        } else {
            display.append(textColor).append(String.format("%.0f/%.0f", Math.max(0, current), max));
        }
        display.append(suffix);
        return display.toString();
    }

    private String buildDefenseDisplay(int physicalDefense, int magicalDefense) {
        StringBuilder display = new StringBuilder(defensePrefix);
        display.append(defensePhysicalPrefix)
                .append(defensePhysicalTextColor)
                .append(physicalDefense);
        display.append(" ").append(defenseSeparator).append(" ");
        display.append(defenseMagicalPrefix)
                .append(defenseMagicalTextColor)
                .append(magicalDefense);
        display.append(defenseSuffix);
        return display.toString();
    }

    private void sendActionBar(Player player, String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredMessage));
    }

    private void clearActionBar(Player player) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
    }

    public void reloadConfig() {
        loadConfigValues();
        if (enabled) {
            startDisplayTask();
        } else {
            stopDisplayTask();
        }
    }

    public void clearPlayerFromCache(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            clearActionBar(player);
            player.setFoodLevel(20);
            player.setSaturation(5f); // Saturação padrão
        }
    }
}