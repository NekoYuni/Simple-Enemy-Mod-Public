package net.nekoyuni.SimpleEnemyMod.entity.unit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.IRoleHolder;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.IAnimatedEntity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.LayeredAnimationManager;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.StrategyType;

import javax.annotation.Nullable;

public abstract class AbstractUnit extends Monster implements IRoleHolder, IAnimatedEntity {

    // ROLE
    protected UnitRole role = UnitRole.DEFAULT;

    // ANIMATIONS
    protected static final int HURT_ANIMATION_DURATION = 20;

    public static final EntityDataAccessor<Integer> DAMAGE_ANIMATION_TICKS =
            SynchedEntityData.defineId(AbstractUnit.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Boolean> BACK_DEATH =
            SynchedEntityData.defineId(AbstractUnit.class, EntityDataSerializers.BOOLEAN);

    protected static final EntityDataAccessor<Integer> DATA_VARIANT_ID =
            SynchedEntityData.defineId(AbstractUnit.class, EntityDataSerializers.INT);

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState hurtAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    // SKINS
    public int currentHurtVariant = -1;
    private LayeredAnimationManager animationManager;
    private boolean hasComputedDeathDirection = false;

    // SOUND
    private int lastAlertSoundTick = -200;
    private int lastHurtSoundTick = -200;

    // TACTICS
    private StrategyType strategy;
    private BlockPos coverBlock;
    private long lastMicroMoveTick = 0L;
    private SoldierState soldierState = SoldierState.IDLE;

    @Nullable
    private Vec3 lastKnownTargetPos = null;
    private long lastSeenTargetTick = 0L;
    private long flankingStartTick = 0L;
    private float flankingAngle = 0.0f;
    private boolean isFlankingActive = false;
    private int flankingDirection = 1;

    @Nullable
    private BlockPos coverSearchOrigin = null;

    // LOCK CONTROL
    private boolean movementLockedByManager = false;
    private long movementLockExpiry = 0L;

    // DEBUG
    private final boolean isDebug = false;


    protected AbstractUnit(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);

        if (this.getNavigation() instanceof GroundPathNavigation groundNav) {
            groundNav.setCanOpenDoors(true);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT_ID, 0);
        this.entityData.define(BACK_DEATH, false);
        this.entityData.define(DAMAGE_ANIMATION_TICKS, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {

        if(!this.level().isClientSide) {
            double health = CommonConfig.UNIT_HEALTH.get();
            double speed = CommonConfig.UNIT_SPEED.get();
            double range = CommonConfig.UNIT_DETECTION_RANGE.get();

            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            this.setHealth((float) health);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(range);


        }

        this.setupRoleGoals();
        this.setPersistenceRequired();

        if (dataTag == null) {
            this.equipRandomGun();
        }

        return super.finalizeSpawn(world, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("UnitRole", this.getRole().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPersistenceRequired();

        if (tag.contains("UnitRole")) {
            this.setRole(UnitRole.valueOf(tag.getString("UnitRole")));
        }

        if (this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            this.equipRandomGun();
        }

        this.setupRoleGoals();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {

            int currentTicks = this.entityData.get(DAMAGE_ANIMATION_TICKS);
            if (currentTicks > 0) {
                this.entityData.set(DAMAGE_ANIMATION_TICKS, currentTicks - 1);
            }

            // DEBUG
            if (isDebug) {
                this.setCustomName(Component.literal(
                        "State: " + this.soldierState.name() +
                                " | HP: " + (int) this.getHealth() +
                                " | Flank: " + this.isFlankingActive
                ));
                this.setCustomNameVisible(true);
                this.setGlowingTag(false);
            }

        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getDirectEntity();
        if (attacker == null) attacker = source.getEntity();

        if (attacker != null && attacker.getClass() == this.getClass()) return false;

        boolean damageOccurred = super.hurt(source, amount);

        if (damageOccurred) {

            if (!this.level().isClientSide()) {
                this.entityData.set(DAMAGE_ANIMATION_TICKS, HURT_ANIMATION_DURATION);
            }
        }
        return damageOccurred;
    }

    @Override
    public void die(DamageSource pDamageSource) {

        if (!this.hasComputedDeathDirection) {
            this.hasComputedDeathDirection = true;

            Entity attacker = pDamageSource.getEntity();
            if (attacker != null) {
                double dX = attacker.getX() - this.getX();
                double dZ = attacker.getZ() - this.getZ();

                float damageYaw = (float) (Mth.atan2(dZ, dX) * (180F / Math.PI)) - 90.0F;
                float entityYaw = this.getYRot();
                float angleDifference = Mth.wrapDegrees(damageYaw - entityYaw);

                boolean shouldPlayBackDeath = Math.abs(angleDifference) > 90.0F;

                if (!this.level().isClientSide()) {
                    this.entityData.set(BACK_DEATH, shouldPlayBackDeath);
                }
            } else {
                if (!this.level().isClientSide()) {
                    this.entityData.set(BACK_DEATH, false);
                }
            }
        }

        super.die(pDamageSource);

        /*
        if (this.level().isClientSide()) {
            this.deathAnimationState.start(this.tickCount);
        }
         */
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

        if (BACK_DEATH.equals(key) && this.level().isClientSide() && this.isDeadOrDying()) {
            this.deathAnimationState.start(this.tickCount);
        }
    }

    public void lockMovementForStrategy(StrategyType strategy) {
        if (strategy == null) strategy = StrategyType.MID_RANGE;

        switch (strategy) {
            case CLOSE_RANGE -> lockMovementForManager(40);
            case MID_RANGE -> lockMovementForManager(80);
            case LONG_RANGE -> lockMovementForManager(120);
        }
    }

    // MISC GETTERS AND SETTERS
    @Override
    public boolean isDeadOrDying() {
        return super.isDeadOrDying() || this.deathAnimationState.isStarted();
    }

    @Override
    public UnitRole getRole() {
        return this.role;
    }

    public void setRole(UnitRole newRole) {
        this.role = newRole;
    }

    public void setAnimationManager(LayeredAnimationManager manager) {
        this.animationManager = manager;
    }

    public LayeredAnimationManager getAnimationManager() {
        return this.animationManager;
    }

    @Override
    public void onAnimationVariantSelected(String layerName, int variantIndex) {
        if ("hurt".equals(layerName)) {
            this.currentHurtVariant = variantIndex;
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity newTarget) {
        LivingEntity oldTarget = this.getTarget();
        super.setTarget(newTarget);

        if (newTarget != null && !newTarget.equals(oldTarget)) {

            if (this.tickCount >= this.lastAlertSoundTick + 200) {
                this.playSound(this.getCustomAlertSound(), 1.5F, this.getVoicePitch());
                this.lastAlertSoundTick = this.tickCount;
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        final int COOLDOWN_TICKS = 20;

        if (this.tickCount >= this.lastHurtSoundTick + COOLDOWN_TICKS) {
            this.lastHurtSoundTick = this.tickCount;
            return this.getCustomHurtSound();
        }
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getCustomDeathSound();
    }

    @Override
    public float getSoundVolume() {
        return 1.0F;
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }


    // TACTICS GETTERS AND SETTERS
    public StrategyType getStrategy() {
        return this.strategy;
    }

    public void setStrategy(StrategyType strategy) {
        if (strategy != null) {
            this.strategy = strategy;
        }
    }

    public BlockPos getCoverBlock() {
        return coverBlock;
    }

    public void setCoverBlock(BlockPos pos) {
        this.coverBlock = pos;
    }

    public void setLastMicroMoveTick(long tick) {
        this.lastMicroMoveTick = tick;
    }

    public long getLastMicroMoveTick() {
        return this.lastMicroMoveTick;
    }

    public SoldierState getSoldierState() {
        return this.soldierState;
    }

    public void setSoldierState(SoldierState state) {
        this.soldierState = state;
    }

    public void setCoverSearchOrigin(@Nullable BlockPos pos) {
        this.coverSearchOrigin = pos;
    }

    // MID_RANGE GETTERS AND SETTERS
    @Nullable
    public BlockPos getCoverSearchOrigin() {
        return this.coverSearchOrigin;
    }

    @Nullable
    public Vec3 getLastKnownTargetPos() {
        return this.lastKnownTargetPos;
    }

    public void setLastKnownTargetPos(@Nullable Vec3 pos) {
        this.lastKnownTargetPos = pos;
    }

    public long getLastSeenTargetTick() {
        return this.lastSeenTargetTick;
    }

    public void setLastSeenTargetTick(long tick) {
        this.lastSeenTargetTick = tick;
    }

    public long getFlankingStartTick() {
        return this.flankingStartTick;
    }

    public void setFlankingStartTick(long tick) {
        this.flankingStartTick = tick;
    }

    public float getFlankingAngle() {
        return this.flankingAngle;
    }

    public void setFlankingAngle(float angle) {
        this.flankingAngle = angle;
    }

    public boolean isFlankingActive() {
        return this.isFlankingActive;
    }

    public void setFlankingActive(boolean active) {
        this.isFlankingActive = active;
    }

    public int getFlankingDirection() {
        return this.flankingDirection;
    }

    public void setFlankingDirection(int direction) {
        this.flankingDirection = direction;
    }

    // LOCK SYSTEM
    public void lockMovementForManager(int ticks) {
        this.movementLockedByManager = true;
        this.movementLockExpiry = this.level().getGameTime() + ticks;
    }

    public boolean isMovementLockedByManager() {
        long now = this.level().getGameTime();

        if (now >= movementLockExpiry) {
            movementLockedByManager = false;
        }

        return movementLockedByManager;
    }

    public void releaseMovementLock() {
        this.movementLockedByManager = false;
    }


    public abstract void equipRandomGun();
    public abstract void setupRoleGoals();
    protected abstract SoundEvent getCustomHurtSound();
    protected abstract SoundEvent getCustomDeathSound();
    protected abstract SoundEvent getCustomAlertSound();

}
