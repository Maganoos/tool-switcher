package eu.magkari.mc.tool_switcher;

import eu.magkari.mc.tool_switcher.config.TSConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
//? if >=1.21 {
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import java.util.Set;
//?}

import java.util.Map;

public class ToolSwitcher implements ClientModInitializer {
    public static final String MOD_ID = "tool-switcher";
    public static int previousSlot = -1;

    public static final Map<TagKey<Block>, TagKey<Item>> TOOL_MAP = Map.of(
            //? if >=1.21.5 {
            /*BlockTags.SWORD_INSTANTLY_MINES, ItemTags.SWORDS,
            *///?}
            BlockTags.MINEABLE_WITH_PICKAXE, ItemTags.PICKAXES,
            BlockTags.MINEABLE_WITH_AXE, ItemTags.AXES,
            BlockTags.MINEABLE_WITH_SHOVEL, ItemTags.SHOVELS,
            BlockTags.MINEABLE_WITH_HOE, ItemTags.HOES,
            BlockTags.SWORD_EFFICIENT, ItemTags.SWORDS
    );

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(MOD_ID, TSConfig.class);

        final KeyMapping toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key." + MOD_ID + ".toggle",
                GLFW.GLFW_KEY_PERIOD,
                //? if >= 1.21.9 {
                /*KeyMapping.Category.MISC
                *///?} else {
                "key.category.minecraft.misc"
                //?}
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                TSConfig.enabled = !TSConfig.enabled;
                MidnightConfig.write(MOD_ID);

                if (TSConfig.showMessage) {
                    client.getChatListener().handleSystemMessage(
                            Component.translatable(MOD_ID + ".msg.state").withStyle(ChatFormatting.GOLD)
                                    .append(Component.translatable(TSConfig.enabled ? "options.on" : "options.off")
                                            .withStyle(TSConfig.enabled ? ChatFormatting.GREEN : ChatFormatting.RED)),
                            true
                    );
                }
            }

            if (!TSConfig.enabled || client.player == null || !TSConfig.goBack) return;

            if (!client.options.keyAttack.isDown() && previousSlot != -1 && TSConfig.enabled) {
                setSelectedSlot(client.player.getInventory(), previousSlot);
                previousSlot = -1;
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, blockPos, direction) -> {
            if (!TSConfig.enabled || (TSConfig.sneaking && player.isShiftKeyDown())) return InteractionResult.PASS;

            final Minecraft client = Minecraft.getInstance();
            if (client.level == null) return InteractionResult.PASS;

            final BlockState blockState = client.level.getBlockState(blockPos);

            if (TSConfig.stateIsDisabled(blockState)) return InteractionResult.PASS;

            TOOL_MAP.entrySet().stream()
                    .filter(entry -> blockState.is(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .ifPresent(toolTag -> switchTool(toolTag, blockState, client));
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!TSConfig.enabled || (!TSConfig.sneaking && player.isShiftKeyDown())) return;
            if (TSConfig.goBack && !Minecraft.getInstance().options.keyAttack.isDown()) setSelectedSlot(player.getInventory(), previousSlot);

            final Minecraft client = Minecraft.getInstance();
            final HitResult hitResult = client.hitResult;
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
    private static void findSwitchTool(final BlockState state, final Minecraft client) {
        TOOL_MAP.entrySet().stream()
                .filter(entry -> state.is(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .ifPresent(toolTag -> switchTool(toolTag, state, client));
    }

    private static void setSelectedSlot(Inventory inventory, int slot) {
        //? if >=1.21.5 {
        /*inventory.setSelectedSlot(slot);
         *///?} else {
        inventory.selected = slot;
        //?}
    }


    /**
     * Selects the optimal tool in the hotbar based on block state and enchantments.
     *
     * @param toolTag Target tool type tag
     * @param state   Block state being mined
     * @param client  Minecraft client instance
     */
    public static void switchTool(final TagKey<Item> toolTag, final BlockState state, final Minecraft client) {
        if ((client.gameMode != null && client.gameMode.getPlayerMode() != GameType.SURVIVAL)
                || client.getConnection() == null
                || client.player == null) {
            return;
        }

        final var inventory = client.player.getInventory();
        //? if >=1.21.5 {
        /*final int currentSlot = inventory.getSelectedSlot();
        *///?} else {
        final int currentSlot = inventory.selected;
        //?}
        final var currentStack = inventory.getItem(currentSlot);

        float bestEfficiency = -1f;
        int bestSlot = -1;
        //? if >=1.21 {
        final var enchantmentRegistry = client.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        final Holder<Enchantment> efficiencyEnchantment = enchantmentRegistry.getOrThrow(Enchantments.EFFICIENCY);
        final Holder<Enchantment> silkEnchantment = enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH);
        final Holder<Enchantment> fortuneEnchantment = enchantmentRegistry.getOrThrow(Enchantments.FORTUNE);
        //?} else {
        /*final Enchantment efficiencyEnchantment = Enchantments.BLOCK_EFFICIENCY;
        final Enchantment fortuneEnchantment = Enchantments.BLOCK_FORTUNE;
        *///?}

        if (currentStack.is(toolTag)) {
            bestEfficiency = currentStack.getDestroySpeed(state);
            final int currentEfficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiencyEnchantment, currentStack);

            if (currentEfficiencyLevel > 0) {
                bestEfficiency += currentEfficiencyLevel * bestEfficiency * 0.3f;
            }
            bestSlot = currentSlot;
            if (TSConfig.respectSilkFortune) {
                //? if >=1.21.1 {
                final Set<Holder<Enchantment>> enchants = currentStack.getEnchantments().keySet();
                if (enchants.contains(silkEnchantment) || enchants.contains(fortuneEnchantment)) return;
                //?} else {
                /*if (EnchantmentHelper.hasSilkTouch(currentStack) || EnchantmentHelper.getItemEnchantmentLevel(fortuneEnchantment, currentStack) > 0) return;
                *///?}
            }
        }

        for (int i = 0; i < 9; i++) {
            final var stack = inventory.getItem(i);
            if (!stack.is(toolTag) || TSConfig.stackIsDisabled(stack)) continue;

            float efficiency = stack.getDestroySpeed(state);
            final int efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiencyEnchantment, stack);

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
            setSelectedSlot(inventory, bestSlot);
        }
    }
}