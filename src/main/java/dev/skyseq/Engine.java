package dev.skyseq;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

    private Engine() {
    }

    public static void tick(Minecraft mc) {
        if (!pendingRelease.isEmpty()) {
            for (InputConstants.Key k : pendingRelease) {
                KeyMapping.set(k, false);
            }
            pendingRelease.clear();
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
        for (Macro m : Config.macros) {
            boolean down = m.enabled && m.code >= 0 && isDown(m);
            boolean pressed = down && !m.wasDown;
            m.wasDown = down;
            if (pressed && allow && now >= m.cooldownUntil && !m.steps.isEmpty()) {
                active = new Runner(m);
                allow = false;
            }
        }
    }

    private static boolean isDown(Macro m) {
        if (m.mouse) {
            return MouseState.isDown(m.code);
        }
        return InputConstants.isKeyDown(m.code);
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
            List<Step> copy = new ArrayList<>(m.steps);
            if (Config.strictMode) {
                // Strict mode: one action per keypress.
                List<Step> first = new ArrayList<>();
                for (Step s : copy) {
                    if (s.type != Step.Type.WAIT && s.type != Step.Type.WAIT_OPEN && s.type != Step.Type.WAIT_CLOSE) {
                        first.add(s);
                        break;
                    }
                }
                copy = first;
            }
            this.steps = copy;

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
        if (code < 0) {
            return;
        }
        if (mc.gui.screen() != null) {
            KeyEvent event = new KeyEvent(code, code, 0);
            mc.gui.screen().keyPressed(event);
            mc.gui.screen().keyReleased(event);
        } else {
            InputConstants.Key key = InputConstants.Type.KEYBOARD.getOrCreate(code);
            KeyMapping.set(key, true);
            KeyMapping.click(key);
            pendingRelease.add(key);
        }
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
