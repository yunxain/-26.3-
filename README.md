# 等身抱枕 Dakimakura（Fabric · Minecraft 26.3）

构建（JDK 25）：`gradlew.bat build`，产物 build/libs/bodypillow-2.0.0+26.3.jar（不要用 -sources 那个）。
需要 Fabric Loader 0.19.5+ 和 Fabric API 0.161.0+26.3。

## 功能
- 等身抱枕实体，三种摆法：右键地面/床 = 平放；潜行+右键地面 = 立在地上；右键墙面 = 挂在墙上。左键回收（图案保留）；放置有羊毛音效
- 圆润模型：程序生成的两面鼓起、边缘捏合的抱枕网格，正反面各一张图
- 双面图案：潜行+对空气右键翻面（物品）；潜行+右键已放置的抱枕翻面
- 空白抱枕：6 个羊毛（2x3）；备用配方：上下两行线、中间一行羊毛（config/bodypillow.json: altRecipe=true）
- 空白抱枕 + 已解锁的图案 → 印上图案；单独合成带图案的抱枕 → 清除图案
- 敌对生物 1% 掉落"未解锁的图案"，右键解锁成随机的具体图案（dropChance / requirePlayerKill）
- 两张已解锁的图案合成 → 新的"未解锁的图案"
- 创造模式专属标签页"等身抱枕"：空白抱枕、未解锁图案，以及图案包里每一张设计图（新增图案包后重进世界即可刷新）
- tooltip：显示图案包名（序号/总数）、名称、作者、flavour-text
- 图案包（与原版 Dakimakura Mod 格式兼容）：`<游戏目录>/dakimakura-mod/<包>/<抱枕>/front.png|jpg、back.png|jpg、daki-info.json`
  支持文件夹和 .zip，png/jpg，数量不限，改图无需重启；详见 dakimakura-mod/readme.txt
  原版的图案包可以直接复制进 dakimakura-mod 文件夹使用

## 未实现（相对原版）
- 服务端把图案自动同步给客户端（目前联机时服务端和客户端都要有相同的图案包）
- 原版的 OBJ 模型文件（这里用程序生成的圆润网格代替）、/dakimakura 命令、方块形态
