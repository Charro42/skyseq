package dev.skyseq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.platform.InputConstants;

/** Loads and saves everything to config/skyseq.json. */
public final class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skyseq.json");

    /** Master switch. */
    public static boolean enabled = true;
    /** Strict mode: every keybind only runs its first action per keypress. */
    public static boolean strictMode = false;
    /** Show short status messages above the hotbar. */
    public static boolean showStatus = true;
    /** How long a "Wait open/close" step waits before giving up. */
    public static int waitTimeoutMs = 4000;
    public static List<Macro> macros = new ArrayList<>();

    private static final class Data {
        boolean enabled = true;
        boolean strictMode = false;
        boolean showStatus = true;
        int waitTimeoutMs = 4000;
        List<Macro> macros = new ArrayList<>();
    }

    private Config() {
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            macros = defaults();
            save();
            return;
        }
        try {
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null) {
                throw new IllegalStateException("empty config");
            }
            enabled = d.enabled;
            strictMode = d.strictMode;
            showStatus = d.showStatus;
            waitTimeoutMs = d.waitTimeoutMs <= 0 ? 4000 : d.waitTimeoutMs;
            macros = d.macros != null ? d.macros : new ArrayList<>();
            for (Macro m : macros) {
                if (m.steps == null) {
                    m.steps = new ArrayList<>();
                }
                if (m.mode == null) {
                    m.mode = Macro.Mode.ONCE;
                }
                if (m.mode == Macro.Mode.HOLD && (m.holdSeconds == 60 || m.holdSeconds == 150)) {
                    m.holdSeconds = 300; // old defaults; the new default is 5 minutes
                }
                for (Step s : m.steps) {
                    if (s.type == null) {
                        s.type = Step.Type.WAIT;
                    }
                    if (s.text == null) {
                        s.text = "";
                    }
                }
            }
        } catch (Exception e) {
            // Keep the broken file around so nothing is lost, then start fresh.
            try {
                Files.move(FILE, FILE.resolveSibling("skyseq.json.broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // nothing more we can do
            }
            macros = defaults();
            save();
        }
    }

    public static synchronized void save() {
        Data d = new Data();
        d.enabled = enabled;
        d.strictMode = strictMode;
        d.showStatus = showStatus;
        d.waitTimeoutMs = waitTimeoutMs;
        d.macros = macros;
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("skyseq.json.tmp");
            Files.writeString(tmp, GSON.toJson(d));
            try {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[SkySeq] Could not save config: " + e);
        }
    }

    /** The loadout-cycling example from the spec, bound to Mouse 5 but switched off until you enable it. */
    private static List<Macro> defaults() {
        Macro m = new Macro();
        m.name = "Loadout cycle";
        m.enabled = false;
        m.mouse = true;
        m.code = InputConstants.MOUSE_BUTTON_5;
        m.mode = Macro.Mode.CYCLE;
        m.cycleValues = "1,2,3";
        m.steps.add(new Step(Step.Type.COMMAND, "/loadout", 100));
        m.steps.add(new Step(Step.Type.WAIT_OPEN, "", 150));
        m.steps.add(new Step(Step.Type.KEY, "{n}", 150));
        m.steps.add(new Step(Step.Type.ESCAPE, "", 0));
        List<Macro> list = new ArrayList<>();
        list.add(m);
        return list;
    }
}
