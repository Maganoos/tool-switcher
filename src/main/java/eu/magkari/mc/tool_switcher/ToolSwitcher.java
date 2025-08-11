package eu.magkari.mc.tool_switcher;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class ToolSwitcher implements ClientModInitializer {
    public static boolean toggled = true;

    @Override
	public void onInitializeClient() {
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tool-switcher.toggle",
                GLFW.GLFW_KEY_PERIOD,
                "key.categories.misc"
        ));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				toggled = !toggled;
			}
		});
	}
}