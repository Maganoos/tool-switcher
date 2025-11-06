package eu.magkari.mc.tool_switcher.config;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TSConfig extends MidnightConfig {
    @Entry() public static boolean enabled = true;
    @Entry() public static boolean sneaking = true;
    @Entry() public static boolean showMessage = true;
    @Entry() public static boolean goBack = true;
    @Entry() public static boolean respectSilkFortune = true;
    @Entry(idMode = 0) public static List<Identifier> disabledTools = new ArrayList<>();
    @Entry(idMode = 1) public static List<Identifier> disabledBlocks = new ArrayList<>();

    public static boolean stackIsDisabled(ItemStack stack) {
        return disabledTools.contains(Registries.ITEM.getId(stack.getItem()));
    }
    public static boolean stateIsDisabled(BlockState state) {
        return disabledBlocks.contains(Registries.BLOCK.getId(state.getBlock()));
    }
}
