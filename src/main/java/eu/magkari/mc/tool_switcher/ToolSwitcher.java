package eu.magkari.mc.tool_switcher;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Objects;

public class ToolSwitcher implements ClientModInitializer {
    public static boolean toggled = true;
	public static int previousSlot = -1;
    public boolean wasAttacking = false;

    public static final Map<TagKey<Block>, TagKey<Item>> TOOL_MAP = Map.of(
            BlockTags.PICKAXE_MINEABLE, ItemTags.PICKAXES,
            BlockTags.AXE_MINEABLE, ItemTags.AXES,
            BlockTags.SHOVEL_MINEABLE, ItemTags.SHOVELS,
            BlockTags.HOE_MINEABLE, ItemTags.HOES
    );

    @Override
	public void onInitializeClient() {
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tool-switcher.toggle",
                GLFW.GLFW_KEY_PERIOD,
                KeyBinding.Category.MISC
        ));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				toggled = !toggled;
			}
            if (!toggled) return;

            if (client.player == null) return;

            KeyBinding attackKey = client.options.attackKey;
            boolean isAttacking = attackKey.isPressed();

            if (wasAttacking && !isAttacking) {
                if (ToolSwitcher.previousSlot != -1) {
                    client.player.getInventory().setSelectedSlot(ToolSwitcher.previousSlot);
                    ToolSwitcher.previousSlot = -1;
                }
            }

            wasAttacking = isAttacking;
		});

        AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
            if (!ToolSwitcher.toggled) {
                return ActionResult.PASS;
            }

            final MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return ActionResult.PASS;
            }

            final BlockState blockState = client.world.getBlockState(blockPos);

            TOOL_MAP.entrySet().stream()
                    .filter(entry -> blockState.isIn(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .ifPresent(toolTag -> switchTool(toolTag, blockState, client));
            return ActionResult.PASS;
        });
	}

    /**
     * Selects the optimal tool in the hotbar based on block state and enchantments.
     *
     * @param toolTag Target tool type tag
     * @param state   Block state being mined
     * @param client  Minecraft client instance
     */
    public static void switchTool(TagKey<Item> toolTag, BlockState state, MinecraftClient client) {
        if (Objects.requireNonNull(client.interactionManager).getCurrentGameMode() != GameMode.SURVIVAL || client.getNetworkHandler() == null || client.player == null) {
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
            previousSlot = currentSlot;
            inventory.setSelectedSlot(bestSlot);
        }
    }


}