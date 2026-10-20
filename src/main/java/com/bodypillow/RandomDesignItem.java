package com.bodypillow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 未解锁的图案：右键解锁成一张随机的具体图案。 */
public class RandomDesignItem extends Item {
    public RandomDesignItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (DakiPacks.list().isEmpty()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            ItemStack result = ModContent.blueprint(DakiPacks.randomId());
            held.consume(1, player);
            if (!player.addItem(result)) player.spawnAtLocation(serverLevel, result);
        }
        return InteractionResult.SUCCESS;
    }
}
