package net.nekoyuni.SimpleEnemyMod.data;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Optional;

public class UnitLoadout {

    public final ResourceLocation gunId;
    public final int ammoCount;
    public final String fireMode;

    public final Optional<ResourceLocation> scopeId;
    public final Optional<ResourceLocation> muzzleId;
    public final Optional<ResourceLocation> gripId;


    public UnitLoadout(ResourceLocation gunId, int ammoCount, String fireMode,
                       @Nullable ResourceLocation scopeId, @Nullable ResourceLocation muzzleId,
                       @Nullable ResourceLocation gripId) {

        this.gunId = gunId;
        this.ammoCount = ammoCount;
        this.fireMode = fireMode;

        this.scopeId = Optional.ofNullable(scopeId);
        this.muzzleId = Optional.ofNullable(muzzleId);
        this.gripId = Optional.ofNullable(gripId);
    }
}
