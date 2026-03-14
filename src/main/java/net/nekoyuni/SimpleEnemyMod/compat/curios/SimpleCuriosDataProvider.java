package net.nekoyuni.SimpleEnemyMod.compat.curios;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;
import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

public class SimpleCuriosDataProvider extends CuriosDataProvider {

    public SimpleCuriosDataProvider(String modId, PackOutput output, ExistingFileHelper fileHelper, CompletableFuture<HolderLookup.Provider> registries) {
        super(modId, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper fileHelper) {
        this.createEntities("units_entities")
                .addEntities(ModEntities.PMCUNIT.get())
                .addSlots("head", "back");
    }
}
