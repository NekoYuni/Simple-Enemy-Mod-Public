package net.nekoyuni.SimpleEnemyMod.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nekoyuni.SimpleEnemyMod.SimpleEnemyMod;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SimpleEnemyMod.MODID);

    // Us_Units Sounds
    public static final RegistryObject<SoundEvent> SOUND_US_UNIT_HURT = registerSoundEvents("sound_us_unit_hurt");
    public static final RegistryObject<SoundEvent> SOUND_US_UNIT_DEATH = registerSoundEvents("sound_us_unit_death");
    public static final RegistryObject<SoundEvent> SOUND_US_UNIT_ALERT = registerSoundEvents("sound_us_unit_alert");

    // Ru_Units Sounds
    public static final RegistryObject<SoundEvent> SOUND_RU_UNIT_HURT = registerSoundEvents("sound_ru_unit_hurt");
    public static final RegistryObject<SoundEvent> SOUND_RU_UNIT_DEATH = registerSoundEvents("sound_ru_unit_death");
    public static final RegistryObject<SoundEvent> SOUND_RU_UNIT_ALERT = registerSoundEvents("sound_ru_unit_alert");

    // Bullet Impact Sounds
    public static final RegistryObject<SoundEvent> SOUND_BULLET_IMPACT = registerSoundEvents("sound_bullet_impact");

    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SimpleEnemyMod.MODID, name)));

    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

}
