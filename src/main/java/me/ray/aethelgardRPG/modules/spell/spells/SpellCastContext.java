package me.ray.aethelgardRPG.modules.spell.spells;

import me.ray.aethelgardRPG.modules.character.CharacterData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SpellCastContext {
    private final Player caster;
    private final CharacterData casterData;
    private final Optional<LivingEntity> targetEntity; // Alvo entidade (se houver)
    private final Optional<Location> targetLocation;   // Alvo localização (se houver)

    public SpellCastContext(Player caster, CharacterData casterData, Optional<LivingEntity> targetEntity) {
        this.caster = caster;
        this.casterData = casterData;
        this.targetEntity = targetEntity;
        this.targetLocation = targetEntity.map(LivingEntity::getLocation);
    }
    public Player getCaster() {
        return caster;
    }
    

}
