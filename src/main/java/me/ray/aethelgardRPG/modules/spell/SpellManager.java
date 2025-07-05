package me.ray.aethelgardRPG.modules.spell;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.admin.AdminModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.spell.animation.AnimationManager;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SpellManager {

    private final AethelgardRPG plugin;
    private final SpellRegistry spellRegistry;
    private final AnimationManager animationManager;
    private final Map<UUID, Map<String, Long>> playerCooldowns;
    private final AdminModule adminModule;

    public SpellManager(AethelgardRPG plugin, SpellRegistry spellRegistry, AnimationManager animationManager) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        this.animationManager = animationManager;
        this.playerCooldowns = new HashMap<>();
        this.adminModule = plugin.getModuleManager().getModule(AdminModule.class);
    }

    public SpellCastResult castSpell(Player caster, Spell spell) {
        Optional<CharacterData> playerDataOpt = plugin.getCharacterAPI().getCharacterData(caster);
        if (playerDataOpt.isEmpty()) {
            caster.sendMessage(plugin.getMessage(caster, "spell.cast.fail.player_data_missing"));
            return SpellCastResult.CASTER_DATA_NOT_FOUND;
        }
        CharacterData characterData = playerDataOpt.get();
        ItemStack weaponInHand = caster.getInventory().getItemInMainHand();

        if (characterData.getSelectedClass() != spell.getRequiredRPGClass()) {
            caster.sendMessage(plugin.getMessage(caster, "spell.cast.fail.wrong_class", spell.getRequiredRPGClass().getDisplayName(caster)));
            return SpellCastResult.FAIL_CLASS_REQUIREMENT;
        }

        if (!spell.isWeaponValid(weaponInHand)) {
            caster.sendMessage(plugin.getMessage(caster, "spell.cast.fail.wrong_weapon"));
            return SpellCastResult.FAIL_WEAPON_REQUIREMENT;
        }

        final boolean isInfiniteStats = adminModule != null && adminModule.isInfiniteStatsEnabled(caster.getUniqueId());

        if (!isInfiniteStats) {
            long currentTime = System.currentTimeMillis();
            long cooldownEnd = playerCooldowns.getOrDefault(caster.getUniqueId(), Collections.emptyMap()).getOrDefault(spell.getId(), 0L);

            if (currentTime < cooldownEnd) {
                long timeLeft = (cooldownEnd - currentTime) / 1000;
                caster.sendMessage(plugin.getMessage(caster, "spell.cast.fail.cooldown", spell.getDisplayName(caster), timeLeft));
                return SpellCastResult.FAIL_COOLDOWN;
            }

            if (characterData.getAttributes().getCurrentMana() < spell.getManaCost()) {
                caster.sendMessage(plugin.getMessage(caster, "spell.cast.fail.no_mana", spell.getDisplayName(caster), spell.getManaCost()));
                return SpellCastResult.FAIL_MANA;
            }
        }

        SpellCastContext context = new SpellCastContext(caster, characterData,
                plugin.getTargetFinder() != null ? plugin.getTargetFinder().findTarget(caster, spell.getMaxRange()) : Optional.empty());

        SpellCastResult castResult = spell.execute(context);

        if (castResult == SpellCastResult.SUCCESS) {
            if (!isInfiniteStats) {
                characterData.getAttributes().setCurrentMana(characterData.getAttributes().getCurrentMana() - spell.getManaCost());
                playerCooldowns.computeIfAbsent(caster.getUniqueId(), k -> new HashMap<>()).put(spell.getId(), System.currentTimeMillis() + spell.getCooldownMillis());
            }

            if (spell.getAnimation() != null) {
                animationManager.playAnimation(spell.getAnimation(), context);
            }
            caster.sendMessage(plugin.getMessage(caster, "spell.cast.success", spell.getDisplayName(caster)));
        } else if (castResult != SpellCastResult.FAIL_NO_TARGET) { // A spell pode enviar sua própria mensagem de falha
            caster.sendMessage(plugin.getMessage(caster, "spell.cast.fail.generic", spell.getDisplayName(caster)));
        }

        return castResult;
    }
}