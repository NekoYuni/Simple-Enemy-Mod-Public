package net.nekoyuni.SimpleEnemyMod.entity.unit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.equipment.RuWeaponEquipper;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.registry.ModSounds;

public class RUunitEntity extends AbstractUnit {

    private static final int VARIANT_COUNT = 5;
    private int variant;


    public RUunitEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.variant = this.random.nextInt(VARIANT_COUNT);
    }

    public int getVariant() {
        return this.variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
        this.entityData.set(DATA_VARIANT_ID, variant);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Variant")) {
            this.setVariant(tag.getInt("Variant"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }



    @Override
    public void equipRandomGun() {
        RuWeaponEquipper.equipRandomGun(this, this.getRandom());
    }

    @Override
    public void setupRoleGoals() {

        this.goalSelector.removeAllGoals(pGoal -> true);
        this.targetSelector.removeAllGoals(pGoal -> true);

        if (this.role == null) {
            this.setRole(UnitRole.DEFAULT);
        }

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers().setUnseenMemoryTicks(600));

        if (!CommonConfig.RU_UNITS_FRIENDLY.get()) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PmcUnitEntity.class, true).setUnseenMemoryTicks(800));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true).setUnseenMemoryTicks(800));
        }

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, USunitEntity.class, true).setUnseenMemoryTicks(800));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Skeleton.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true, (target) -> {

            if (target.getClass() == this.getClass()) {
                return false;
            }

            if (CommonConfig.RU_UNITS_FRIENDLY.get() && target instanceof PmcUnitEntity) {
                return false;
            }

            if (target instanceof AbstractUnit) {
                return true;
            }

            return target instanceof Enemy;
        }));


        switch(this.getRole()) {

            case DEFAULT:
                UnitRole.DEFAULT.getGoals().addGoals(this);
                break;
            case SQUAD_LEADER:
                UnitRole.SQUAD_LEADER.getGoals().addGoals(this);
                break;
            case SQUAD_UNIT:
                UnitRole.SQUAD_UNIT.getGoals().addGoals(this);
                break;
            default:
                UnitRole.DEFAULT.getGoals().addGoals(this);
                break;
        }
    }

    @Override
    protected SoundEvent getCustomHurtSound() {
        return ModSounds.SOUND_RU_UNIT_HURT.get();
    }

    @Override
    protected SoundEvent getCustomDeathSound() {
        return ModSounds.SOUND_RU_UNIT_DEATH.get();
    }

    @Override
    protected SoundEvent getCustomAlertSound() {
        return ModSounds.SOUND_RU_UNIT_ALERT.get();
    }

    @Override
    public float getSoundVolume() {
        return 1.5F;
    }
}
