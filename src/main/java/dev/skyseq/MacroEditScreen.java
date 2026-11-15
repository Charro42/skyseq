package dev.skyseq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Editor for one keybind.
 *
 * In Cycle / Repeat mode there is a row of tabs (All, n=1, n=2 ...). "All" shows only the shared steps (they run for
 * every number). A number tab shows the whole run for that number: shared steps (greyed, edit them in All) plus the
 * steps that are only for that number (yellow, editable). Steps added in a number tab are tagged for that number, and
 * the "When n" box lets one step cover several numbers, e.g. "1,3".
 */
public final class MacroEditScreen extends Screen {

    private static final int MAX_TABS = 8;

    private final Screen parent;
    private final Macro macro;
    private int page = 0;
    private boolean capturing = false;
    private boolean capturingHold = false;
    /** null = "All" tab, otherwise the value being viewed. */
    private String view = null;

    public MacroEditScreen(Screen parent, Macro macro) {
        super(Component.literal("Edit keybind"));
        this.parent = parent;
        this.macro = macro;
    }

    private List<String> tabValues() {
        List<String> out = new ArrayList<>();
        if (macro.mode == Macro.Mode.CYCLE) {
            for (String v : macro.values()) {
                if (out.size() < MAX_TABS && !out.contains(v)) {
                    out.add(v);
                }
            }
        } else if (macro.mode == Macro.Mode.REPEAT) {
            for (int i = 1; i <= Math.min(macro.repeatCount, MAX_TABS); i++) {
                out.add(String.valueOf(i));
            }
        }
        return out;
    }

    private static boolean isShared(Step s) {
        return s.when == null || s.when.isBlank();
    }

    private static Component selected(String text) {
        return Component.literal(text).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
    }

    @Override
    protected void init() {
        int cw = Math.min(420, this.width - 10);
        int left = (this.width - cw) / 2;
        int half = cw / 2;

        List<String> tabs = tabValues();
        boolean hasTabs = !tabs.isEmpty();
        if (view != null && !tabs.contains(view)) {
            view = null;
        }

        this.addRenderableWidget(new StringWidget(left, 7, cw, 9, this.title, this.font));

        // Row A: name + trigger
        EditBox name = new EditBox(this.font, left, 19, half - 2, 20, Component.literal("Name"));
        name.setMaxLength(40);
        name.setHint(Component.literal("Keybind name"));
        name.setValue(macro.name == null ? "" : macro.name);
        name.setResponder(v -> macro.name = v);
        this.addRenderableWidget(name);

        Component bindLabel = capturing
                ? Component.literal("> Press a key or button <").withStyle(ChatFormatting.YELLOW)
                : Component.literal("Trigger: " + macro.bindingName());
        this.addRenderableWidget(Button.builder(bindLabel, b -> {
            if (capturing) {
                Capture.cancel();
                capturing = false;
            } else {
                capturing = true;
                capturingHold = false;
                Capture.start(res -> {
                    capturing = false;
                    if (res != null) {
                        macro.mouse = res[0] == 1;
                        macro.code = res[1];
                    }
                    this.rebuildWidgets();
                });
            }
            this.rebuildWidgets();
        }).bounds(left + half + 2, 19, half - 2, 20).build());

        // Row B: mode buttons, cooldown, enabled
        this.addRenderableWidget(new StringWidget(left, 43, 120, 9, Component.literal("What happens on each press"), this.font));
        this.addRenderableWidget(new StringWidget(left + 244, 43, 80, 9, Component.literal("Cooldown ms"), this.font));
        int mw = 58;
        Macro.Mode[] modes = Macro.Mode.values();
        for (int i = 0; i < modes.length; i++) {
            Macro.Mode mode = modes[i];
            boolean isSelected = macro.mode == mode;
            Button button = Button.builder(isSelected ? selected(mode.label) : Component.literal(mode.label), b -> {
                macro.mode = mode;
                view = null;
                page = 0;
                this.rebuildWidgets();
            }).bounds(left + i * (mw + 2), 53, mw, 20).build();
            button.active = !isSelected;
            this.addRenderableWidget(button);
        }
        EditBox cooldown = new EditBox(this.font, left + 244, 53, 60, 20, Component.literal("Cooldown"));
        cooldown.setMaxLength(5);
        cooldown.setValue(String.valueOf(macro.cooldownMs));
        cooldown.setResponder(v -> macro.cooldownMs = parse(v, 0, 60000));
        this.addRenderableWidget(cooldown);
        this.addRenderableWidget(Button.builder(
                Component.literal("Enabled: " + (macro.enabled ? "ON" : "OFF"))
                        .withStyle(macro.enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                b -> {
                    macro.enabled = !macro.enabled;
                    this.rebuildWidgets();
                }).bounds(left + cw - 84, 53, 84, 20).build());

        // Row C: cycle values / repeat count, and areas
        boolean isHold = macro.mode == Macro.Mode.HOLD;
        boolean needsValueBox = macro.mode != Macro.Mode.ONCE && !isHold;
        int areaX = needsValueBox ? left + half + 2 : left;
        int areaW = needsValueBox ? half - 2 : cw;
        if (needsValueBox) {
            String title = macro.mode == Macro.Mode.CYCLE ? "Cycle values - n takes these in turn" : "Repeat times";
            this.addRenderableWidget(new StringWidget(left, 77, half, 9, Component.literal(title), this.font));
            EditBox values = new EditBox(this.font, left, 87, half - 2 - 46, 20, Component.literal("Values"));
            values.setMaxLength(120);
            if (macro.mode == Macro.Mode.CYCLE) {
                values.setValue(macro.cycleValues == null ? "" : macro.cycleValues);
                values.setHint(Component.literal("1,2,3"));
                values.setResponder(v -> {
                    macro.cycleValues = v;
                    macro.cycleIndex = 0;
                });
            } else {
                values.setValue(String.valueOf(macro.repeatCount));
                values.setResponder(v -> macro.repeatCount = parse(v, 1, 50));
            }
            this.addRenderableWidget(values);
            this.addRenderableWidget(Button.builder(Component.literal("Apply"), b -> this.rebuildWidgets())
                    .bounds(left + half - 2 - 42, 87, 42, 20).build());
        }
        if (isHold) {
            areaX = left + half + 2;
            areaW = half - 2;
            this.addRenderableWidget(new StringWidget(left, 77, half, 9, Component.literal("Click to pick what to hold  /  stop after (s)"), this.font));
            Component holdLabel = capturingHold
                    ? Component.literal("> Press a key or button <").withStyle(ChatFormatting.YELLOW)
                    : Component.literal(Keys.holdName(macro.holdTarget));
            this.addRenderableWidget(Button.builder(holdLabel, b -> {
                if (capturingHold) {
                    Capture.cancel();
                    capturingHold = false;
                } else {
                    capturingHold = true;
                    capturing = false;
                    Capture.start(res -> {
                        capturingHold = false;
                        if (res != null) {
                            macro.holdTarget = (res[0] == 1 ? "mouse:" : "key:") + res[1];
                        }
                        this.rebuildWidgets();
                    }, true);
                }
                this.rebuildWidgets();
            }).bounds(left, 87, half - 2 - 46, 20).build());
            EditBox secs = new EditBox(this.font, left + half - 2 - 42, 87, 42, 20, Component.literal("Seconds"));
            secs.setMaxLength(3);
            secs.setValue(String.valueOf(macro.holdSeconds));
            secs.setResponder(v -> macro.holdSeconds = parse(v, 1, 600));
            this.addRenderableWidget(secs);
        }
        this.addRenderableWidget(new StringWidget(areaX, 77, areaW, 9,
                Component.literal("Only in areas  (now: " + Area.describe() + ")"), this.font));
        EditBox areas = new EditBox(this.font, areaX, 87, areaW, 20, Component.literal("Areas"));
        areas.setMaxLength(120);
        areas.setHint(Component.literal("everywhere"));
        areas.setValue(macro.areas == null ? "" : macro.areas);
        areas.setResponder(v -> macro.areas = v);
        this.addRenderableWidget(areas);

        if (isHold) {
            this.addRenderableWidget(new StringWidget(left, 113, cw, 9,
                    Component.literal("Keys that do NOT stop the hold"), this.font));
            EditBox allow = new EditBox(this.font, left, 123, cw, 20, Component.literal("Allowed keys"));
            allow.setMaxLength(120);
            allow.setHint(Component.literal("w,a,s,d"));
            allow.setValue(macro.holdAllow == null ? "" : macro.holdAllow);
            allow.setResponder(v -> macro.holdAllow = v);
            this.addRenderableWidget(allow);
            String[] info = {
                "Press the trigger once to start holding, press it again to let go.",
                "The hold also stops on any other key, any mouse click, a menu opening,",
                "or when the time limit is reached.",
                "Tip: add jump (space) or sneak to the allowed list if you want those to keep it."
            };
            for (int i = 0; i < info.length; i++) {
                this.addRenderableWidget(new StringWidget(left, 152 + i * 12, cw, 9, Component.literal(info[i]), this.font));
            }
            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                    .bounds(left, this.height - 26, cw, 20).build());
            return;
        }

        // Tabs
        int y = 113;
        if (hasTabs) {
            this.addRenderableWidget(new StringWidget(left, y + 6, 70, 9, Component.literal("Show steps for:"), this.font));
            int tabX = left + 80;
            int tabW = Math.min(40, (cw - 80 - 4 * 2) / (tabs.size() + 1) - 2);
            addTab(tabX, y, tabW, "All", null);
            for (int i = 0; i < tabs.size(); i++) {
                addTab(tabX + (i + 1) * (tabW + 2), y, tabW, "n=" + tabs.get(i), tabs.get(i));
            }
            y += 24;
        }

        // Step list header
        int typeW = 84;
        boolean showWhen = view != null;
        int whenW = showWhen ? 44 : 0;
        int delayW = 46;
        int small = 18;
        int gap = 3;
        int gaps = showWhen ? 6 : 5;
        int textW = cw - typeW - whenW - delayW - 3 * small - gaps * gap;
        int cx = left;
        this.addRenderableWidget(new StringWidget(cx, y, typeW, 9, Component.literal("Action"), this.font));
        cx += typeW + gap;
        this.addRenderableWidget(new StringWidget(cx, y, textW, 9, Component.literal("Command / key / button / menu title"), this.font));
        cx += textW + gap;
        if (showWhen) {
            this.addRenderableWidget(new StringWidget(cx, y, whenW, 9, Component.literal("When n"), this.font));
            cx += whenW + gap;
        }
        this.addRenderableWidget(new StringWidget(cx, y, delayW, 9, Component.literal("Delay ms"), this.font));

        int rowsTop = y + 12;
        int rowH = 22;
        int perPage = Math.max(1, (this.height - 56 - rowsTop) / rowH);

        List<Integer> visible = new ArrayList<>();
        for (int i = 0; i < macro.steps.size(); i++) {
            Step candidate = macro.steps.get(i);
            if (view == null ? (!hasTabs || isShared(candidate)) : candidate.appliesTo(view)) {
                visible.add(i);
            }
        }
        int count = visible.size();
        int pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Mth.clamp(page, 0, pages - 1);

        for (int row = 0; row < perPage; row++) {
            int pos = page * perPage + row;
            if (pos >= count) {
                break;
            }
            int idx = visible.get(pos);
            Step s = macro.steps.get(idx);
            boolean readOnly = view != null && isShared(s);
            int ry = rowsTop + row * rowH;
            int x = left;

            Component typeLabel = (view != null && !readOnly)
                    ? Component.literal(s.type.label).withStyle(ChatFormatting.YELLOW)
                    : Component.literal(s.type.label);
            Button typeButton = Button.builder(typeLabel, b ->
                    this.minecraft.setScreenAndShow(new StepTypeScreen(this, type -> {
                        s.type = type;
                        this.rebuildWidgets();
                    }))).bounds(x, ry, typeW, 20).build();
            typeButton.active = !readOnly;
            this.addRenderableWidget(typeButton);
            x += typeW + gap;

            EditBox text = new EditBox(this.font, x, ry, textW, 20, Component.literal("Text"));
            text.setMaxLength(200);
            boolean usesText = s.type == Step.Type.COMMAND || s.type == Step.Type.KEY || s.type == Step.Type.RUN
                    || s.type == Step.Type.CLICK || s.type == Step.Type.WAIT_OPEN;
            text.setHint(Component.literal(hint(s.type)));
            if (usesText) {
                text.setValue(s.text == null ? "" : s.text);
                text.setResponder(v -> s.text = v);
            }
            if (!usesText || readOnly) {
                text.setEditable(false);
            }
            this.addRenderableWidget(text);
            x += textW + gap;

            if (showWhen) {
                EditBox when = new EditBox(this.font, x, ry, whenW, 20, Component.literal("When"));
                when.setMaxLength(24);
                when.setHint(Component.literal("all"));
                when.setValue(s.when == null ? "" : s.when);
                when.setResponder(v -> s.when = v);
                when.setEditable(!readOnly);
                this.addRenderableWidget(when);
                x += whenW + gap;
            }

            EditBox delay = new EditBox(this.font, x, ry, delayW, 20, Component.literal("Delay"));
            delay.setMaxLength(5);
            delay.setValue(String.valueOf(s.delayMs));
            delay.setResponder(v -> s.delayMs = parse(v, 0, 60000));
            delay.setEditable(!readOnly);
            this.addRenderableWidget(delay);
            x += delayW + gap;

            this.addRenderableWidget(Button.builder(Component.literal("^"), b -> {
                Collections.swap(macro.steps, idx, visible.get(pos - 1));
                this.rebuildWidgets();
            }).bounds(x, ry, small, 20).build()).active = pos > 0;
            x += small + gap;
            this.addRenderableWidget(Button.builder(Component.literal("v"), b -> {
                Collections.swap(macro.steps, idx, visible.get(pos + 1));
                this.rebuildWidgets();
            }).bounds(x, ry, small, 20).build()).active = pos < count - 1;
            x += small + gap;
            this.addRenderableWidget(Button.builder(Component.literal("x").withStyle(ChatFormatting.RED), b -> {
                macro.steps.remove(idx);
                this.rebuildWidgets();
            }).bounds(x, ry, small, 20).build()).active = !readOnly;
        }

        // Pager
        int pagerY = this.height - 50;
        this.addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            page--;
            this.rebuildWidgets();
        }).bounds(left, pagerY, 60, 20).build()).active = page > 0;
        String info = view == null
                ? "Page " + (page + 1) + " / " + pages + "  (" + count + (hasTabs ? " shared steps)" : " steps)")
                : "Page " + (page + 1) + " / " + pages + "  -  grey = shared (edit in All), yellow = n=" + view + " only";
        this.addRenderableWidget(new StringWidget(left + 66, pagerY + 6, cw - 132, 9, Component.literal(info), this.font));
        this.addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            page++;
            this.rebuildWidgets();
        }).bounds(left + cw - 60, pagerY, 60, 20).build()).active = page < pages - 1;

        // Bottom row
        int bottomY = this.height - 26;
        this.addRenderableWidget(Button.builder(Component.literal("+ Add step"), b ->
                this.minecraft.setScreenAndShow(new StepTypeScreen(this, type -> {
                    Step step = new Step(type, "", 150);
                    if (view != null) {
                        step.when = view;
                    }
                    macro.steps.add(step);
                    page = Integer.MAX_VALUE;
                    this.rebuildWidgets();
                }))).bounds(left, bottomY, half - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(left + half + 2, bottomY, half - 2, 20).build());
    }

    private void addTab(int x, int y, int w, String label, String value) {
        boolean isSelected = (view == null && value == null) || (view != null && view.equals(value));
        Button button = Button.builder(isSelected ? selected(label) : Component.literal(label), b -> {
            view = value;
            page = 0;
            this.rebuildWidgets();
        }).bounds(x, y, w, 20).build();
        button.active = !isSelected;
        this.addRenderableWidget(button);
    }

    private static String hint(Step.Type type) {
        return switch (type) {
            case COMMAND -> "/command or chat text";
            case KEY -> "1, E, F5, space, {n}";
            case CLICK -> "left, right, middle, 4, 5";
            case WAIT_OPEN -> "menu title (optional)";
            case ESCAPE -> "closes the open menu";
            case WAIT -> "just waits (see delay)";
            case WAIT_CLOSE -> "waits until menu closes";
            case RUN -> "name of the keybind to run next";
        };
    }

    private static int parse(String v, int min, int max) {
        try {
            return Mth.clamp(Integer.parseInt(v.trim()), min, max);
        } catch (NumberFormatException e) {
            return min;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // While waiting for a key press, Escape cancels the capture instead of closing the screen.
        return !capturing && !capturingHold;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }

    @Override
    public void removed() {
        Capture.cancel();
        capturing = false;
        capturingHold = false;
        Config.save();
    }
}
