package net.nekoyuni.SimpleEnemyMod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.nekoyuni.SimpleEnemyMod.config.common.*;

public final class CommonConfig {

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        GeneralConfig.init(builder);
        FactionsConfig.init(builder);
        UnitAttributesConfig.init(builder);

        AiDifficultyConfig.init(builder);
        AiShootingConfig.init(builder);

        EventSpawnConfig.init(builder);
        DropsConfig.init(builder);
        EffectConfig.init(builder);

        SPEC = builder.build();
    }
}
