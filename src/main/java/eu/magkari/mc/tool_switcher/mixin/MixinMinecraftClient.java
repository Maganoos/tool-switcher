package eu.magkari.mc.tool_switcher.mixin;

import eu.magkari.mc.tool_switcher.ToolSwitcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static eu.magkari.mc.tool_switcher.ToolSwitcher.toggled;
import static eu.magkari.mc.tool_switcher.ToolSwitcher.previousSlot;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Unique
    private boolean wasAttacking = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void ToolSwitcher$onClientTick(CallbackInfo ci) {
        if (!toggled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        KeyBinding attackKey = client.options.attackKey;
        boolean isAttacking = attackKey.isPressed();

        if (wasAttacking && !isAttacking) {
            revertToPreviousSlot(client.player);
        }

        wasAttacking = isAttacking;
    }

    @Unique
    private void revertToPreviousSlot(ClientPlayerEntity player) {
        if (previousSlot != -1) {
            player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
        }
    }
}