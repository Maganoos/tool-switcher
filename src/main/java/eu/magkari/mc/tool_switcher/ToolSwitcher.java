package eu.magkari.mc.tool_switcher;

import eu.magkari.mc.tool_switcher.config.TSConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Set;

public final class ToolSwitcher implements ClientModInitializer {
    public static final String MOD_ID = "tool-switcher";
    public static int previousSlot = -1;

    public static final Map<TagKey<Block>, TagKey<Item>> TOOL_MAP = Map.of(
            BlockTags.PICKAXE_MINEABLE, ItemTags.PICKAXES,
            BlockTags.AXE_MINEABLE, ItemTags.AXES,
            BlockTags.SHOVEL_MINEABLE, ItemTags.SHOVELS,
            BlockTags.HOE_MINEABLE, ItemTags.HOES,
            BlockTags.SWORD_INSTANTLY_MINES, ItemTags.SWORDS
    );

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(MOD_ID, TSConfig.class);

        final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".toggle",
                GLFW.GLFW_KEY_PERIOD,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                TSConfig.enabled = !TSConfig.enabled;
                MidnightConfig.write(MOD_ID);
                if (TSConfig.showMessage) {
                    client.getMessageHandler().onGameMessage(
                            Text.translatable(MOD_ID + ".msg.state").formatted(Formatting.GOLD)
                                    .append(Text.translatable(TSConfig.enabled ? "options.on" : "options.off")
                                            .formatted(TSConfig.enabled ? Formatting.GREEN : Formatting.RED)),
                            true
                    );
                }
            }

            if (!TSConfig.enabled || client.player == null || !TSConfig.goBack) return;

            while (client.options.attackKey.wasPressed()) {
                if (ToolSwitcher.previousSlot != -1) {
                    client.player.getInventory().setSelectedSlot(ToolSwitcher.previousSlot);
                    ToolSwitcher.previousSlot = -1;
                }
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, blockPos, direction) -> {
            if (!TSConfig.enabled || (!TSConfig.sneaking || player.isSneaking())) return ActionResult.PASS;

            final MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return ActionResult.PASS;

            final BlockState blockState = client.world.getBlockState(blockPos);

            if (TSConfig.stateIsDisabled(blockState)) return ActionResult.PASS;

            TOOL_MAP.entrySet().stream()
                    .filter(entry -> blockState.isIn(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .ifPresent(toolTag -> switchTool(toolTag, blockState, client));
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!TSConfig.enabled || (!TSConfig.sneaking || player.isSneaking())) return;

            final MinecraftClient client = MinecraftClient.getInstance();
            final HitResult hitResult = client.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

            final BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
            final BlockState targetState = world.getBlockState(targetPos);

            if (TSConfig.stateIsDisabled(targetState)) return;

            findSwitchTool(targetState, client);
        });
    }

    /**
     * Finds the matching tool tag for a given block state and switches to the best tool.
     *
     * @param state  The block state being interacted with
     * @param client The Minecraft client instance
     */
    private static void findSwitchTool(final BlockState state, final MinecraftClient client) {
        TOOL_MAP.entrySet().stream()
                .filter(entry -> state.isIn(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .ifPresent(toolTag -> switchTool(toolTag, state, client));
    }


    /**
     * Selects the optimal tool in the hotbar based on block state and enchantments.
     *
     * @param toolTag Target tool type tag
     * @param state   Block state being mined
     * @param client  Minecraft client instance
     */
    public static void switchTool(final TagKey<Item> toolTag, final BlockState state, final MinecraftClient client) {
        if ((client.interactionManager != null && client.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL)
                || client.getNetworkHandler() == null
                || client.player == null) {
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
        final RegistryEntry<Enchantment> silkEnchantment = client.getNetworkHandler()
                .getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        final RegistryEntry<Enchantment> fortuneEnchantment = client.getNetworkHandler()
                .getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE);

        if (currentStack.isIn(toolTag)) {
            bestEfficiency = currentStack.getMiningSpeedMultiplier(state);
            final int currentEfficiencyLevel = EnchantmentHelper.getLevel(efficiencyEnchantment, currentStack);

            if (currentEfficiencyLevel > 0) {
                bestEfficiency += currentEfficiencyLevel * bestEfficiency * 0.3f;
            }
            bestSlot = currentSlot;
            if (TSConfig.respectSilkFortune) {
                final Set<RegistryEntry<Enchantment>> enchants = currentStack.getEnchantments().getEnchantments();
                if (enchants.contains(silkEnchantment) || enchants.contains(fortuneEnchantment)) return;
            }
        }

        for (int i = 0; i < 9; i++) {
            final var stack = inventory.getStack(i);
            if (!stack.isIn(toolTag) || TSConfig.stackIsDisabled(stack)) continue;

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
