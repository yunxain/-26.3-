package com.bodypillow;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 两张不要的图案图纸 -> 一个新的"随机图案"（右键揭晓）。 */
public class PatternRecycleRecipe extends CustomRecipe {
    public PatternRecycleRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (!ModContent.isBlueprint(s)) return false;
            count++;
        }
        return count == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(ModContent.RANDOM_DESIGN);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModContent.RECYCLE_SERIALIZER;
    }
}
