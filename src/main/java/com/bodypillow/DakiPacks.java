package com.bodypillow;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 图案包系统（目录格式与原版 Dakimakura Mod 兼容）：
 *   <游戏目录>/dakimakura-mod/<包>/<抱枕>/front.png|jpg  back.png|jpg  daki-info.json(可选)
 *   <包> 可以是文件夹，也可以是 .zip；包根目录可放 pack-info.json(可选)
 * 抱枕 ID = "<包目录名>/<抱枕目录路径>"。
 */
public final class DakiPacks {
    private static final Logger LOG = LoggerFactory.getLogger("bodypillow");

    public interface Source {
        Set<String> files() throws IOException;
        byte[] read(String path) throws IOException;
        long stamp(String path);
    }

    private record FolderSource(Path root) implements Source {
        public Set<String> files() throws IOException {
            Set<String> out = new HashSet<>();
            try (Stream<Path> s = Files.walk(root, 8)) {
                s.filter(Files::isRegularFile)
                        .forEach(p -> out.add(root.relativize(p).toString().replace('\\', '/')));
            }
            return out;
        }
        public byte[] read(String path) throws IOException {
            return Files.readAllBytes(root.resolve(path));
        }
        public long stamp(String path) {
            try { return Files.getLastModifiedTime(root.resolve(path)).toMillis(); }
            catch (IOException e) { return -1; }
        }
    }

    private record ZipSource(Path zip) implements Source {
        public Set<String> files() throws IOException {
            Set<String> out = new HashSet<>();
            try (ZipFile zf = new ZipFile(zip.toFile())) {
                zf.stream().filter(e -> !e.isDirectory()).forEach(e -> out.add(e.getName()));
            }
            return out;
        }
        public byte[] read(String path) throws IOException {
            try (ZipFile zf = new ZipFile(zip.toFile())) {
                ZipEntry e = zf.getEntry(path);
                if (e == null) throw new IOException("missing " + path);
                try (InputStream in = zf.getInputStream(e)) { return in.readAllBytes(); }
            }
        }
        public long stamp(String path) {
            try { return Files.getLastModifiedTime(zip).toMillis(); }
            catch (IOException e) { return -1; }
        }
    }

    public record Daki(String id, String packName, String path, String name, String author, String flavour,
                       boolean smooth, String front, String back, int index, int total, Source source) {
        public String displayName() {
            if (!name.isBlank()) return name;
            int i = path.lastIndexOf('/');
            return i >= 0 ? path.substring(i + 1) : path;
        }
    }

    private static volatile List<Daki> cache = List.of();
    private static volatile Map<String, Daki> byId = Map.of();
    private static long lastScan = -100000;

    public static Path root() {
        return FabricLoader.getInstance().getGameDir().resolve("dakimakura-mod");
    }

    public static List<Daki> list() {
        if (System.currentTimeMillis() - lastScan > 5000) scan();
        return cache;
    }

    public static Daki get(String id) {
        Daki d = byId.get(id);
        if (d == null && System.currentTimeMillis() - lastScan > 3000) {
            scan();
            d = byId.get(id);
        }
        return d;
    }

    /** 随机抽一个抱枕 ID；没有任何图案时返回 ""。 */
    public static String randomId() {
        List<Daki> l = list();
        return l.isEmpty() ? "" : l.get(ThreadLocalRandom.current().nextInt(l.size())).id();
    }

    private static String fileOf(Daki d, boolean back) {
        if (back) return d.back() != null ? d.back() : d.front();
        return d.front() != null ? d.front() : d.back();
    }

    public static byte[] readRaw(Daki d, boolean back) throws IOException {
        return d.source().read(fileOf(d, back));
    }

    public static long stamp(Daki d, boolean back) {
        return d.source().stamp(fileOf(d, back));
    }

    public static synchronized void scan() {
        lastScan = System.currentTimeMillis();
        List<Daki> all = new ArrayList<>();
        Path root = root();
        if (Files.isDirectory(root)) {
            try (Stream<Path> s = Files.list(root)) {
                for (Path p : s.sorted().toList()) {
                    String n = p.getFileName().toString();
                    if (n.startsWith(".")) continue;
                    try {
                        if (Files.isDirectory(p)) all.addAll(scanPack(n, new FolderSource(p)));
                        else if (n.toLowerCase(Locale.ROOT).endsWith(".zip"))
                            all.addAll(scanPack(n.substring(0, n.length() - 4), new ZipSource(p)));
                    } catch (Exception e) {
                        LOG.warn("读取图案包 {} 失败: {}", n, e.toString());
                    }
                }
            } catch (IOException e) {
                LOG.warn("扫描图案包文件夹失败", e);
            }
        }
        Map<String, Daki> map = new LinkedHashMap<>();
        for (Daki d : all) map.put(d.id(), d);
        cache = List.copyOf(map.values());
        byId = map;
    }

    private record Tmp(String dir, JsonObject info, String front, String back) {}

    private static List<Daki> scanPack(String packDir, Source src) throws IOException {
        Set<String> files = src.files();
        Map<String, String> lower = new HashMap<>();
        for (String f : files) lower.put(f.toLowerCase(Locale.ROOT), f);

        String packName = packDir;
        String infoPath = lower.get("pack-info.json");
        if (infoPath == null) {
            for (String f : files) {
                if (f.toLowerCase(Locale.ROOT).matches("[^/]+/pack-info\\.json")) { infoPath = f; break; }
            }
        }
        if (infoPath != null) {
            JsonObject o = parse(src.read(infoPath));
            String n = o != null ? str(o, "name") : null;
            if (n != null && !n.isBlank()) packName = n;
        }

        Set<String> dirs = new TreeSet<>();
        for (String f : files) {
            int i = f.lastIndexOf('/');
            if (i <= 0) continue;
            String name = f.substring(i + 1).toLowerCase(Locale.ROOT);
            if (name.matches("(front|back)\\.(png|jpg|jpeg)") || name.equals("daki-info.json"))
                dirs.add(f.substring(0, i));
        }

        List<Tmp> found = new ArrayList<>();
        for (String d : dirs) {
            String ip = lower.get((d + "/daki-info.json").toLowerCase(Locale.ROOT));
            JsonObject info = ip != null ? parse(src.read(ip)) : null;
            String front = pick(lower, d, info, "image-front", "front");
            String back = pick(lower, d, info, "image-back", "back");
            if (front == null && back == null) continue;
            found.add(new Tmp(d, info, front, back));
        }

        List<Daki> out = new ArrayList<>();
        for (int i = 0; i < found.size(); i++) {
            Tmp t = found.get(i);
            JsonObject o = t.info();
            String name = o != null && str(o, "name") != null ? str(o, "name") : "";
            String author = o != null && str(o, "author") != null ? str(o, "author") : "";
            String flavour = o != null && str(o, "flavour-text") != null ? str(o, "flavour-text") : "";
            boolean smooth = o == null || !o.has("smooth") || o.get("smooth").getAsBoolean();
            out.add(new Daki(packDir + "/" + t.dir(), packName, t.dir(), name, author, flavour, smooth,
                    t.front(), t.back(), i + 1, found.size(), src));
        }
        return out;
    }

    private static String pick(Map<String, String> lower, String dir, JsonObject info, String key, String base) {
        if (info != null && str(info, key) != null) {
            String a = lower.get((dir + "/" + str(info, key)).toLowerCase(Locale.ROOT));
            if (a != null) return a;
        }
        for (String ext : new String[]{"png", "jpg", "jpeg"}) {
            String a = lower.get((dir + "/" + base + "." + ext).toLowerCase(Locale.ROOT));
            if (a != null) return a;
        }
        return null;
    }

    private static String str(JsonObject o, String key) {
        try { return o.has(key) ? o.get(key).getAsString() : null; }
        catch (Exception e) { return null; }
    }

    private static JsonObject parse(byte[] bytes) {
        try { return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (Exception e) { return null; }
    }

    private static final String README = """
            Body Pillow 图案包说明 / Pack readme (format compatible with the original Dakimakura Mod)

            目录结构 / Layout:
              <游戏目录>/dakimakura-mod/<包>/<抱枕>/front.png
                                                   /back.png
                                                   /daki-info.json   (可选)
              <包>/pack-info.json (可选)；<包> 也可以是 .zip 文件。
            图片: png / jpg / jpeg；推荐 1:3 比例（抱枕约 50cm x 150cm）。过大的图片会按配置缩小。
            只有 front 或只有 back 时，另一面会使用同一张图。
            新增/修改图片无需重启，几秒内生效。

            daki-info.json（全部可选）:
              { "name": "显示名", "author": "作者", "image-front": "front.png", "image-back": "back.png",
                "flavour-text": "tooltip 底部的一句话", "smooth": true }
              smooth：缩放时是否平滑；照片用 true，像素画用 false。

            pack-info.json（全部可选）: { "name": "包名", "author": "作者", "version": "1.0" }

            联机：服务端和每个客户端都需要这个文件夹里的相同内容（服务端负责抽取图案，客户端负责显示）。
            删除整个 dakimakura-mod 文件夹会重置示例包。
            """;

    public static void ensureDefaults() {
        Path root = root();
        try {
            if (Files.exists(root)) return;
            Files.createDirectories(root);
            Files.writeString(root.resolve("readme.txt"), README, StandardCharsets.UTF_8);
            try (InputStream idx = DakiPacks.class.getResourceAsStream("/dakimakura-mod-defaults/index.txt")) {
                if (idx == null) return;
                for (String rel : new String(idx.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    rel = rel.trim();
                    if (rel.isEmpty()) continue;
                    try (InputStream in = DakiPacks.class.getResourceAsStream("/dakimakura-mod-defaults/" + rel)) {
                        if (in == null) continue;
                        Path target = root.resolve(rel);
                        Files.createDirectories(target.getParent());
                        Files.copy(in, target);
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("释放示例图案包失败", e);
        }
    }

    private DakiPacks() {}
}
