package me.ray.aethelgardRPG.modules.spell.spells;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.spell.animation.Animation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public abstract class BaseSpell implements Spell {

    protected final AethelgardRPG plugin;
    private final String id;
    private final String nameKey;
    private final String descriptionKey;
    private final int cooldownMillis;
    private final double manaCost;
    private final RPGClass requiredRPGClass;
    private final int requiredLevel; // NOVO
    private final double maxRange;
    protected Animation animation;

    protected BaseSpell(AethelgardRPG plugin, String id, String nameKey, String descriptionKey,
                        int cooldownSeconds, double manaCost, RPGClass requiredRPGClass,
                        int requiredLevel, double maxRange) { // NOVO PARÂMETRO
        this.plugin = plugin;
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.cooldownMillis = cooldownSeconds * 1000;
        this.manaCost = manaCost;
        this.requiredRPGClass = requiredRPGClass;
        this.requiredLevel = requiredLevel; // NOVO
        this.maxRange = maxRange;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getNameKey() { return nameKey; }

    @Override
    public String getDisplayName(Player player) {
        return plugin.getMessage(player, nameKey);
    }

    @Override
    public String getDescriptionKey() { return descriptionKey; }

    @Override
    public String getDisplayDescription(Player player) {
        return plugin.getMessage(player, descriptionKey);
    }

    @Override
    public int getCooldownMillis() { return cooldownMillis; }

    @Override
    public double getManaCost() { return manaCost; }

    @Override
    public RPGClass getRequiredRPGClass() { return requiredRPGClass; }

    @Override
    public int getRequiredLevel() { return requiredLevel; } // NOVO

    @Override
    public double getMaxRange() { return maxRange; }

    @Override
    public boolean isWeaponValid(ItemStack itemInHand) {
        RPGClass spellRequiredClass = getRequiredRPGClass();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            // MELHORIA: Usa a chave centralizada de PDCKeys
            if (pdc.has(PDCKeys.REQUIRED_CLASS_KEY, PersistentDataType.STRING)) {
                String itemClassString = pdc.get(PDCKeys.REQUIRED_CLASS_KEY, PersistentDataType.STRING);
                if (itemClassString != null) {
                    RPGClass itemRPGClass = RPGClass.fromString(itemClassString);
                    return itemRPGClass == spellRequiredClass;
                }
            }
        }
        return false;
    }

    @Override
    public Animation getAnimation() { return animation; }
}