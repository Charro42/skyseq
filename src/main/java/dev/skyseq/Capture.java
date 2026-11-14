package dev.skyseq;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Consumer;

/**
 * "Press a key or mouse button" capture for the bind button. Polled from the client tick, so the
 * config screen does not have to override any input methods.
 */
public final class Capture {

    private static Consumer<int[]> callback;
    private static boolean armed;

    private Capture() {
    }

    public static boolean active() {
        return callback != null;
    }

    /** Callback gets {1 = mouse / 0 = key, code}, or null if cancelled. */
    public static void start(Consumer<int[]> cb) {
        callback = cb;
        armed = false;
    }

    public static void cancel() {
        callback = null;
    }

    static void poll() {
        Consumer<int[]> cb = callback;
        if (cb == null) {
            return;
        }
        if (Keys.isDown(Keys.escape())) {
            callback = null;
            cb.accept(null);
            return;
        }

        int foundKey = -1;
        for (int k : Keys.allKeys()) {
            if (Keys.isDown(k)) {
                foundKey = k;
                break;
            }
        }
        int left = InputConstants.MOUSE_BUTTON_LEFT;
        int right = InputConstants.MOUSE_BUTTON_RIGHT;
        int foundMouse = MouseState.firstExtraButton(left, right);
        boolean leftOrRightDown = MouseState.isDown(left) || MouseState.isDown(right);

        // Wait until everything (including the click on the bind button) has been released first.
        if (!armed) {
            armed = foundKey < 0 && foundMouse < 0 && !leftOrRightDown;
            return;
        }
        // Left and right click are not allowed as triggers: they would fire on every click.
        if (foundKey >= 0) {
            callback = null;
            cb.accept(new int[] {0, foundKey});
        } else if (foundMouse >= 0) {
            callback = null;
            cb.accept(new int[] {1, foundMouse});
        }
    }
}
