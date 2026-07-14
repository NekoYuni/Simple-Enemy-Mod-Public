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
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.config.common.UnitAttributesConfig;
import net.nekoyuni.SimpleEnemyMod.config.common.TacticalAiConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.decision.CombatDecisionContext;
import net.nekoyuni.SimpleEnemyMod.entity.ai.decision.TacticalCombatState;
import net.nekoyuni.SimpleEnemyMod.entity.ai.decision.TacticalSchmittThresholds;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.LadderRouteNavigator;
import net.nekoyuni.SimpleEnemyMod.entity.ai.navigation.NavigationSoftObstacleHelper;
import net.nekoyuni.SimpleEnemyMod.entity.ai.squad.SquadCombatMemory;
import net.nekoyuni.SimpleEnemyMod.entity.ai.squad.SquadContext;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.IRoleHolder;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.IAnimatedEntity;
import net.nekoyuni.SimpleEnemyMod.entity.client.animation.core.LayeredAnimationManager;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.SoldierState;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.UnitFactionTags;
import net.nekoyuni.SimpleEnemyMod.entity.unit.util.StrategyType;

import net.nekoyuni.SimpleEnemyMod.scenario.SemScenarioRuntimeManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

    // RENDER - Shoot Fix
    public static final EntityDataAccessor<Boolean> IS_IN_COMBAT =
            SynchedEntityData.defineId(AbstractUnit.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Float> COMBAT_YAW =
            SynchedEntityData.defineId(AbstractUnit.class, EntityDataSerializers.FLOAT);


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

    // PHASE 3: SUPPRESSION AND SOUND AWARENESS
    private static final long GUNFIRE_SOURCE_DEBOUNCE_TICKS = 40L;
    private float genericSuppressionLevel = 0.0F;
    private long lastSuppressionCoverRequestTick = -9999L;
    private final Map<UUID, Long> lastSuppressionBySource = new HashMap<>();
    private final Map<UUID, Long> lastAcceptedGunfireBySource = new HashMap<>();

    @Nullable
    private Vec3 lastHeardEnemyPosition = null;
    @Nullable
    private Entity lastHeardEnemySource = null;
    private int heardCombatNoiseTicks = 0;
    private double lastHeardEnemyStrength = 0.0D;
    private boolean lastHeardWasGunshot = false;
    private long lastCombatNoiseInvestigationLogTick = -40L;

    // PHASE 4: THREAT / CONFIDENCE DECISION LAYER
    private TacticalCombatState tacticalCombatState = TacticalCombatState.HOLD;
    private float threatPressure = 0.0F;
    private float combatConfidence = 0.50F;
    private double tacticalSpeedMultiplier = 1.0D;
    private long lastTacticalDecisionTick = -9999L;
    @Nullable
    private CombatDecisionContext lastCombatDecisionContext = null;
    private SquadContext squadContext = SquadContext.empty();

    // DEBUG
    private final boolean isDebug = false;


    protected AbstractUnit(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);

        if (this.getNavigation() instanceof GroundPathNavigation groundNav) {
            groundNav.setCanOpenDoors(true);
        }

        this.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_OTHER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.POWDER_SNOW, -1.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT_ID, 0);
        this.entityData.define(BACK_DEATH, false);
        this.entityData.define(DAMAGE_ANIMATION_TICKS, 0);
        // Shoot fix
        this.entityData.define(IS_IN_COMBAT, false);
        this.entityData.define(COMBAT_YAW, 0.0f);
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
            double health = UnitAttributesConfig.UNIT_HEALTH.get();
            double speed = UnitAttributesConfig.UNIT_SPEED.get();
            double range = UnitAttributesConfig.UNIT_DETECTION_RANGE.get();

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

            LadderRouteNavigator.tickUnit(this);
            NavigationSoftObstacleHelper.tickTraversalSupport(this);
            tickSuppressionDecay();
            tickCombatNoiseAwareness();

            LivingEntity target = getTarget();

            boolean inCombat = (target != null && target.isAlive())
                    || (this.tickCount - this.getLastHurtByMobTimestamp() < 100);
            this.entityData.set(IS_IN_COMBAT, inCombat);

            if (target != null && target.isAlive()) {
                double dx = target.getX() - getX();
                double dz = target.getZ() - getZ();
                float angle = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;

                this.entityData.set(COMBAT_YAW, angle);
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

        // Client Patch
        if (this.entityData.get(IS_IN_COMBAT) && !this.isDeadOrDying()) {
            float combatYaw = this.entityData.get(COMBAT_YAW);

            this.yBodyRot = combatYaw;
            this.yHeadRot = combatYaw;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getDirectEntity();
        if (attacker == null) attacker = source.getEntity();

        if (UnitFactionTags.isFriendlyFire(this, attacker)) return false;

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
            if (attacker != null && !this.level().isClientSide()) {
                double dX = attacker.getX() - this.getX();
                double dZ = attacker.getZ() - this.getZ();
                float damageYaw = (float)(Mth.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;

                float referenceYaw = this.isInCombat()
                        ? this.entityData.get(COMBAT_YAW)
                        : this.getYRot();

                float angleDifference = Mth.wrapDegrees(damageYaw - referenceYaw);
                boolean shouldPlayBackDeath = Math.abs(angleDifference) > 90.0F;

                this.entityData.set(BACK_DEATH, shouldPlayBackDeath);
            } else if (!this.level().isClientSide()) {
                this.entityData.set(BACK_DEATH, false);
            }
        }

        super.die(pDamageSource);
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

        if (this.level().isClientSide()) {
            return super.isDeadOrDying() || this.deathAnimationState.isStarted();
        }

        return super.isDeadOrDying();
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


    // RENDER - SHOOT FIX
    public boolean isInCombat() {
        return this.entityData.get(IS_IN_COMBAT);
    }

    public float getCombatYaw() {
        return this.entityData.get(COMBAT_YAW);
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


    // PHASE 3: SUPPRESSION AND SOUND AWARENESS
    private void tickSuppressionDecay() {
        if (this.genericSuppressionLevel <= 0.0F) {
            this.genericSuppressionLevel = 0.0F;
            return;
        }
        this.genericSuppressionLevel = Math.max(0.0F,
                this.genericSuppressionLevel - (float) TacticalAiConfig.suppressionDecayPerTick());
    }

    public boolean addGenericSuppression(Entity source, Vec3 impactPosition, float amount) {
        if (!TacticalAiConfig.enableTacticalSuppression() || amount <= 0.0F) {
            return false;
        }
        if (source == this || UnitFactionTags.isFriendlyFire(this, source) || UnitFactionTags.isSameTaggedFaction(this, source)) {
            return false;
        }

        long now = this.level().getGameTime();
        UUID sourceId = source != null ? source.getUUID() : null;
        if (sourceId != null) {
            Long lastTick = this.lastSuppressionBySource.get(sourceId);
            int debounceTicks = TacticalAiConfig.suppressionSourceDebounceTicks();
            if (debounceTicks > 0
                    && lastTick != null
                    && now - lastTick < debounceTicks
                    && this.genericSuppressionLevel >= TacticalAiConfig.suppressionStrongThreshold()) {
                return false;
            }
            this.lastSuppressionBySource.put(sourceId, now);
            if (this.lastSuppressionBySource.size() > 48) {
                this.lastSuppressionBySource.entrySet().removeIf(entry -> now - entry.getValue() > 120L);
            }
        }

        float scaledAmount = Math.min(amount, 0.35F);
        this.genericSuppressionLevel = Math.min(3.0F, this.genericSuppressionLevel + scaledAmount);
        SquadCombatMemory.recordSuppressionMemory(this, impactPosition, source, scaledAmount);

        if (source instanceof LivingEntity shooter
                && shooter.isAlive()
                && shooter.level() == this.level()
                && this.getTarget() == null
                && this.canAttack(shooter)
                && !UnitFactionTags.isSameTaggedFaction(this, shooter)) {
            this.setTarget(shooter);
        }

        if (this.genericSuppressionLevel >= TacticalAiConfig.suppressionStrongThreshold()
                && now - this.lastSuppressionCoverRequestTick >= 55L) {
            this.lastSuppressionCoverRequestTick = now;
            this.setCoverSearchOrigin(this.blockPosition());

            Vec3 threatPos = null;
            if (this.getTarget() != null && this.getTarget().isAlive()) {
                threatPos = this.getTarget().position();
            } else if (source instanceof LivingEntity livingSource && livingSource.isAlive()) {
                threatPos = livingSource.position();
            } else if (impactPosition != null) {
                threatPos = impactPosition;
            }
            this.setLastKnownTargetPos(threatPos);
            this.setSoldierState(SoldierState.SEEK_COVER);
            this.lockMovementForManager(TacticalAiConfig.suppressionCoverLockTicks());

            SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                    "SUPPRESSION_COVER_REQUEST",
                    this,
                    "source=" + (source != null ? source.getUUID() : "unknown")
                            + " level=" + String.format(Locale.ROOT, "%.2f", this.genericSuppressionLevel)
                            + " amount=" + String.format(Locale.ROOT, "%.2f", scaledAmount)
            );
        }
        return true;
    }

    public float getGenericSuppressionLevel() {
        return this.genericSuppressionLevel;
    }

    public float getSuppressionSpreadPenalty() {
        return this.genericSuppressionLevel * (float) TacticalAiConfig.suppressionAccuracySpreadPenalty();
    }

    public boolean shouldAcceptCombatNoiseFrom(Entity source, boolean gunshot) {
        if (!gunshot || source == null) {
            return true;
        }

        long now = this.level().getGameTime();
        UUID sourceId = source.getUUID();
        Long lastAcceptedTick = this.lastAcceptedGunfireBySource.get(sourceId);
        if (lastAcceptedTick != null && now - lastAcceptedTick < GUNFIRE_SOURCE_DEBOUNCE_TICKS) {
            return false;
        }

        this.lastAcceptedGunfireBySource.put(sourceId, now);
        pruneOldGunfireDebounceEntries(now);
        return true;
    }

    private void pruneOldGunfireDebounceEntries(long now) {
        if (this.lastAcceptedGunfireBySource.size() <= 32) {
            return;
        }

        Iterator<Map.Entry<UUID, Long>> iterator = this.lastAcceptedGunfireBySource.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now - entry.getValue() > GUNFIRE_SOURCE_DEBOUNCE_TICKS * 4L) {
                iterator.remove();
            }
        }
    }

    public void hearCombatNoise(Vec3 soundPosition, Entity source, boolean gunshot, double strength) {
        if (!TacticalAiConfig.enableTacticalHearing() || !gunshot) {
            return;
        }
        if (strength < TacticalAiConfig.soundMinStrength()) {
            return;
        }

        this.lastHeardEnemyPosition = soundPosition;
        this.lastHeardEnemySource = source;
        this.lastHeardEnemyStrength = strength;
        this.lastHeardWasGunshot = gunshot;
        this.heardCombatNoiseTicks = Math.max(this.heardCombatNoiseTicks, TacticalAiConfig.gunshotMemoryTicks());
        this.setLastKnownTargetPos(soundPosition);
        SquadCombatMemory.recordSoundMemory(this, soundPosition, source, strength);

        boolean alreadyEngaged = this.getTarget() != null && this.getTarget().isAlive();
        if (!alreadyEngaged) {
            this.releaseMovementLock();
            this.getNavigation().stop();
            this.setSoldierState(SoldierState.SEARCH);
        }

        SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                "ALERT_FROM_SOUND",
                this,
                "source=" + (source != null ? source.getUUID() : "unknown")
                        + " pos=" + formatVec(soundPosition)
                        + " strength=" + String.format(Locale.ROOT, "%.2f", strength)
                        + " mode=" + (alreadyEngaged ? "direct_combat_memory" : "investigate")
        );
    }

    private void tickCombatNoiseAwareness() {
        if (this.heardCombatNoiseTicks > 0) {
            this.heardCombatNoiseTicks--;
        }
        if (this.lastHeardEnemyPosition == null) {
            return;
        }
        if (this.heardCombatNoiseTicks <= 0) {
            clearHeardThreat();
            return;
        }

        Vec3 heardPosition = this.lastHeardEnemyPosition;
        LivingEntity currentTarget = this.getTarget();
        if (currentTarget == null || !currentTarget.isAlive()) {
            this.getLookControl().setLookAt(heardPosition.x, heardPosition.y + 1.0D, heardPosition.z);
        }

        long now = this.level().getGameTime();
        if (now - this.lastCombatNoiseInvestigationLogTick >= 40L) {
            this.lastCombatNoiseInvestigationLogTick = now;
            SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                    "INVESTIGATE_SOUND",
                    this,
                    "pos=" + formatVec(heardPosition)
                            + " strength=" + String.format(Locale.ROOT, "%.2f", this.lastHeardEnemyStrength)
                            + " memoryTicks=" + this.heardCombatNoiseTicks
                            + " mode=" + (currentTarget != null && currentTarget.isAlive() ? "direct_combat_memory" : "investigate")
            );
        }
    }

    public boolean hasHeardThreat() {
        return this.lastHeardEnemyPosition != null && this.heardCombatNoiseTicks > 0;
    }

    @Nullable
    public Vec3 getLastHeardEnemyPosition() {
        return this.lastHeardEnemyPosition;
    }

    @Nullable
    public Entity getLastHeardEnemySource() {
        return this.lastHeardEnemySource;
    }

    public double getLastHeardEnemyStrength() {
        return this.lastHeardEnemyStrength;
    }

    public boolean wasLastHeardThreatGunshot() {
        return this.lastHeardWasGunshot;
    }

    public int getHeardCombatNoiseTicks() {
        return this.heardCombatNoiseTicks;
    }

    public void clearHeardThreat() {
        this.lastHeardEnemyPosition = null;
        this.lastHeardEnemySource = null;
        this.lastHeardEnemyStrength = 0.0D;
        this.lastHeardWasGunshot = false;
        this.heardCombatNoiseTicks = 0;
        if (this.getSoldierState() == SoldierState.SEARCH) {
            this.setSoldierState(SoldierState.IDLE);
        }
    }

    private static String formatVec(Vec3 vec) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f", vec.x, vec.y, vec.z);
    }

    // PHASE 4: THREAT / CONFIDENCE DECISION LAYER
    public CombatDecisionContext updateCombatDecisionContext(@Nullable LivingEntity target, boolean hasTacticalLos, double distance) {
        if (!TacticalAiConfig.enableTacticalDecisionLayer()) {
            CombatDecisionContext context = CombatDecisionContext.empty(this);
            this.squadContext = SquadContext.empty();
            this.lastCombatDecisionContext = context;
            return context;
        }

        long now = this.level().getGameTime();
        int interval = Math.max(1, TacticalAiConfig.tacticalDecisionIntervalTicks());
        if (target != null && this.lastCombatDecisionContext != null && now - this.lastTacticalDecisionTick < interval) {
            return this.lastCombatDecisionContext;
        }

        CombatDecisionContext context = target == null || !target.isAlive()
                ? CombatDecisionContext.empty(this)
                : CombatDecisionContext.evaluate(this, target, hasTacticalLos, distance);
        this.squadContext = SquadCombatMemory.updateAndGetContext(this, target, hasTacticalLos, distance, context);
        context = context.withSquadContext(this.squadContext);
        this.lastTacticalDecisionTick = now;
        this.lastCombatDecisionContext = context;
        this.threatPressure = context.threatPressure();
        this.combatConfidence = context.combatConfidence();

        TacticalCombatState previous = this.tacticalCombatState;
        TacticalCombatState next = selectTacticalCombatState(previous, context);
        this.tacticalCombatState = next;
        this.tacticalSpeedMultiplier = computeTacticalSpeedMultiplier(next, context);

        if (previous != next) {
            SemScenarioRuntimeManager.get().logTrackedEntityEvent(
                    "TACTICAL_STATE_CHANGE",
                    this,
                    "from=" + previous.name()
                            + " to=" + next.name()
                            + " pressure=" + String.format(Locale.ROOT, "%.2f", this.threatPressure)
                            + " confidence=" + String.format(Locale.ROOT, "%.2f", this.combatConfidence)
                            + " reason=" + context.reason()
            );
        }

        return context;
    }

    private TacticalCombatState selectTacticalCombatState(TacticalCombatState current, CombatDecisionContext context) {
        TacticalSchmittThresholds thresholds = TacticalSchmittThresholds.forRole(this.getRole());
        float pressure = context.threatPressure();
        float confidence = context.combatConfidence();

        if (context.killzone() || (pressure >= TacticalAiConfig.tacticalRetreatPressureThreshold() && confidence <= thresholds.exitRetreat())) {
            return TacticalCombatState.RETREAT;
        }

        if (current == TacticalCombatState.RETREAT && pressure > thresholds.exitRetreat() && confidence < thresholds.enterCover()) {
            return TacticalCombatState.RETREAT;
        }

        if (context.shouldHoldAmbush() && confidence <= thresholds.exitAmbush()) {
            return TacticalCombatState.AMBUSH;
        }

        if (current == TacticalCombatState.AMBUSH && context.shouldHoldAmbush() && confidence < thresholds.enterAmbush()) {
            return TacticalCombatState.AMBUSH;
        }

        if (pressure >= TacticalAiConfig.tacticalCoverPressureThreshold() || confidence <= thresholds.enterCover()) {
            return TacticalCombatState.COVER;
        }

        if (current == TacticalCombatState.COVER && pressure > thresholds.exitCover() && confidence < TacticalAiConfig.tacticalPushConfidenceThreshold()) {
            return TacticalCombatState.COVER;
        }

        if (context.shouldAdvance() && confidence >= TacticalAiConfig.tacticalPushConfidenceThreshold() && pressure < 0.72F) {
            return TacticalCombatState.PUSH;
        }

        if (current == TacticalCombatState.PUSH && confidence >= thresholds.exitPush() && pressure < 0.76F) {
            return TacticalCombatState.PUSH;
        }

        return TacticalCombatState.HOLD;
    }

    private double computeTacticalSpeedMultiplier(TacticalCombatState state, CombatDecisionContext context) {
        double multiplier = switch (state) {
            case RETREAT -> 1.22D;
            case COVER -> 1.08D;
            case PUSH -> 1.14D;
            case AMBUSH -> 0.82D;
            case HOLD -> 1.00D;
        };
        if (context.threatPressure() >= 0.85F && state != TacticalCombatState.AMBUSH) {
            multiplier += 0.05D;
        }
        if (this.genericSuppressionLevel >= TacticalAiConfig.suppressionStrongThreshold()) {
            multiplier += state == TacticalCombatState.COVER || state == TacticalCombatState.RETREAT ? 0.08D : -0.04D;
        }
        return Math.max(0.65D, Math.min(1.35D, multiplier));
    }

    public TacticalCombatState getTacticalCombatState() {
        return this.tacticalCombatState;
    }

    public float getThreatPressure() {
        return this.threatPressure;
    }

    public float getCombatConfidence() {
        return this.combatConfidence;
    }

    public double getTacticalSpeedMultiplier() {
        return this.tacticalSpeedMultiplier;
    }

    @Nullable
    public CombatDecisionContext getLastCombatDecisionContext() {
        return this.lastCombatDecisionContext;
    }

    public SquadContext getSquadContext() {
        return this.squadContext == null ? SquadContext.empty() : this.squadContext;
    }

    @Nullable
    public Vec3 getSharedEnemyPosition() {
        SquadContext context = getSquadContext();
        return context.hasSharedEnemyMemory() ? context.sharedEnemyPosition() : null;
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
