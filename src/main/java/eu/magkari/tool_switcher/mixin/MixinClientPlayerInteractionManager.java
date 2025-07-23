package eu.magkari.tool_switcher.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import static eu.magkari.tool_switcher.ToolSwitcher.toggled;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

	@Inject(method = "attackBlock", at = @At("HEAD"))
	private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (toggled) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null) return;

			BlockState state = client.world.getBlockState(pos);

			TagKey<Item> toolTag = null;
			if (state.isIn(BlockTags.PICKAXE_MINEABLE)) {
				toolTag = ItemTags.PICKAXES;
			} else if (state.isIn(BlockTags.AXE_MINEABLE)) {
				toolTag = ItemTags.AXES;
			} else if (state.isIn(BlockTags.SHOVEL_MINEABLE)) {
				toolTag = ItemTags.SHOVELS;
			} else if (state.isIn(BlockTags.HOE_MINEABLE)) {
				toolTag = ItemTags.HOES;
			}
			if (toolTag != null) {
				switchTool(toolTag, state);
			}
		}
	}

	@Unique
	private void switchTool(TagKey<Item> toolTag, BlockState state) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null && !client.player.getAbilities().creativeMode) return;

		int bestSlot = -1;
		int bestEfficiency = -1;

		for (int i = 0; i < 9; i++) {
			if (client.player.getInventory().getStack(i).isIn(toolTag)) {
				int efficiency = (int) client.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
				if (efficiency > bestEfficiency) {
					bestEfficiency = efficiency;
					bestSlot = i;
				}
			}
		}

		if (bestSlot != -1) {
			client.player.getInventory().setSelectedSlot(bestSlot);
		}
	}
}

