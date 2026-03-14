package net.nekoyuni.SimpleEnemyMod.entity.unit;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import net.nekoyuni.SimpleEnemyMod.config.CommonConfig;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.AttackSpecificTargetGoal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.CommanderOrderGoal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.NoPlayerHurtByTargetGoal;
import net.nekoyuni.SimpleEnemyMod.entity.ai.orders.ICommandableMob;
import net.nekoyuni.SimpleEnemyMod.entity.ai.orders.OrderType;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.equipment.PmcUnitWeaponEquipper;
import net.nekoyuni.SimpleEnemyMod.inventory.PmcUnitMenu;
import net.nekoyuni.SimpleEnemyMod.inventory.UnitInventoryHandler;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class PmcUnitEntity extends AbstractUnit implements ICommandableMob {

    private static final int VARIANT_COUNT = 6;
    private int variant;

    // ORDERS
    private static final EntityDataAccessor<Integer> ORDER_TYPE_ID =
            SynchedEntityData.defineId(PmcUnitEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(PmcUnitEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<String> SYNC_ROLE =
            SynchedEntityData.defineId(PmcUnitEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> FORMATION_INDEX =
            SynchedEntityData.defineId(PmcUnitEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> ATTACK_TARGET_ID =
            SynchedEntityData.defineId(PmcUnitEntity.class, EntityDataSerializers.INT);

    private Vec3 moveOrderPosition = Vec3.ZERO;

    // UNIT INVENTORY
    private final UnitInventoryHandler inventory = new UnitInventoryHandler(this);
    private final LazyOptional<IItemHandler> inventoryOptional = LazyOptional.of(() -> inventory);


    public PmcUnitEntity(EntityType<? extends Monster> entityType, Level level) {
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
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(ORDER_TYPE_ID, OrderType.FREE_FIRE.ordinal()); // Before FOLLOW COMMANDER
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(SYNC_ROLE, UnitRole.FRIENDLY_DEFAULT.name());
        this.entityData.define(FORMATION_INDEX, 0);
        this.entityData.define(ATTACK_TARGET_ID, -1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Variant")) {
            this.setVariant(tag.getInt("Variant"));
        }

        if (tag.contains("CurrentOrder")) {
            this.setOrder(OrderType.values()[tag.getInt("CurrentOrder")]);
        }

        if (tag.hasUUID("OwnerUUID")) {
            this.setOwner(tag.getUUID("OwnerUUID"));
        }

        if (tag.contains("MoveX")) {
            this.moveOrderPosition = new Vec3(tag.getDouble("MoveX"), tag.getDouble("MoveY"), tag.getDouble("MoveZ"));
        }

        if (tag.contains("FormationIndex")) {
            this.setFormationIndex(tag.getInt("FormationIndex"));
        }

        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }


    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("Variant", this.getVariant());
        tag.putInt("CurrentOrder", this.getOrder().ordinal());

        if (this.getOwnerUUID() != null) {
            tag.putUUID("OwnerUUID", this.getOwnerUUID());
        }

        if (this.moveOrderPosition != Vec3.ZERO) {
            tag.putDouble("MoveX", moveOrderPosition.x);
            tag.putDouble("MoveY", moveOrderPosition.y);
            tag.putDouble("MoveZ", moveOrderPosition.z);
        }

        tag.putInt("FormationIndex", this.getFormationIndex());

        tag.put("Inventory", inventory.serializeNBT());
    }

    // INVENTORY
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryOptional.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryOptional.invalidate();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
            }
        }
    }

    public void syncEquipmentToInventory() {
        inventory.setStackInSlot(0, this.getItemBySlot(EquipmentSlot.MAINHAND).copy());
        inventory.setStackInSlot(1, this.getItemBySlot(EquipmentSlot.OFFHAND).copy());
        inventory.setStackInSlot(2, this.getItemBySlot(EquipmentSlot.FEET).copy());
        inventory.setStackInSlot(3, this.getItemBySlot(EquipmentSlot.LEGS).copy());
        inventory.setStackInSlot(4, this.getItemBySlot(EquipmentSlot.CHEST).copy());
        inventory.setStackInSlot(5, this.getItemBySlot(EquipmentSlot.HEAD).copy());
    }


    @Override
    public void equipRandomGun() {
        PmcUnitWeaponEquipper.equipRandomGun(this, this.getRandom());
    }

    @Override
    public void setupRoleGoals() {

        this.goalSelector.removeAllGoals(pGoal -> true);
        this.targetSelector.removeAllGoals(pGoal -> true);

        if (this.role == null) {
            this.setRole(UnitRole.FRIENDLY_DEFAULT);
        }

        System.out.println("[Goal Setup] Entity started with: " + this.getRole());

        this.targetSelector.addGoal(0, new AttackSpecificTargetGoal(this));

        java.util.function.Predicate<LivingEntity> fireFilter = (target) -> this.isFireAllowed();


        this.targetSelector.addGoal(1, new NoPlayerHurtByTargetGoal(this).setAlertOthers().setUnseenMemoryTicks(600));

        if (!CommonConfig.US_UNITS_FRIENDLY.get()) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, USunitEntity.class, true, fireFilter).setUnseenMemoryTicks(800));
        }

        if (!CommonConfig.RU_UNITS_FRIENDLY.get()) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, RUunitEntity.class, true, fireFilter).setUnseenMemoryTicks(800));
        }

        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, true, fireFilter));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Skeleton.class, true, fireFilter));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true, fireFilter));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true, (target) -> {

            if (target.getClass() == this.getClass()) {
                return false;
            }

            if (CommonConfig.RU_UNITS_FRIENDLY.get() && target instanceof RUunitEntity) {
                return false;
            }

            if (CommonConfig.US_UNITS_FRIENDLY.get() && target instanceof USunitEntity) {
                return false;
            }

            if (target instanceof AbstractUnit) {
                return true;
            }

            return target instanceof Enemy;
        }));


        switch(this.getRole()) {
            case FRIENDLY_DEFAULT:
                UnitRole.FRIENDLY_DEFAULT.getGoals().addGoals(this);
                System.out.println("[Goal Setup] Creating an Entity with: " + this.getRole());
                break;

            case FRIENDLY_SQUAD_LEADER:
                UnitRole.FRIENDLY_SQUAD_LEADER.getGoals().addGoals(this);
                System.out.println("[Goal Setup] Creating an Entity with: " + this.getRole());
                break;

            case FRIENDLY_SQUAD_UNIT:
                UnitRole.FRIENDLY_SQUAD_UNIT.getGoals().addGoals(this);
                System.out.println("[Goal Setup] Creating an Entity with: " + this.getRole());
                break;

            default:
                UnitRole.FRIENDLY_DEFAULT.getGoals().addGoals(this);
                System.out.println("[Goal Setup] Creating an Entity with: " + this.getRole());
                break;
        }
    }


    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {

        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {

            if (this.getOwnerUUID() == null) {

                this.setOwner(player.getUUID());
                player.sendSystemMessage(Component.literal("§a[SEM] Unit Recruited! Awaiting orders."));
                this.playSound(SoundEvents.VILLAGER_TRADE, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;

            } else if (this.isOwnedBy(player)) {

                if (player.isShiftKeyDown()) {
                    this.syncEquipmentToInventory();

                    if (player instanceof ServerPlayer serverPlayer) {
                        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                                (id, playerInv, p) -> new PmcUnitMenu(id, playerInv, this),
                                Component.literal("PMC Unit Equipment")
                        ), buf -> {
                            buf.writeInt(this.getId());
                        });
                    }

                    return InteractionResult.SUCCESS;
                }

            } else {
                player.sendSystemMessage(Component.literal("§e[SEM] Unit Status: " + this.getOrder().name()));
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    public void resetCommanderGoalCooldown() {
        this.goalSelector.getAvailableGoals().forEach(wrappedGoal -> {

            if (wrappedGoal.getGoal() instanceof CommanderOrderGoal commanderGoal) {
                commanderGoal.resetCombatCooldown();
            }

        });
    }

    @Override
    protected SoundEvent getCustomHurtSound() {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getCustomDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    protected SoundEvent getCustomAlertSound() {
        return SoundEvents.VILLAGER_NO;
    }

    @Override
    public float getSoundVolume() {
        return 1.5F;
    }

    @Override
    public void setRole(UnitRole role) {
        this.entityData.set(SYNC_ROLE, role.name());
    }

    @Override
    public UnitRole getRole() {
        try {
            return UnitRole.valueOf(this.entityData.get(SYNC_ROLE));
        } catch (IllegalArgumentException e) {
            return UnitRole.FRIENDLY_DEFAULT;
        }
    }

    // GETTERS AND SETTERS FOR ORDERS

    public void setOrder(OrderType order) {
        this.entityData.set(ORDER_TYPE_ID, order.ordinal());
    }

    public OrderType getOrder() {
        return OrderType.values()[this.entityData.get(ORDER_TYPE_ID)];
    }

    public void setMoveToTarget(Vec3 pos) {
        this.moveOrderPosition = pos;
        this.setOrder(OrderType.MOVE_TO_POSITION);
    }

    public Vec3 getMoveToTarget() {
        return this.moveOrderPosition;
    }

    // OWNER GETTERS AND SETTERS
    public void setOwner(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(this.getOwnerUUID());
    }

    public void setFormationIndex(int index) {
        this.entityData.set(FORMATION_INDEX, index);
    }

    public int getFormationIndex() {
        return this.entityData.get(FORMATION_INDEX);
    }

    public void setAttackTargetId(int entityId) {
        this.entityData.set(ATTACK_TARGET_ID, entityId);
    }

    public int getAttackTargetId() {
        return this.entityData.get(ATTACK_TARGET_ID);
    }

    public boolean isFireAllowed() {
        return this.getOrder() != OrderType.CEASE_FIRE;
    }


}
