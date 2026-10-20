package com.bodypillow;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 空白抱枕 + 已解锁的图案 -> 印好图案的抱枕（两者各消耗 1 个）。 */
public class PillowPrintRecipe extends CustomRecipe {
    public PillowPrintRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int pillows = 0, designs = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (ModContent.isBlankPillow(s)) pillows++;
            else if (ModContent.isBlueprint(s)) designs++;
            else return false;
        }
        return pillows == 1 && designs == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (ModContent.isBlueprint(s)) return ModContent.pillow(ModContent.patternOf(s));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModContent.PRINT_SERIALIZER;
    }
}
