package com.bodypillow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.bodypillow.BodyPillowMod.id;

public final class ModContent {
    /** 物品上的抱枕 ID（见 DakiPacks）；空 / 无 = 空白 */
    public static final DataComponentType<String> PATTERN = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, id("pattern"),
            DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** 是否翻到背面 */
    public static final DataComponentType<Boolean> FLIPPED = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, id("flipped"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final Item BODY_PILLOW = Registry.register(BuiltInRegistries.ITEM, id("body_pillow"),
            new PillowItem(props("body_pillow").stacksTo(16)
                    .component(DataComponents.LORE, simpleLore("tooltip.bodypillow.hug", "tooltip.bodypillow.blank"))));

    /** 已解锁的图案（带具体抱枕 ID） */
    public static final Item PATTERN_BLUEPRINT = Registry.register(BuiltInRegistries.ITEM, id("pattern_blueprint"),
            new Item(props("pattern_blueprint").stacksTo(16)
                    .component(DataComponents.LORE, simpleLore("tooltip.bodypillow.design_usage"))));

    /** 未解锁的图案：右键解锁成随机的一张 */
    public static final Item RANDOM_DESIGN = Registry.register(BuiltInRegistries.ITEM, id("random_design"),
            new RandomDesignItem(props("random_design").stacksTo(64)
                    .component(DataComponents.LORE, simpleLore("tooltip.bodypillow.design_unlock"))));

    public static final EntityType<PillowEntity> PILLOW_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("body_pillow"),
            EntityType.Builder.<PillowEntity>of(PillowEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.2f)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("body_pillow"))));

    public static final RecipeSerializer<PillowPrintRecipe> PRINT_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, id("pillow_print"), simple(PillowPrintRecipe::new));
    public static final RecipeSerializer<PillowClearRecipe> CLEAR_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, id("pillow_clear"), simple(PillowClearRecipe::new));
    public static final RecipeSerializer<PatternRecycleRecipe> RECYCLE_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, id("pattern_recycle"), simple(PatternRecycleRecipe::new));
    public static final RecipeSerializer<PillowAltRecipe> ALT_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, id("pillow_alt"), simple(PillowAltRecipe::new));

    /** 无参数的特殊合成配方：JSON 里只需要 {"type": "bodypillow:xxx"} */
    private static <T extends CraftingRecipe> RecipeSerializer<T> simple(Supplier<T> factory) {
        return new RecipeSerializer<>(
                MapCodec.unit(factory),
                StreamCodec.<RegistryFriendlyByteBuf, T>unit(factory.get()));
    }

    private static Item.Properties props(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    private static ItemLore simpleLore(String... keys) {
        List<Component> lines = new ArrayList<>();
        for (String k : keys) lines.add(Component.translatable(k));
        return new ItemLore(lines);
    }

    /** 创造模式里的专属标签页：空白抱枕、未解锁图案，以及所有图案包里的每一张设计图 */
    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), id("designs"));

    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(BODY_PILLOW))
                    .title(Component.translatable("creativeTab.bodypillow"))
                    .displayItems((params, output) -> {
                        output.accept(new ItemStack(BODY_PILLOW));
                        output.accept(new ItemStack(RANDOM_DESIGN));
                        for (DakiPacks.Daki d : DakiPacks.list()) {
                            output.accept(blueprint(d.id()));
                        }
                    })
                    .build());

    /** 调用一次即可触发本类的静态注册 */
    public static void init() {}

    // ---------- 工具方法 ----------
    public static String patternOf(ItemStack s) {
        return s.getOrDefault(PATTERN, "");
    }

    public static boolean flippedOf(ItemStack s) {
        return s.getOrDefault(FLIPPED, false);
    }

    private static ItemLore describe(DakiPacks.Daki d, boolean flipHint) {
        List<Component> l = new ArrayList<>();
        if (flipHint) l.add(Component.translatable("tooltip.bodypillow.flip"));
        if (d != null) {
            l.add(Component.translatable("tooltip.bodypillow.pack", d.packName(), d.index(), d.total()));
            l.add(Component.translatable("tooltip.bodypillow.name", d.displayName()));
            if (!d.author().isBlank()) l.add(Component.translatable("tooltip.bodypillow.author", d.author()));
            if (!d.flavour().isBlank()) l.add(Component.literal(d.flavour()));
        }
        return new ItemLore(l);
    }

    /** 空白抱枕（id 为空）或印好图案的抱枕 */
    public static ItemStack pillow(String id) {
        return pillow(id, false);
    }

    public static ItemStack pillow(String id, boolean flipped) {
        ItemStack s = new ItemStack(BODY_PILLOW);
        if (id.isEmpty()) return s;
        DakiPacks.Daki d = DakiPacks.get(id);
        s.set(PATTERN, id);
        if (flipped) s.set(FLIPPED, true);
        s.set(DataComponents.ITEM_NAME,
                Component.translatable("item.bodypillow.body_pillow_named", d != null ? d.displayName() : id));
        s.set(DataComponents.LORE, describe(d, true));
        return s;
    }

    /** 已解锁的图案 */
    public static ItemStack blueprint(String id) {
        ItemStack s = new ItemStack(PATTERN_BLUEPRINT);
        if (id.isEmpty()) return s;
        DakiPacks.Daki d = DakiPacks.get(id);
        s.set(PATTERN, id);
        s.set(DataComponents.ITEM_NAME,
                Component.translatable("item.bodypillow.pattern_blueprint_named", d != null ? d.displayName() : id));
        s.set(DataComponents.LORE, describe(d, false));
        return s;
    }

    public static boolean isBlankPillow(ItemStack s) {
        return s.is(BODY_PILLOW) && patternOf(s).isEmpty();
    }

    public static boolean isPrintedPillow(ItemStack s) {
        return s.is(BODY_PILLOW) && !patternOf(s).isEmpty();
    }

    /** 已解锁（带具体图案）的图案物品 */
    public static boolean isBlueprint(ItemStack s) {
        return s.is(PATTERN_BLUEPRINT) && !patternOf(s).isEmpty();
    }

    private ModContent() {}
}
