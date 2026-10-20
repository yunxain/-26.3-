package com.bodypillow.client;

import com.bodypillow.PillowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 程序生成的圆润抱枕网格：两面鼓起、四周边缘捏合（像真的抱枕）。
 * 局部坐标：x 为宽，z 为长（图片顶部在 -z），y 为厚度方向（正面在 +y）。
 */
final class PillowMesh {
    private static final int NA = 14, NB = 40;
    private static final int W = NB + 1;
    /** 每个顶点：x y z nx ny nz u v */
    private static final float[][] V = new float[(NA + 1) * W][];

    static {
        for (int i = 0; i <= NA; i++) {
            for (int j = 0; j <= NB; j++) {
                double a = -1 + 2.0 * i / NA;
                double b = -1 + 2.0 * j / NB;
                double e = 0.01;
                double dx = (h(a + e, b) - h(a - e, b)) / (2 * e) / PillowEntity.HW;
                double dz = (h(a, b + e) - h(a, b - e)) / (2 * e) / PillowEntity.HL;
                double len = Math.sqrt(dx * dx + 1 + dz * dz);
                V[i * W + j] = new float[]{
                        (float) (a * PillowEntity.HW), h(a, b), (float) (b * PillowEntity.HL),
                        (float) (-dx / len), (float) (1 / len), (float) (-dz / len),
                        (float) ((a + 1) / 2), (float) ((b + 1) / 2)};
            }
        }
    }

    /** 厚度分布：中间最厚，四周归零，截面近似圆弧 */
    private static float h(double a, double b) {
        a = Math.max(-1, Math.min(1, a));
        b = Math.max(-1, Math.min(1, b));
        double ya = Math.sqrt(1 - a * a);
        double yb = Math.sqrt(Math.max(0, 1 - Math.pow(Math.abs(b), 8)));
        return (float) (PillowEntity.T * ya * yb);
    }

    /** bottom = false 画 +y 面（正面），true 画 -y 面（背面，图片左右镜像以便从背面看是正的）。 */
    static void emit(PoseStack.Pose pose, VertexConsumer vc, int light, boolean bottom) {
        for (int i = 0; i < NA; i++) {
            for (int j = 0; j < NB; j++) {
                int[] idx = {i * W + j, (i + 1) * W + j, (i + 1) * W + j + 1, i * W + j + 1};
                if (bottom) idx = new int[]{idx[3], idx[2], idx[1], idx[0]};
                for (int k : idx) vertex(pose, vc, light, V[k], bottom);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, int light, float[] v, boolean bottom) {
        float s = bottom ? -1 : 1;
        vc.addVertex(pose, v[0], v[1] * s, v[2])
                .setColor(-1)
                .setUv(bottom ? 1 - v[6] : v[6], v[7])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, v[3], v[4] * s, v[5]);
    }

    private PillowMesh() {}
}
