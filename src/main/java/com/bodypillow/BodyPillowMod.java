package com.bodypillow;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BodyPillowMod implements ModInitializer {
    public static final String MOD_ID = "bodypillow";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        org.slf4j.LoggerFactory.getLogger("bodypillow").info("Body Pillow 2.2.0 loaded (rounded mesh, standing + wall modes, creative tab)");
        DakiPacks.ensureDefaults();
        ModContent.init();

        // 敌对生物击杀后有概率掉落"未解锁的图案"（概率见 config/bodypillow.json）
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) return;
            if (!(entity instanceof Enemy)) return;
            ModConfig cfg = ModConfig.get();
            if (cfg.requirePlayerKill && !(source.getEntity() instanceof Player)) return;
            if (entity.getRandom().nextDouble() >= cfg.dropChance) return;
            if (DakiPacks.list().isEmpty()) return;
            entity.spawnAtLocation(level, new ItemStack(ModContent.RANDOM_DESIGN));
        });

        // 潜行 + 右键放在地上/床上的抱枕：翻面
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (entity instanceof PillowEntity pillow && player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
                if (world instanceof ServerLevel) pillow.setFlipped(!pillow.isFlipped());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
