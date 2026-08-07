package net.nekoyuni.SimpleEnemyMod.event.common;

import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.entity.client.NpcMuzzleFlashRenderer;
import net.nekoyuni.SimpleEnemyMod.entity.unit.AbstractUnit;


@Mod.EventBusSubscriber(modid = "simpleenemymod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TaczGunShootEventListener {

    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.CLIENT) return;
        if (!(event.getShooter() instanceof AbstractUnit)) return;

        NpcMuzzleFlashRenderer.onNpcShoot(event.getShooter().getId());
    }
}