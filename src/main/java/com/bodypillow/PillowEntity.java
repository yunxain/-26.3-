package com.bodypillow;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 等身抱枕实体。三种摆法：
 *  LYING    平放在地面/床上
 *  STANDING 立在地面上，正面朝向放置者
 *  WALL     贴在墙面上，正面朝外
 * 左键回收，潜行+右键翻面。
 */
public class PillowEntity extends Entity {
    public static final int LYING = 0, STANDING = 1, WALL = 2;
    /** 抱枕尺寸：半宽、半长、半厚（最厚处） */
    public static final float HW = 0.25f, HL = 0.75f, T = 0.10f;

    private static final EntityDataAccessor<String> PATTERN =
            SynchedEntityData.defineId(PillowEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FLIPPED =
            SynchedEntityData.defineId(PillowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(PillowEntity.class, EntityDataSerializers.INT);

    public PillowEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public String getPattern() { return this.entityData.get(PATTERN); }
    public void setPattern(String pattern) { this.entityData.set(PATTERN, pattern); }
    public boolean isFlipped() { return this.entityData.get(FLIPPED); }
    public void setFlipped(boolean flipped) { this.entityData.set(FLIPPED, flipped); }
    public int getMode() { return this.entityData.get(MODE); }

    public void setMode(int mode) {
        this.entityData.set(MODE, mode);
        refreshDimensions();
    }

    public ItemStack toItem() {
        return ModContent.pillow(getPattern(), isFlipped());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.entityData == null || getMode() == LYING) return EntityDimensions.fixed(1.0f, 0.2f);
        return EntityDimensions.fixed(0.6f, 1.5f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (MODE.equals(accessor)) refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PATTERN, "");
        builder.define(FLIPPED, false);
        builder.define(MODE, LYING);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setPattern(input.getStringOr("Pattern", ""));
        setFlipped(input.getBooleanOr("Flipped", false));
        setMode(input.getIntOr("Mode", LYING));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("Pattern", getPattern());
        output.putBoolean("Flipped", isFlipped());
        output.putInt("Mode", getMode());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public ItemStack getPickResult() {
        return toItem();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved()) return false;
        if (source.getEntity() instanceof Player player) {
            if (!player.hasInfiniteMaterials()) spawnAtLocation(level, toItem());
            discard();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // 支撑方块（脚下 / 身后的墙）没了就掉落成物品
        if (level() instanceof ServerLevel level && this.tickCount % 10 == 0) {
            BlockPos support;
            if (getMode() == WALL) {
                double rad = Math.toRadians(getYRot());
                double reach = T + 0.1;
                support = BlockPos.containing(getX() - Math.sin(rad) * reach, getY() + 0.75, getZ() + Math.cos(rad) * reach);
            } else {
                support = BlockPos.containing(getX(), getY() - 0.05, getZ());
            }
            if (level.getBlockState(support).getCollisionShape(level, support).isEmpty()) {
                spawnAtLocation(level, toItem());
                discard();
            }
        }
    }
}
