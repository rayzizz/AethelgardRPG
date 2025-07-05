package me.ray.aethelgardRPG.modules.spell.spells;

public enum SpellCastResult {
    SUCCESS,
    FAIL_COOLDOWN,
    FAIL_MANA,
    FAIL_CLASS_REQUIREMENT,
    FAIL_WEAPON_REQUIREMENT,
    FAIL_NO_TARGET, // MELHORIA: Removida a duplicata FAILED_NO_TARGET
    FAIL_INVALID_TARGET,
    FAIL_OTHER,
    CASTER_DATA_NOT_FOUND
}