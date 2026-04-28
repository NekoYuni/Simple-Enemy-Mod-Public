package net.nekoyuni.SimpleEnemyMod.entity.unit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.bandit.BanditMoveToCoverGoal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.bandit.BanditRetreatGoal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.equipment.BanditWeaponEquipper;
import net.nekoyuni.SimpleEnemyMod.registry.ModSounds;

import javax.annotation.Nullable;

public class BanditUnitEntity extends AbstractUnit {

    private static final int VARIANT_COUNT = 3; // Bandits reuse the existing US texture set.

    private int variant;
    private int retreatTicks;
    private int coverTravelTicks;
    private int coverHoldTicks;

    @Nullable
    private LivingEntity retreatThreat;

    @Nullable
    private Vec3 retreatCoverPos;

    private BanditRetreatState retreatState = BanditRetreatState.NONE;

    public BanditUnitEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.variant = this.random.nextInt(VARIANT_COUNT); // Pick a US skin variant for visual variety.
    }

    public int getVariant() {
        return this.variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
        this.entityData.set(DATA_VARIANT_ID, variant); // Keep client rendering synchronized.
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Variant")) {
            this.setVariant(tag.getInt("Variant")); // Preserve the selected US skin variant.
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant()); // Save only visual state; retreat state is runtime-only.
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            this.tickRetreatState(); // Server owns retreat/despawn decisions.
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damageOccurred = super.hurt(source, amount);

        if (!this.level().isClientSide() && damageOccurred && this.isAlive() && source.getEntity() instanceof Player player) {
            float healthRatio = this.getHealth() / this.getMaxHealth(); // Check health after damage is applied.

            if (CommonConfig.BANDIT_RETREAT_ENABLED.get()
                    && healthRatio < CommonConfig.BANDIT_RETREAT_HEALTH_THRESHOLD.get().floatValue()) {
                this.startRetreatingFrom(player); // Player damage below threshold starts the two-step retreat.
            }
        }

        return damageOccurred;
    }

    @Override
    public void equipRandomGun() {
        BanditWeaponEquipper.equipRandomGun(this, this.getRandom()); // Use the bandits loadout pool.
    }

    @Override
    public void setupRoleGoals() {
        this.goalSelector.removeAllGoals(pGoal -> true);
        this.targetSelector.removeAllGoals(pGoal -> true);

        if (this.role == null) {
            this.setRole(UnitRole.DEFAULT);
        }

        this.goalSelector.addGoal(0, new BanditMoveToCoverGoal(this)); // Step 1: try to break line of sight first.
        this.goalSelector.addGoal(1, new BanditRetreatGoal(this)); // Step 2: fallback/full retreat away from players.

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(BanditUnitEntity.class).setUnseenMemoryTicks(600));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true).setUnseenMemoryTicks(800));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PmcUnitEntity.class, true).setUnseenMemoryTicks(800));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, USunitEntity.class, true).setUnseenMemoryTicks(800));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, RUunitEntity.class, true).setUnseenMemoryTicks(800));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Skeleton.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true, (target) -> {
            if (target.getClass() == this.getClass()) {
                return false; // Bandits do not target other bandits.
            }

            if (target instanceof AbstractUnit) {
                return true; // Bandits are hostile to other unit factions.
            }

            return target instanceof Enemy; // Keep vanilla hostile mobs as valid targets.
        }));

        switch (this.getRole()) {
            case DEFAULT -> UnitRole.DEFAULT.getGoals().addGoals(this);
            case SQUAD_LEADER -> UnitRole.SQUAD_LEADER.getGoals().addGoals(this);
            case SQUAD_UNIT -> UnitRole.SQUAD_UNIT.getGoals().addGoals(this);
            default -> UnitRole.DEFAULT.getGoals().addGoals(this);
        }
    }

    public void startRetreatingFrom(LivingEntity threat) {
        this.retreatThreat = threat;
        this.retreatTicks = CommonConfig.BANDIT_RETREAT_DURATION_TICKS.get();
        this.coverTravelTicks = 0;
        this.coverHoldTicks = 0;
        this.setTarget(null); // Stop retaliation while panic retreat is active.
        this.releaseMovementLock(); // Tactical manager locks should not pin a retreating bandit.

        if (CommonConfig.BANDIT_RETREAT_USE_COVER.get()) {
            this.retreatCoverPos = this.findCoverPosition(threat);
            if (this.retreatCoverPos != null) {
                this.retreatState = BanditRetreatState.MOVING_TO_COVER; // First phase: reach cover if possible.
                return;
            }
        }

        this.startFullRetreat(threat); // No usable cover found, so use the existing retreat behavior.
    }

    public void startFullRetreat(LivingEntity threat) {
        this.retreatThreat = threat;
        this.retreatCoverPos = null;
        this.retreatTicks = CommonConfig.BANDIT_RETREAT_DURATION_TICKS.get();
        this.coverTravelTicks = 0;
        this.coverHoldTicks = 0;
        this.retreatState = BanditRetreatState.FULL_RETREAT;
        this.setTarget(null); // Full retreat also suppresses normal combat targeting.
        this.releaseMovementLock();
    }

    public void stopRetreating() {
        this.retreatThreat = null;
        this.retreatCoverPos = null;
        this.retreatTicks = 0;
        this.coverTravelTicks = 0;
        this.coverHoldTicks = 0;
        this.retreatState = BanditRetreatState.NONE;
    }

    public BanditRetreatState getRetreatState() {
        return this.retreatState;
    }

    @Nullable
    public LivingEntity getRetreatThreat() {
        return this.retreatThreat;
    }

    @Nullable
    public Vec3 getRetreatCoverPos() {
        return this.retreatCoverPos;
    }

    public boolean isMovingToCover() {
        return this.retreatState == BanditRetreatState.MOVING_TO_COVER && this.retreatCoverPos != null;
    }

    public boolean isFullRetreating() {
        return this.retreatState == BanditRetreatState.FULL_RETREAT;
    }

    public void onCoverPathFailed() {
        LivingEntity threat = this.findNearestPlayerInsideDespawnDistance();
        if (threat != null) {
            this.startFullRetreat(threat); // If cover movement fails and danger remains, continue with full retreat.
        } else {
            this.discard(); // If no player is close enough, the escaped bandit is removed without drops.
        }
    }

    private void tickRetreatState() {
        if (this.retreatState == BanditRetreatState.NONE) {
            return;
        }

        if (this.retreatState == BanditRetreatState.MOVING_TO_COVER) {
            this.tickCoverRetreatState();
            return;
        }

        if (this.retreatState == BanditRetreatState.FULL_RETREAT) {
            this.tickFullRetreatState();
        }
    }

    private void tickCoverRetreatState() {
        if (this.retreatThreat == null || !this.retreatThreat.isAlive()) {
            this.stopRetreating(); // The specific threat is gone, so panic retreat can end.
            return;
        }

        this.coverTravelTicks++;
        if (this.coverTravelTicks > CommonConfig.BANDIT_RETREAT_COVER_MAX_TRAVEL_TICKS.get()) {
            this.startFullRetreat(this.retreatThreat); // Do not get stuck trying to reach a bad cover point.
            return;
        }

        if (this.retreatCoverPos == null || this.position().distanceToSqr(this.retreatCoverPos) > this.getCoverReachedDistanceSqr()) {
            return; // Bandit is still traveling toward the cover point.
        }

        if (this.coverHoldTicks < CommonConfig.BANDIT_RETREAT_COVER_HOLD_TICKS.get()) {
            this.coverHoldTicks++; // Optional short pause makes cover retreat readable in-game.
            return;
        }

        LivingEntity nearbyPlayer = this.findNearestPlayerInsideDespawnDistance();
        if (nearbyPlayer == null) {
            this.discard(); // Same safety rule as full retreat: no nearby players means successful escape.
        } else {
            this.startFullRetreat(nearbyPlayer); // A player is still too close, so continue with the existing retreat loop.
        }
    }

    private void tickFullRetreatState() {
        if (this.retreatThreat == null || !this.retreatThreat.isAlive()) {
            this.stopRetreating(); // The original threat is gone, so the bandit stops retreating.
            return;
        }

        if (this.retreatTicks > 0) {
            this.retreatTicks--;
            return;
        }

        LivingEntity nearbyPlayer = this.findNearestPlayerInsideDespawnDistance();
        if (nearbyPlayer == null) {
            this.discard(); // Escaped far enough from every player.
        } else {
            this.startFullRetreat(nearbyPlayer); // Still unsafe, so restart the full retreat timer from the nearest player.
        }
    }

    @Nullable
    public Player findNearestPlayerInsideDespawnDistance() {
        double maxDistanceSqr = CommonConfig.BANDIT_RETREAT_DESPAWN_DISTANCE.get() * CommonConfig.BANDIT_RETREAT_DESPAWN_DISTANCE.get();
        Player nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        for (Player player : this.level().players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue; // Spectators and dead players should not block escape despawn.
            }

            double distanceSqr = this.distanceToSqr(player);
            if (distanceSqr <= maxDistanceSqr && distanceSqr < nearestDistanceSqr) {
                nearest = player;
                nearestDistanceSqr = distanceSqr;
            }
        }

        return nearest;
    }

    @Nullable
    private Vec3 findCoverPosition(LivingEntity threat) {
        int radius = CommonConfig.BANDIT_RETREAT_COVER_SEARCH_RADIUS.get();
        int attempts = CommonConfig.BANDIT_RETREAT_COVER_MAX_ATTEMPTS.get();
        Vec3 bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        BlockPos origin = this.blockPosition();

        for (int i = 0; i < attempts; i++) {
            int x = origin.getX() + this.random.nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + this.random.nextInt(radius * 2 + 1) - radius;
            int y = origin.getY() + this.random.nextInt(7) - 3; // Search a small vertical band around the bandit.
            BlockPos candidateBlock = new BlockPos(x, y, z);
            Vec3 candidate = Vec3.atBottomCenterOf(candidateBlock);

            if (!this.isUsableCoverPosition(candidateBlock, candidate, threat)) {
                continue;
            }

            double distanceFromThreat = candidate.distanceToSqr(threat.position());
            double distanceFromBandit = candidate.distanceToSqr(this.position());
            double score = distanceFromThreat - (distanceFromBandit * 0.35); // Prefer cover that is safer but still reachable.

            if (score > bestScore) {
                bestScore = score;
                bestPos = candidate;
            }
        }

        return bestPos;
    }

    private boolean isUsableCoverPosition(BlockPos blockPos, Vec3 candidate, LivingEntity threat) {
        if (!this.level().getBlockState(blockPos).isAir() || !this.level().getBlockState(blockPos.above()).isAir()) {
            return false; // The bandit needs two free blocks to stand at the cover point.
        }

        if (!this.level().getBlockState(blockPos.below()).isSolidRender(this.level(), blockPos.below())) {
            return false; // Avoid choosing air, water-like gaps, or unsupported positions.
        }

        double minDistance = 4.0D;
        if (candidate.distanceToSqr(threat.position()) < minDistance * minDistance) {
            return false; // Cover too close to the threat is not useful.
        }

        Vec3 threatEye = threat.getEyePosition();
        Vec3 candidateEye = candidate.add(0.0D, this.getEyeHeight(), 0.0D);
        HitResult hitResult = this.level().clip(new ClipContext(
                threatEye,
                candidateEye,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        return hitResult.getType() == HitResult.Type.BLOCK; // A blocking ray means the player does not have direct line of sight.
    }

    private double getCoverReachedDistanceSqr() {
        double reachedDistance = CommonConfig.BANDIT_RETREAT_COVER_REACHED_DISTANCE.get();
        return reachedDistance * reachedDistance;
    }

    @Override
    protected SoundEvent getCustomHurtSound() {
        return ModSounds.SOUND_US_UNIT_HURT.get(); // Bandits currently reuse US voice assets.
    }

    @Override
    protected SoundEvent getCustomDeathSound() {
        return ModSounds.SOUND_US_UNIT_DEATH.get();
    }

    @Override
    protected SoundEvent getCustomAlertSound() {
        return ModSounds.SOUND_US_UNIT_ALERT.get();
    }

    @Override
    public float getSoundVolume() {
        return 1.1F;
    }
}
