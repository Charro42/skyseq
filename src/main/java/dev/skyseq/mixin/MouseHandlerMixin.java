package dev.skyseq.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.skyseq.MouseState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read-only listener: records mouse button presses/releases. Never cancels or changes anything. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"))
    private void skyseq$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (action == InputConstants.PRESS) {
            MouseState.onButton(info.button(), true);
        } else if (action == InputConstants.RELEASE) {
            MouseState.onButton(info.button(), false);
        }
    }
}
