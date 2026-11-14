package dev.skyseq;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * Key and mouse button names. Everything goes through Minecraft's own InputConstants so we never assume
 * what number a key has (Minecraft 26.x no longer uses GLFW).
 */
public final class Keys {

    /** Name (upper case) -> key code. Also defines which keys can be captured/used as triggers. */
    private static final Map<String, Integer> KEYS = new LinkedHashMap<>();

    static {
        int[] letters = {
            InputConstants.KEY_A, InputConstants.KEY_B, InputConstants.KEY_C, InputConstants.KEY_D, InputConstants.KEY_E,
            InputConstants.KEY_F, InputConstants.KEY_G, InputConstants.KEY_H, InputConstants.KEY_I, InputConstants.KEY_J,
            InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_M, InputConstants.KEY_N, InputConstants.KEY_O,
            InputConstants.KEY_P, InputConstants.KEY_Q, InputConstants.KEY_R, InputConstants.KEY_S, InputConstants.KEY_T,
            InputConstants.KEY_U, InputConstants.KEY_V, InputConstants.KEY_W, InputConstants.KEY_X, InputConstants.KEY_Y,
            InputConstants.KEY_Z
        };
        for (int i = 0; i < letters.length; i++) {
            KEYS.put(String.valueOf((char) ('A' + i)), letters[i]);
        }
        int[] digits = {
            InputConstants.KEY_0, InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3, InputConstants.KEY_4,
            InputConstants.KEY_5, InputConstants.KEY_6, InputConstants.KEY_7, InputConstants.KEY_8, InputConstants.KEY_9
        };
        for (int i = 0; i < digits.length; i++) {
            KEYS.put(String.valueOf(i), digits[i]);
        }
        int[] fkeys = {
            InputConstants.KEY_F1, InputConstants.KEY_F2, InputConstants.KEY_F3, InputConstants.KEY_F4,
            InputConstants.KEY_F5, InputConstants.KEY_F6, InputConstants.KEY_F7, InputConstants.KEY_F8,
            InputConstants.KEY_F9, InputConstants.KEY_F10, InputConstants.KEY_F11, InputConstants.KEY_F12
        };
        for (int i = 0; i < fkeys.length; i++) {
            KEYS.put("F" + (i + 1), fkeys[i]);
        }
        int[] pad = {
            InputConstants.KEY_NUMPAD0, InputConstants.KEY_NUMPAD1, InputConstants.KEY_NUMPAD2, InputConstants.KEY_NUMPAD3,
            InputConstants.KEY_NUMPAD4, InputConstants.KEY_NUMPAD5, InputConstants.KEY_NUMPAD6, InputConstants.KEY_NUMPAD7,
            InputConstants.KEY_NUMPAD8, InputConstants.KEY_NUMPAD9
        };
        for (int i = 0; i < pad.length; i++) {
            KEYS.put("NUM" + i, pad[i]);
        }
        KEYS.put("SPACE", InputConstants.KEY_SPACE);
        KEYS.put("TAB", InputConstants.KEY_TAB);
        KEYS.put("ENTER", InputConstants.KEY_RETURN);
        KEYS.put("BACKSPACE", InputConstants.KEY_BACKSPACE);
        KEYS.put("UP", InputConstants.KEY_UP);
        KEYS.put("DOWN", InputConstants.KEY_DOWN);
        KEYS.put("LEFT", InputConstants.KEY_LEFT);
        KEYS.put("RIGHT", InputConstants.KEY_RIGHT);
        KEYS.put("INSERT", InputConstants.KEY_INSERT);
        KEYS.put("DELETE", InputConstants.KEY_DELETE);
        KEYS.put("HOME", InputConstants.KEY_HOME);
        KEYS.put("END", InputConstants.KEY_END);
        KEYS.put("PAGEUP", InputConstants.KEY_PAGEUP);
        KEYS.put("PAGEDOWN", InputConstants.KEY_PAGEDOWN);
        KEYS.put("SHIFT", InputConstants.KEY_LSHIFT);
        KEYS.put("CTRL", InputConstants.KEY_LCONTROL);
        KEYS.put("ALT", InputConstants.KEY_LALT);
        KEYS.put("RSHIFT", InputConstants.KEY_RSHIFT);
        KEYS.put("RCTRL", InputConstants.KEY_RCONTROL);
        KEYS.put("RALT", InputConstants.KEY_RALT);
        KEYS.put("-", InputConstants.KEY_MINUS);
        KEYS.put("=", InputConstants.KEY_EQUALS);
        KEYS.put(",", InputConstants.KEY_COMMA);
        KEYS.put(".", InputConstants.KEY_PERIOD);
        KEYS.put("/", InputConstants.KEY_SLASH);
        KEYS.put(";", InputConstants.KEY_SEMICOLON);
        KEYS.put("'", InputConstants.KEY_APOSTROPHE);
        KEYS.put("[", InputConstants.KEY_LBRACKET);
        KEYS.put("]", InputConstants.KEY_RBRACKET);
        KEYS.put("\\", InputConstants.KEY_BACKSLASH);
        KEYS.put("`", InputConstants.KEY_GRAVE);
    }

    private Keys() {
    }

    /** Is this key currently held down? */
    public static boolean isDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }

    /** All key codes that can be polled/captured. */
    public static Iterable<Integer> allKeys() {
        return KEYS.values();
    }

    /** Escape is handled separately (it cancels capture). */
    public static int escape() {
        return InputConstants.KEY_ESCAPE;
    }

    /** Parses "1", "e", "F5", "space", "enter", "esc" ... into a key code, or -1. */
    public static int parseKey(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return -1;
        }
        if (s.equals("ESC") || s.equals("ESCAPE")) {
            return InputConstants.KEY_ESCAPE;
        }
        if (s.equals("RETURN")) {
            s = "ENTER";
        }
        Integer code = KEYS.get(s);
        return code == null ? -1 : code;
    }

    /**
     * Parses a mouse button into a Minecraft mouse-button code, or -1.
     * Numbering follows the usual "Mouse N" names: 1 = left, 2 = right, 3 = middle, 4, 5 ...
     */
    public static int parseMouse(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace("mouse", "").replace("button", "").trim();
        return switch (s) {
            case "left", "l", "1" -> InputConstants.MOUSE_BUTTON_LEFT;
            case "right", "r", "2" -> InputConstants.MOUSE_BUTTON_RIGHT;
            case "middle", "m", "3" -> InputConstants.MOUSE_BUTTON_MIDDLE;
            case "4" -> InputConstants.MOUSE_BUTTON_4;
            case "5" -> InputConstants.MOUSE_BUTTON_5;
            case "6" -> InputConstants.MOUSE_BUTTON_6;
            case "7" -> InputConstants.MOUSE_BUTTON_7;
            case "8" -> InputConstants.MOUSE_BUTTON_8;
            default -> -1;
        };
    }

    public static String mouseName(int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            return "Left Click";
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            return "Right Click";
        }
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE) {
            return "Middle Click";
        }
        if (button == InputConstants.MOUSE_BUTTON_4) {
            return "Mouse 4";
        }
        if (button == InputConstants.MOUSE_BUTTON_5) {
            return "Mouse 5";
        }
        if (button == InputConstants.MOUSE_BUTTON_6) {
            return "Mouse 6";
        }
        if (button == InputConstants.MOUSE_BUTTON_7) {
            return "Mouse 7";
        }
        if (button == InputConstants.MOUSE_BUTTON_8) {
            return "Mouse 8";
        }
        return "Mouse #" + button;
    }

    public static String keyName(int code) {
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
        } catch (Throwable t) {
            return "Key " + code;
        }
    }
}
