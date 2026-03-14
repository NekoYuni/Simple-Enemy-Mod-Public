package net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils;

import net.minecraft.util.RandomSource;

public final class AiGunSpreadHelper {


    public static float CalculateSpread(float idealAngle, float distance, float baseSpread,
            float spreadIncreasePerBlock, RandomSource random) {

        float spread = baseSpread + Math.max(0, distance - 5.0f) * spreadIncreasePerBlock;
        spread = Math.min(spread, 2.2f);
        return idealAngle + (random.nextFloat() - 0.5f) * 2.0f * spread;
    }

}
