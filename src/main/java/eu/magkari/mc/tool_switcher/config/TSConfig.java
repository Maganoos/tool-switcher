package eu.magkari.mc.tool_switcher.config;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class TSConfig extends MidnightConfig {
    @Entry public static boolean enabled = true;
    @Entry public static boolean sneaking = true;
    @Entry public static boolean showMessage = true;
    @Entry public static boolean goBack = true;
    @Entry public static boolean respectSilkFortune = true;
    @Entry(idMode = 0) public static List<ResourceLocation> disabledTools = new ArrayList<>();
    @Entry(idMode = 1) public static List<ResourceLocation> disabledBlocks = new ArrayList<>();

    public static boolean stackIsDisabled(ItemStack stack) {
        return disabledTools.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
    public static boolean stateIsDisabled(BlockState state) {
        return disabledBlocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}
