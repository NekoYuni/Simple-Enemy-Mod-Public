package net.nekoyuni.SimpleEnemyMod.event.server;


import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.RangedGunAttackGoal;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.RUunitEntity;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.text2speech.Narrator.LOGGER;


@Mod.EventBusSubscriber(modid = SimpleEnemyMod.MODID)
public class FriendlyFireEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FriendlyFireEventHandler.class);
    private final static boolean isDebug = false;

    @SubscribeEvent
    public static void onEntityHurtByGunPre(EntityHurtByGunEvent.Pre event) {

        Entity victimEntity = event.getHurtEntity();
        LivingEntity attacker = event.getAttacker();

        debug("ModEvents.onEntityHurtByGunPre Fired! Victim: {}, Attacker: {}, Amount: {}",
                (victimEntity != null ? victimEntity.getName().getString() : "null"),
                (attacker != null ? attacker.getName().getString() : "null"),
                event.getBaseAmount()
        );


        if (victimEntity instanceof LivingEntity victim && attacker != null && victim != attacker) {

            debug("Checking friendly fire: VictimClass={}, AttackerClass={}",
                    victim.getClass().getSimpleName(), attacker.getClass().getSimpleName()
            );

            // RUunit vs RUunit
            if (victim instanceof RUunitEntity && attacker instanceof RUunitEntity) {
                LOGGER.info(" -> FRIENDLY FIRE DETECTED (RU vs RU) via TACZ Event! Cancelling event.");
                event.setCanceled(true);
                return;
            }

            // USunit vs USunit
            if (victim instanceof USunitEntity && attacker instanceof USunitEntity) {
                info(" -> FRIENDLY FIRE DETECTED (US vs US) via TACZ Event! Cancelling event.");
                event.setCanceled(true);
                return;
            }

            if (victim instanceof PmcUnitEntity && attacker instanceof PmcUnitEntity) {
                event.setCanceled(true);
                return;
            }

            if (victim instanceof Player && attacker instanceof PmcUnitEntity) {
                event.setCanceled(true);
            }


        } else {
            debug("Victim or Attacker invalid/same for friendly fire check.");
        }

    }

    private static void debug(String message, Object... args) {
        if (isDebug && LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    private static void info(String message, Object... args) {
        if (isDebug && LOGGER.isInfoEnabled()) {
            LOGGER.debug(message, args);
        }
    }

}
