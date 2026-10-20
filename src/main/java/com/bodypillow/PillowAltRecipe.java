package com.bodypillow;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 备用配方（配置 altRecipe=true 才生效）：上下两行线、中间一行羊毛 -> 空白抱枕。 */
public class PillowAltRecipe extends CustomRecipe {
    public PillowAltRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!ModConfig.get().altRecipe) return false;
        if (input.width() != 3 || input.height() != 3) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack s = input.getItem(i);
            boolean middle = i >= 3 && i < 6;
            if (middle ? !s.is(ItemTags.WOOL) : !s.is(Items.STRING)) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return ModContent.pillow("");
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModContent.ALT_SERIALIZER;
    }
}
