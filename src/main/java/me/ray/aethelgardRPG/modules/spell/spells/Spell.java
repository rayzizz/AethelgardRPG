package me.ray.aethelgardRPG.modules.spell.spells;

import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.spell.animation.Animation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Spell {

    String getId();
    String getNameKey();
    String getDisplayName(Player player);
    String getDescriptionKey();
    String getDisplayDescription(Player player);
    int getCooldownMillis();
    double getManaCost();
    RPGClass getRequiredRPGClass();
    int getRequiredLevel(); // NOVO: Adiciona o requisito de nível
    double getMaxRange();
    boolean isWeaponValid(ItemStack weapon);
    SpellCastResult execute(SpellCastContext context);
    Animation getAnimation();

}