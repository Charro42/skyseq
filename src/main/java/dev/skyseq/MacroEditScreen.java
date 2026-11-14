package dev.skyseq;

import java.util.Collections;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Editor for one keybind: binding, mode, and its list of steps. */
public final class MacroEditScreen extends Screen {

    private final Screen parent;
    private final Macro macro;
    private int page = 0;
    private boolean capturing = false;

    public MacroEditScreen(Screen parent, Macro macro) {
        super(Component.literal("Edit keybind"));
        this.parent = parent;
        this.macro = macro;
    }

    @Override
    protected void init() {
        int cw = Math.min(360, this.width - 10);
        int left = (this.width - cw) / 2;
        int half = cw / 2;

        this.addRenderableWidget(new StringWidget(left, 8, cw, 9, this.title, this.font));

        // Row 1: name + bind button
        EditBox name = new EditBox(this.font, left, 22, half - 2, 20, Component.literal("Name"));
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
        }).bounds(left + half + 2, 22, half - 2, 20).build());

        // Row 2 headings
        int modeW = 100;
        int cdW = 70;
        int enW = 50;
        int midW = cw - modeW - cdW - enW - 12;
        this.addRenderableWidget(new StringWidget(left, 47, modeW, 9, Component.literal("Mode"), this.font));
        String midTitle = switch (macro.mode) {
            case CYCLE -> "Cycle values ({n}) e.g. 1,2,3";
            case REPEAT -> "Repeat times";
            default -> "";
        };
        this.addRenderableWidget(new StringWidget(left + modeW + 4, 47, midW, 9, Component.literal(midTitle), this.font));
        this.addRenderableWidget(new StringWidget(left + modeW + midW + 8, 47, cdW, 9, Component.literal("Cooldown ms"), this.font));

        // Row 2 controls
        this.addRenderableWidget(Button.builder(Component.literal(macro.mode.label), b -> {
            macro.mode = macro.mode.next();
            this.rebuildWidgets();
        }).bounds(left, 57, modeW, 20).build());

        if (macro.mode == Macro.Mode.CYCLE) {
            EditBox values = new EditBox(this.font, left + modeW + 4, 57, midW, 20, Component.literal("Values"));
            values.setMaxLength(120);
            values.setValue(macro.cycleValues == null ? "" : macro.cycleValues);
            values.setResponder(v -> {
                macro.cycleValues = v;
                macro.cycleIndex = 0;
            });
            this.addRenderableWidget(values);
        } else if (macro.mode == Macro.Mode.REPEAT) {
            EditBox times = new EditBox(this.font, left + modeW + 4, 57, midW, 20, Component.literal("Repeat"));
            times.setMaxLength(2);
            times.setValue(String.valueOf(macro.repeatCount));
            times.setResponder(v -> macro.repeatCount = parse(v, 1, 50));
            this.addRenderableWidget(times);
        }

        EditBox cooldown = new EditBox(this.font, left + modeW + midW + 8, 57, cdW, 20, Component.literal("Cooldown"));
        cooldown.setMaxLength(5);
        cooldown.setValue(String.valueOf(macro.cooldownMs));
        cooldown.setResponder(v -> macro.cooldownMs = parse(v, 0, 60000));
        this.addRenderableWidget(cooldown);

        this.addRenderableWidget(Button.builder(
                Component.literal(macro.enabled ? "ON" : "OFF").withStyle(macro.enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                b -> {
                    macro.enabled = !macro.enabled;
                    this.rebuildWidgets();
                }).bounds(left + cw - enW, 57, enW, 20).build());

        // Area filter
        this.addRenderableWidget(new StringWidget(left, 82, cw, 9,
                Component.literal("Only in areas, e.g. garden, dungeon (blank = everywhere).  Now: " + Area.describe()), this.font));
        EditBox areas = new EditBox(this.font, left, 92, cw, 20, Component.literal("Areas"));
        areas.setMaxLength(120);
        areas.setHint(Component.literal("everywhere"));
        areas.setValue(macro.areas == null ? "" : macro.areas);
        areas.setResponder(v -> macro.areas = v);
        this.addRenderableWidget(areas);

        // Step list
        int typeW = 90;
        int delayW = 46;
        int small = 18;
        int gap = 3;
        int textW = cw - typeW - delayW - 3 * small - 4 * gap;
        int headY = 118;
        this.addRenderableWidget(new StringWidget(left, headY, typeW, 9, Component.literal("Action"), this.font));
        this.addRenderableWidget(new StringWidget(left + typeW + gap, headY, textW, 9,
                Component.literal("Command / key / button / menu title"), this.font));
        this.addRenderableWidget(new StringWidget(left + typeW + textW + 2 * gap, headY, delayW, 9,
                Component.literal("Delay ms"), this.font));

        int rowsTop = 130;
        int rowH = 22;
        int perPage = Math.max(1, (this.height - 56 - rowsTop) / rowH);
        int count = macro.steps.size();
        int pages = Math.max(1, (count + perPage - 1) / perPage);
        page = Mth.clamp(page, 0, pages - 1);

        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= count) {
                break;
            }
            Step s = macro.steps.get(idx);
            int y = rowsTop + i * rowH;
            int x = left;

            this.addRenderableWidget(Button.builder(Component.literal(s.type.label), b -> {
                s.type = s.type.next();
                this.rebuildWidgets();
            }).bounds(x, y, typeW, 20).build());
            x += typeW + gap;

            EditBox text = new EditBox(this.font, x, y, textW, 20, Component.literal("Text"));
            text.setMaxLength(200);
            boolean usesText = s.type == Step.Type.COMMAND || s.type == Step.Type.KEY
                    || s.type == Step.Type.CLICK || s.type == Step.Type.WAIT_OPEN;
            text.setHint(Component.literal(hint(s.type)));
            if (usesText) {
                text.setValue(s.text == null ? "" : s.text);
                text.setResponder(v -> s.text = v);
            } else {
                text.setEditable(false);
            }
            this.addRenderableWidget(text);
            x += textW + gap;

            EditBox delay = new EditBox(this.font, x, y, delayW, 20, Component.literal("Delay"));
            delay.setMaxLength(5);
            delay.setValue(String.valueOf(s.delayMs));
            delay.setResponder(v -> s.delayMs = parse(v, 0, 60000));
            this.addRenderableWidget(delay);
            x += delayW + gap;

            this.addRenderableWidget(Button.builder(Component.literal("^"), b -> {
                Collections.swap(macro.steps, idx, idx - 1);
                this.rebuildWidgets();
            }).bounds(x, y, small, 20).build()).active = idx > 0;
            x += small + gap;
            this.addRenderableWidget(Button.builder(Component.literal("v"), b -> {
                Collections.swap(macro.steps, idx, idx + 1);
                this.rebuildWidgets();
            }).bounds(x, y, small, 20).build()).active = idx < count - 1;
            x += small + gap;
            this.addRenderableWidget(Button.builder(Component.literal("x").withStyle(ChatFormatting.RED), b -> {
                macro.steps.remove(idx);
                this.rebuildWidgets();
            }).bounds(x, y, small, 20).build());
        }

        // Pager
        int pagerY = this.height - 50;
        this.addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            page--;
            this.rebuildWidgets();
        }).bounds(left, pagerY, 60, 20).build()).active = page > 0;
        this.addRenderableWidget(new StringWidget(left + 60, pagerY + 6, cw - 120, 9,
                Component.literal("Steps - page " + (page + 1) + " / " + pages + "  (" + count + " steps)"), this.font));
        this.addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            page++;
            this.rebuildWidgets();
        }).bounds(left + cw - 60, pagerY, 60, 20).build()).active = page < pages - 1;

        // Bottom row
        int bottomY = this.height - 26;
        this.addRenderableWidget(Button.builder(Component.literal("+ Add step"), b -> {
            macro.steps.add(new Step());
            page = Integer.MAX_VALUE;
            this.rebuildWidgets();
        }).bounds(left, bottomY, half - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(left + half + 2, bottomY, half - 2, 20).build());
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
        return !capturing;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }

    @Override
    public void removed() {
        Capture.cancel();
        capturing = false;
        Config.save();
    }
}
