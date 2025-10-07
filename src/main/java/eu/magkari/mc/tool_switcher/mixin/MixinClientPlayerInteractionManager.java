package eu.magkari.mc.tool_switcher.mixin;

import eu.magkari.mc.tool_switcher.ToolSwitcher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
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
	private void ToolSwitcher$onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (!ToolSwitcher.toggled) {
			return;
		}

		final MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}

		final BlockState blockState = client.world.getBlockState(pos);

		TOOL_MAP.entrySet().stream()
				.filter(entry -> blockState.isIn(entry.getKey()))
				.map(Map.Entry::getValue)
				.findFirst()
				.ifPresent(toolTag -> switchTool(toolTag, blockState, client));
	}

	@Inject(method = "breakBlock", at = @At("TAIL"))
	private void ToolSwitcher$onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!ToolSwitcher.toggled) return;

		final MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) return;

		final var hitResult = client.crosshairTarget;
		if (hitResult == null || hitResult.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) return;

		final var blockHitResult = (net.minecraft.util.hit.BlockHitResult) hitResult;
		final BlockPos targetPos = blockHitResult.getBlockPos();
		final BlockState targetState = client.world.getBlockState(targetPos);

		TOOL_MAP.entrySet().stream()
				.filter(entry -> targetState.isIn(entry.getKey()))
				.map(Map.Entry::getValue)
				.findFirst()
				.ifPresent(toolTag -> switchTool(toolTag, targetState, client));
	}

	/**
	 * Selects the optimal tool in the hotbar based on block state and enchantments.
	 *
	 * @param toolTag Target tool type tag
	 * @param state   Block state being mined
	 * @param client  Minecraft client instance
	 */
	@Unique
	private void switchTool(TagKey<Item> toolTag, BlockState state, MinecraftClient client) {
		if (client.player == null) {
			throw new IllegalStateException("Cannot switch tools: MinecraftClient.player is null.");
		}
		if (client.getNetworkHandler() == null) {
			throw new IllegalStateException("Cannot switch tools: MinecraftClient network handler is null.");
		}
		if (Objects.requireNonNull(client.interactionManager).getCurrentGameMode() != GameMode.SURVIVAL) {
			return;
		}

		final var inventory = client.player.getInventory();
		final int currentSlot = inventory.getSelectedSlot();
		final var currentStack = inventory.getStack(currentSlot);

		float bestEfficiency = -1f;
		int bestSlot = -1;

		final RegistryEntry<Enchantment> efficiencyEnchantment = client.getNetworkHandler()
				.getRegistryManager()
				.getOrThrow(RegistryKeys.ENCHANTMENT)
				.getOrThrow(Enchantments.EFFICIENCY);

		if (currentStack.isIn(toolTag)) {
			bestEfficiency = currentStack.getMiningSpeedMultiplier(state);
			final int currentEfficiencyLevel = EnchantmentHelper.getLevel(efficiencyEnchantment, currentStack);

			if (currentEfficiencyLevel > 0) {
				bestEfficiency += currentEfficiencyLevel * bestEfficiency * 0.3f;
			}
			bestSlot = currentSlot;
		}

		for (int i = 0; i < 9; i++) {
			final var stack = inventory.getStack(i);
			if (!stack.isIn(toolTag)) {
				continue;
			}

			float efficiency = stack.getMiningSpeedMultiplier(state);
			final int efficiencyLevel = EnchantmentHelper.getLevel(efficiencyEnchantment, stack);

			if (efficiencyLevel > 0) {
				efficiency += efficiencyLevel * efficiency * 0.3f;
			}

			if (efficiency > bestEfficiency) {
				bestEfficiency = efficiency;
				bestSlot = i;
			}
		}

		if (bestSlot != currentSlot && bestSlot != -1) {
			ToolSwitcher.previousSlot = currentSlot;
			inventory.setSelectedSlot(bestSlot);
		}
	}
}
