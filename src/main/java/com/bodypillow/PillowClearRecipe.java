package com.bodypillow;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 单独一个已印图案的抱枕 -> 空白抱枕。 */
public class PillowClearRecipe extends CustomRecipe {
    public PillowClearRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (!ModContent.isPrintedPillow(s)) return false;
            count++;
        }
        return count == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return ModContent.pillow("");
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModContent.CLEAR_SERIALIZER;
    }
}
