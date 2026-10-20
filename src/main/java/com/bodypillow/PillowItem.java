package com.bodypillow;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PillowItem extends Item {
    public PillowItem(Properties properties) {
        super(properties);
    }

    /**
     * 对方块顶面右键：平放（地面、床）；潜行 + 右键顶面：立在地上；
     * 对方块侧面右键：挂在墙上。
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction face = ctx.getClickedFace();
        if (face == Direction.DOWN) return InteractionResult.FAIL;
        if (ctx.getLevel() instanceof ServerLevel level) {
            ItemStack stack = ctx.getItemInHand();
            Player player = ctx.getPlayer();
            Vec3 c = ctx.getClickLocation();
            double x = c.x, y = c.y, z = c.z;
            int mode;
            float yaw;
            if (face == Direction.UP) {
                mode = (player != null && player.isShiftKeyDown()) ? PillowEntity.STANDING : PillowEntity.LYING;
                yaw = Math.round(ctx.getRotation() / 15.0f) * 15.0f;
            } else {
                mode = PillowEntity.WALL;
                yaw = face.getOpposite().toYRot();
                x += face.getStepX() * PillowEntity.T;
                z += face.getStepZ() * PillowEntity.T;
                // 点击处作为抱枕中部，但不低于被点击方块的底部
                y = Math.max(ctx.getClickedPos().getY(), c.y - PillowEntity.HL);
            }
            PillowEntity e = new PillowEntity(ModContent.PILLOW_ENTITY, level);
            e.setMode(mode);
            e.setPos(x, y, z);
            e.setYRot(yaw);
            e.setPattern(ModContent.patternOf(stack));
            e.setFlipped(ModContent.flippedOf(stack));
            level.addFreshEntity(e);
            level.playSound(null, x, y, z, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /** 潜行 + 对空气右键：翻面（正面 / 背面）。 */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !ModContent.patternOf(stack).isEmpty()) {
            if (level instanceof ServerLevel) {
                stack.set(ModContent.FLIPPED, !ModContent.flippedOf(stack));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
