package net.nekoyuni.SimpleEnemyMod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;

import java.util.Objects;
import java.util.function.Supplier;

public class RoleSpawnEggItem extends ForgeSpawnEggItem {

    private final UnitRole role;

    public RoleSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> typeIn,
                            int primaryColorIn, int secondaryColorIn, Properties builder, UnitRole role) {

        super(typeIn, primaryColorIn, secondaryColorIn, builder);
        this.role = role;
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {

        Level level = pContext.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;

        } else {
            ItemStack itemstack = pContext.getItemInHand();
            BlockPos blockpos = pContext.getClickedPos();
            Direction direction = pContext.getClickedFace();
            BlockState blockstate = level.getBlockState(blockpos);

            BlockPos finalSpawnPos = blockpos;
            if (blockstate.getCollisionShape(level, blockpos).isEmpty()) {
                finalSpawnPos = blockpos.relative(direction);
            }

            CompoundTag itemTag = itemstack.getOrCreateTag();
            CompoundTag entityData = new CompoundTag();

            entityData.putString("UnitRole", this.role.name());
            itemTag.put("EntityTag", entityData);

            EntityType<?> entitytype = this.getType(itemstack.getTag());

            Entity entity = entitytype.spawn(
                    (ServerLevel) level,
                    itemstack,
                    pContext.getPlayer(),
                    finalSpawnPos,
                    MobSpawnType.SPAWN_EGG,
                    true,
                    !Objects.equals(blockpos, pContext.getClickedPos())
            );

            itemTag.remove("EntityTag");

            if (entity != null) {
                itemstack.shrink(1);
                return InteractionResult.CONSUME;
            }

            return InteractionResult.PASS;
        }
    }

    @Override
    public Component getName(ItemStack pStack) {
        return Component.translatable(this.getDescriptionId());
    }

}