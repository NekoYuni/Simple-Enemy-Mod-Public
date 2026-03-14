package net.nekoyuni.SimpleEnemyMod.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class BulletImpactSoundInstance extends AbstractTickableSoundInstance {
    private static final int MAX_LIFETIME_TICKS = 100;
    private static final int MIN_PLAY_DURATION = 40;
    private int age = 0;
    private boolean hasStartedPlaying = false;

    public BulletImpactSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, Vec3 pos) {
        super(sound, source, RandomSource.create());
        this.volume = volume;
        this.pitch = pitch;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.looping = false;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        this.relative = false;
    }

    @Override
    public void tick() {
        this.age++;

        if (!this.hasStartedPlaying && this.age > 0) {
            this.hasStartedPlaying = true;
        }

        if (this.age > MAX_LIFETIME_TICKS ||
                (this.hasStartedPlaying && this.age > MIN_PLAY_DURATION)) {
            this.stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !this.isStopped();
    }

    public static BulletImpactSoundInstance create(SoundEvent sound, Vec3 pos, float volume, float pitch) {
        return new BulletImpactSoundInstance(sound, SoundSource.NEUTRAL, volume, pitch, pos);
    }
}
