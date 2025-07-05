package me.ray.aethelgardRPG.modules.custommob.ai;

public enum AIType {
    /**
     * O mob não ataca e pode vaguear passivamente.
     */
    PASSIVE,
    /**
     * O mob fica parado, não se move por conta própria, mas pode atacar ou usar habilidades se provocado ou se alvos entrarem no alcance.
     */
    STATIONARY,
    /**
     * O mob persegue ativamente e ataca alvos corpo a corpo.
     */
    AGGRESSIVE_MELEE,
    /**
     * O mob tenta manter distância e atacar com habilidades de longo alcance.
     */
    AGGRESSIVE_RANGED, // <-- NOVO TIPO DE IA
    /**
     * O mob foca em usar habilidades de suporte ou invocar outros mobs.
     */
    SUPPORT_SUMMONER,
    /**
     * Nenhuma IA customizada será aplicada, usa a IA vanilla do baseEntityType.
     */
    VANILLA,
    /**
     * IA para mobs que são invocações de jogadores ou outros mobs, com comportamento específico.
     */
    SUMMONED_MINION,
    /**
     * IA específica para o esqueleto invocado pela magia SummonSkeletonSpell.
     */
    SKELETON_MINION_FROM_SPELL
}