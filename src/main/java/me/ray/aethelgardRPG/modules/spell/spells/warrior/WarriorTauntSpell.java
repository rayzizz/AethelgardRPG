package me.ray.aethelgardRPG.modules.spell.spells.warrior;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import me.ray.aethelgardRPG.modules.spell.spells.BaseSpell;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastResult;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class WarriorTauntSpell extends BaseSpell {

    private static final double TAUNT_RADIUS = 15.0;
    private static final int TAUNT_DURATION_SECONDS = 8;

    private final CustomMobManager customMobManager;

    public WarriorTauntSpell(AethelgardRPG plugin) {
        super(plugin,
                "warrior_taunt",
                "spell.warrior.taunt.name",
                "spell.warrior.taunt.description",
                20, // Cooldown em segundos
                40, // Custo de mana
                RPGClass.GUERREIRO,
                5,  // Nível Requerido
                0   // Alcance (afeta área ao redor do caster)
        );
        // Acessa o CustomMobModule para obter o manager
        CustomMobModule customMobModule = plugin.getModuleManager().getModule(CustomMobModule.class);
        this.customMobManager = (customMobModule != null) ? customMobModule.getCustomMobManager() : null;
    }

    // --- CORREÇÃO APLICADA AQUI ---
    /**
     * Sobrescreve o método padrão para fornecer os valores para os placeholders.
     * @param player O jogador para contexto de idioma.
     * @return A descrição formatada com os valores corretos.
     */
    @Override
    public String getDisplayDescription(Player player) {
        // Passa os valores TAUNT_RADIUS e TAUNT_DURATION_SECONDS para os placeholders {0} e {1}
        return plugin.getMessage(player, getDescriptionKey(), String.valueOf(TAUNT_RADIUS), String.valueOf(TAUNT_DURATION_SECONDS));
    }
    // --- FIM DA CORREÇÃO ---

    @Override
    public SpellCastResult execute(SpellCastContext context) {
        Player caster = context.getCaster();

        if (customMobManager == null) {
            caster.sendMessage(plugin.getMessage(caster, "spell.error.custom_mob_module_missing"));
            return SpellCastResult.FAIL_OTHER;
        }

        // Efeitos visuais e sonoros no caster
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_VINDICATOR_HURT, 1.2F, 0.5F);
        caster.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, caster.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.1);

        List<Entity> nearbyEntities = caster.getNearbyEntities(TAUNT_RADIUS, TAUNT_RADIUS, TAUNT_RADIUS);
        int targetsTaunted = 0;

        for (Entity entity : nearbyEntities) {
            // Verifica se é um LivingEntity, se é um mob customizado e não é um lacaio de jogador
            if (entity instanceof LivingEntity && customMobManager.isCustomMob(entity) && !customMobManager.getMobDataFromEntity(entity).map(d -> d.isPlayerSummonedMinion()).orElse(false)) { // Corrigido: aceita Entity
                LivingEntity mob = (LivingEntity) entity;
                ActiveMobInfo mobInfo = customMobManager.getActiveMobInfo(mob.getUniqueId());

                if (mobInfo != null) {
                    // Aplica o efeito de taunt
                    mobInfo.setTauntedBy(caster.getUniqueId());
                    mobInfo.setTauntEndTime(System.currentTimeMillis() + (long) TAUNT_DURATION_SECONDS * 1000);
                    mobInfo.setInCombat(true);
                    mobInfo.setLastCombatTimeMillis(System.currentTimeMillis());

                    // Força o alvo imediatamente
                    if (mob instanceof Creature) {
                        ((Creature) mob).setTarget(caster);
                    }

                    // Efeito visual no mob provocado
                    mob.getWorld().spawnParticle(Particle.CRIT, mob.getEyeLocation(), 15, 0.3, 0.3, 0.3, 0.1);
                    targetsTaunted++;
                }
            }
        }

        if (targetsTaunted > 0) {
            caster.sendMessage(plugin.getMessage(caster, "spell.warrior.taunt.success", targetsTaunted));
        } else {
            caster.sendMessage(plugin.getMessage(caster, "spell.warrior.taunt.no_targets"));
        }

        return SpellCastResult.SUCCESS;
    }
}