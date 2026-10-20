package com.bodypillow.client;

import com.bodypillow.PillowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * 渲染圆润抱枕网格：+y 面贴"当前朝外的一面"，-y 面贴另一面。
 * 平放：整体抬高半个厚度；立放/挂墙：绕 X 轴转 90° 让图片顶部朝上、正面朝外。
 * 所有"容易随 Minecraft 版本变动"的渲染 API 都集中在这个文件和 PillowMesh 里。
 */
public class PillowRenderer extends EntityRenderer<PillowEntity, PillowRenderState> {
    public PillowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PillowRenderState createRenderState() {
        return new PillowRenderState();
    }

    @Override
    public void extractRenderState(PillowEntity entity, PillowRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.pattern = entity.getPattern();
        state.flipped = entity.isFlipped();
        state.mode = entity.getMode();
        state.yaw = entity.getYRot();
    }

    @Override
    public void submit(PillowRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        int light = state.lightCoords;
        poseStack.pushPose();
        if (state.mode == PillowEntity.LYING) {
            poseStack.translate(0.0f, PillowEntity.T, 0.0f);
            poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(180.0f - state.yaw)));
        } else {
            poseStack.translate(0.0f, PillowEntity.HL, 0.0f);
            poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(180.0f - state.yaw)));
            poseStack.mulPose(new Matrix4f().rotationX((float) (Math.PI / 2)));
        }

        Identifier facing = PatternTextures.get(state.pattern, state.flipped);
        Identifier other = PatternTextures.get(state.pattern, !state.flipped);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(facing),
                (pose, vc) -> PillowMesh.emit(pose, vc, light, false));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(other),
                (pose, vc) -> PillowMesh.emit(pose, vc, light, true));

        poseStack.popPose();
    }
}
