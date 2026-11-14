package dev.skyseq;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import dev.skyseq.mixin.KeyboardHandlerAccessor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * Watches for keybind presses and runs their sequences, one step per client tick (50 ms).
 *
 * Safeguards: only one sequence runs at a time, new triggers are ignored while one is running
 * or cooling down, triggers are ignored while any screen is open, and every wait has a timeout.
 */
public final class Engine {

    private enum Wait { NONE, OPEN, CLOSE }

    private static Runner active;
    private static final List<InputConstants.Key> pendingRelease = new ArrayList<>();
    private static final List<KeyEvent> pendingKeyReleases = new ArrayList<>();

    private Engine() {
    }

    public static void tick(Minecraft mc) {
        if (!pendingRelease.isEmpty()) {
            for (InputConstants.Key k : pendingRelease) {
                KeyMapping.set(k, false);
            }
            pendingRelease.clear();
        }
        if (!pendingKeyReleases.isEmpty()) {
            if (mc.getWindow() != null) {
                KeyboardHandlerAccessor keyboard = (KeyboardHandlerAccessor) mc.keyboardHandler;
                for (KeyEvent event : pendingKeyReleases) {
                    keyboard.skyseq$keyPress(mc.getWindow().handle(), InputConstants.RELEASE, event);
                }
            }
            pendingKeyReleases.clear();
        }
        // The bind button in the config screen also works from the title screen.
        if (Capture.active()) {
            Capture.poll();
            return;
        }
        if (mc.player == null) {
            active = null;
            return;
        }

        if (active != null) {
            active.tick(mc);
        }

        boolean allow = Config.enabled && mc.gui.screen() == null && active == null;
        long now = System.nanoTime();
        Macro pick = null;
        for (Macro m : Config.macros) {
            boolean down = m.enabled && m.code >= 0 && isDown(m);
            boolean pressed = down && !m.wasDown;
            m.wasDown = down;
            if (!pressed || !allow || now < m.cooldownUntil || m.steps.isEmpty()) {
                continue;
            }
            boolean restricted = m.areas != null && !m.areas.isBlank();
            if (restricted && !Area.matches(m.areas)) {
                continue;
            }
            // Area-restricted keybinds beat unrestricted ones on the same button; otherwise the one higher in the list wins.
            if (pick == null || (restricted && (pick.areas == null || pick.areas.isBlank()))) {
                pick = m;
            }
        }
        if (pick != null) {
            active = new Runner(pick);
        }
    }

    private static boolean isDown(Macro m) {
        if (m.mouse) {
            return MouseState.isDown(m.code);
        }
        return Keys.isDown(m.code);
    }

    private static long ms(int value) {
        return Math.max(0, Math.min(60_000, value)) * 1_000_000L;
    }

    private static final class Runner {
        final Macro macro;
        final List<Step> steps;
        int totalIterations = 1;
        int iteration = 0;
        int index = 0;
        String value = "";
        long notBefore = 0;
        Wait wait = Wait.NONE;
        Step waitStep;
        long deadline;

        Runner(Macro m) {
            this.macro = m;
            this.steps = new ArrayList<>(m.steps);
            switch (m.mode) {
                case CYCLE -> {
                    List<String> values = m.values();
                    value = values.isEmpty() ? "" : values.get(Math.floorMod(m.cycleIndex, values.size()));
                }
                case REPEAT -> {
                    totalIterations = Math.max(1, Math.min(50, m.repeatCount));
                    value = "1";
                }
                default -> {
                }
            }
            if (Config.strictMode) {
                // Strict mode: one action per keypress (the first real action that applies to this value).
                List<Step> first = new ArrayList<>();
                for (Step s : steps) {
                    boolean waitOnly = s.type == Step.Type.WAIT || s.type == Step.Type.WAIT_OPEN || s.type == Step.Type.WAIT_CLOSE;
                    if (!waitOnly && applies(s)) {
                        first.add(s);
                        break;
                    }
                }
                steps.clear();
                steps.addAll(first);
            }
        }

        /** "When n" tags only matter in Cycle and Repeat mode. */
        private boolean applies(Step s) {
            return macro.mode == Macro.Mode.ONCE || s.appliesTo(value);
        }

        void tick(Minecraft mc) {
            if (mc.player == null) {
                active = null;
                return;
            }
            long now = System.nanoTime();
            if (now < notBefore) {
                return;
            }

            if (wait == Wait.OPEN) {
                if (screenMatches(mc, waitStep.text)) {
                    wait = Wait.NONE;
                    notBefore = now + ms(waitStep.delayMs);
                } else if (now > deadline) {
                    abort(mc, "menu did not open");
                }
                return;
            }
            if (wait == Wait.CLOSE) {
                if (mc.gui.screen() == null) {
                    wait = Wait.NONE;
                    notBefore = now + ms(waitStep.delayMs);
                } else if (now > deadline) {
                    abort(mc, "menu did not close");
                }
                return;
            }

            while (index < steps.size() && !applies(steps.get(index))) {
                index++;
            }
            if (index >= steps.size()) {
                nextIterationOrFinish(mc, now);
                return;
            }

            Step s = steps.get(index++);
            switch (s.type) {
                case COMMAND -> sendText(mc, resolve(s.text));
                case KEY -> pressKey(mc, Keys.parseKey(resolve(s.text)));
                case CLICK -> click(mc, Keys.parseMouse(resolve(s.text)));
                case ESCAPE -> {
                    if (mc.gui.screen() != null) {
                        mc.gui.screen().onClose();
                    }
                }
                case WAIT -> {
                }
                case WAIT_OPEN -> {
                    wait = Wait.OPEN;
                    waitStep = s;
                    deadline = now + ms(Config.waitTimeoutMs);
                    return;
                }
                case WAIT_CLOSE -> {
                    wait = Wait.CLOSE;
                    waitStep = s;
                    deadline = now + ms(Config.waitTimeoutMs);
                    return;
                }
            }
            notBefore = now + ms(s.delayMs);
        }

        private void nextIterationOrFinish(Minecraft mc, long now) {
            iteration++;
            if (iteration < totalIterations) {
                index = 0;
                value = String.valueOf(iteration + 1);
                notBefore = now + ms(100);
                return;
            }
            if (macro.mode == Macro.Mode.CYCLE) {
                int size = macro.values().size();
                if (size > 0) {
                    status(mc, macro.name + ": " + value);
                    macro.cycleIndex = (macro.cycleIndex + 1) % size;
                    Config.save();
                }
            }
            macro.cooldownUntil = now + ms(macro.cooldownMs);
            active = null;
        }

        private void abort(Minecraft mc, String why) {
            status(mc, macro.name + " stopped: " + why);
            macro.cooldownUntil = System.nanoTime() + ms(macro.cooldownMs);
            active = null;
        }

        private String resolve(String text) {
            return text == null ? "" : text.replace("{n}", value);
        }
    }

    private static boolean screenMatches(Minecraft mc, String filter) {
        if (mc.gui.screen() == null) {
            return false;
        }
        if (filter == null || filter.isBlank()) {
            return true;
        }
        String title = mc.gui.screen().getTitle().getString().toLowerCase(Locale.ROOT);
        return title.contains(filter.trim().toLowerCase(Locale.ROOT));
    }

    private static void sendText(Minecraft mc, String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            return;
        }
        if (t.startsWith("/")) {
            mc.player.connection.sendCommand(t.substring(1));
        } else {
            mc.player.connection.sendChat(t);
        }
    }

    private static void pressKey(Minecraft mc, int code) {
        if (code < 0 || mc.getWindow() == null) {
            return;
        }
        // Goes through the game's own keyboard handler: works the same in menus and in the world,
        // and is seen by vanilla key mappings and other mods. The release is sent on the next tick.
        KeyEvent event = new KeyEvent(code, 0, 0);
        KeyboardHandlerAccessor keyboard = (KeyboardHandlerAccessor) mc.keyboardHandler;
        keyboard.skyseq$keyPress(mc.getWindow().handle(), InputConstants.PRESS, event);
        pendingKeyReleases.add(event);
    }

    private static void click(Minecraft mc, int button) {
        if (button < 0) {
            return;
        }
        if (mc.gui.screen() != null) {
            double x = mc.mouseHandler.getScaledXPos(mc.getWindow());
            double y = mc.mouseHandler.getScaledYPos(mc.getWindow());
            MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
            mc.gui.screen().mouseClicked(event, false);
            mc.gui.screen().mouseReleased(event);
        } else {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
            KeyMapping.set(key, true);
            KeyMapping.click(key);
            pendingRelease.add(key);
        }
    }

    private static void status(Minecraft mc, String message) {
        if (Config.showStatus && mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal("[SkySeq] " + message));
        }
    }
}
