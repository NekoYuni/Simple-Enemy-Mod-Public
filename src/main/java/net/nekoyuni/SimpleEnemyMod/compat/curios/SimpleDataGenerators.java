package net.nekoyuni.SimpleEnemyMod.compat.curios;

import net.minecraftforge.data.event.GatherDataEvent;

public class SimpleDataGenerators {

    public static void onGatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeServer(),
                new SimpleCuriosDataProvider(
                        "simpleenemymod",
                        event.getGenerator().getPackOutput(),
                        event.getExistingFileHelper(),
                        event.getLookupProvider()
                )
        );
    }
}
