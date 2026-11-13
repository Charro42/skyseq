package dev.skyseq;

import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;

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

    static void poll(long window) {
        Consumer<int[]> cb = callback;
        if (cb == null) {
            return;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            callback = null;
            cb.accept(null);
            return;
        }

        int foundKey = -1;
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            if (k != GLFW.GLFW_KEY_ESCAPE && GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS) {
                foundKey = k;
                break;
            }
        }
        int foundMouse = -1;
        boolean leftOrRightDown = false;
        for (int b = 0; b <= GLFW.GLFW_MOUSE_BUTTON_LAST; b++) {
            if (GLFW.glfwGetMouseButton(window, b) == GLFW.GLFW_PRESS) {
                if (b <= 1) {
                    leftOrRightDown = true;
                } else if (foundMouse < 0) {
                    foundMouse = b;
                }
            }
        }

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
