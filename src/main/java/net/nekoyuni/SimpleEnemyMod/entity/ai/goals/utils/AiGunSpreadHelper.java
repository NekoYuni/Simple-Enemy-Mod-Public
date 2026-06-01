package net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils;

import net.minecraft.util.RandomSource;
import static net.nekoyuni.SimpleEnemyMod.config.common.AiShootingConfig.MAX_SPREAD;

public final class AiGunSpreadHelper {


    public static float CalculateSpread(float idealAngle, float distance, float baseSpread,
            float spreadIncreasePerBlock, RandomSource random) {

        float spread = baseSpread + Math.max(0, distance - 5.0f) * spreadIncreasePerBlock;
        spread = Math.min(spread, MAX_SPREAD.get().floatValue()); // 2.2
        return idealAngle + (random.nextFloat() - 0.5f) * 2.0f * spread;
    }

}
