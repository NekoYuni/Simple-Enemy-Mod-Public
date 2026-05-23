package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class AiDifficultyConfig {

    public static ForgeConfigSpec.EnumValue<AiDifficultyConfig.AIDifficulty> DIFFICULTY;
    public enum AIDifficulty {
        NORMAL, ADVANCED
    }

    public static void init (ForgeConfigSpec.Builder builder){
        builder.push("ai_difficulty");

        DIFFICULTY = builder
                .comment(
                        "AI Difficulty preset.",
                        "Does NOT affect soldiers accuracy.",
                        "Only affects tactical maneuver timers and movement speeds.",
                        "",
                        "NORMAL: More static soldiers, slower combat pacing.",
                        "ADVANCED: More dynamic and aggressive soldiers."
                )
                .defineEnum("difficulty", AiDifficultyConfig.AIDifficulty.NORMAL);

        builder.pop();

    }

}
