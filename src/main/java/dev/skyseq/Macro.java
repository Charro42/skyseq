package dev.skyseq;

import java.util.ArrayList;
import java.util.List;

/** A user-defined keybind and the sequence of steps it runs. */
public final class Macro {

    public enum Mode {
        ONCE("Run once"),
        CYCLE("Cycle"),
        REPEAT("Repeat"),
        HOLD("Hold");

        public final String label;

        Mode(String label) {
            this.label = label;
        }

        public Mode next() {
            Mode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    public String name = "New keybind";
    public boolean enabled = true;
    /** true = mouse button, false = keyboard key. */
    public boolean mouse = false;
    /** Minecraft key or mouse-button code (InputConstants). -1 = unbound. */
    public int code = -1;
    public Mode mode = Mode.ONCE;
    /** CYCLE mode: comma-separated values. "{n}" in steps becomes the current value. */
    public String cycleValues = "1,2,3";
    /** REPEAT mode: how many times the whole sequence runs. */
    public int repeatCount = 2;
    /** Ignore new triggers for this long after the sequence finishes. */
    public int cooldownMs = 400;
    /** CYCLE mode: which value is used next. Saved so it survives restarts. */
    public int cycleIndex = 0;
    /** HOLD mode: what to hold, e.g. "w", "shift", "left click", "mouse4". */
    public String holdTarget = "";
    /** HOLD mode: stop holding after this many seconds no matter what. */
    public int holdSeconds = 60;
    /** HOLD mode: keys you can still press without stopping the hold. */
    public String holdAllow = "w,a,s,d";
    /** Only run in these SkyBlock areas (comma-separated). Blank = everywhere. */
    public String areas = "";
    public List<Step> steps = new ArrayList<>();

    // Runtime-only state (not saved).
    public transient boolean wasDown;
    public transient long cooldownUntil;

    public List<String> values() {
        List<String> out = new ArrayList<>();
        if (cycleValues != null) {
            for (String part : cycleValues.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    public String bindingName() {
        if (code < 0) {
            return "Unbound";
        }
        return mouse ? Keys.mouseName(code) : Keys.keyName(code);
    }
}
