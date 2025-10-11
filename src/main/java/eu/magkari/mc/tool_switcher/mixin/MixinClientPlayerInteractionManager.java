package eu.magkari.mc.tool_switcher.mixin;

import eu.magkari.mc.tool_switcher.ToolSwitcher;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

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

		ToolSwitcher.TOOL_MAP.entrySet().stream()
				.filter(entry -> targetState.isIn(entry.getKey()))
				.map(Map.Entry::getValue)
				.findFirst()
				.ifPresent(toolTag -> ToolSwitcher.switchTool(toolTag, targetState, client));
	}
}
