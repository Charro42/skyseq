package dev.skyseq;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import org.lwjgl.glfw.GLFW;

/** Key and mouse button name handling. */
public final class Keys {

    private Keys() {
    }

    /** Parses "1", "e", "F5", "space", "enter", "esc" ... into a GLFW key code, or -1. */
    public static int parseKey(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return -1;
        }
        if (s.length() == 1) {
            char c = s.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
        }
        if (s.matches("F([1-9]|1[0-9]|2[0-5])")) {
            return GLFW.GLFW_KEY_F1 + Integer.parseInt(s.substring(1)) - 1;
        }
        return switch (s) {
            case "ESC", "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
            case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "SHIFT", "LSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "CTRL", "LCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "ALT", "LALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            default -> -1;
        };
    }

    /**
     * Parses a mouse button into a GLFW button index, or -1.
     * Numbering follows the usual "Mouse N" names: 1 = left, 2 = right, 3 = middle, 4, 5 ...
     */
    public static int parseMouse(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace("mouse", "").replace("button", "").trim();
        switch (s) {
            case "left", "l" -> {
                return 0;
            }
            case "right", "r" -> {
                return 1;
            }
            case "middle", "m" -> {
                return 2;
            }
            default -> {
            }
        }
        try {
            int n = Integer.parseInt(s);
            if (n >= 1 && n <= 8) {
                return n - 1;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return -1;
    }

    public static String mouseName(int button) {
        return switch (button) {
            case 0 -> "Left Click";
            case 1 -> "Right Click";
            case 2 -> "Middle Click";
            default -> "Mouse " + (button + 1);
        };
    }

    public static String keyName(int code) {
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
        } catch (Throwable t) {
            return "Key " + code;
        }
    }
}
