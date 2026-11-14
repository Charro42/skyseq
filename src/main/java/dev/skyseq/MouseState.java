package dev.skyseq;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which mouse buttons are held. Minecraft 26.x does not expose Mouse 4/5 state, so
 * MouseHandlerMixin feeds every button event in here and Engine/Capture read it.
 */
public final class MouseState {

    private static final Set<Integer> DOWN = new HashSet<>();

    private MouseState() {
    }

    /** Called from the game's input thread (the main thread in practice). */
    public static synchronized void onButton(int button, boolean pressed) {
        if (pressed) {
            DOWN.add(button);
        } else {
            DOWN.remove(button);
        }
    }

    public static synchronized boolean isDown(int button) {
        return DOWN.contains(button);
    }

    /** First held button other than left/right, or -1. */
    public static synchronized int firstExtraButton(int left, int right) {
        for (int b : DOWN) {
            if (b != left && b != right) {
                return b;
            }
        }
        return -1;
    }

    /** Copy of the buttons currently held. */
    public static synchronized Set<Integer> snapshot() {
        return new HashSet<>(DOWN);
    }

    public static synchronized boolean anyDown() {
        return !DOWN.isEmpty();
    }

    public static synchronized void clear() {
        DOWN.clear();
    }
}
