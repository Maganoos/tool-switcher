package eu.magkari.mc.tool_switcher.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Objects;

import static eu.magkari.mc.tool_switcher.ToolSwitcher.toggled;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
	@Unique
	private static final Map<TagKey<Block>, TagKey<Item>> TOOL_MAP = Map.of(
			BlockTags.PICKAXE_MINEABLE, ItemTags.PICKAXES,
			BlockTags.AXE_MINEABLE, ItemTags.AXES,
			BlockTags.SHOVEL_MINEABLE, ItemTags.SHOVELS,
			BlockTags.HOE_MINEABLE, ItemTags.HOES
	);

	@Inject(method = "attackBlock", at = @At("HEAD"))
	private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (toggled) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null) return;

			BlockState state = client.world.getBlockState(pos);

			TOOL_MAP.entrySet().stream()
					.filter(entry -> state.isIn(entry.getKey()))
					.map(Map.Entry::getValue)
					.findFirst().ifPresent(toolTag -> switchTool(toolTag, state, client, pos));
		}
	}

	@Unique
	private void switchTool(TagKey<Item> toolTag, BlockState state, MinecraftClient client, BlockPos pos) {
		if (client.player == null || Objects.requireNonNull(client.interactionManager).getCurrentGameMode() != GameMode.SURVIVAL)
			return;
		var inventory = client.player.getInventory();

		int bestSlot = -1;
		float bestEfficiency = -1;

		int oldSlot = inventory.getSelectedSlot();
		for (int i = 0; i < 9; i++) {
			var stack = inventory.getStack(i);
			if (!stack.isIn(toolTag)) continue;
			inventory.setSelectedSlot(i);
			var efficiency = stack.getMiningSpeedMultiplier(state);
			if (efficiency > bestEfficiency) {
				bestEfficiency = efficiency;
				bestSlot = i;
			}
		}
		inventory.setSelectedSlot(bestSlot);
	}
}
