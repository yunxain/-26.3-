package com.bodypillow.client;

import com.bodypillow.BodyPillowMod;
import com.bodypillow.DakiPacks;
import com.bodypillow.ModConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 读取图案包里的 png/jpg（含 zip），必要时缩小，注册成动态贴图。
 * 每 2 秒检查一次文件是否变化，所以改图无需重启。
 */
public final class PatternTextures {
    private static final Logger LOG = LoggerFactory.getLogger("bodypillow");
    public static final Identifier BLANK = BodyPillowMod.id("textures/entity/pillow_blank.png");

    private record Entry(Identifier texture, long stamp, long checkedAt) {}

    private static final Map<String, Entry> CACHE = new HashMap<>();

    public static Identifier get(String id, boolean back) {
        if (id == null || id.isEmpty()) return BLANK;
        String key = id + (back ? "|b" : "|f");
        long now = System.currentTimeMillis();
        Entry e = CACHE.get(key);
        if (e != null && now - e.checkedAt() < 2000) return e.texture();

        Identifier fallback = e != null ? e.texture() : BLANK;
        DakiPacks.Daki daki = DakiPacks.get(id);
        if (daki == null) {
            CACHE.put(key, new Entry(fallback, -1, now));
            return fallback;
        }
        long stamp = DakiPacks.stamp(daki, back);
        if (e != null && e.stamp() == stamp) {
            CACHE.put(key, new Entry(e.texture(), stamp, now));
            return e.texture();
        }
        try {
            Identifier tex = (e != null && !e.texture().equals(BLANK))
                    ? e.texture()
                    : BodyPillowMod.id("daki/" + Integer.toHexString(id.hashCode()) + "_" + id.length() + (back ? "_b" : "_f"));
            byte[] png = toPng(DakiPacks.readRaw(daki, back), daki.smooth());
            try (InputStream in = new ByteArrayInputStream(png)) {
                NativeImage img = NativeImage.read(in);
                Minecraft.getInstance().getTextureManager()
                        .register(tex, new DynamicTexture(() -> "bodypillow:" + key, img));
            }
            CACHE.put(key, new Entry(tex, stamp, now));
            return tex;
        } catch (Exception ex) {
            LOG.warn("加载图案 {} 失败: {}", id, ex.toString());
            CACHE.put(key, new Entry(fallback, stamp, now + 10000));
            return fallback;
        }
    }

    /** 任意格式（png/jpg…）-> png 字节，过大则按比例缩小 */
    private static byte[] toPng(byte[] raw, boolean smooth) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
        if (img == null) throw new IOException("不支持的图片格式");
        ModConfig cfg = ModConfig.get();
        int maxW = Math.max(16, Math.min(cfg.maxImageWidth, 4096));
        int maxH = Math.max(16, Math.min(cfg.maxImageHeight, 8192));
        int w = img.getWidth(), h = img.getHeight();
        double scale = Math.min(1.0, Math.min((double) maxW / w, (double) maxH / h));
        BufferedImage out = img;
        if (scale < 1.0 || img.getType() != BufferedImage.TYPE_INT_ARGB) {
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, smooth
                    ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                    : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bo);
        return bo.toByteArray();
    }

    private PatternTextures() {}
}
