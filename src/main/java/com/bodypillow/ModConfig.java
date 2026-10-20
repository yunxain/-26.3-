package com.bodypillow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/bodypillow.json —— 修改后无需重启，最多 5 秒内自动生效。 */
public final class ModConfig {
    private static final Logger LOG = LoggerFactory.getLogger("bodypillow");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 敌对生物击杀后掉落随机图案图纸的概率，0.01 = 1% */
    public double dropChance = 0.01;
    /** true = 必须由玩家击杀才掉落 */
    public boolean requirePlayerKill = true;

    /** true = 启用备用配方：6 根线 + 3 个羊毛（上下两行线，中间一行羊毛） */
    public boolean altRecipe = false;

    /** 客户端加载图片时的最大尺寸，超过会按比例缩小（上限 4096x8192） */
    public int maxImageWidth = 1024;
    public int maxImageHeight = 2048;

    private static ModConfig cached;
    private static long lastCheck = 0;
    private static long lastModified = -1;

    public static synchronized ModConfig get() {
        long now = System.currentTimeMillis();
        if (cached != null && now - lastCheck < 5000) return cached;
        lastCheck = now;
        Path file = FabricLoader.getInstance().getConfigDir().resolve("bodypillow.json");
        try {
            if (Files.exists(file)) {
                long mod = Files.getLastModifiedTime(file).toMillis();
                if (cached == null || mod != lastModified) {
                    ModConfig c = GSON.fromJson(Files.readString(file), ModConfig.class);
                    cached = c != null ? c : new ModConfig();
                    lastModified = mod;
                }
            } else {
                cached = new ModConfig();
                Files.writeString(file, GSON.toJson(cached));
            }
        } catch (Exception e) {
            LOG.warn("读取 bodypillow.json 失败，使用默认值", e);
            if (cached == null) cached = new ModConfig();
        }
        return cached;
    }
}
