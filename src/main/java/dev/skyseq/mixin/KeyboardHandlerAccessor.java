package dev.skyseq.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets Engine feed a key press/release through the game's own keyboard handler, exactly like a
 * physical key, so menus, vanilla key mappings and other mods all see it.
 */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {

    @Invoker("keyPress")
    void skyseq$keyPress(long window, int action, KeyEvent event);
}
