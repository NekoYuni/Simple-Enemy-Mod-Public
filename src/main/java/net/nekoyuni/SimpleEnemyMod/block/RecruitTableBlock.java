package net.nekoyuni.SimpleEnemyMod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import net.nekoyuni.SimpleEnemyMod.registry.ModEntities;

public class RecruitTableBlock extends HorizontalDirectionalBlock {

    public RecruitTableBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(
                this.getStateDefinition().any().setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemInHand = player.getItemInHand(hand);

        if (itemInHand.getItem() != Items.EMERALD || itemInHand.getCount() < 16) {
            player.displayClientMessage(
                    Component.literal("You need 16 or more emeralds to recruit a PMC!!"),
                    true
            );
            return InteractionResult.FAIL;
        }

        PmcUnitEntity unit = ModEntities.PMCUNIT.get().create(level);
        if (unit == null) {
            return InteractionResult.FAIL;
        }

        itemInHand.shrink(16);

        unit.moveTo(
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                0.0F,
                0.0F
        );

        unit.finalizeSpawn(
                (ServerLevelAccessor) level,
                level.getCurrentDifficultyAt(pos),
                MobSpawnType.TRIGGERED,
                null,
                null
        );

        unit.setOwner(player.getUUID());
        level.addFreshEntity(unit);

        playContractSuccessSound(level, pos);

        return InteractionResult.SUCCESS;
    }


    // Sound and Particle Offset and visual effect
    private void playContractSuccessSound (Level level, BlockPos pos) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (level.getServer() == null) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.WHITE_ASH,
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                10,
                0.2, 0.2, 0.2,
                0.05
        );

        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);

        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 10, () -> {

            level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 1.0F, 1.0F);

            serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    15,
                    0.4, 0.4, 0.4,
                    0.1
            );

        }));

    }
}

