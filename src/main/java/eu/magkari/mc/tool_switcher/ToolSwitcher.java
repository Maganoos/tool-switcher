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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;
//? if >=1.21 {
import net.minecraft.core.registries.Registries;
//?}
//? if >=1.20.5 {
import net.minecraft.tags.EnchantmentTags;
//?}

import java.util.Map;

public class ToolSwitcher implements ClientModInitializer {
    public static final String MOD_ID = "tool-switcher";
    public static int previousSlot = -1;

    public static final Map<TagKey<Block>, TagKey<Item>> TOOL_MAP = Map.of(
            //? if >=1.21.5 {
            BlockTags.SWORD_INSTANTLY_MINES, ItemTags.SWORDS,
             //?}
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
                KeyMapping.Category.MISC
                 //?} else {
                /*"key.category.minecraft.misc"
                *///?}
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                TSConfig.enabled = !TSConfig.enabled;
                MidnightConfig.write(MOD_ID);

                if (TSConfig.showMessage) client.getChatListener().handleSystemMessage(
                        Component.translatable(MOD_ID + ".msg.state").withStyle(ChatFormatting.GOLD)
                                .append(Component.translatable(TSConfig.enabled ? "options.on" : "options.off")
                                        .withStyle(TSConfig.enabled ? ChatFormatting.GREEN : ChatFormatting.RED)),
                        true
                );
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

            findAndSwitchTool(blockState, client);
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!TSConfig.enabled || (!TSConfig.sneaking && player.isShiftKeyDown())) return;
            if (TSConfig.goBack && !Minecraft.getInstance().options.keyAttack.isDown())
                setSelectedSlot(player.getInventory(), previousSlot);

            final Minecraft client = Minecraft.getInstance();
            final HitResult hitResult = client.hitResult;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

            final BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
            final BlockState targetState = world.getBlockState(targetPos);

            if (TSConfig.stateIsDisabled(targetState)) return;

            findAndSwitchTool(targetState, client);
        });
    }

    /**
     * Finds the matching tool tag for a given block state and switches to the best tool.
     *
     * @param state  The block state being interacted with
     * @param client The Minecraft client instance
     */
    private static void findAndSwitchTool(final BlockState state, final Minecraft client) {
        TOOL_MAP.entrySet().stream()
                .filter(entry -> state.is(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .ifPresent(toolTag -> switchTool(toolTag, state, client));
    }

    private static void setSelectedSlot(Inventory inventory, final int slot) {
        //? if >=1.21.5 {
        inventory.setSelectedSlot(slot);
         //?} else {
        /*inventory.selected = slot;
        *///?}
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
        final int currentSlot = inventory.getSelectedSlot();
         //?} else {
        /*final int currentSlot = inventory.selected;
        *///?}
        final var currentStack = inventory.getItem(currentSlot);

        float bestEfficiency = -1f;
        int bestSlot = -1;
        //? if >=1.21 {
        final var efficiencyEnchantment = client.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
        //?} else {
        /*final var efficiencyEnchantment = Enchantments.BLOCK_EFFICIENCY;
         *///?}

        if (currentStack.is(toolTag)) {
            if (TSConfig.respectSilkFortune) {
                //? if >=1.20.5 {
                if (EnchantmentHelper.hasTag(currentStack, EnchantmentTags.MINING_EXCLUSIVE)) return;
                //?} else {
                /*if (EnchantmentHelper.hasSilkTouch(currentStack) || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, currentStack) > 0) return;
                 *///?}
            }
            bestEfficiency = currentStack.getDestroySpeed(state);
            final int efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiencyEnchantment, currentStack);

            if (efficiencyLevel > 0) {
                bestEfficiency += efficiencyLevel * bestEfficiency * 0.3f;
            }
            bestSlot = currentSlot;
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